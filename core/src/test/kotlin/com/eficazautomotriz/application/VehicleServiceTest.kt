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
    fun `the client sees only their own vehicles and staff see all of them`() {
        val env = TestEnvironment()

        val ofClient = env.vehicleService.listVisibleTo(env.firstClient)
        val ofStaff = env.vehicleService.listVisibleTo(env.staff)

        assertEquals(3, ofClient.size)
        assertTrue(ofClient.all { it.ownerId == "USR-001" })
        assertEquals(4, ofStaff.size)
    }

    @Test
    fun `registering a vehicle assigns an identifier with a prefix`() {
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
    fun `a duplicate plate is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P123456", "Mazda", "3", 2021, 12_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `a plate owned by another client is rejected as duplicate, in any case`() {
        val env = TestEnvironment()
        val othersPlate = requireNotNull(env.repositories.vehicles.findById("VEH-004")).plate

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", othersPlate.lowercase(), "Mazda", "3", 2021, 12_000, env.today,
        )

        val invalid = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.DuplicatePlate>(invalid.validation)
    }

    @Test
    fun `a plate with spaces is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P 4455", "Mazda", "3", 2021, 12_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `a plate that is too long is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P12332231213123123", "Honda", "Fit", 2026, 30_000, env.today,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.InvalidPlateFormat>(error.validation)
    }

    @Test
    fun `a plate with no type letter is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "123456", "Honda", "Fit", 2021, 12_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `an alphanumeric plate from El Salvador is accepted`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "p12300a", "Honda", "Fit", 2021, 12_000, env.today,
        )

        val vehicle = assertIs<Outcome.Success<Vehicle>>(result).value
        assertEquals("P12300A", vehicle.plate)
    }

    @Test
    fun `the user's alphanumeric examples are accepted and stored without the hyphen`() {
        val env = TestEnvironment()

        val first = env.vehicleService.register(
            env.firstClient, "USR-001", "P123-AB2", "Honda", "Fit", 2021, 12_000, env.today,
        )
        val second = env.vehicleService.register(
            env.firstClient, "USR-001", "pa12-bb3", "Honda", "Civic", 2022, 8_000, env.today,
        )

        assertEquals("P123AB2", assertIs<Outcome.Success<Vehicle>>(first).value.plate)
        assertEquals("PA12BB3", assertIs<Outcome.Success<Vehicle>>(second).value.plate)
    }

    @Test
    fun `a plate that does not start with P is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "A123456", "Honda", "Fit", 2021, 12_000, env.today,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.InvalidPlateFormat>(error.validation)
    }

    @Test
    fun `a duplicate typed with the display hyphen is still a duplicate`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P123-456", "Mazda", "3", 2021, 12_000, env.today,
        )

        val invalid = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.DuplicatePlate>(invalid.validation)
    }

    @Test
    fun `a year before the configured minimum is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P778899", "Ford", "T", 1930, 1_000, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `a year after next year is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P778899", "Ford", "Ranger", env.today.year + 2, 0, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `a negative mileage is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P778899", "Ford", "Ranger", 2020, -1, env.today,
        )

        assertIs<DomainError.Invalid>(result.errorOrNull())
    }

    @Test
    fun `a client cannot look up someone else's vehicle`() {
        val env = TestEnvironment()

        val result = env.vehicleService.findAccessible(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `a client cannot delete someone else's vehicle`() {
        val env = TestEnvironment()

        val result = env.vehicleService.delete(env.secondClient, "VEH-001")

        assertIs<DomainError.Forbidden>(result.errorOrNull())
        assertTrue(env.repositories.vehicles.findById("VEH-001") != null)
    }

    @Test
    fun `a client cannot update the mileage of someone else's vehicle`() {
        val env = TestEnvironment()

        val result = env.vehicleService.updateMileage(env.secondClient, "VEH-001", 40_000, env.today)

        assertIs<DomainError.Forbidden>(result.errorOrNull())
    }

    @Test
    fun `staff can reach any client's vehicle`() {
        val env = TestEnvironment()

        val result = env.vehicleService.findAccessible(env.staff, "VEH-001")

        assertIs<Outcome.Success<Vehicle>>(result)
    }

    @Test
    fun `mileage cannot decrease`() {
        val env = TestEnvironment()

        val result = env.vehicleService.updateMileage(env.firstClient, "VEH-001", 29_999, env.today)

        val error = assertIs<DomainError.MileageDecrease>(result.errorOrNull())
        assertEquals(30_000, error.lastKnown)
    }

    @Test
    fun `updating the mileage to the same value is allowed`() {
        val env = TestEnvironment()

        val result = env.vehicleService.updateMileage(env.firstClient, "VEH-001", 30_000, env.today)

        assertIs<Outcome.Success<Vehicle>>(result)
    }

    @Test
    fun `deleting a vehicle removes its maintenance records with it`() {
        val env = TestEnvironment()

        val result = env.vehicleService.delete(env.firstClient, "VEH-001")

        assertIs<Outcome.Success<Vehicle>>(result)
        assertNull(env.repositories.vehicles.findById("VEH-001"))
        assertTrue(env.repositories.maintenanceRecords.findByVehicle("VEH-001").isEmpty())
    }

    @Test
    fun `a vehicle that does not exist returns NotFound`() {
        val env = TestEnvironment()

        val result = env.vehicleService.findAccessible(env.staff, "VEH-999")

        assertIs<DomainError.NotFound>(result.errorOrNull())
    }

    // --- Limits on the text fields and the numeric fields ---

    @Test
    fun `a make longer than its limit is rejected`() {
        val env = TestEnvironment()
        val tooLong = "M".repeat(TextFields.MAKE.maxLength + 1)

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P445566", tooLong, "3", 2021, 12_000, env.today,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.TextTooLong>(error.validation)
    }

    @Test
    fun `a model with characters that are not allowed is rejected`() {
        val env = TestEnvironment()

        val result = env.vehicleService.register(
            env.firstClient, "USR-001", "P445566", "Mazda", "<script>", 2021, 12_000, env.today,
        )

        val error = assertIs<DomainError.Invalid>(result.errorOrNull())
        assertIs<ValidationError.InvalidCharacters>(error.validation)
    }

    @Test
    fun `a mileage above the ceiling is rejected the same way as a negative one`() {
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
    fun `the configured minimum year is accepted and the year before it is not`() {
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
    fun `editing a vehicle applies the same limits as registering it`() {
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