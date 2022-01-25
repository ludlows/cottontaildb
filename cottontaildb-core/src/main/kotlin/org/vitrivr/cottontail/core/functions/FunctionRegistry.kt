package org.vitrivr.cottontail.core.functions

import org.vitrivr.cottontail.core.functions.exception.FunctionNotFoundException

/**
 * A [FunctionRegistry] manages all the [Function] instances generated and used by Cottontail DB. On a high level, there
 * are two kinds of [Function]s:
 *
 * [Function]s are generated by a [FunctionGenerator] upon request. They may or may not be cached.
 *
 * @author Ralph Gasser
 * @version 1.0.2
 */
class FunctionRegistry {
    /**
     * The internal data structure to organize [Function]s.
     *
     * Function resolution always occurs as follows #Arguments -> Name -> Types. A function exhibiting
     * the concrete types are then found by brute force.
     */
    private val registry = mutableMapOf<Signature.Closed<*>, Function<*>>()

    /**
     * The internal data structure to organize [Function]s.
     *
     * Function resolution always occurs as follows #Arguments -> Name -> Types. A function exhibiting
     * the concrete types are then found by brute force.
     */
    private val generators = mutableMapOf<Signature.Open<*>, FunctionGenerator<*>>()

    /**
     * Registers a new [Function] with this [FunctionRegistry].
     *
     * @param function The [Function] to register.
     * @throws IllegalStateException If a [Function] or a [FunctionGenerator] with a colliding [Signature] has been registered.
     */
    fun register(function: Function<*>) {
        check(!this.registry.containsKey(function.signature)) { "Function ${function.signature} collides with existing function." }
        val collision = this.generators.keys.find { it.collides(function.signature) }
        check(collision == null) {
            "Function generator $collision collides with function ${function.signature}."
        }
        this.registry[function.signature] = function
    }

    /**
     * Registers a new [FunctionGenerator] with this [FunctionRegistry].
     *
     * @param generator The [FunctionGenerator] to register.
     * @throws IllegalStateException If a [Function] or a [FunctionGenerator] with a colliding [Signature] has been registered.
     */
    fun register(generator: FunctionGenerator<*>) {
        check(!this.generators.containsKey(generator.signature)) { "Function generator for name ${generator.signature} collides with existing generator." }
        val collision = this.registry.keys.find { it.collides(generator.signature) }
        check(collision == null) {
            "Static function $collision collides with function generator ${generator.signature}"
        }
        this.generators[generator.signature] = generator
    }

    /**
     * Obtains a [Function] instance for the given [Signature.Closed].
     *
     * @param signature The signature of the desired function, i.e., the number of arguments it accepts.
     * @return [Function] instance
     * @throws [FunctionNotFoundException] If neither a registered [Function] nor [FunctionGenerator] exists.
     */
    fun obtain(signature: Signature.Closed<*>): Function<*> {
        val function = this.registry[signature]
        if (function != null) return function
        return obtain(signature.toOpen()).generate(*signature.arguments)
    }

    /**
     * Obtains a [FunctionGenerator] instance for the given [Signature.Open].
     *
     * @param signature The [Signature.Open<*>] to obtain a [FunctionGenerator] for,
     * @return [FunctionGenerator] instance
     * @throws [FunctionNotFoundException] If no registered [FunctionGenerator] exists.
     */
    fun obtain(signature: Signature.Open<*>): FunctionGenerator<*> = this.generators[signature] ?: throw FunctionNotFoundException(signature)
}