package org.vitrivr.cottontail.dbms.queries.planning.nodes.physical.management

import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.queries.planning.cost.Cost
import org.vitrivr.cottontail.dbms.entity.Entity
import org.vitrivr.cottontail.dbms.entity.EntityTx
import org.vitrivr.cottontail.dbms.queries.QueryContext
import org.vitrivr.cottontail.dbms.queries.planning.nodes.logical.management.DeleteLogicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.planning.nodes.physical.UnaryPhysicalOperatorNode
import org.vitrivr.cottontail.execution.operators.basics.Operator
import org.vitrivr.cottontail.execution.operators.management.DeleteOperator

/**
 * A [DeletePhysicalOperatorNode] that formalizes a delete operation on an [Entity].
 *
 * @author Ralph Gasser
 * @version 2.2.1
 */
class DeletePhysicalOperatorNode(input: Physical? = null, val entity: EntityTx) : UnaryPhysicalOperatorNode(input) {

    companion object {
        private const val NODE_NAME = "Delete"
    }

    /** The name of this [DeleteLogicalOperatorNode]. */
    override val name: String
        get() = NODE_NAME

    /** The [DeletePhysicalOperatorNode] produces the [ColumnDef]s defined in the [DeleteOperator]. */
    override val columns: List<ColumnDef<*>> = DeleteOperator.COLUMNS

    /** The [DeletePhysicalOperatorNode] does not require any [ColumnDef]. */
    override val requires: List<ColumnDef<*>> = emptyList()

    /** The [DeletePhysicalOperatorNode] produces a single record. */
    override val outputSize: Long = 1L

    /** The [Cost] of this [DeletePhysicalOperatorNode]. */
    override val cost: Cost = Cost(io = this.entity.count() * Cost.COST_DISK_ACCESS_WRITE) * (this.input?.outputSize ?: 0)

    /** The [DeletePhysicalOperatorNode]s cannot be partitioned. */
    override val canBePartitioned: Boolean = false

    /**
     * Creates and returns a copy of this [DeletePhysicalOperatorNode] without any children or parents.
     *
     * @return Copy of this [DeletePhysicalOperatorNode].
     */
    override fun copy() = DeletePhysicalOperatorNode(entity = this.entity)

    /**
     * Converts this [DeletePhysicalOperatorNode] to a [DeleteOperator].
     *
     * @param ctx The [QueryContext] used for the conversion (e.g. late binding).
     */
    override fun toOperator(ctx: QueryContext): Operator = DeleteOperator(
        this.input?.toOperator(ctx) ?: throw IllegalStateException("Cannot convert disconnected OperatorNode to Operator (node = $this)"),
        this.entity
    )

    override fun toString(): String = "${super.toString()}[${this.entity.dbo.name}]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeletePhysicalOperatorNode) return false

        if (entity != other.entity) return false

        return true
    }

    override fun hashCode(): Int = this.entity.hashCode()
}