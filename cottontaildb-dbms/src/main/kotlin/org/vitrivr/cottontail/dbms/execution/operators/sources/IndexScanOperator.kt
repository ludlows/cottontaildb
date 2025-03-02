package org.vitrivr.cottontail.dbms.execution.operators.sources

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vitrivr.cottontail.core.basics.Record
import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.queries.GroupId
import org.vitrivr.cottontail.core.queries.binding.Binding
import org.vitrivr.cottontail.core.queries.predicates.Predicate
import org.vitrivr.cottontail.core.recordset.StandaloneRecord
import org.vitrivr.cottontail.dbms.entity.EntityTx
import org.vitrivr.cottontail.dbms.execution.operators.basics.Operator
import org.vitrivr.cottontail.dbms.execution.transactions.TransactionContext
import org.vitrivr.cottontail.dbms.index.Index
import org.vitrivr.cottontail.dbms.index.IndexTx

/**
 * An [Operator.SourceOperator] that scans an [Index] and streams all [Record]s found within.
 *
 * @author Ralph Gasser
 * @version 1.6.0
 */
class IndexScanOperator(
    groupId: GroupId,
    private val index: IndexTx,
    private val predicate: Predicate,
    private val fetch: List<Pair<Binding.Column, ColumnDef<*>>>,
    private val partitionIndex: Int = 0,
    private val partitions: Int = 1
) : Operator.SourceOperator(groupId) {

    companion object {
        /** [Logger] instance used by [IndexScanOperator]. */
        private val LOGGER: Logger = LoggerFactory.getLogger(IndexScanOperator::class.java)
    }

    /** The [ColumnDef] produced by this [IndexScanOperator]. */
    override val columns: List<ColumnDef<*>> = this.fetch.map {
        require(this.index.columnsFor(this.predicate).contains(it.second)) { "The given column $it is not produced by the selected index ${this.index.dbo}. This is a programmer's error!"}
        it.first.column
    }

    /**
     * Converts this [IndexScanOperator] to a [Flow] and returns it.
     *
     * @param context The [TransactionContext] used for execution.
     * @return [Flow] representing this [IndexScanOperator]
     */
    override fun toFlow(context: TransactionContext): Flow<Record> = flow {
        val columns = this@IndexScanOperator.fetch.map { it.first.column }.toTypedArray()
        val cursor = if (this@IndexScanOperator.partitions == 1) {
            this@IndexScanOperator.index.filter(this@IndexScanOperator.predicate)
        } else {
            val entityTx = this@IndexScanOperator.index.context.getTx(this@IndexScanOperator.index.dbo.parent) as EntityTx
            this@IndexScanOperator.index.filter(this@IndexScanOperator.predicate, entityTx.partitionFor(this@IndexScanOperator.partitionIndex, this@IndexScanOperator.partitions))
        }
        var read = 0
        while (cursor.moveNext()) {
            val record = cursor.value() as StandaloneRecord
            for ((i,c) in columns.withIndex()) {
                record.columns[i] = c
            }
            this@IndexScanOperator.fetch.first().first.context.update(record)
            emit(record)
            read += 1
        }
        cursor.close()
        LOGGER.debug("Read $read entries from ${this@IndexScanOperator.index.dbo.name}.")
    }
}