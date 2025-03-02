package org.vitrivr.cottontail.server

import io.grpc.ServerBuilder
import org.vitrivr.cottontail.config.Config
import org.vitrivr.cottontail.dbms.catalogue.DefaultCatalogue
import org.vitrivr.cottontail.dbms.execution.ExecutionManager
import org.vitrivr.cottontail.dbms.execution.transactions.TransactionManager
import org.vitrivr.cottontail.server.grpc.services.DDLService
import org.vitrivr.cottontail.server.grpc.services.DMLService
import org.vitrivr.cottontail.server.grpc.services.DQLService
import org.vitrivr.cottontail.server.grpc.services.TXNService
import kotlin.time.ExperimentalTime

/**
 * Main server class for Cottontail DB. This is where all comes together!
 *
 * @author Ralph Gasser
 * @version 1.3.0
 */
@ExperimentalTime
class CottontailServer(config: Config) {

    /** The [DefaultCatalogue] instance used by this [CottontailServer]. */
    internal val catalogue = DefaultCatalogue(config)

    /** The [ExecutionManager] used for handling gRPC calls and executing queries. */
    private val executor = ExecutionManager(config)

    /** The [TransactionManager] used by this [CottontailServer] instance. */
    private val transactionManager: TransactionManager = TransactionManager(this.executor, config.execution.transactionTableSize, config.execution.transactionHistorySize, this.catalogue)

    /** The internal gRPC server; if building that server fails then the [DefaultCatalogue] is closed again! */
    private val grpc = ServerBuilder.forPort(config.server.port)
        .executor(this.executor.connectionWorkerPool)
        .addService(DDLService(this.catalogue, this.transactionManager))
        .addService(DMLService(this.catalogue, this.transactionManager))
        .addService(DQLService(this.catalogue, this.transactionManager))
        .addService(TXNService(this.catalogue, this.transactionManager))
        .let {
            if (config.server.useTls) {
                val certFile = config.server.certFile!!.toFile()
                val privateKeyFile = config.server.privateKey!!.toFile()
                it.useTransportSecurity(certFile, privateKeyFile)
            } else {
                it
            }
        }.build()


    /* Start server and close catalogue upon failure. */
    init {
        try {
            this.grpc.start()
        } catch (e: Throwable) {
            this.executor.shutdownAndWait()
            this.catalogue.close()
            throw e
        }
    }

    /**
     * Returns true if this [CottontailServer] is currently running, and false otherwise.
     */
    @Volatile
    var isRunning: Boolean = true
        private set

    /**
     * Stops this instance of [CottontailServer].
     */
    fun shutdownAndWait() {
        if (this.isRunning) {
            /* Shutdown gRPC server. */
            this.grpc.shutdown().awaitTermination()

            /* Shutdown thread pool executor. */
            this.executor.shutdownAndWait()

            /* Close catalogue. */
            this.catalogue.close()

            /* Update flag. */
            this.isRunning = false
        }

    }
}