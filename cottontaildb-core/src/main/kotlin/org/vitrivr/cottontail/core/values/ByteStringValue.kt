package org.vitrivr.cottontail.core.values

import org.vitrivr.cottontail.core.values.types.ScalarValue
import org.vitrivr.cottontail.core.values.types.Types
import org.vitrivr.cottontail.core.values.types.Value

@JvmInline
value class ByteStringValue(override val value: ByteArray) : ScalarValue<ByteArray> {

    companion object {
        val EMPTY = ByteStringValue(ByteArray(0))
    }

    override val logicalSize: Int
        get() = this.value.size

    override val type: Types<*>
        get() = Types.ByteString

    override fun isEqual(other: Value): Boolean = (other is ByteStringValue) && other.value.contentEquals(this.value)

    override fun compareTo(other: Value): Int = if (other is ByteStringValue) {
        this.value.hashCode().compareTo(other.value.hashCode()) //stable but somewhat nonsensical
    } else {
        throw IllegalArgumentException("ByteStringValues can only be compared to other ByteStringValues.")
    }
}