package org.vitrivr.cottontail.dbms.index

import org.vitrivr.cottontail.core.basics.Countable
import org.vitrivr.cottontail.core.basics.Cursor
import org.vitrivr.cottontail.core.basics.Filterable
import org.vitrivr.cottontail.core.basics.Record
import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.database.TupleId
import org.vitrivr.cottontail.core.queries.nodes.traits.Trait
import org.vitrivr.cottontail.core.queries.nodes.traits.TraitType
import org.vitrivr.cottontail.core.queries.planning.cost.Cost
import org.vitrivr.cottontail.core.queries.predicates.Predicate
import org.vitrivr.cottontail.dbms.general.Tx
import org.vitrivr.cottontail.dbms.operations.Operation

/**
 * A [Tx] that operates on a single [Index]. [Tx]s are a unit of isolation for data operations (read/write).
 *
 * @author Ralph Gasser
 * @version 3.0.0
 */
interface IndexTx: Tx, Filterable, Countable {

    /** Reference to the [Index] this [IndexTx] belongs to. */
    override val dbo: Index

    /** The [ColumnDef]s indexed by the [Index] backing this [IndexTx]. */
    val columns: Array<ColumnDef<*>>

    /** True, if the [Index] backing this [IndexTx] supports incremental updates, and false otherwise. */
    val supportsIncrementalUpdate: Boolean

    /** True, if the [Index] backing this [IndexTx] supports querying filtering a partition. */
    override val supportsPartitioning: Boolean

    /** The [IndexType] of the [Index] that underpins this [IndexTx]. */
    val state: IndexState

    /** The configuration map used for the [Index] that underpins this [IndexTx]. */
    val config: IndexConfig<*>

    /**
     * Calculates the cost estimate of this [IndexTx] processing the provided [Predicate].
     *
     * @param predicate [Predicate] to check.
     * @return Cost estimate for the [Predicate]
     */
    fun costFor(predicate: Predicate): Cost

    /**
     * Returns the [ColumnDef] produced by this [IndexTx] for the given [Predicate].
     *
     * @param predicate [Predicate] to obtain columns for.
     * @return List of [ColumnDef] produced by this [IndexTx]
     */
    fun columnsFor(predicate: Predicate): List<ColumnDef<*>>

    /**
     * Returns the list of [Trait]s for the given [Predicate].
     *
     * @param predicate [Predicate] to obtain [Trait]s for.
     * @return List of [Trait]s produced by this [IndexTx]
     */
    fun traitsFor(predicate: Predicate): Map<TraitType<*>,Trait>

    /**
     * (Re-)builds the underlying [Index] completely.
     */
    fun rebuild()

    /**
     * Clears the [Index] underlying this [IndexTx] and removes all entries it contains.
     */
    fun clear()

    /**
     * Inserts a new entry in the [Index] underlying this [IndexTx] based on the provided [Operation.DataManagementOperation.UpdateOperation].
     *
     * Not all [Index] implementations support incremental updates. Should be indicated by [Index.supportsIncrementalUpdate]
     *
     * @param operation [Operation.DataManagementOperation.InsertOperation] that should be processed.
     */
    fun insert(operation: Operation.DataManagementOperation.InsertOperation)

    /**
     * Updates an entry in the [Index] underlying this [IndexTx] based on the provided [Operation.DataManagementOperation.UpdateOperation].
     *
     * Not all [Index] implementations support incremental updates. Should be indicated by [Index.supportsIncrementalUpdate]
     *
     * @param operation [Operation.DataManagementOperation.UpdateOperation] that should be processed.
     */
    fun update(operation: Operation.DataManagementOperation.UpdateOperation)

    /**
     * Deletes an entry from the [Index] underlying this [IndexTx] based on the provided [Operation.DataManagementOperation.DeleteOperation].
     *
     * Not all [Index] implementations support incremental updates. Should be indicated by [Index.supportsIncrementalUpdate]
     *
     * @param operation [Operation.DataManagementOperation.DeleteOperation] that should be processed.
     */
    fun delete(operation: Operation.DataManagementOperation.DeleteOperation)

    /**
     * Performs a lookup through this [IndexTx] and returns a [Cursor] of all the [Record]s that match the [Predicate].
     *
     * @param predicate The [Predicate] to perform the lookup.
     * @return The resulting [Cursor].
     */
    override fun filter(predicate: Predicate): Cursor<Record>

    /**
     * Performs a lookup through this [IndexTx] and returns a [Cursor] of all the [Record]s that match the [Predicate]
     * and fall within the specified data [LongRange], which must lie in 0..[count].
     *
     * Not all [Index] implementations support range filtering.
     *
     * @param predicate The [Predicate] to perform the lookup.
     * @param partition The [LongRange] specifying the [TupleId]s that should be considered.
     * @return The resulting [Cursor].
     */
    override fun filter(predicate: Predicate, partition: LongRange): Cursor<Record>
}