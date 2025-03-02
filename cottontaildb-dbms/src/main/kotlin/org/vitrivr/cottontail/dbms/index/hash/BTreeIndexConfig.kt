package org.vitrivr.cottontail.dbms.index.hash

import jetbrains.exodus.bindings.ComparableBinding
import jetbrains.exodus.util.LightOutputStream
import org.vitrivr.cottontail.dbms.index.IndexConfig
import java.io.ByteArrayInputStream

/**
 * The [IndexConfig] for the [BTreeIndex].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object BTreeIndexConfig: IndexConfig<BTreeIndex>, ComparableBinding() {
    override fun readObject(stream: ByteArrayInputStream): Comparable<BTreeIndexConfig> = BTreeIndexConfig
    override fun writeObject(output: LightOutputStream, `object`: Comparable<BTreeIndexConfig>) {
        /* No op. */
    }
}