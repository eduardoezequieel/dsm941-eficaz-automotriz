package com.eficazautomotriz.app.ui.navigation

/** Navigation routes (plan 4.2). */
object Routes {
    const val ARG_ID = "id"

    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"

    const val CLIENT_HOME = "client/home"
    const val CLIENT_VEHICLES = "client/vehicles"
    const val CLIENT_VEHICLE_NEW = "client/vehicles/new"
    const val CLIENT_VEHICLE_DETAIL = "client/vehicles/{id}"
    const val CLIENT_VEHICLE_MILEAGE = "client/vehicles/{id}/mileage"
    const val CLIENT_VEHICLE_HISTORY = "client/vehicles/{id}/history"
    const val CLIENT_APPOINTMENTS = "client/appointments"
    const val CLIENT_APPOINTMENT_NEW = "client/appointments/new"
    const val CLIENT_APPOINTMENT_DETAIL = "client/appointments/{id}"
    const val CLIENT_SERVICE_DETAIL = "client/services/{id}"
    /** Combined service history of all the client's vehicles (bottom bar "Historial"). */
    const val CLIENT_HISTORY = "client/history"
    const val CLIENT_PROFILE = "client/profile"

    const val STAFF_PANEL = "staff/panel"
    const val STAFF_APPOINTMENT_DETAIL = "staff/appointments/{id}"
    const val ARG_APPOINTMENT_ID = "appointmentId"
    /** The optional argument preloads vehicle and service from a confirmed appointment. */
    const val STAFF_ORDER_NEW = "staff/orders/new?appointmentId={appointmentId}"
    const val STAFF_ORDER_DETAIL = "staff/orders/{id}"
    const val STAFF_ORDER_EVIDENCE = "staff/orders/{id}/evidence"
    const val STAFF_ORDER_COMPLETE = "staff/orders/{id}/complete"
    const val STAFF_REPORTS = "staff/reports"
    /** All service orders (bottom bar "Servicios"), with the "Nueva orden" FAB. */
    const val STAFF_SERVICES = "staff/services"
    const val STAFF_PROFILE = "staff/profile"
    /** Reached from the staff profile; no longer a bottom bar tab. */
    const val STAFF_SETTINGS = "staff/settings"
    /** Still registered so an old deep link does not crash; slots are edited from [STAFF_SETTINGS]. */
    const val STAFF_SLOTS = "staff/slots"

    fun vehicleDetail(id: String) = "client/vehicles/$id"
    fun vehicleMileage(id: String) = "client/vehicles/$id/mileage"
    fun vehicleHistory(id: String) = "client/vehicles/$id/history"
    fun appointmentDetail(id: String) = "client/appointments/$id"
    fun serviceDetail(id: String) = "client/services/$id"

    fun staffAppointment(id: String) = "staff/appointments/$id"
    fun newOrder(appointmentId: String? = null) =
        if (appointmentId == null) "staff/orders/new" else "staff/orders/new?appointmentId=$appointmentId"
    fun orderDetail(id: String) = "staff/orders/$id"
    fun orderEvidence(id: String) = "staff/orders/$id/evidence"
    fun orderComplete(id: String) = "staff/orders/$id/complete"

    /** Bottom bar destinations (mockups f2/f5 and f8/f9): no back arrow is shown on them. */
    val CLIENT_TABS = listOf(CLIENT_HOME, CLIENT_VEHICLES, CLIENT_APPOINTMENTS, CLIENT_HISTORY, CLIENT_PROFILE)
    val STAFF_TABS = listOf(STAFF_PANEL, STAFF_SERVICES, STAFF_REPORTS, STAFF_PROFILE)
}
