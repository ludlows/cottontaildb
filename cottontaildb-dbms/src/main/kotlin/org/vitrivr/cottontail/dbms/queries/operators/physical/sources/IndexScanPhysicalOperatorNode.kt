package org.vitrivr.cottontail.dbms.queries.operators.physical.sources

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.queries.binding.Binding
import org.vitrivr.cottontail.core.queries.nodes.traits.*
import org.vitrivr.cottontail.core.queries.planning.cost.Cost
import org.vitrivr.cottontail.core.queries.planning.cost.CostPolicy
import org.vitrivr.cottontail.core.queries.predicates.BooleanPredicate
import org.vitrivr.cottontail.core.queries.predicates.Predicate
import org.vitrivr.cottontail.core.queries.predicates.ProximityPredicate
import org.vitrivr.cottontail.core.values.types.Value
import org.vitrivr.cottontail.dbms.column.ColumnTx
import org.vitrivr.cottontail.dbms.entity.Entity
import org.vitrivr.cottontail.dbms.entity.EntityTx
import org.vitrivr.cottontail.dbms.execution.operators.basics.Operator
import org.vitrivr.cottontail.dbms.execution.operators.sources.IndexScanOperator
import org.vitrivr.cottontail.dbms.index.AbstractIndex
import org.vitrivr.cottontail.dbms.index.Index
import org.vitrivr.cottontail.dbms.index.IndexTx
import org.vitrivr.cottontail.dbms.queries.context.QueryContext
import org.vitrivr.cottontail.dbms.queries.operators.OperatorNode
import org.vitrivr.cottontail.dbms.queries.operators.physical.NullaryPhysicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.operators.physical.merge.MergeLimitingSortPhysicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.operators.physical.merge.MergePhysicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.operators.physical.transform.LimitPhysicalOperatorNode
import org.vitrivr.cottontail.dbms.statistics.columns.ValueStatistics
import org.vitrivr.cottontail.dbms.statistics.selectivity.NaiveSelectivityCalculator

/**
 * A [IndexScanPhysicalOperatorNode] that represents a predicated lookup using an [AbstractIndex].
 *
 * @author Ralph Gasser
 * @version 2.6.0
 */
@Suppress("UNCHECKED_CAST")
class IndexScanPhysicalOperatorNode(override val groupId: Int,
                                    val index: IndexTx,
                                    val predicate: Predicate,
                                    val fetch: List<Pair<Binding.Column, ColumnDef<*>>>,
                                    val partitionIndex: Int = 0,
                                    val partitions: Int = 1) : NullaryPhysicalOperatorNode() {
    companion object {
        private const val NODE_NAME = "ScanIndex"
    }

    /** The name of this [IndexScanPhysicalOperatorNode]. */
    override val name: String
        get() = NODE_NAME

    /** The [ColumnDef]s accessed by this [IndexScanPhysicalOperatorNode] depends on the [ColumnDef]s produced by the [Index]. */
    override val physicalColumns: List<ColumnDef<*>>

    /** The [ColumnDef]s produced by this [IndexScanPhysicalOperatorNode] depends on the [ColumnDef]s produced by the [Index]. */
    override val columns: List<ColumnDef<*>>

    /** [IndexScanPhysicalOperatorNode] are always executable. */
    override val executable: Boolean = true

    /** [ValueStatistics] are taken from the underlying [Entity]. The query planner uses statistics for [Cost] estimation. */
    override val statistics = Object2ObjectLinkedOpenHashMap<ColumnDef<*>, ValueStatistics<*>>()

    /** Cost estimation for [IndexScanPhysicalOperatorNode]s is delegated to the [Index]. */
    override val cost: Cost = this.index.costFor(this.predicate)

    /** Returns [Map] of [Trait]s for this [IndexScanOperator], which is derived directly from the [Index]*/
    override val traits: Map<TraitType<*>, Trait>

    /** The estimated output size of this [IndexScanPhysicalOperatorNode]. */
    override val outputSize: Long = when (this.predicate) {
        is ProximityPredicate -> this.predicate.k
        is BooleanPredicate -> {
            val selectivity = NaiveSelectivityCalculator.estimate(this.predicate, this.statistics)
            val entityTx = this.index.context.getTx(this.index.dbo.parent) as EntityTx
            selectivity(entityTx.count())
        }
    }

    init {
        val entityTx = this.index.context.getTx(this.index.dbo.parent) as EntityTx
        val columns = mutableListOf<ColumnDef<*>>()
        val physicalColumns = mutableListOf<ColumnDef<*>>()
        val indexProduces = this.index.columnsFor(this.predicate)
        val entityProduces = entityTx.listColumns()
        for ((binding, physical) in this.fetch) {
            require(indexProduces.contains(physical)) { "The given column $physical is not produced by the selected index ${this.index.dbo}. This is a programmer's error!"}

            /* Populate list of columns. */
            columns.add(binding.column)
            physicalColumns.add(physical)

            /* Populate statistics. */
            if (!this.statistics.containsKey(binding.column) && entityProduces.contains(physical)) {
                this.statistics[binding.column] = (this.index.context.getTx(entityTx.columnForName(physical.name)) as ColumnTx<*>).statistics() as ValueStatistics<Value>
            }
        }

        val indexTraits = this.index.traitsFor(this.predicate)
        val traits = mutableMapOf<TraitType<*>,Trait>()
        for ((type, trait) in indexTraits) {
            when (type) {
                OrderTrait -> { /* Map physical columns to bound columns. */
                    val order = (trait as OrderTrait)
                    val newOrder = order.order.map { (c1, o) ->
                        val find = this.fetch.single { (_, c2) -> c2 == c1 }
                        find.first.column to o
                    }
                    traits[type] = OrderTrait(newOrder)
                }
                else -> traits[type] = trait
            }
        }
        this.traits = traits

        /* Initialize local fields. */
        this.columns = columns
        this.physicalColumns = physicalColumns
    }

    /**
     * Creates and returns a copy of this [IndexScanPhysicalOperatorNode] without any children or parents.
     *
     * @return Copy of this [IndexScanPhysicalOperatorNode].
     */
    override fun copy() = IndexScanPhysicalOperatorNode(this.groupId, this.index, this.predicate, this.fetch)

    /**
     * [IndexScanPhysicalOperatorNode] can be partitioned if the underlying input allows for partitioning.
     *
     * @param policy The [CostPolicy] to use when determining the optimal number of partitions.
     * @param max The maximum number of partitions to create.
     * @return [OperatorNode.Physical] operator at the based of the new query plan.
     */
    override fun tryPartition(policy: CostPolicy, max: Int): Physical? {
        if (this.hasTrait(NotPartitionableTrait)) return null

        /* Determine optimal number of partitions and create them. */
        val partitions = policy.parallelisation(this.parallelizableCost, this.totalCost, max)
        if (partitions <= 1) return null
        val inbound = (0 until partitions).map { this.partition(partitions, it) }

        /* Merge strategy depends on the traits of the underlying index. */
        val merge = when {
            this.hasTrait(LimitTrait) && this.hasTrait(OrderTrait) -> {
                val order = this[OrderTrait]!!
                val limit = this[LimitTrait]!!
                MergeLimitingSortPhysicalOperatorNode(*inbound.toTypedArray(), sortOn = order.order, limit = limit.limit)
            }
            this.hasTrait(LimitTrait) -> {
                val limit = this[LimitTrait]!!
                LimitPhysicalOperatorNode(MergePhysicalOperatorNode(*inbound.toTypedArray()), limit = limit.limit)
            }
            this.hasTrait(OrderTrait) -> {
                TODO()
            }
            else -> MergePhysicalOperatorNode(*inbound.toTypedArray())
        }
        return this.output?.copyWithOutput(merge)
    }

    /**
     * Generates a partitioned version of this [IndexScanPhysicalOperatorNode].
     *
     * @param partitions The total number of partitions.
     * @param p The partition index.
     * @return Partitioned [IndexScanPhysicalOperatorNode]
     */
    override fun partition(partitions: Int, p: Int): Physical {
        check(!this.hasTrait(NotPartitionableTrait)) { "IndesScanPhysicalOperatorNode with index ${this.index.dbo.name} cannot be partitioned. This is a programmer's error!"}
        return IndexScanPhysicalOperatorNode(p, this.index, this.predicate, this.fetch, p, partitions)
    }

    /**
     * Converts this [IndexScanPhysicalOperatorNode] to a [IndexScanOperator].
     *
     * @param ctx The [QueryContext] used for the conversion (e.g. late binding).
     */
    override fun toOperator(ctx: QueryContext): Operator {
        /** Bind all column bindings to context. */
        this.fetch.forEach { it.first.bind(ctx.bindings) }

        /** Generate and return IndexScanOperator. */
        return IndexScanOperator(this.groupId, this.index, this.predicate, this.fetch, this.partitionIndex, this.partitions)
    }

    /** Generates and returns a [String] representation of this [EntityScanPhysicalOperatorNode]. */
    override fun toString() = "${super.toString()}[${this.index.dbo.type},${this.predicate}]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndexScanPhysicalOperatorNode) return false

        if (depth != other.depth) return false
        if (predicate != other.predicate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depth.hashCode()
        result = 31 * result + predicate.hashCode()
        return result
    }
}