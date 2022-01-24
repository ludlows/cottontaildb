package org.vitrivr.cottontail.dbms.queries.planning.nodes.logical.function

import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.functions.Function
import org.vitrivr.cottontail.core.queries.binding.Binding
import org.vitrivr.cottontail.dbms.queries.planning.nodes.logical.UnaryLogicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.planning.nodes.physical.function.NestedFunctionPhysicalOperatorNode

/**
 * A [UnaryLogicalOperatorNode] that represents the execution of a [Function] that is not manifested in an additional [ColumnDef], i.e., a nested function.
 *
 * Usually, [NestedFunctionLogicalOperatorNode] occur when other nodes that contain function calls are being evaluated.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class NestedFunctionLogicalOperatorNode(input: Logical? = null, val function: Function<*>, val arguments: List<Binding>, val out: Binding.Literal) : UnaryLogicalOperatorNode(input) {

    companion object {
        private const val NODE_NAME = "NestedFunction"
    }

    /** The [NestedFunctionLogicalOperatorNode] requires all [ColumnDef] used in the [Function]. */
    override val requires: List<ColumnDef<*>>
        get() = this.arguments.filterIsInstance<Binding.Column>().map { it.column }

    /** [NestedFunctionLogicalOperatorNode] can only be executed if [Function] can be executed. */
    override val executable: Boolean
        get() = super.executable && this.function.executable

    /** Human-readable name of this [NestedFunctionLogicalOperatorNode]. */
    override val name: String
        get() = NODE_NAME

    /**
     * Creates and returns a copy of this [NestedFunctionLogicalOperatorNode] without any children or parents.
     *
     * @return Copy of this [NestedFunctionLogicalOperatorNode].
     */
    override fun copy() = NestedFunctionLogicalOperatorNode(function = this.function, arguments = this.arguments, out = this.out)

    /**
     * Returns a [NestedFunctionLogicalOperatorNode] representation of this [NestedFunctionLogicalOperatorNode]
     *
     * @return [NestedFunctionLogicalOperatorNode]
     */
    override fun implement() = NestedFunctionPhysicalOperatorNode(this.input?.implement(), this.function, this.arguments, this.out)

    /**
     * Compares this [NestedFunctionLogicalOperatorNode] to the given [Object] and returns true if they're equal and false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NestedFunctionLogicalOperatorNode) return false
        if (this.function != other.function) return false
        if (this.out != other.out) return false
        return true
    }

    /**
     * Generates and returns a hash code for this [NestedFunctionLogicalOperatorNode].
     */
    override fun hashCode(): Int {
        var result = this.function.hashCode()
        result = 31 * result + this.out.hashCode()
        return result
    }

    /** Generates and returns a [String] representation of this [NestedFunctionLogicalOperatorNode]. */
    override fun toString() = "${super.toString()}[${this.function.signature}]"
}