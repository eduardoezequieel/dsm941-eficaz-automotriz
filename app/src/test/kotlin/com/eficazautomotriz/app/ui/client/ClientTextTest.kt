package com.eficazautomotriz.app.ui.client

import com.eficazautomotriz.domain.model.Vehicle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ClientTextTest {

    private val friday = LocalDate.of(2026, 8, 21)

    @Test
    fun `dates read as in the mockups`() {
        assertEquals("Viernes 21 de agosto", ClientText.longDayDate(friday))
        assertEquals("Vie 21 ago 2026", ClientText.shortDate(friday))
        assertEquals("Vie 21 ago", ClientText.shortDayMonth(friday))
        assertEquals("Mié 19 ago", ClientText.shortDayMonth(LocalDate.of(2026, 8, 19)))
        assertEquals("12 ago 2026", ClientText.compactDate(LocalDate.of(2026, 8, 12)))
        assertEquals("12 de julio de 2026", ClientText.dayMonthYear(LocalDate.of(2026, 7, 12)))
        assertEquals("Agosto 2026", ClientText.monthYear(friday))
    }

    @Test
    fun `ranges carry one suffix when both ends share it`() {
        assertEquals("10:00 – 11:00 a.m.", ClientText.range12(LocalTime.of(10, 0), LocalTime.of(11, 0)))
        assertEquals("9:00 – 10:00 a.m.", ClientText.range12(LocalTime.of(9, 0), LocalTime.of(10, 0)))
        assertEquals("11:00 a.m. – 12:00 p.m.", ClientText.range12(LocalTime.of(11, 0), LocalTime.of(12, 0)))
        assertEquals("2:00 – 3:00 p.m.", ClientText.range12(LocalTime.of(14, 0), LocalTime.of(15, 0)))
    }

    @Test
    fun `times use the 12-hour clock`() {
        assertEquals("1:00", ClientText.hour12(LocalTime.of(13, 0)))
        assertEquals("12:00", ClientText.hour12(LocalTime.of(12, 0)))
        assertEquals("12:30 a.m.", ClientText.time12(LocalTime.of(0, 30)))
        assertEquals("9:12 a.m.", ClientText.time12(LocalTime.of(9, 12)))
        assertEquals("12/07/2026 · 3:40 p.m.", ClientText.dateTime12(LocalDateTime.of(2026, 7, 12, 15, 40)))
    }

    @Test
    fun `vehicle lines format the plate`() {
        val vehicle = Vehicle("VEH-2", "USR-1", "P987321", "Nissan", "Frontier", 2019, 84_100, friday)
        assertEquals("Nissan Frontier 2019", ClientText.vehicleName(vehicle))
        assertEquals("Nissan Frontier 2019 · P987-321", ClientText.vehicleLine(vehicle))
        assertEquals("Nissan Frontier · P987-321", ClientText.vehicleShortLine(vehicle))
        assertEquals("Placa P987-321 · Diésel", ClientText.plateDetail(vehicle))
        assertEquals("Placa P000-000 · 2014", ClientText.plateDetail(vehicle.copy(plate = "P000000", year = 2014)))
        assertEquals("Cambio de aceite recomendado", ClientText.alertAction("Cambio de aceite y filtro"))
        assertEquals("Rotación de neumáticos recomendada", ClientText.alertAction("Rotación de neumáticos"))
        assertEquals("3 vehículos registrados", ClientText.vehicleCount(3))
        assertEquals("1 vehículo registrado", ClientText.vehicleCount(1))
    }
}
