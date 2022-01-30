package org.vitrivr.cottontail.execution.operators.sources

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.vitrivr.cottontail.core.basics.Record
import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.queries.GroupId
import org.vitrivr.cottontail.core.queries.binding.Binding
import org.vitrivr.cottontail.dbms.entity.Entity
import org.vitrivr.cottontail.dbms.entity.EntityTx
import org.vitrivr.cottontail.dbms.queries.QueryContext
import org.vitrivr.cottontail.execution.TransactionContext
import org.vitrivr.cottontail.execution.operators.basics.Operator
import java.util.*

/**
 * An [Operator.SourceOperator] that samples an [Entity] and streams all [Record]s found within.
 *
 * @author Ralph Gasser
 * @version 1.6.0
 */
class EntitySampleOperator(groupId: GroupId, val entity: EntityTx, val fetch: List<Pair<Binding.Column, ColumnDef<*>>>, val p: Float, val seed: Long) : Operator.SourceOperator(groupId) {

    /** The [ColumnDef] fetched by this [EntitySampleOperator]. */
    override val columns: List<ColumnDef<*>> = this.fetch.map { it.first.column }

    /**
     * Converts this [EntitySampleOperator] to a [Flow] and returns it.
     *
     * @param context The [QueryContext] used for execution.
     * @return [Flow] representing this [EntitySampleOperator].
     */
    override fun toFlow(context: TransactionContext): Flow<Record> {
        val fetch = this.fetch.map { it.second }.toTypedArray()
        return flow {
            val random = SplittableRandom(this@EntitySampleOperator.seed)
            for (record in this@EntitySampleOperator.entity.scan(fetch)) {
                if (random.nextDouble(0.0, 1.0) <= this@EntitySampleOperator.p) {
                    for (i in 0 until record.size) {
                        this@EntitySampleOperator.fetch[i].first.update(record[i])
                    }
                    emit(record)
                }
            }
        }
    }
}