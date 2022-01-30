package org.vitrivr.cottontail.functions.math.distance.binary

import org.vitrivr.cottontail.core.database.Name
import org.vitrivr.cottontail.core.queries.functions.Function
import org.vitrivr.cottontail.core.queries.functions.exception.FunctionNotSupportedException
import org.vitrivr.cottontail.core.queries.functions.math.VectorDistance
import org.vitrivr.cottontail.core.queries.functions.Argument
import org.vitrivr.cottontail.core.queries.functions.FunctionGenerator
import org.vitrivr.cottontail.core.queries.functions.Signature
import org.vitrivr.cottontail.core.queries.planning.cost.Cost
import org.vitrivr.cottontail.core.values.*
import org.vitrivr.cottontail.core.values.types.Types
import org.vitrivr.cottontail.core.values.types.Value
import org.vitrivr.cottontail.core.values.types.VectorValue
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A [VectorDistance] implementation to calculate the Cosine distance between two [VectorValue]s.
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
sealed class CosineDistance<T : VectorValue<*>>(type: Types.Vector<T,*>): VectorDistance<T>(type) {

    companion object: FunctionGenerator<DoubleValue> {
        val FUNCTION_NAME = Name.FunctionName("cosine")

        override val signature: Signature.Open
            get() = Signature.Open(FUNCTION_NAME, arrayOf(Argument.Vector, Argument.Vector))

        override fun obtain(signature: Signature.SemiClosed): Function<DoubleValue> {
            check(this.signature.collides(signature)) { "Provided signature $signature is incompatible with generator signature ${this.signature}. This is a programmer's error!"  }
            return when(val type = signature.arguments[0].type) {
                is Types.DoubleVector -> DoubleVector(type)
                is Types.FloatVector -> FloatVector(type)
                is Types.LongVector -> LongVector(type)
                is Types.IntVector -> IntVector(type)
                else -> throw FunctionNotSupportedException("Function generator ${this.signature} cannot generate function with signature $signature.")
            }
        }

        override fun resolve(signature: Signature.Open): List<Signature.Closed<*>> {
            if (this.signature != signature) throw FunctionNotSupportedException("Function generator ${this.signature} cannot generate function with signature $signature.")
            return listOf(
                DoubleVector(Types.DoubleVector(1)).signature,
                FloatVector(Types.FloatVector(1)).signature,
                LongVector(Types.LongVector(1)).signature,
                IntVector(Types.IntVector(1)).signature
            )
        }
    }

    /** The cost of applying this [CosineDistance] to a single [VectorValue]. */
    override val cost: Float
        get() = d * (6.0f * Cost.COST_FLOP + 4.0f * Cost.COST_MEMORY_ACCESS) + 4.0f * Cost.COST_FLOP + 3.0f * Cost.COST_MEMORY_ACCESS

    /**
     * [CosineDistance] for a [DoubleVectorValue].
     */
    class DoubleVector(type: Types.Vector<DoubleVectorValue,*>): CosineDistance<DoubleVectorValue>(type) {
        override val name: Name.FunctionName = FUNCTION_NAME
        override fun invoke(vararg arguments: Value?): DoubleValue {
            val probing = arguments[0] as DoubleVectorValue
            val query = arguments[1] as DoubleVectorValue
            var dotp = 0.0
            var normq = 0.0
            var normv = 0.0
            for (i in 0 until this.d) {
                dotp += (query.data[i] * probing.data[i])
                normq += query.data[i].pow(2)
                normv += probing.data[i].pow(2)
            }
            return DoubleValue(dotp / (sqrt(normq) * sqrt(normv)))
        }
        override fun copy(d: Int) = DoubleVector(Types.DoubleVector(d))
    }

    /**
     * [CosineDistance] for a [FloatVectorValue].
     */
    class FloatVector(type: Types.Vector<FloatVectorValue,*>): CosineDistance<FloatVectorValue>(type) {
        override val name: Name.FunctionName = FUNCTION_NAME
        override fun invoke(vararg arguments: Value?): DoubleValue {
            val probing = arguments[0] as FloatVectorValue
            val query = arguments[1] as FloatVectorValue
            var dotp = 0.0
            var normq = 0.0
            var normv = 0.0
            for (i in 0 until this.d) {
                dotp += (query.data[i] * probing.data[i])
                normq += query.data[i].pow(2)
                normv += probing.data[i].pow(2)
            }
            return DoubleValue(dotp / (sqrt(normq) * sqrt(normv)))
        }
        override fun copy(d: Int) = FloatVector(Types.FloatVector(d))
    }

    /**
     * [CosineDistance] for a [LongVectorValue].
     */
    class LongVector(type: Types.Vector<LongVectorValue,*>): CosineDistance<LongVectorValue>(type) {
        override val name: Name.FunctionName = FUNCTION_NAME
        override fun invoke(vararg arguments: Value?): DoubleValue {
            val probing = arguments[0] as LongVectorValue
            val query = arguments[1] as LongVectorValue
            var dotp = 0.0
            var normq = 0.0
            var normv = 0.0
            for (i in 0 until this.d) {
                dotp += (query.data[i] * probing.data[i])
                normq += query.data[i].toDouble().pow(2)
                normv += probing.data[i].toDouble().pow(2)
            }
            return DoubleValue(dotp / (sqrt(normq) * sqrt(normv)))
        }
        override fun copy(d: Int) = LongVector(Types.LongVector(d))
    }

    /**
     * [CosineDistance] for a [IntVectorValue].
     */
    class IntVector(type: Types.Vector<IntVectorValue,*>): CosineDistance<IntVectorValue>(type) {
        override val name: Name.FunctionName = FUNCTION_NAME
        override fun invoke(vararg arguments: Value?): DoubleValue {
            val probing = arguments[0] as IntVectorValue
            val query = arguments[1] as IntVectorValue
            var dotp = 0.0
            var normq = 0.0
            var normv = 0.0
            for (i in 0 until this.d) {
                dotp += (query.data[i] * probing.data[i])
                normq += query.data[i].toDouble().pow(2)
                normv += probing.data[i].toDouble().pow(2)
            }
            return DoubleValue(dotp / (sqrt(normq) * sqrt(normv)))
        }
        override fun copy(d: Int) = IntVector(Types.IntVector(d))
    }
}