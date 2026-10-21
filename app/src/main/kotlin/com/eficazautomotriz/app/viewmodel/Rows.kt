package com.eficazautomotriz.app.viewmodel

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.application.ServiceOrderDetail
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.AppointmentStatus

/**
 * Display-ready rows: names and slots are resolved in the ViewModel (on IO), never from a
 * Composable, because each lookup may hit Firestore.
 */
data class AppointmentRow(
    val appointment: Appointment,
    val vehicle: Vehicle?,
    val serviceName: String,
    val slot: TimeSlot?,
    /** Only in staff views, which handle appointments from many clients. */
    val clientName: String? = null,
) {
    /**
     * Pending and confirmed appointments offer "Cancelar cita"; a rescheduled one is answered
     * with accept/decline instead (the service checks both again).
     */
    val cancellable: Boolean
        get() = appointment.status == AppointmentStatus.PENDING || appointment.status == AppointmentStatus.CONFIRMED

    /** The shop moved the appointment: the client accepts the new date or declines it. */
    val canRespondToReschedule: Boolean get() = appointment.status == AppointmentStatus.RESCHEDULED
}

/** Order in staff lists (without evidence: `listAll` does not return it). */
data class StaffOrderRow(
    val order: ServiceOrder,
    val serviceName: String,
    val vehicle: Vehicle?,
)

data class ServiceOrderRow(
    val detail: ServiceOrderDetail,
    val serviceName: String,
)

/** Must be called inside a `run` block (IO thread). */
internal fun AppGraph.appointmentRows(
    appointments: List<Appointment>,
    vehicles: List<Vehicle>,
    withClientNames: Boolean = false,
): List<AppointmentRow> {
    val vehiclesById = vehicles.associateBy { it.id }
    val serviceNames = mutableMapOf<String, String>()
    val slots = mutableMapOf<String, TimeSlot?>()
    val clientNames = mutableMapOf<String, String>()
    return appointments.map { appointment ->
        AppointmentRow(
            appointment = appointment,
            vehicle = vehiclesById[appointment.vehicleId],
            serviceName = serviceNames.getOrPut(appointment.serviceTypeId) {
                appointmentService.serviceTypeName(appointment.serviceTypeId)
            },
            slot = slots.getOrPut(appointment.slotId) { appointmentService.slotOf(appointment.slotId) },
            clientName = if (withClientNames) {
                clientNames.getOrPut(appointment.clientId) { authService.nameOf(appointment.clientId) }
            } else {
                null
            },
        )
    }
}

/** Must be called inside a `run` block (IO thread). */
internal fun AppGraph.serviceOrderRows(details: List<ServiceOrderDetail>): List<ServiceOrderRow> {
    val names = mutableMapOf<String, String>()
    return details.map { detail ->
        ServiceOrderRow(
            detail,
            names.getOrPut(detail.order.serviceTypeId) { serviceOrderService.serviceTypeName(detail.order.serviceTypeId) },
        )
    }
}

/** Must be called inside a `run` block (IO thread). */
internal fun AppGraph.staffOrderRows(orders: List<ServiceOrder>, vehicles: List<Vehicle>): List<StaffOrderRow> {
    val vehiclesById = vehicles.associateBy { it.id }
    val names = mutableMapOf<String, String>()
    return orders.map { order ->
        StaffOrderRow(
            order,
            names.getOrPut(order.serviceTypeId) { serviceOrderService.serviceTypeName(order.serviceTypeId) },
            vehiclesById[order.vehicleId],
        )
    }
}
