package org.vitrivr.cottontail.functions.exception

import org.vitrivr.cottontail.functions.basics.Signature

/**
 * A exception thrown by the [FunctionRegistry] if a function lookup fails.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class FunctionNotFoundException(signature: Signature<*>): Throwable("Function with signature $signature not found in registry.")