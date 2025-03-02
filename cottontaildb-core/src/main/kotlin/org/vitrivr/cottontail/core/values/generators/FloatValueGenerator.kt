package org.vitrivr.cottontail.core.values.generators

import org.apache.commons.math3.random.RandomGenerator
import org.vitrivr.cottontail.core.values.FloatValue

/**
 * A [NumericValueGenerator] for [FloatValue]s.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object FloatValueGenerator: NumericValueGenerator<FloatValue> {
    override fun random(rnd: RandomGenerator) = FloatValue(rnd.nextFloat())
    override fun one() = FloatValue.ONE
    override fun zero() = FloatValue.ZERO
    override fun of(number: Number) = FloatValue(number)
}