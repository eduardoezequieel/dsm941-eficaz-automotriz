package com.eficazautomotriz.application

import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.error.errorOrNull
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Staff catalog: service types and maintenance intervals, on the seeded graph. */
class CatalogServiceTest {

    @Test
    fun `catalogs list every seeded row, active or not, sorted by name`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        assertEquals(
            listOf(
                "Aire acondicionado",
                "Diagnostico por escaner",
                "Mantenimiento preventivo",
                "Reparacion de motor",
                "Sistema de frenos",
                "Sistema electrico",
            ),
            catalog.serviceTypes().map { it.name },
        )
        assertEquals(
            listOf(
                "Cambio de aceite y filtro",
                "Cambio de filtro de aire",
                "Revision de frenos",
                "Rotacion de neumaticos",
            ),
            catalog.maintenanceTypes().map { it.name },
        )
    }

    @Test
    fun `a client cannot create or toggle a service type`() {
        val env = TestEnvironment()
        val catalog = env.catalog()
        val before = env.repositories.serviceTypes.findAll().size

        assertIs<DomainError.Forbidden>(
            catalog.saveServiceType(env.firstClient, null, "Alineacion", "", true).errorOrNull(),
        )
        assertIs<DomainError.Forbidden>(
            catalog.setServiceTypeActive(env.firstClient, "SVT-001", false).errorOrNull(),
        )
        assertEquals(before, env.repositories.serviceTypes.findAll().size)
        assertTrue(requireNotNull(env.repositories.serviceTypes.findById("SVT-001")).active)
    }

    @Test
    fun `staff creates a service type with the next id and a blank description`() {
        val env = TestEnvironment()

        val saved = assertIs<Outcome.Success<ServiceType>>(
            env.catalog().saveServiceType(env.staff, null, "  Alineacion  ", "   ", active = true),
        ).value

        assertEquals("SVT-007", saved.id)
        assertEquals("Alineacion", saved.name)
        assertEquals("", saved.description)
        assertTrue(saved.active)
        assertEquals(saved, env.repositories.serviceTypes.findById("SVT-007"))
    }

    @Test
    fun `a service name must be present, short, and made of allowed characters`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        val blank = catalog.saveServiceType(env.staff, null, "   ", "detalle", true)
        assertIs<ValidationError.BlankText>(assertIs<DomainError.Invalid>(blank.errorOrNull()).validation)

        val tooLong = catalog.saveServiceType(env.staff, null, "a".repeat(61), "detalle", true)
        val length = assertIs<ValidationError.TextTooLong>(assertIs<DomainError.Invalid>(tooLong.errorOrNull()).validation)
        assertEquals(60, length.text.maxLength)

        val symbols = catalog.saveServiceType(env.staff, null, "Aceite #1", "detalle", true)
        assertIs<ValidationError.InvalidCharacters>(assertIs<DomainError.Invalid>(symbols.errorOrNull()).validation)

        assertEquals(6, env.repositories.serviceTypes.findAll().size)
        assertEquals("SVT-007", env.repositories.serviceTypes.nextId())
    }

    @Test
    fun `a service description is optional and capped`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        val tooLong = catalog.saveServiceType(env.staff, null, "Alineacion", "d".repeat(201), true)
        assertIs<ValidationError.TextTooLong>(assertIs<DomainError.Invalid>(tooLong.errorOrNull()).validation)

        val saved = assertIs<Outcome.Success<ServiceType>>(
            catalog.saveServiceType(env.staff, null, "Alineacion", "Incluye balanceo (delantero).", true),
        ).value
        assertEquals("Incluye balanceo (delantero).", saved.description)
    }

    @Test
    fun `staff edits a service type and an unknown id is not created`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        val saved = assertIs<Outcome.Success<ServiceType>>(
            catalog.saveServiceType(env.staff, "SVT-006", "Climatizacion", "Carga de gas.", active = false),
        ).value

        assertEquals("SVT-006", saved.id)
        assertEquals("Climatizacion", saved.name)
        assertEquals(false, saved.active)
        assertTrue(env.appointmentService.activeServiceTypes().none { it.id == "SVT-006" })
        assertTrue(catalog.serviceTypes().any { it.id == "SVT-006" && !it.active })

        val missing = catalog.saveServiceType(env.staff, "SVT-999", "Fantasma", "", true)
        assertIs<DomainError.NotFound>(missing.errorOrNull())
        assertEquals(6, env.repositories.serviceTypes.findAll().size)
    }

    @Test
    fun `staff toggles a service type and a missing id is not found`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        val off = assertIs<Outcome.Success<ServiceType>>(
            catalog.setServiceTypeActive(env.staff, "SVT-001", false),
        ).value
        assertEquals(false, off.active)
        val on = assertIs<Outcome.Success<ServiceType>>(
            catalog.setServiceTypeActive(env.staff, "SVT-001", true),
        ).value
        assertTrue(on.active)

        assertIs<DomainError.NotFound>(catalog.setServiceTypeActive(env.staff, "SVT-999", false).errorOrNull())
    }

    @Test
    fun `a client cannot create or toggle a maintenance interval`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        assertIs<DomainError.Forbidden>(
            catalog.saveMaintenanceType(env.firstClient, null, "Correa", 40_000, true).errorOrNull(),
        )
        assertIs<DomainError.Forbidden>(
            catalog.setMaintenanceTypeActive(env.firstClient, "MNT-001", false).errorOrNull(),
        )
        assertEquals(4, env.repositories.maintenanceTypes.findAll().size)
        assertTrue(requireNotNull(env.repositories.maintenanceTypes.findById("MNT-001")).active)
    }

    @Test
    fun `staff creates a maintenance type and the interval must be inside the odometer ceiling`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        val saved = assertIs<Outcome.Success<MaintenanceType>>(
            catalog.saveMaintenanceType(env.staff, null, "  Correa de distribucion  ", 60_000, true),
        ).value
        assertEquals("MNT-005", saved.id)
        assertEquals("Correa de distribucion", saved.name)
        assertEquals(60_000, saved.intervalKm)

        val atCeiling = assertIs<Outcome.Success<MaintenanceType>>(
            catalog.saveMaintenanceType(env.staff, null, "Tope", env.config.maxMileage, true),
        ).value
        assertEquals(env.config.maxMileage, atCeiling.intervalKm)

        listOf(0, -1, env.config.maxMileage + 1).forEach { interval ->
            val rejected = catalog.saveMaintenanceType(env.staff, null, "Malo", interval, true)
            val error = assertIs<ValidationError.IntervalOutOfRange>(
                assertIs<DomainError.Invalid>(rejected.errorOrNull()).validation,
            )
            assertEquals(interval, error.value)
            assertEquals("intervalKm", error.field)
        }
        assertEquals(6, env.repositories.maintenanceTypes.findAll().size)
    }

    @Test
    fun `a maintenance name follows the same text rules and a bad interval does not replace the row`() {
        val env = TestEnvironment()
        val catalog = env.catalog()
        val original = requireNotNull(env.repositories.maintenanceTypes.findById("MNT-001"))

        val blank = catalog.saveMaintenanceType(env.staff, "MNT-001", "  ", 5_000, true)
        assertIs<ValidationError.BlankText>(assertIs<DomainError.Invalid>(blank.errorOrNull()).validation)

        val rejected = catalog.saveMaintenanceType(env.staff, "MNT-001", original.name, 0, false)
        assertIs<ValidationError.IntervalOutOfRange>(assertIs<DomainError.Invalid>(rejected.errorOrNull()).validation)
        assertEquals(original, env.repositories.maintenanceTypes.findById("MNT-001"))
    }

    @Test
    fun `staff edits a maintenance interval and hiding it removes it from the active catalog`() {
        val env = TestEnvironment()
        val catalog = env.catalog()

        val saved = assertIs<Outcome.Success<MaintenanceType>>(
            catalog.saveMaintenanceType(env.staff, "MNT-002", "Rotacion", 8_000, active = false),
        ).value
        assertEquals("MNT-002", saved.id)
        assertEquals(8_000, saved.intervalKm)
        assertTrue(env.maintenanceService.activeTypes().none { it.id == "MNT-002" })
        assertTrue(catalog.maintenanceTypes().any { it.id == "MNT-002" && it.intervalKm == 8_000 })

        assertIs<DomainError.NotFound>(
            catalog.saveMaintenanceType(env.staff, "MNT-999", "Fantasma", 1_000, true).errorOrNull(),
        )
        assertIs<DomainError.NotFound>(catalog.setMaintenanceTypeActive(env.staff, "MNT-999", false).errorOrNull())

        val back = assertIs<Outcome.Success<MaintenanceType>>(
            catalog.setMaintenanceTypeActive(env.staff, "MNT-002", true),
        ).value
        assertTrue(back.active)
        assertTrue(env.maintenanceService.activeTypes().any { it.id == "MNT-002" })
    }

    private fun TestEnvironment.catalog() = CatalogService(
        repositories.serviceTypes,
        repositories.maintenanceTypes,
        config,
        logger,
    )
}
