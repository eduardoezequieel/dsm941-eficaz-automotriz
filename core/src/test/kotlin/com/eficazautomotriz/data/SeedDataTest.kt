package com.eficazautomotriz.data

import com.eficazautomotriz.data.memory.InMemoryMaintenanceTypeRepository
import com.eficazautomotriz.data.memory.InMemoryServiceTypeRepository
import com.eficazautomotriz.data.memory.InMemoryTimeSlotRepository
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** `loadCatalogs` runs when the app starts: calling it again must not duplicate anything. */
class SeedDataTest {

    private val serviceTypes = InMemoryServiceTypeRepository()
    private val maintenanceTypes = InMemoryMaintenanceTypeRepository()
    private val timeSlots = InMemoryTimeSlotRepository()

    private fun loadCatalogs() = SeedData.loadCatalogs(serviceTypes, maintenanceTypes, timeSlots)

    @Test
    fun `loading the catalogs twice does not duplicate records`() {
        loadCatalogs()
        loadCatalogs()

        assertEquals(6, serviceTypes.findAll().size)
        assertEquals(4, maintenanceTypes.findAll().size)
        assertEquals(36, timeSlots.findAll().size)
    }

    @Test
    fun `the slots keep the fixed identifiers that the seed appointments reference`() {
        loadCatalogs()

        val monday = requireNotNull(timeSlots.findById("SLT-001"))
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(LocalTime.of(9, 0), monday.startTime)

        val tuesday = requireNotNull(timeSlots.findById("SLT-009"))
        assertEquals(DayOfWeek.TUESDAY, tuesday.dayOfWeek)
        assertEquals(LocalTime.of(10, 0), tuesday.startTime)

        val saturday = requireNotNull(timeSlots.findById("SLT-036"))
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        assertEquals(LocalTime.of(13, 0), saturday.endTime)
        assertEquals(2, saturday.capacity)
    }

    @Test
    fun `the sequence continues after the seed identifiers`() {
        loadCatalogs()

        assertEquals("SLT-037", timeSlots.nextId())
        assertEquals("SVT-007", serviceTypes.nextId())
    }
}