package ch.unibas.dmi.dbis.cottontail.knn.metrics

interface DistanceFunction {
    /**
     *
     */
    operator fun invoke(a: FloatArray, b: FloatArray): Double

    /**
     *
     */
    operator fun invoke(a: DoubleArray, b: DoubleArray): Double
}