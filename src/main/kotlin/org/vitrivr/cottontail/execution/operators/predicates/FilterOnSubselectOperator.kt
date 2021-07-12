package org.vitrivr.cottontail.execution.operators.predicates

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.flow.*
import org.vitrivr.cottontail.database.column.ColumnDef
import org.vitrivr.cottontail.database.queries.QueryContext
import org.vitrivr.cottontail.database.queries.binding.Binding
import org.vitrivr.cottontail.database.queries.predicates.bool.BooleanPredicate
import org.vitrivr.cottontail.database.queries.predicates.bool.ComparisonOperator
import org.vitrivr.cottontail.execution.operators.basics.Operator
import org.vitrivr.cottontail.execution.operators.basics.take
import org.vitrivr.cottontail.model.basics.Record
import org.vitrivr.cottontail.model.exceptions.ExecutionException

/**
 * An [Operator.MergingPipelineOperator] used during query execution.
 *
 * It filters the input generated by the parent [Operator] using the given [BooleanPredicate]. Depends on prior execution
 * of the provided [subSelects] [Operator]s.
 *
 * @author Ralph Gasser
 * @version 11.0
 */
class FilterOnSubselectOperator(val parent: Operator, val subSelects: List<Operator>, val predicate: BooleanPredicate) : Operator.MergingPipelineOperator(subSelects + parent) {

    /** This is technically a pipeline breaker because it has to wait for the completion of the sub-SELECTS. */
    override val breaker: Boolean = true

    /** The [ColumnDef] generated by this [FilterOnSubselectOperator]. */
    override val columns: Array<ColumnDef<*>>
        get() = parent.columns

    override fun toFlow(context: QueryContext): Flow<Record> {
        /* Prepare main branch of query execution + sub-select branches. */
        val query = this.parent.toFlow(context)
        val subSelects = this.subSelects.map { it.groupId to it.toFlow(context) }

        /* Prepare map with all comparison operators that depend on sub-select. */
        val atomics = Int2ObjectOpenHashMap<ComparisonOperator>()
        this.predicate.atomics.forEach {
            if (it.dependsOn > 0) {
                atomics[it.dependsOn] = it.operator
            }
        }
        return flow {
            subSelects.forEach { select ->
                val op = atomics[select.first]
                when (op) {
                    is ComparisonOperator.Binary.Equal -> select.second.take(1).onEach { (op.right as Binding.Literal).value = it[it.columns[0]] }
                    is ComparisonOperator.Binary.Greater -> select.second.take(1).onEach { (op.right as Binding.Literal).value = it[it.columns[0]] }
                    is ComparisonOperator.Binary.GreaterEqual -> select.second.take(1).onEach { (op.right as Binding.Literal).value = it[it.columns[0]] }
                    is ComparisonOperator.Binary.Less -> select.second.take(1).onEach { (op.right as Binding.Literal).value = it[it.columns[0]] }
                    is ComparisonOperator.Binary.LessEqual -> select.second.take(1).onEach { (op.right as Binding.Literal).value = it[it.columns[0]] }
                    is ComparisonOperator.Binary.Like -> select.second.take(1).onEach { (op.right as Binding.Literal).value = it[it.columns[0]] }
                    is ComparisonOperator.In -> {
                        select.second.onEach {
                            val value = it[it.columns[0]]
                            if (value != null) {
                                op.addRef(context.bindings.bind(value))
                            }
                        }
                    }
                    else -> throw ExecutionException.OperatorExecutionException(this@FilterOnSubselectOperator, "Operator of type $op does not support integration of sub-selects.")
                }.collect()
            }

            /* Stage 2: Make comparison */
            emitAll(query.filter { this@FilterOnSubselectOperator.predicate.isMatch() })
        }
    }
}