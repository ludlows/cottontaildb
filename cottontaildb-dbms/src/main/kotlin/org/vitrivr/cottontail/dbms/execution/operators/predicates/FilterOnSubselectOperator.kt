package org.vitrivr.cottontail.dbms.execution.operators.predicates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import org.vitrivr.cottontail.core.basics.Record
import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.queries.binding.Binding
import org.vitrivr.cottontail.core.queries.predicates.BooleanPredicate
import org.vitrivr.cottontail.core.queries.predicates.ComparisonOperator
import org.vitrivr.cottontail.dbms.execution.operators.basics.Operator
import org.vitrivr.cottontail.dbms.execution.operators.basics.take
import org.vitrivr.cottontail.dbms.execution.transactions.TransactionContext

/**
 * An [Operator.MergingPipelineOperator] used during query execution.
 *
 * It filters the input generated by the parent [Operator] using the given [BooleanPredicate]. Depends on prior execution
 * of the provided [subqueries] [Operator]s.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class FilterOnSubselectOperator(val parent: Operator, val subqueries: List<Operator>, val predicate: BooleanPredicate) : Operator.MergingPipelineOperator(subqueries + parent) {

    /** This is technically a pipeline breaker because it has to wait for the completion of the sub-SELECTS. */
    override val breaker: Boolean = true

    /** The [ColumnDef] generated by this [FilterOnSubselectOperator]. */
    override val columns: List<ColumnDef<*>>
        get() = this.parent.columns

    override fun toFlow(context: TransactionContext): Flow<Record> = flow {
        /* Prepare main branch of query execution + sub-select branches. */
        val query = this@FilterOnSubselectOperator.parent.toFlow(context)
        val subqueries = this@FilterOnSubselectOperator.subqueries.map { it.groupId to it.toFlow(context) }

        /* Execute incoming subqueries and update bindings. */
        for ((groupId, subquery) in subqueries) {
            for (a in this@FilterOnSubselectOperator.predicate.atomics) {
                val op = a.operator

                /* Case 1: Binary operator that depends on sub-query. */
                if (op is ComparisonOperator.Binary) {
                    /* Case 1a: Left-hand side depends on sub-query. */
                    if (op.left is Binding.Subquery && (op.left as Binding.Subquery).dependsOn == groupId) {
                        subquery.take(1L).collect {
                            val value = it[(op.left as Binding.Subquery).column]
                            if (value != null) {
                                (op.left as Binding.Subquery).append(value)
                            }
                        }
                    }

                    /* Case 1b: Right-hand side depends on sub-query. */
                    if (op.right is Binding.Subquery && (op.right as Binding.Subquery).dependsOn == groupId) {
                        subquery.take(1L).collect {
                            val value = it[(op.left as Binding.Subquery).column]
                            if (value != null) {
                                (op.left as Binding.Subquery).append(value)
                            }
                        }
                    }
                }

                /* Case 2: IN operator that depends on sub-query. */
                if (op is ComparisonOperator.In) {
                    for (b in op.right) {
                        if (b is Binding.Subquery && b.dependsOn == groupId) {
                            subquery.collect {
                                val value = it[0]
                                if (value != null) {
                                    (op.left as Binding.Subquery).append(value)
                                }
                            }
                        }
                    }
                }
            }
        }

        /* Stage 2: Make comparison */
        emitAll(query.filter { this@FilterOnSubselectOperator.predicate.isMatch() })

    }
}