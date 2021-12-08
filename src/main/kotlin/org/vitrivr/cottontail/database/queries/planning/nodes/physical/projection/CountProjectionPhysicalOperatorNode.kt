package org.vitrivr.cottontail.database.queries.planning.nodes.physical.projection

import org.vitrivr.cottontail.database.column.ColumnDef
import org.vitrivr.cottontail.database.queries.OperatorNode
import org.vitrivr.cottontail.database.queries.QueryContext
import org.vitrivr.cottontail.database.queries.planning.cost.Cost
import org.vitrivr.cottontail.database.queries.planning.nodes.logical.projection.CountProjectionLogicalOperatorNode
import org.vitrivr.cottontail.database.queries.planning.nodes.physical.UnaryPhysicalOperatorNode
import org.vitrivr.cottontail.database.queries.projection.Projection
import org.vitrivr.cottontail.execution.operators.projection.CountProjectionOperator
import org.vitrivr.cottontail.model.basics.Name
import org.vitrivr.cottontail.model.basics.Type

/**
 * A [UnaryPhysicalOperatorNode] that represents a projection operation involving aggregate functions such as [Projection.COUNT].
 *
 * @author Ralph Gasser
 * @version 2.3.0
 */
class CountProjectionPhysicalOperatorNode(input: Physical? = null, val alias: Name.ColumnName? = null) : AbstractProjectionPhysicalOperatorNode(input, Projection.COUNT) {

    /** The [ColumnDef] generated by this [CountProjectionLogicalOperatorNode]. */
    override val columns: List<ColumnDef<*>>
        get() {
            val name = this.alias ?: (this.input?.columns?.first()?.name?.entity()?.column(Projection.COUNT.label()) ?: Name.ColumnName(Projection.COUNT.label()))
            return listOf(ColumnDef(name, Type.Long, false))
        }

    /** The [ColumnDef] required by this [CountProjectionLogicalOperatorNode]. */
    override val requires: List<ColumnDef<*>>
        get() = emptyList()

    /** The output size of this [CountProjectionPhysicalOperatorNode] is always one. */
    override val outputSize: Long = 1

    /** The [Cost] of a [CountProjectionPhysicalOperatorNode]. */
    override val cost: Cost
        get() = Cost(cpu = Cost.COST_MEMORY_ACCESS) * (this.input?.outputSize ?: 0)

    /**The [ExistsProjectionPhysicalOperatorNode] cannot be partitioned. */
    override val canBePartitioned: Boolean = false

    /**
     * Creates and returns a copy of this [CountProjectionPhysicalOperatorNode] without any children or parents.
     *
     * @return Copy of this [CountProjectionPhysicalOperatorNode].
     */
    override fun copy() = CountProjectionPhysicalOperatorNode(alias = this.alias)

    /**
     * Partitions this [CountProjectionPhysicalOperatorNode].
     *
     * @param p The number of partitions to create.
     * @return List of [OperatorNode.Physical], each representing a partition of the original tree.
     */
    override fun partition(p: Int): List<Physical> {
        throw UnsupportedOperationException("CountProjectionPhysicalOperatorNode cannot be partitioned.")
    }

    /**
     * Converts this [CountProjectionPhysicalOperatorNode] to a [CountProjectionOperator].
     *
     * @param ctx The [QueryContext] used for the conversion (e.g. late binding).
     */
    override fun toOperator(ctx: QueryContext) = CountProjectionOperator(this.input?.toOperator(ctx) ?: throw IllegalStateException("Cannot convert disconnected OperatorNode to Operator (node = $this)"))

    /**
     * Compares this [CountProjectionPhysicalOperatorNode] to another object.
     *
     * @param other The other [Any] to compare this [CountProjectionPhysicalOperatorNode] to.
     * @return True if other equals this, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountProjectionPhysicalOperatorNode) return false
        if (this.type != other.type) return false
        if (this.alias != other.alias) return false
        return true
    }

    /**
     * Generates and returns a hash code for this [CountProjectionPhysicalOperatorNode].
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + this.alias.hashCode()
        return result
    }
}