package org.vitrivr.cottontail.functions

import org.vitrivr.cottontail.core.queries.functions.Function
import org.vitrivr.cottontail.core.queries.functions.FunctionRegistry
import org.vitrivr.cottontail.core.queries.functions.math.VectorDistance
import org.vitrivr.cottontail.functions.math.distance.binary.*
import org.vitrivr.cottontail.functions.math.distance.other.HyperplaneDistance
import org.vitrivr.cottontail.functions.math.distance.ternary.WeightedManhattanDistance
import org.vitrivr.cottontail.functions.math.random.RandomFloatVector
import org.vitrivr.cottontail.functions.math.score.FulltextScore

/**
 * Registers default [Function]s.
 */
fun FunctionRegistry.initialize() {
    this.register(FulltextScore)
    this.initializeArithmetics()
    this.initializeVectorDistance()
    this.initializeRandoms()
}

/**
 * Registers default arithmetics functions.
 */
private fun FunctionRegistry.initializeArithmetics() {
    this.register(org.vitrivr.cottontail.functions.math.arithmetics.scalar.Maximum)
    this.register(org.vitrivr.cottontail.functions.math.arithmetics.scalar.Minimum)
    this.register(org.vitrivr.cottontail.functions.math.arithmetics.vector.Maximum)
    this.register(org.vitrivr.cottontail.functions.math.arithmetics.vector.Minimum)
}

/**
 * Registers default [VectorDistance] functions.
 */
private fun FunctionRegistry.initializeVectorDistance() {
    this.register(ManhattanDistance)
    this.register(EuclideanDistance)
    this.register(SquaredEuclideanDistance)
    this.register(HammingDistance)
    this.register(HaversineDistance)
    this.register(CosineDistance)
    this.register(ChisquaredDistance)
    this.register(InnerProductDistance)
    this.register(HyperplaneDistance)
    this.register(WeightedManhattanDistance)
}

/**
 * Registers default random functions.
 */
private fun FunctionRegistry.initializeRandoms() {
    this.register(RandomFloatVector)
}