package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.errorOrNull
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.validation.TextFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleServiceTest {

    @Test
    fun `el cliente solo ve sus propios vehiculos y el personal los ve todos`() {
        val env = TestEnvironment()

        val ofClient = env.vehicleService.listVisibleTo(env.firstClient)
        val ofStaff = env.vehicleService.listVisibleTo(env.staff)

        assertEquals(3, ofClient.size)
        assertTrue(ofClient.all { it.ownerId == "USR-001" })
        assertEquals(4, ofStaff.size)
    }

    @Test
    fun `registrar un vehiculo asigna un identificador con prefijo`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P445566", "Mazda", "3", 2021, 12_000, env.today,
        )

        val vehicle = assertIs<Outcome.Success<Vehicle>>(result).value
        assertTrue(vehicle.id.startsWith("VEH-"))
        assertEquals("P445566", vehicle.plate)
        assertEquals(4, env.vehicleService.listVisibleTo(env.firstClient).size)
    }

    @Test
    fun `una placa repetida se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P123456", "Mazda", "3", 2021, 12_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `una placa con espacios se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P 4455", "Mazda", "3", 2021, 12_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `una placa demasiado larga se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P12332231213123123", "Honda", "Fit", 2026, 30_000, env.today,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.InvalidPlateFormat>(error.validation)
    }

    @Test
    fun `una placa sin letra de tipo se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "123456", "Honda", "Fit", 2021, 12_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `una placa alfanumerica de El Salvador se acepta`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "p12300a", "Honda", "Fit", 2021, 12_000, env.today,
        )

        val vehicle = assertIs<Outcome.Success<Vehicle>>(result).value
        assertEquals("P12300A", vehicle.plate)
    }

    @Test
    fun `un año anterior al minimo configurado se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P778899", "Ford", "T", 1930, 1_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `un año posterior al proximo se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P778899", "Ford", "Ranger", env.today.year + 2, 0, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `un kilometraje negativo se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P778899", "Ford", "Ranger", 2020, -1, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `un cliente no puede consultar un vehiculo ajeno`() {
        val env = TestEnvironment()

        val result = env.vehicleService.findAccessible(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `un cliente no puede eliminar un vehiculo ajeno`() {
        val env = TestEnvironment()

        val result = env.vehicleService.delete(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
        assertTrue(env.repositories.vehicles.findById("VEH-001") != null)
    }

    @Test
    fun `un cliente no puede actualizar el kilometraje de un vehiculo ajeno`() {
        val env = TestEnvironment()

        val result = env.vehicleService.updateMileage(env.secondClient, "VEH-001", 40_000, env.today)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `el personal si alcanza el vehiculo de cualquier cliente`() {
        val env = TestEnvironment()

        val result = env.vehicleService.findAccessible(env.staff, "VEH-001")

        assertIs<Outcome.Success<Vehicle>>(result)
    }

    @Test
    fun `el kilometraje no puede decrecer`() {
        val env = TestEnvironment()

        val result = env.vehicleService.updateMileage(env.firstClient, "VEH-001", 29_999, env.today)

        val error = assertIs<DomainError.MileageDecrease>(result.errorOrNull())
        assertEquals(30_000, error.lastKnown)
    }

    @Test
    fun `actualizar el kilometraje al mismo valor se permite`() {
        val env = TestEnvironment()

        val result = env.vehicleService.updateMileage(env.firstClient, "VEH-001", 30_000, env.today)

        assertIs<Outcome.Success<Vehicle>>(result)
    }

    @Test
    fun `eliminar un vehiculo arrastra sus registros de mantenimiento`() {
        val env = TestEnvironment()

        val result = env.vehicleService.delete(env.firstClient, "VEH-001")

        assertIs<Outcome.Success<Vehicle>>(result)
        assertNull(env.repositories.vehicles.findById("VEH-001"))
        assertTrue(env.repositories.maintenanceRecords.findByVehicle("VEH-001").isEmpty())
    }

    @Test
    fun `un vehiculo inexistente devuelve NotFound`() {
        val env = TestEnvironment()

        val result = env.vehicleService.findAccessible(env.staff, "VEH-999")

        assertIs<DomainError.NotFound>(result.errorOrNull())
    }

    // --- Limites de los campos de texto y numericos ---

    @Test
    fun `una marca mas larga que su limite se rechaza`() {
        val env = TestEnvironment()
        val tooLong = "M".repeat(TextFields.MAKE.maxLength + 1)

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P445566", tooLong, "3", 2021, 12_000, env.today,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.TextTooLong>(error.validation)
    }

    @Test
    fun `un modelo con caracteres no permitidos se rechaza`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P445566", "Mazda", "<script>", 2021, 12_000, env.today,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.InvalidCharacters>(error.validation)
    }

    @Test
    fun `un kilometraje por encima del techo se rechaza igual que uno negativo`() {
        val env = TestEnvironment()

        val above = env.vehicleService.register(
            env.firstClient, "USR-001", "P445566", "Mazda", "3", 2021, env.config.maxMileage + 1, env.today,
        )
        val negative = env.vehicleService.register(
            env.firstClient, "USR-001", "P445567", "Mazda", "3", 2021, -1, env.today,
        )

        assertIs<ValidationError.MileageOutOfRange>(assertIs<DomainError.Invalid>(above.errorOrNull()).validation)
        assertIs<ValidationError.MileageOutOfRange>(assertIs<DomainError.Invalid>(negative.errorOrNull()).validation)
    }

    @Test
    fun `el año minimo configurado se acepta y el anterior no`() {
        val env = TestEnvironment()
        val minimum = env.config.minVehicleYear

        val accepted = env.vehicleService.register(
            env.firstClient, "USR-001", "P445566", "Ford", "F-100", minimum, 12_000, env.today,
        )
        val rejected = env.vehicleService.register(
            env.firstClient, "USR-001", "P445567", "Ford", "F-100", minimum - 1, 12_000, env.today,
        )

        assertIs<Outcome.Success<Vehicle>>(accepted)
        assertIs<ValidationError.YearOutOfRange>(assertIs<DomainError.Invalid>(rejected.errorOrNull()).validation)
    }

    @Test
    fun `editar un vehiculo aplica los mismos limites que darlo de alta`() {
        val env = TestEnvironment()
        val vehicle = env.vehicleService.listVisibleTo(env.firstClient).first()

        val blank = env.vehicleService.update(env.firstClient, vehicle.id, "  ", "Corolla", 2021, env.today)
        val tooLong = env.vehicleService.update(
            env.firstClient, vehicle.id, "Toyota", "C".repeat(TextFields.MODEL.maxLength + 1), 2021, env.today,
        )
        val valid = env.vehicleService.update(env.firstClient, vehicle.id, "Toyota", "Corolla", 2021, env.today)

        assertIs<ValidationError.BlankText>(assertIs<DomainError.Invalid>(blank.errorOrNull()).validation)
        assertIs<ValidationError.TextTooLong>(assertIs<DomainError.Invalid>(tooLong.errorOrNull()).validation)
        assertEquals("Corolla", assertIs<Outcome.Success<Vehicle>>(valid).value.model)
    }
}
