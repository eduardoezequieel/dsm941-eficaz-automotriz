package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.UserRole
import com.eficazautomotriz.logging.ErrorLogger

/**
 * Shared base for the use cases: it centralizes rejecting an operation and
 * recording it in the log, so no service forgets to leave a trace of an error.
 */
abstract class ApplicationService(protected val logger: ErrorLogger) {

    /** Returns the business error and logs it as WARN with the operation context. */
    protected fun fail(operation: String, error: DomainError, data: String = ""): Outcome<Nothing> {
        logger.warn("${javaClass.simpleName}.$operation", describe(error), data)
        return Outcome.failure(error)
    }

    protected fun failInvalid(
        operation: String,
        validation: ValidationError,
        data: String = "",
    ): Outcome<Nothing> = fail(operation, DomainError.Invalid(validation), data)

    /** The permission is checked here and not only in the menu: the menu is a convenience. */
    protected fun requireStaff(operation: String, actor: User, action: String): Outcome<Unit> =
        if (actor.role == UserRole.STAFF) Outcome.success(Unit)
        else fail(operation, DomainError.Forbidden(action), "userId=${actor.id} role=${actor.role}")

    /** A client reaches only their own data; staff reach everyone's. */
    protected fun requireOwnershipOrStaff(
        operation: String,
        actor: User,
        ownerId: String,
        action: String,
    ): Outcome<Unit> =
        if (actor.role == UserRole.STAFF || actor.id == ownerId) Outcome.success(Unit)
        else fail(operation, DomainError.Forbidden(action), "userId=${actor.id} ownerId=$ownerId")

    private fun describe(error: DomainError): String = when (error) {
        is DomainError.SlotUnavailable -> "SlotUnavailable(${error.reason})"
        is DomainError.InvalidTransition -> "InvalidTransition(${error.from}->${error.to})"
        is DomainError.MileageDecrease -> "MileageDecrease(${error.attempted}<${error.lastKnown})"
        is DomainError.NotFound -> "NotFound(${error.entity}:${error.id})"
        is DomainError.Forbidden -> "Forbidden(${error.action})"
        is DomainError.MissingBaseline -> "MissingBaseline(${error.vehicleId})"
        is DomainError.Invalid -> "Invalid(${error.validation.field})"
    }
}
