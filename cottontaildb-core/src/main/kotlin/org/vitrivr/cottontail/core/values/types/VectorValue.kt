package org.vitrivr.cottontail.core.values.types

import org.vitrivr.cottontail.core.values.IntValue

/**
 * Represents a vector value of any type, i.e., a value that consists only more than one entry.
 *
 * [VectorValue] are always numeric! This is an abstraction over the existing primitive array types provided
 * by Kotlin. It allows for the advanced type system implemented by Cottontail DB.
 *
 * @version 1.4.0
 * @author Ralph Gasser
 */
interface VectorValue<T : Number> : Value {

    /**
     * Compares this [VectorValue] to another [Value]. Returns -1, 0 or 1 of other value is smaller,
     * equal or greater than this value. [VectorValue] can only be compared to other [VectorValue]s
     * and the norm of the [VectorValue] is used for comparison.
     *
     * @param other Value to compare to.
     * @return -1, 0 or 1 of other value is smaller, equal or greater than this value
     */
    override fun compareTo(other: Value): Int = when (other) {
        is VectorValue<*> -> this.norm2().compareTo(other.norm2())
        else -> throw IllegalArgumentException("VectorValue can can only be compared to other vector values.")
    }

    /**
     * Returns the i-th entry of  this [VectorValue].
     *
     * @param i Index of the entry.
     * @return The value at index i.
     */
    operator fun get(i: Int): NumericValue<T>

    /**
     * Returns the i-th entry of  this [VectorValue] as [Boolean].
     *
     * @param i Index of the entry.
     * @return The value at index i.
     */
    fun getAsBool(i: Int): Boolean

    /**
     * Returns a sub vector of this [VectorValue] starting at the component [start]
     * and containing [length] components.
     *
     * @param start Index of the first entry of the returned vector.
     * @param length how many elements, including start, to return
     *
     * @return The [VectorValue] representing the sub-vector.
     */
    fun slice(start: Int, length: Int): VectorValue<T>

    /**
     * Returns true, if this [VectorValue] consists of all zeroes, i.e. [0, 0, ... 0]
     *
     * @return True, if this [VectorValue] consists of all zeroes
     */
    fun allZeros(): Boolean

    /**
     * Returns true, if this [VectorValue] consists of all ones, i.e. [1, 1, ... 1]
     *
     * @return True, if this [VectorValue] consists of all ones
     */
    fun allOnes(): Boolean

    /**
     * Returns the indices of this [VectorValue].
     *
     * @return The indices of this [VectorValue]
     */
    val indices: IntRange

    /**
     * Creates and returns an exact copy of this [VectorValue].
     *
     * @return Exact copy of this [VectorValue].
     */
    fun copy(): VectorValue<T>

    /**
     * Creates and returns a new [VectorValue] of the same type and dimension as this [VectorValue].
     *
     * @return New [VectorValue] of the same type and dimension as this [VectorValue].
     */
    fun new(): VectorValue<T>

    /**
     * Calculates the element-wise sum of this and the other [VectorValue].
     *
     * @param other The [VectorValue] to add to this [VectorValue].
     * @return [VectorValue] that contains the element-wise sum of the two input [VectorValue]s
     */
    operator fun plus(other: VectorValue<*>): VectorValue<T>

    /**
     * Calculates the element-wise difference of this and the other [VectorValue].
     *
     * @param other The [VectorValue] to subtract from this [VectorValue].
     * @return [VectorValue] that contains the element-wise difference of the two input [VectorValue]s
     */
    operator fun minus(other: VectorValue<*>): VectorValue<T>

    /**
     * Calculates the element-wise product of this and the other [VectorValue].
     *
     * @param other The [VectorValue] to multiply this [VectorValue] with.
     * @return [VectorValue] that contains the element-wise product of the two input [VectorValue]s
     */
    operator fun times(other: VectorValue<*>): VectorValue<T>

    /**
     * Calculates the element-wise quotient of this and the other [VectorValue].
     *
     * @param other The [VectorValue] to divide this [VectorValue] by.
     * @return [VectorValue] that contains the element-wise quotient of the two input [VectorValue]s
     */
    operator fun div(other: VectorValue<*>): VectorValue<T>

    operator fun plus(other: NumericValue<*>): VectorValue<T>
    operator fun minus(other: NumericValue<*>): VectorValue<T>
    operator fun times(other: NumericValue<*>): VectorValue<T>
    operator fun div(other: NumericValue<*>): VectorValue<T>

    /**
     * Creates a new [VectorValue] that contains the absolute values this [VectorValue]'s elements.
     *
     * @return [VectorValue] with the element-wise absolute values.
     */
    fun abs(): RealVectorValue<T>

    /**
     * Creates a new [VectorValue] that contains the values this [VectorValue]'s elements raised to the power of x.
     *
     * @param x The exponent for the operation.
     * @return [VectorValue] with the element-wise values raised to the power of x.
     */
    fun pow(x: Int): VectorValue<*>

    /**
     * Creates a new [VectorValue] that contains the square root values this [VectorValue]'s elements.
     *
     * @return [VectorValue] with the element-wise square root values.
     */
    fun sqrt(): VectorValue<*>

    /**
     * Builds the sum of the elements of this [VectorValue].
     *
     * <strong>Warning:</string> This function will make best effort to retain the type of the elements
     * it holds. However, since the value generated by this function might not fit into the type held by
     * this [VectorValue], the [NumericValue] returned by this function may differ from it.
     *
     * @return Sum of the elements of this [VectorValue].
     */
    fun sum(): NumericValue<*>

    /**
     * Calculates the magnitude of this [VectorValue] with respect to the L2 / Euclidean distance.
     */
    fun norm2(): RealValue<*>

    /**
     * Builds the dot product between this and the other [VectorValue].
     *
     * <strong>Warning:</string> This function will make best effort to retain the type of the elements it holds.
     * However, since the value generated by this function might not fit into the type held by this [VectorValue],
     * the [NumericValue] returned by this function may differ from it.
     *
     * @return Sum of the elements of this [VectorValue].
     */
    infix fun dot(other: VectorValue<*>): NumericValue<*>

    /**
     * Special implementation of the L1 / Manhattan distance. Can be overridden to create optimized versions of it.
     *
     * <strong>Warning:</string> This function will make best effort to retain the type of the elements it holds.
     * However, since the value generated by this function might not fit into the type held by this [VectorValue],
     * the [NumericValue] returned by this function may differ from it.
     *
     * @param other The [VectorValue] to calculate the distance to.
     */
    infix fun l1(other: VectorValue<*>): RealValue<*> = ((this - other).abs()).sum().real

    /**
     * Special implementation of the L2 / Euclidean distance. Can be overridden to create optimized versions of it.
     *
     * <strong>Warning:</string> This function will make best effort to retain the type of the elements it holds.
     * However, since the value generated by this function might not fit into the type held by this [VectorValue],
     * the [NumericValue] returned by this function may differ from it.
     *
     * @param other The [VectorValue] to calculate the distance to.
     */
    infix fun l2(other: VectorValue<*>): RealValue<*> = ((this - other).abs().pow(2)).sum().sqrt().real

    /**
     * Special implementation of the LP / Minkowski distance. Can be overridden to create optimized versions of it.
     *
     * <strong>Warning:</string> This function will make best effort to retain the type of the elements it holds.
     * However, since the value generated by this function might not fit into the type held by this [VectorValue],
     * the [NumericValue] returned by this function may differ from it.
     *
     * @param other The [VectorValue] to calculate the distance to.
     */
    fun lp(other: VectorValue<*>, p: Int): RealValue<*> = ((this - other).abs().pow(p)).sum().pow(1.0 / p).real

    /**
     * Special implementation of the Hamming distance. Can be overridden to create optimized versions of it.
     *
     * <strong>Warning:</string> This function will make best effort to retain the type of the elements it holds.
     * However, since the value generated by this function might not fit into the type held by this [VectorValue],
     * the [NumericValue] returned by this function may differ from it.
     *
     * @param other The [VectorValue] to calculate the distance to.
     */
    infix fun hamming(other: VectorValue<*>): RealValue<*> {
        var sum = 0
        for (i in 0 until this.logicalSize) {
            if (other[i] == this[i]) {
                sum += 1
            }
        }
        return IntValue(sum)
    }
}