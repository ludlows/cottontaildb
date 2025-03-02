package org.vitrivr.cottontail.dbms.execution.operators.projection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.vitrivr.cottontail.core.basics.Record
import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.database.Name
import org.vitrivr.cottontail.core.recordset.StandaloneRecord
import org.vitrivr.cottontail.core.values.*
import org.vitrivr.cottontail.core.values.types.Types
import org.vitrivr.cottontail.dbms.exceptions.ExecutionException
import org.vitrivr.cottontail.dbms.execution.exceptions.OperatorSetupException
import org.vitrivr.cottontail.dbms.execution.operators.basics.Operator
import org.vitrivr.cottontail.dbms.execution.transactions.TransactionContext
import org.vitrivr.cottontail.dbms.queries.projection.Projection

/**
 * An [Operator.PipelineOperator] used during query execution. It calculates the SUM of all values it
 * has encountered and returns it as a [Record].
 *
 * Only produces a single [Record] and converts the projected columns to a [Types.Double] column.
 * Acts as pipeline breaker.
 *
 * @author Ralph Gasser
 * @version 1.4.0
 */
class SumProjectionOperator(parent: Operator, fields: List<Name.ColumnName>) : Operator.PipelineOperator(parent) {
    /** [SumProjectionOperator] does act as a pipeline breaker. */
    override val breaker: Boolean = true

    /** Columns produced by [SumProjectionOperator]. */
    override val columns: List<ColumnDef<*>> = this.parent.columns.mapNotNull { c ->
        val match = fields.find { f -> f.matches(c.name) }
        if (match != null) {
            if (c.type !is Types.Numeric<*>) throw OperatorSetupException(this, "The provided column $match cannot be used for a ${Projection.SUM} projection because it has the wrong type.")
            ColumnDef(c.name, Types.Double)
        } else {
            null
        }
    }

    /** Parent [ColumnDef] to access and aggregate. */
    private val parentColumns = this.parent.columns.filter { c -> fields.any { f -> f.matches(c.name) } }

    /**
     * Converts this [SumProjectionOperator] to a [Flow] and returns it.
     *
     * @param context The [TransactionContext] used for execution
     * @return [Flow] representing this [SumProjectionOperator]
     */
    override fun toFlow(context: TransactionContext): Flow<Record> {
        val parentFlow = this.parent.toFlow(context)
        val columns = this.columns.toTypedArray()
        val values = this.parentColumns.map { 0.0 }.toTypedArray()
        return flow {
            /* Prepare holder of type double. */
            parentFlow.onEach {
                this@SumProjectionOperator.parentColumns.forEachIndexed { i, c ->
                    values[i] += when (val value = it[c]) {
                        is ByteValue -> value.value.toDouble()
                        is ShortValue -> value.value.toDouble()
                        is IntValue -> value.value.toDouble()
                        is LongValue -> value.value.toDouble()
                        is FloatValue -> value.value.toDouble()
                        is DoubleValue -> value.value
                        null -> 0.0
                        else -> throw ExecutionException.OperatorExecutionException(this@SumProjectionOperator, "The provided column $c cannot be used for a ${Projection.SUM} projection. ")
                    }
                }
            }.collect()

            /** Emit record. */
            emit(StandaloneRecord(0L, columns, values.map { DoubleValue(it) }.toTypedArray()))
        }
    }
}