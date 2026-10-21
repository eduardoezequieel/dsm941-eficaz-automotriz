package com.eficazautomotriz.app.viewmodel.validation

import com.eficazautomotriz.domain.config.SystemConfig
import com.eficazautomotriz.domain.validation.TextFields
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Locale

/** Early validation of the client forms, without Android. */
class FormValidatorsTest {

    private val config = SystemConfig()
    private val today = LocalDate.of(2026, 10, 14)
    private val auth = AuthFormValidator { Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(it) }

    // --- Authentication ---

    @Test
    fun `login requires a valid email and a password`() {
        val errors = auth.login("ana-sin-arroba", "")
        assertNotNull(errors["email"])
        assertEquals("Escriba su contraseña.", errors["password"])
        assertTrue(auth.login("  ana@ejemplo.com  ", "secreto").isValid)
    }

    @Test
    fun `email is trimmed, rejects spaces and stops at 254 characters`() {
        assertNotNull(auth.login("ana @ejemplo.com", "secreto")["email"])
        assertEquals("Correo inválido. Ejemplo: nombre@dominio.com", auth.email("ana-sin-arroba"))
        assertNull(auth.email("  ana@ejemplo.com  "))
        val atLimit = "a".repeat(TextFields.EMAIL.maxLength - "@b.co".length) + "@b.co"
        assertEquals(TextFields.EMAIL.maxLength, atLimit.length)
        assertNull(auth.email(atLimit))
        assertNotNull(auth.email(atLimit + "x"))
        assertTrue(auth.email(atLimit + "x")!!.contains(TextFields.EMAIL.maxLength.toString()))
    }

    @Test
    fun `password allows 6 to 64 characters and the confirmation must match`() {
        val short = auth.register("Ana Melara", "70123456", "ana@ejemplo.com", "12345", "54321")
        assertNotNull(short["password"])
        assertEquals("Las contraseñas no coinciden.", short["confirmation"])
        assertNull(short["name"])
        assertNull(short["phone"])

        assertTrue(auth.login("ana@ejemplo.com", "123456").isValid)
        val max = "a".repeat(TextFields.PASSWORD.maxLength)
        assertTrue(auth.register("Ana Melara", "70123456", "ana@ejemplo.com", max, max).isValid)
        assertNotNull(auth.login("ana@ejemplo.com", max + "a")["password"])
        assertNotNull(auth.login("ana@ejemplo.com", "12345")["password"])
    }

    @Test
    fun `sign-up validates name and a salvadoran phone`() {
        val errors = auth.register("Ana <script>", "70a2", "ana@ejemplo.com", "secreto1", "secreto1")
        assertNotNull(errors["name"])
        assertNotNull(errors["phone"])
        assertTrue(errors["phone"]!!.contains("7123-4567"))

        assertTrue(auth.register("José Peña-López", "70123456", "ana@ejemplo.com", "secreto1", "secreto1").isValid)
        assertTrue(auth.register("  O'Brien  ", "22501188", "ana@ejemplo.com", "secreto1", "secreto1").isValid)
        assertTrue(auth.register("Ña", "61234567", "ana@ejemplo.com", "secreto1", "secreto1").isValid)

        assertTrue(auth.register("a", "71234567", "ana@ejemplo.com", "secreto1", "secreto1")["name"]!!.contains("2 letras"))
        assertTrue(auth.register("...", "71234567", "ana@ejemplo.com", "secreto1", "secreto1")["name"]!!.contains("2 letras"))
        assertTrue(auth.register("A.", "71234567", "ana@ejemplo.com", "secreto1", "secreto1")["name"]!!.contains("2 letras"))
        assertNotNull(auth.register("Ana2", "71234567", "ana@ejemplo.com", "secreto1", "secreto1")["name"])

        assertNotNull(auth.register("Ana Melara", "81234567", "ana@ejemplo.com", "secreto1", "secreto1")["phone"])
        assertNotNull(auth.register("Ana Melara", "7123456", "ana@ejemplo.com", "secreto1", "secreto1")["phone"])
        assertNotNull(auth.register("Ana Melara", "712345678", "ana@ejemplo.com", "secreto1", "secreto1")["phone"])
        assertNotNull(auth.register("Ana Melara", "7012-3456", "ana@ejemplo.com", "secreto1", "secreto1")["phone"])
        assertEquals("Escriba su teléfono.", auth.register("Ana Melara", "  ", "ana@ejemplo.com", "secreto1", "secreto1")["phone"])
    }

    @Test
    fun `account limits are the core text fields`() {
        assertEquals(TextFields.PERSON_NAME, ProfileFields.NAME)
        assertEquals(60, ProfileFields.NAME.maxLength)
        assertEquals(TextFields.PHONE, ProfileFields.PHONE)
        assertEquals(8, ProfileFields.PHONE.maxLength)
        assertEquals(254, ProfileFields.EMAIL.maxLength)
        assertEquals(64, ProfileFields.PASSWORD.maxLength)
    }

    // --- Vehicle registration ---

    @Test
    fun `a valid vehicle normalizes the plate and accepts a thousands separator`() {
        val (errors, input) = VehicleFormValidator.validate(" p123456 ", "Toyota", "Corolla", "2019", "30,000", config, today)
        assertTrue(errors.isValid)
        assertEquals(VehicleInput("P123456", "Toyota", "Corolla", 2019, 30_000), input)
    }

    @Test
    fun `a make made only of signs is rejected`() {
        val (errors, input) = VehicleFormValidator.validate("P123456", "///", "Corolla", "2019", "0", config, today)
        assertNull(input)
        assertEquals(setOf("make"), errors.byField.keys)
    }

    @Test
    fun `a vehicle with an invalid plate and malformed numbers reports each field`() {
        val (errors, input) = VehicleFormValidator.validate("P12", "Toyota", "", "dos mil", "1,2,3", config, today)
        assertNull(input)
        assertEquals(setOf("plate", "model", "year", "mileage"), errors.byField.keys)
    }

    @Test
    fun `the year allows next year's model but nothing later or below the minimum`() {
        val next = today.year + 1
        assertTrue(VehicleFormValidator.validate("P123456", "Kia", "Rio", "$next", "0", config, today).first.isValid)
        assertNotNull(VehicleFormValidator.validate("P123456", "Kia", "Rio", "${next + 1}", "0", config, today).first["year"])
        assertNotNull(VehicleFormValidator.validate("P123456", "Kia", "Rio", "${config.minVehicleYear - 1}", "0", config, today).first["year"])
    }

    @Test
    fun `mileage above the odometer limit is rejected`() {
        val (errors, _) = VehicleFormValidator.validate("P123456", "Kia", "Rio", "2020", "${config.maxMileage + 1}", config, today)
        assertNotNull(errors["mileage"])
        val atCeiling = String.format(Locale.US, "%,d", config.maxMileage)
        assertNull(VehicleFormValidator.validate("P123456", "Kia", "Rio", "2020", atCeiling, config, today).first["mileage"])
    }

    @Test
    fun `make and model use the shared text limits`() {
        val tooLong = "M".repeat(TextFields.MAKE.maxLength + 1)
        val errors = VehicleFormValidator.validate("P123456", tooLong, "C@r", "2019", "0", config, today).first
        assertNotNull(errors["make"])
        assertNotNull(errors["model"])
        val ok = VehicleFormValidator.validate("P123-AB2", "Mercedes-Benz", "C-HR", "2019", "0", config, today).first
        assertTrue(ok.isValid)
    }

    @Test
    fun `the year must be four digits before the range is checked`() {
        assertNotNull(VehicleFormValidator.validate("P123456", "Kia", "Rio", "201", "0", config, today).first["year"])
        assertNotNull(VehicleFormValidator.validate("P123456", "Kia", "Rio", "20199", "0", config, today).first["year"])
        assertNull(VehicleFormValidator.validate("P123456", "Kia", "Rio", "2019", "0", config, today).first["year"])
    }

    // --- Mileage ---

    @Test
    fun `mileage cannot go below the last recorded value`() {
        assertNotNull(MileageValidator.validate("29,999", lastKnown = 30_000, config = config).first)
        assertEquals(null to 30_000, MileageValidator.validate("30,000", lastKnown = 30_000, config = config))
        assertNotNull(MileageValidator.validate("treinta", lastKnown = 0, config = config).first)
    }

    // --- New appointment ---

    @Test
    fun `the appointment date runs from today to the configured horizon`() {
        val last = today.plusYears(config.schedulingHorizonYears)
        assertTrue(NewAppointmentValidator.isSchedulable(today, today, config))
        assertTrue(NewAppointmentValidator.isSchedulable(last, today, config))
        assertFalse(NewAppointmentValidator.isSchedulable(today.minusDays(1), today, config))
        assertFalse(NewAppointmentValidator.isSchedulable(last.plusDays(1), today, config))
        assertNotNull(NewAppointmentValidator.dateError(null, today, config))
        assertNull(NewAppointmentValidator.dateError(today.plusDays(3), today, config))
        assertEquals("Elija una franja horaria.", NewAppointmentValidator.slotError(slotSelected = false))
        assertNull(NewAppointmentValidator.slotError(slotSelected = true))
    }

    @Test
    fun `DatePicker milliseconds are read in UTC without shifting the day`() {
        val date = LocalDate.of(2026, 10, 18)
        val millis = NewAppointmentValidator.pickerMillisOf(date)
        assertEquals(1_792_281_600_000L, millis) // 2026-10-18T00:00Z
        assertEquals(date, NewAppointmentValidator.dateFromPickerMillis(millis))
    }
}
