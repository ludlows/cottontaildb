package org.vitrivr.cottontail.database.queries.planning.nodes.logical.sources

import org.vitrivr.cottontail.database.column.ColumnDef
import org.vitrivr.cottontail.database.entity.Entity
import org.vitrivr.cottontail.database.queries.planning.nodes.logical.NullaryLogicalOperatorNode

/**
 * A [NullaryLogicalOperatorNode] that formalizes accessing data from an [Entity].
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
abstract class EntitySourceLogicalOperatorNode(val entity: Entity, override val columns: Array<ColumnDef<*>>) : NullaryLogicalOperatorNode() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntitySourceLogicalOperatorNode) return false

        if (entity != other.entity) return false
        if (!columns.contentEquals(other.columns)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entity.hashCode()
        result = 31 * result + columns.contentHashCode()
        return result
    }
}