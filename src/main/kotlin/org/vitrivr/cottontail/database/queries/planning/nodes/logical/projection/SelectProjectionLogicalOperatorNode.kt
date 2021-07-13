package org.vitrivr.cottontail.database.queries.planning.nodes.logical.projection

import org.vitrivr.cottontail.database.column.ColumnDef
import org.vitrivr.cottontail.database.queries.planning.nodes.logical.UnaryLogicalOperatorNode
import org.vitrivr.cottontail.database.queries.planning.nodes.physical.projection.SelectProjectionPhysicalOperatorNode
import org.vitrivr.cottontail.database.queries.projection.Projection
import org.vitrivr.cottontail.model.basics.Name
import org.vitrivr.cottontail.model.exceptions.QueryException

/**
 * A [UnaryLogicalOperatorNode] that represents a projection operation on a [org.vitrivr.cottontail.model.recordset.Recordset].
 *
 * @author Ralph Gasser
 * @version 2.2.0
 */
class SelectProjectionLogicalOperatorNode(input: Logical? = null, type: Projection, fields: List<Pair<Name.ColumnName, Name.ColumnName?>>) : AbstractProjectionLogicalOperatorOperator(input, type, fields) {

    init {
        /* Sanity check. */
        require(this.type in arrayOf(Projection.SELECT, Projection.SELECT_DISTINCT)) {
            "Projection of type ${this.type} cannot be used with instances of AggregatingProjectionLogicalNodeExpression."
        }
    }

    /** The [ColumnDef] generated by this [SelectProjectionLogicalOperatorNode]. */
    override val columns: List<ColumnDef<*>>
        get() = this.input?.columns?.mapNotNull { inColumn ->
            val match = this.fields.find { f -> f.first.matches(inColumn.name) }
            if (match != null) {
                val alias = match.second
                val outColumn = if (alias != null) {
                    inColumn.copy(name = alias)
                } else {
                    inColumn
                }
                outColumn
            } else {
                null
            }
        } ?: emptyList()

    /** The [ColumnDef] required by this [SelectProjectionLogicalOperatorNode]. */
    override val requires: List<ColumnDef<*>>
        get() = this.input?.columns?.mapNotNull { inColumn ->
            val match = this.fields.find { f -> f.first.matches(inColumn.name) }
            if (match != null) {
                inColumn
            } else {
                null
            }
        } ?: emptyList()

    init {
        /* Sanity check. */
        if (this.fields.isEmpty()) {
            throw QueryException.QuerySyntaxException("Projection of type $type must specify at least one column.")
        }
    }

    /**
     * Creates and returns a copy of this [SelectProjectionLogicalOperatorNode] without any children or parents.
     *
     * @return Copy of this [SelectProjectionLogicalOperatorNode].
     */
    override fun copy() = SelectProjectionLogicalOperatorNode(type = this.type, fields = this.fields)

    /**
     * Returns a [SelectProjectionPhysicalOperatorNode] representation of this [SelectProjectionLogicalOperatorNode]
     *
     * @return [SelectProjectionPhysicalOperatorNode]
     */
    override fun implement(): Physical = SelectProjectionPhysicalOperatorNode(this.input?.implement(), this.type, this.fields)
}