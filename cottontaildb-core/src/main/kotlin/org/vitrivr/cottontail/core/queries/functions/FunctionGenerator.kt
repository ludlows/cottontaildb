package org.vitrivr.cottontail.core.queries.functions

import org.vitrivr.cottontail.core.queries.functions.exception.FunctionNotSupportedException
import org.vitrivr.cottontail.core.values.types.Value

/**
 * A [FunctionGenerator] can be used by Cottontail DB to generate an arbitrary [Function].
 *
 * Some [Function]s are parametrized by the types arguments they produce and depending on a concrete type,
 * a separate instance is required.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface FunctionGenerator<R: Value> {
    /** [Signature.Open] of the [Function]s generated by this [FunctionGenerator]. */
    val signature: Signature.Open

    /**
     * Tries to resolve a [Signature.Closed] for the provided [Signature.Open]s and this [FunctionGenerator].
     * This is a step usually required when parsing queries and mapping them to concrete function calls.
     *
     * @param signature The [Signature.Open]s to lookup.
     * @return List of [Signature.Closed] that match the [Signature].
     */
    fun resolve(signature: Signature.Open): List<Signature.Closed<*>>

    /**
     * Tries to resolve a [Signature.SemiClosed] for the provided [Signature]s and this [FunctionGenerator].
     * This is a step usually required when parsing queries and mapping them to concrete function calls.
     *
     * @param signature The [Signature.SemiClosed]s to lookup.
     * @return [Signature.Closed] that matches the [Signature.SemiClosed].
     */
    fun resolve(signature: Signature.SemiClosed): Signature.Closed<*> {
        if (this.signature.collides(signature)) throw FunctionNotSupportedException("Provided signature $signature is incompatible with generator signature ${this.signature}.")
        return this.obtain(signature).signature
    }

    /**
     * Generates a [Function] for the given [Signature.SemiClosed].
     *
     * @param signature The [Signature.SemiClosed] to generate a [Function] for.
     * @return The generated [Function]
     */
    fun obtain(signature: Signature.SemiClosed): Function<R>

    /**
     * Generates a [Function] for the given [ Signature.Closed].
     *
     * @param signature The [Signature.Closed] to generate a [Function] for.
     * @return The generated [Function]
     */
    fun obtain(signature: Signature.Closed<R>): Function<R>
        = obtain(Signature.SemiClosed(signature.name, signature.arguments))
}