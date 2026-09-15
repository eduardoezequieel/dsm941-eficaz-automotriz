package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Column
import com.eficazautomotriz.cli.format.Labels
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.User

/**
 * Consulta y configuracion de los catalogos que alimentan las reglas de negocio.
 * El personal ajusta aqui la agenda del taller: que franjas admiten citas y cuantas
 * caben en cada una. Los dos parametros los lee `AppointmentAvailabilityRule`.
 */
class CatalogMenu(context: AppContext, actor: User) : Menu(context, actor) {

    override val title: String = "Catalogos"

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Tipos de servicio") { showServiceTypes() },
        MenuItem("Tipos de mantenimiento") { showMaintenanceTypes() },
        MenuItem("Franjas horarias") { showTimeSlots() },
        MenuItem("Habilitar o deshabilitar una franja") { toggleSlot() },
        MenuItem("Cambiar la capacidad de una franja") { changeSlotCapacity() },
    )

    private fun showServiceTypes() {
        val columns = listOf(
            Column<ServiceType>("ID", 8) { it.id },
            Column("NOMBRE", 26) { it.name },
            Column("DESCRIPCION", 46) { it.description },
        )
        showTable(
            "Tipos de servicio activos",
            columns,
            context.appointments.activeServiceTypes(),
            "No hay tipos de servicio activos.",
        )
        context.reader.pause()
    }

    private fun showMaintenanceTypes() {
        val columns = listOf(
            Column<MaintenanceType>("ID", 8) { it.id },
            Column("NOMBRE", 30) { it.name },
            Column("INTERVALO", 12, alignRight = true) { Labels.mileage(it.intervalKm) },
        )
        showTable(
            "Tipos de mantenimiento activos",
            columns,
            context.maintenance.activeTypes(),
            "No hay tipos de mantenimiento activos.",
        )
        context.writer.hint("El intervalo alimenta la regla de mantenimiento; el umbral de aviso vive en SystemConfig.")
        context.reader.pause()
    }

    private fun showTimeSlots() {
        val columns = listOf(
            Column<TimeSlot>("ID", 8) { it.id },
            Column("DIA", 12) { Labels.dayOfWeek(it.dayOfWeek) },
            Column("HORARIO", 14) { Labels.slotRange(it) },
            Column("CAPACIDAD", 10, alignRight = true) { it.capacity.toString() },
            Column("HABILITADA", 11) { if (it.enabled) "Si" else "No" },
        )
        showTable(
            "Franjas horarias configuradas",
            columns,
            context.appointments.slotCatalog(),
            "No hay franjas configuradas.",
        )
        context.writer.hint("El domingo no aparece porque el taller no atiende ese dia.")
        context.writer.hint("Capacidad 0 significa que la franja usa la capacidad por omision del sistema.")
        context.reader.pause()
    }

    /**
     * Una franja deshabilitada la rechaza la primera comprobacion de la regla de
     * disponibilidad; las citas ya agendadas en ella siguen en pie.
     */
    private fun toggleSlot() {
        val slot = selectSlot("Elija la franja a habilitar o deshabilitar") ?: return
        val enabled = !slot.enabled
        val action = if (enabled) "Habilitar" else "Deshabilitar"
        if (!context.reader.confirm("$action la franja ${slot.id} (${Labels.dayOfWeek(slot.dayOfWeek)} ${Labels.slotRange(slot)})")) {
            context.writer.warning("Operacion cancelada.")
            context.reader.pause()
            return
        }

        report(context.appointments.setSlotEnabled(actor, slot.id, enabled)) {
            "Franja ${it.id} ahora esta ${if (it.enabled) "habilitada" else "deshabilitada"}."
        }
        if (!enabled) {
            context.writer.hint("Las citas ya agendadas en esta franja no se cancelan: solo deja de admitir nuevas.")
        }
        context.reader.pause()
    }

    /**
     * Reducir la capacidad no expulsa citas ya agendadas: la regla solo bloquea nuevas
     * solicitudes mientras las ocupantes igualen o superen el nuevo valor.
     */
    private fun changeSlotCapacity() {
        val slot = selectSlot("Elija la franja a reconfigurar") ?: return
        context.writer.hint(
            "Capacidad declarada: ${slot.capacity}. Capacidad efectiva: " +
                "${context.appointments.effectiveCapacityOf(slot)}."
        )
        context.writer.hint("Escriba 0 para que la franja use la capacidad por omision del sistema.")
        val capacity = context.reader.readInt("Nueva capacidad", 0, context.config.maxSlotCapacity) ?: return

        report(context.appointments.setSlotCapacity(actor, slot.id, capacity)) {
            "Franja ${it.id} configurada con capacidad ${it.capacity} " +
                "(efectiva ${context.appointments.effectiveCapacityOf(it)})."
        }
        context.writer.hint("Reducir la capacidad no cancela citas ya agendadas; solo frena las nuevas.")
        context.reader.pause()
    }

    private fun selectSlot(section: String): TimeSlot? = select(
        section = section,
        prompt = "Franja",
        options = context.appointments.slotCatalog(),
        emptyMessage = "No hay franjas configuradas.",
    ) {
        "${it.id}  ${Labels.dayOfWeek(it.dayOfWeek)}  ${Labels.slotRange(it)}  " +
            "capacidad ${it.capacity}  ${if (it.enabled) "habilitada" else "deshabilitada"}"
    }
}