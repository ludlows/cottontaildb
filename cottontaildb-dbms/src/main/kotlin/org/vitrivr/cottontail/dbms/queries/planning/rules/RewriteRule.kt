package org.vitrivr.cottontail.dbms.queries.planning.rules

import org.vitrivr.cottontail.dbms.queries.operators.OperatorNode
import org.vitrivr.cottontail.dbms.queries.QueryContext

/**
 * A [RewriteRule] is a rule used during query planning. It tries to replace an [OperatorNode]
 * or a combination of [OperatorNode]s through another, more optimal combination.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface RewriteRule {
    /**
     * Checks if this [RewriteRule] can be applied to the given [OperatorNode]
     *
     * @param node The input [OperatorNode] to check.
     * @return True if [RewriteRule] can be applied, false otherwise.
     */
    fun canBeApplied(node: org.vitrivr.cottontail.dbms.queries.operators.OperatorNode): Boolean

    /**
     * Transforms the given [OperatorNode] (and potentially its parents and children) to a new
     * [OperatorNode] that produces the equivalent output.
     *
     * @param node The input [OperatorNode].
     * @param ctx The [QueryContext] used for planning.
     *
     * @return The output [OperatorNode] or null, if no rewrite was done.
     */
    fun apply(node: org.vitrivr.cottontail.dbms.queries.operators.OperatorNode, ctx: QueryContext): org.vitrivr.cottontail.dbms.queries.operators.OperatorNode?
}