package org.vitrivr.cottontail.database.queries.planning.nodes.physical.sources

import org.vitrivr.cottontail.database.column.ColumnDef
import org.vitrivr.cottontail.database.index.Index
import org.vitrivr.cottontail.database.index.IndexTx
import org.vitrivr.cottontail.database.queries.QueryContext
import org.vitrivr.cottontail.database.queries.planning.nodes.physical.NullaryPhysicalOperatorNode
import org.vitrivr.cottontail.database.queries.predicates.Predicate
import org.vitrivr.cottontail.database.statistics.entity.RecordStatistics
import org.vitrivr.cottontail.execution.operators.basics.Operator
import org.vitrivr.cottontail.execution.operators.sources.IndexScanOperator
import java.lang.Math.floorDiv

/**
 * A [NullaryPhysicalOperatorNode] that formalizes a scan of a physical [Index] in Cottontail DB on a given range.
 *
 * @author Ralph Gasser
 * @version 2.2.0
 */
class RangedIndexScanPhysicalOperatorNode(override val groupId: Int, val index: IndexTx, val predicate: Predicate, val partitionIndex: Int, val partitions: Int) : NullaryPhysicalOperatorNode() {
    companion object {
        private const val NODE_NAME = "ScanIndex"
    }

    /** The name of this [RangedIndexScanPhysicalOperatorNode]. */
    override val name: String
        get() = NODE_NAME


    override val outputSize = floorDiv(this.index.dbo.parent.statistics.count, this.partitions)
    override val statistics: RecordStatistics = this.index.dbo.parent.statistics
    override val columns: Array<ColumnDef<*>> = this.index.dbo.produces
    override val executable: Boolean = true
    override val canBePartitioned: Boolean = false
    override val cost = this.index.dbo.cost(this.predicate)

    init {
        require(this.partitionIndex >= 0) { "The partitionIndex of a ranged index scan must be greater than zero." }
        require(this.partitions > 0) { "The number of partitions for a ranged index scan must be greater than zero." }
    }

    /**
     * Creates and returns a copy of this [RangedIndexScanPhysicalOperatorNode] without any children or parents.
     *
     * @return Copy of this [RangedIndexScanPhysicalOperatorNode].
     */
    override fun copy() = RangedIndexScanPhysicalOperatorNode(this.groupId, this.index, this.predicate, this.partitionIndex, this.partitions)

    /**
     * Converts this [RangedIndexScanPhysicalOperatorNode] to a [IndexScanOperator].
     *
     * @param ctx The [QueryContext] used for the conversion (e.g. late binding).
     */
    override fun toOperator(ctx: QueryContext): Operator = IndexScanOperator(this.groupId, this.index, this.predicate, this.partitionIndex, this.partitions)

    /**
     * [RangedIndexScanPhysicalOperatorNode] cannot be partitioned.
     */
    override fun partition(p: Int): List<Physical> {
        throw UnsupportedOperationException("RangedIndexScanPhysicalOperatorNode cannot be further partitioned.")
    }

    /** Generates and returns a [String] representation of this [RangedIndexScanPhysicalOperatorNode]. */
    override fun toString() = "${super.toString()}[${this.index.type},${this.predicate},${this.partitionIndex}/${this.partitions}/]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RangedIndexScanPhysicalOperatorNode) return false

        if (this.depth != other.depth) return false
        if (this.predicate != other.predicate) return false
        if (this.partitionIndex != other.partitionIndex) return false
        if (this.partitions != other.partitions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depth.hashCode()
        result = 31 * result + predicate.hashCode()
        result = 31 * result + partitionIndex.hashCode()
        result = 31 * result + partitions.hashCode()
        return result
    }
}