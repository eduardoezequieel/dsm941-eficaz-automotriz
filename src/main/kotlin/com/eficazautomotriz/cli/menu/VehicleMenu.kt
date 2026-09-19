package com.eficazautomotriz.cli.menu

import com.eficazautomotriz.cli.AppContext
import com.eficazautomotriz.cli.format.Column
import com.eficazautomotriz.cli.format.Labels
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.UserRole
import com.eficazautomotriz.domain.validation.TextFields

/** Gestion principal (CRUD) de vehiculos, mas la actualizacion del odometro. */
class VehicleMenu(context: AppContext, actor: User) : Menu(context, actor) {

    // El personal ve los vehiculos de todos los clientes: «Mis» solo aplica al cliente.
    override val title: String = when (actor.role) {
        UserRole.CLIENT -> "Mis vehiculos"
        UserRole.STAFF -> "Vehiculos"
    }

    override fun items(): List<MenuItem> = listOf(
        MenuItem("Listar vehiculos") { list() },
        MenuItem("Agregar vehiculo") { add() },
        MenuItem("Editar vehiculo") { edit() },
        MenuItem("Eliminar vehiculo") { remove() },
        MenuItem("Actualizar kilometraje") { updateMileage() },
    )

    fun list() {
        showTable(
            section = "Vehiculos registrados",
            columns = columns(),
            rows = context.vehicles.listVisibleTo(actor),
            emptyHint = "Aun no hay vehiculos. Use la opcion de agregar.",
        )
        context.reader.pause()
    }

    /** Devuelve el vehiculo elegido de la lista visible para este usuario. */
    fun selectVehicle(prompt: String): Vehicle? = select(
        section = prompt,
        prompt = "Vehiculo",
        options = context.vehicles.listVisibleTo(actor),
        emptyMessage = "No hay vehiculos disponibles.",
    ) { "${it.plate} - ${it.make} ${it.model} (${Labels.mileage(it.currentMileage)})" }

    private fun columns(): List<Column<Vehicle>> {
        val columns = mutableListOf(
            Column<Vehicle>("ID", 8) { it.id },
            Column("PLACA", 10) { it.plate },
            Column("MARCA", 12) { it.make },
            Column("MODELO", 14) { it.model },
            Column("AÑO", 6, alignRight = true) { it.year.toString() },
            Column("KILOMETRAJE", 13, alignRight = true) { Labels.mileage(it.currentMileage) },
            Column("ACTUALIZADO", 12) { Labels.date(it.mileageUpdatedAt) },
        )
        if (actor.role == UserRole.STAFF) {
            columns += Column("PROPIETARIO", 22) { context.auth.nameOf(it.ownerId) }
        }
        return columns
    }

    private fun add() {
        context.writer.section("Nuevo vehiculo")
        val ownerId = resolveOwnerId() ?: return
        val plate = context.reader.readPlate("Placa") ?: return
        val details = readDetails() ?: return
        val mileage = readMileage("Kilometraje actual") ?: return
        val today = context.today()

        report(
            context.vehicles.register(
                actor, ownerId, plate, details.make, details.model, details.year, mileage, today,
            )
        ) { "Vehiculo ${it.id} registrado: ${it.plate} ${it.make} ${it.model} ${it.year}." }
        context.reader.pause()
    }

    private fun edit() {
        val vehicle = selectVehicle("Elija el vehiculo a editar") ?: return
        context.writer.hint("Datos actuales: ${vehicle.make} ${vehicle.model} ${vehicle.year}")
        val details = readDetails() ?: return

        report(
            context.vehicles.update(actor, vehicle.id, details.make, details.model, details.year, context.today())
        ) { "Vehiculo ${it.id} actualizado: ${it.make} ${it.model} ${it.year}." }
        context.reader.pause()
    }

    private fun remove() {
        val vehicle = selectVehicle("Elija el vehiculo a eliminar") ?: return
        if (!context.reader.confirm("Eliminar ${vehicle.plate} y su historial de mantenimiento")) {
            context.writer.warning("Operacion cancelada.")
            return
        }

        report(context.vehicles.delete(actor, vehicle.id)) { "Vehiculo ${it.id} (${it.plate}) eliminado." }
        context.reader.pause()
    }

    private fun updateMileage() {
        val vehicle = selectVehicle("Elija el vehiculo") ?: return
        context.writer.hint("Kilometraje actual: ${Labels.mileage(vehicle.currentMileage)}")
        val mileage = readMileage("Nuevo kilometraje") ?: return

        report(
            context.vehicles.updateMileage(actor, vehicle.id, mileage, context.today())
        ) { "Kilometraje de ${it.plate} actualizado a ${Labels.mileage(it.currentMileage)}." }
        context.reader.pause()
    }

    /** Los tres campos que alta y edicion piden por igual. */
    private fun readDetails(): VehicleDetails? {
        val make = context.reader.readText("Marca", TextFields.MAKE) ?: return null
        val model = context.reader.readText("Modelo", TextFields.MODEL) ?: return null
        // El maximo es el ano entrante: los modelos se venden adelantados al calendario.
        val year = context.reader.readInt("Año", context.config.minVehicleYear, context.today().year + 1)
            ?: return null
        return VehicleDetails(make, model, year)
    }

    private fun readMileage(prompt: String): Int? =
        context.reader.readInt(prompt, 0, context.config.maxMileage)

    /** El personal puede registrar a nombre de un cliente; el cliente solo a nombre propio. */
    private fun resolveOwnerId(): String? = when (actor.role) {
        UserRole.CLIENT -> actor.id
        UserRole.STAFF -> {
            val clients = context.auth.availableUsers().filter { it.role == UserRole.CLIENT }
            context.reader.readChoice("Propietario", clients) { "${it.name} (${it.id})" }?.id
        }
    }

    private data class VehicleDetails(val make: String, val model: String, val year: Int)
}
