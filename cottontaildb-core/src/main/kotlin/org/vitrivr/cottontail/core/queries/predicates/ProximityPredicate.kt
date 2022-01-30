package org.vitrivr.cottontail.core.queries.predicates

import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.queries.functions.math.VectorDistance
import org.vitrivr.cottontail.core.queries.binding.Binding
import org.vitrivr.cottontail.core.queries.binding.BindingContext

/**
 * The nearest [ProximityPredicate] is a data structure used to bridge the gap between an expressed,
 * proximity based query (through a function invocation) and an equivalent index lookup
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
sealed interface ProximityPredicate: Predicate {

    /** The [ColumnDef] this [ProximityPredicate] operates on. */
    val column: ColumnDef<*>

    /** The [VectorDistance] this [ProximityPredicate] evaluates. */
    val distance: VectorDistance<*>

    /** The limiting predicate for this [ProximityPredicate]. */
    val k: Int

    /** The [Binding] that represents the query vector. */
    val query: Binding

    /** Columns affected by this [ProximityPredicate]. */
    override val columns: Set<ColumnDef<*>>
        get() = setOf(this.column)

    /** CPU cost required for applying this [ProximityPredicate] to a single record. */
    override val atomicCpuCost: Float
        get() = this.distance.cost

    /**
     * A [ProximityPredicate] that expresses a k nearest neighbour search.
     */
    data class NNS(override val column: ColumnDef<*>, override val k: Int, override val distance: VectorDistance<*>, override val query: Binding): ProximityPredicate {
        override fun copy() = NNS(this.column, this.k, this.distance.copy(), this.query.copy())
        override fun digest(): Long = 13L * this.hashCode()
        override fun bind(context: BindingContext) = this.query.bind(context)
    }

    /**
     * A [ProximityPredicate] that expresses a k farthest neighbour search.
     */
    data class FNS(override val column: ColumnDef<*>, override val k: Int, override val distance: VectorDistance<*>, override val query: Binding): ProximityPredicate {
        override fun copy() = NNS(this.column, this.k, this.distance.copy(), this.query.copy())
        override fun digest(): Long = 7L * this.hashCode()
        override fun bind(context: BindingContext) = this.query.bind(context)
    }
}