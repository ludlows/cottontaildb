package org.vitrivr.cottontail.database.queries.planning.nodes.physical.sources

import org.vitrivr.cottontail.database.column.ColumnDef
import org.vitrivr.cottontail.database.index.AbstractIndex
import org.vitrivr.cottontail.database.index.Index
import org.vitrivr.cottontail.database.queries.OperatorNode
import org.vitrivr.cottontail.database.queries.QueryContext
import org.vitrivr.cottontail.database.queries.binding.BindingContext
import org.vitrivr.cottontail.database.queries.planning.cost.Cost
import org.vitrivr.cottontail.database.queries.planning.nodes.physical.NullaryPhysicalOperatorNode
import org.vitrivr.cottontail.database.queries.predicates.Predicate
import org.vitrivr.cottontail.database.queries.predicates.bool.BooleanPredicate
import org.vitrivr.cottontail.database.queries.predicates.knn.KnnPredicate
import org.vitrivr.cottontail.database.queries.predicates.knn.KnnPredicateHint
import org.vitrivr.cottontail.database.statistics.entity.RecordStatistics
import org.vitrivr.cottontail.database.statistics.selectivity.NaiveSelectivityCalculator
import org.vitrivr.cottontail.execution.TransactionContext
import org.vitrivr.cottontail.execution.operators.basics.Operator
import org.vitrivr.cottontail.execution.operators.sources.IndexScanOperator
import org.vitrivr.cottontail.execution.operators.transform.MergeOperator
import org.vitrivr.cottontail.model.values.types.Value

/**
 * A [IndexScanPhysicalOperatorNode] that represents a predicated lookup using an [AbstractIndex].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class IndexScanPhysicalOperatorNode(override val groupId: Int, val index: Index, val predicate: Predicate) : NullaryPhysicalOperatorNode() {
    override val statistics: RecordStatistics = this.index.parent.statistics
    override val columns: Array<ColumnDef<*>> = this.index.produces
    override val executable: Boolean = true
    override val canBePartitioned: Boolean = this.index.supportsPartitioning
    override val cost: Cost = this.index.cost(this.predicate)
    override val outputSize: Long = when (this.predicate) {
        is BooleanPredicate -> NaiveSelectivityCalculator.estimate(this.predicate, this.statistics)(this.index.parent.numberOfRows)
        is KnnPredicate -> this.predicate.k.toLong()
        else -> this.index.parent.numberOfRows
    }

    /**
     * Returns a copy of this [IndexScanPhysicalOperatorNode].
     *
     * @return Copy of this [IndexScanPhysicalOperatorNode].
     */
    override fun copyWithInputs() = IndexScanPhysicalOperatorNode(this.groupId, this.index, this.predicate)

    /**
     * Returns a copy of this [IndexScanPhysicalOperatorNode] and its output.
     *
     * @param input The [OperatorNode.Logical] that should act as inputs.
     * @return Copy of this [IndexScanPhysicalOperatorNode] and its output.
     */
    override fun copyWithOutput(input: OperatorNode.Physical?): OperatorNode.Physical {
        require(input == null) { "No input is allowed for copyWithOutput() on nullary physical operator node." }
        val scan = IndexScanPhysicalOperatorNode(this.groupId, this.index, this.predicate)
        return (this.output?.copyWithOutput(scan) ?: scan)
    }

    /**
     * Partitions this [IndexScanPhysicalOperatorNode].
     *
     * @param p The number of partitions to create.
     * @return List of [OperatorNode.Physical], each representing a partition of the original tree.
     */
    override fun partition(p: Int): List<NullaryPhysicalOperatorNode> {
        check(this.index.supportsPartitioning) { "Index ${index.name} does not support partitioning!" }
        val partitionSize = Math.floorDiv(this.outputSize, p.toLong())
        return (0 until p).map {
            val start = (it * partitionSize)
            val end = ((it + 1) * partitionSize) - 1
            RangedIndexScanPhysicalOperatorNode(this.groupId, this.index, this.predicate, start until end)
        }
    }

    /**
     * Converts this [IndexScanPhysicalOperatorNode] to a [IndexScanOperator].
     *
     * @param tx The [TransactionContext] used for execution.
     * @param ctx The [QueryContext] used for the conversion (e.g. late binding).
     */
    override fun toOperator(tx: TransactionContext, ctx: QueryContext): Operator = when (this.predicate) {
        is KnnPredicate -> {
            val hint = this.predicate.hint
            val p = if (hint is KnnPredicateHint.ParallelKnnHint) {
                Integer.max(hint.min, this.totalCost.parallelisation(hint.max))
            } else {
                this.totalCost.parallelisation()
            }

            if (p > 1 && this.canBePartitioned) {
                val partitions = this.partition(p)
                val operators = partitions.map { it.toOperator(tx, ctx) }
                MergeOperator(operators)
            } else {
                IndexScanOperator(this.groupId, this.index, this.predicate)
            }
        }
        is BooleanPredicate -> IndexScanOperator(this.groupId, this.index, this.predicate)
        else -> throw UnsupportedOperationException("Unknown type of predicate ${this.predicate} cannot be converted to operator.")
    }

    /**
     * Binds values from the provided [BindingContext] to this [IndexScanPhysicalOperatorNode]'s [Predicate].
     *
     * @param ctx The [BindingContext] used for value binding.
     */
    override fun bindValues(ctx: BindingContext<Value>): OperatorNode {
        this.predicate.bindValues(ctx)
        return super.bindValues(ctx)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndexScanPhysicalOperatorNode) return false

        if (index != other.index) return false
        if (predicate != other.predicate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + predicate.hashCode()
        return result
    }

    /** Generates and returns a [String] representation of this [IndexScanPhysicalOperatorNode]. */
    override fun toString() = "${super.toString()}(${this.index.type}, ${this.predicate})"
}