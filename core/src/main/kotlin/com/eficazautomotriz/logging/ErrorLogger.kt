package com.eficazautomotriz.logging

/**
 * Contract for recording events. The application depends on this interface and not on the
 * concrete implementation: in stage 3 it can be replaced by an Android logger
 * without touching the use cases.
 */
interface ErrorLogger {

    /** Expected business errors: the operation was rejected by a rule, not by a failure. */
    fun warn(context: String, message: String, data: String = "")

    /** Unexpected failures: exceptions the program did not anticipate. */
    fun error(context: String, message: String, data: String = "")
}
