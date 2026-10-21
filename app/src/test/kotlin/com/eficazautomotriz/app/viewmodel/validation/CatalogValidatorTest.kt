package com.eficazautomotriz.app.viewmodel.validation

import com.eficazautomotriz.domain.config.SystemConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Early validation of the staff service and maintenance catalog dialogs. */
class CatalogValidatorTest {

    private val config = SystemConfig()

    @Test
    fun `a service name is required, capped at 60, and keeps accents`() {
        assertNotNull(CatalogValidator.serviceType("   ", "detalle").first["name"])
        assertNotNull(CatalogValidator.serviceType("a".repeat(61), "").first["name"])
        assertNotNull(CatalogValidator.serviceType("Aceite #1", "").first["name"])

        val (errors, input) = CatalogValidator.serviceType("  Alineación  ", "  ")
        assertTrue(errors.isValid)
        assertEquals(ServiceTypeInput("Alineación", ""), input)
        assertNull(CatalogValidator.serviceType("a".repeat(60), "d".repeat(200)).first["name"])
    }

    @Test
    fun `a service description is optional and capped`() {
        assertNull(CatalogValidator.serviceType("Alineación", "").first["description"])
        assertNull(CatalogValidator.serviceType("Alineación", "   ").first["description"])
        assertNotNull(CatalogValidator.serviceType("Alineación", "d".repeat(201)).first["description"])

        val (_, input) = CatalogValidator.serviceType("Alineación", " Incluye balanceo (delantero). ")
        assertEquals("Incluye balanceo (delantero).", input?.description)
    }

    @Test
    fun `a service form can report the name and the description together`() {
        val errors = CatalogValidator.serviceType("", "d".repeat(201)).first
        assertNotNull(errors["name"])
        assertNotNull(errors["description"])
    }

    @Test
    fun `a maintenance interval accepts thousands separators inside the odometer ceiling`() {
        val (errors, input) = CatalogValidator.maintenanceType("  Correa  ", " 60,000 ", config)
        assertTrue(errors.isValid)
        assertEquals(MaintenanceTypeInput("Correa", 60_000), input)

        assertEquals(
            MaintenanceTypeInput("Tope", config.maxMileage),
            CatalogValidator.maintenanceType("Tope", "${config.maxMileage}", config).second,
        )
        assertNull(CatalogValidator.maintenanceType("Minimo", "1", config).first["intervalKm"])
    }

    @Test
    fun `a maintenance interval rejects blank, zero, negative, and values past the ceiling`() {
        assertNotNull(CatalogValidator.maintenanceType("Correa", "", config).first["intervalKm"])
        assertNotNull(CatalogValidator.maintenanceType("Correa", "no es numero", config).first["intervalKm"])
        assertNotNull(CatalogValidator.maintenanceType("Correa", "0", config).first["intervalKm"])
        assertNotNull(CatalogValidator.maintenanceType("Correa", "-5", config).first["intervalKm"])
        assertNotNull(CatalogValidator.maintenanceType("Correa", "${config.maxMileage + 1}", config).first["intervalKm"])
        assertNotNull(CatalogValidator.maintenanceType("   ", "5000", config).first["name"])
    }
}
