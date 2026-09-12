package com.eficazautomotriz.domain.config

/**
 * Parametros configurables del sistema. Ningun valor numerico de negocio puede estar
 * escrito directamente en las reglas ni en los menus: todo entra por aqui.
 *
 * Los valores por omision son de demostracion, no politicas de Eficaz Automotriz.
 */
data class SystemConfig(
    val defaultSlotCapacity: Int = 1,
    val warningThresholdKm: Int = 500,
    val errorLogPath: String = "logs/errors.log",
    val minVehicleYear: Int = 1950,
    // Tope del odometro: por encima de esto la lectura es un error de digitacion, no un vehiculo.
    val maxMileage: Int = 2_000_000,
    val maxSlotCapacity: Int = 20,
    // Ventana de fechas que la consola acepta al agendar o reprogramar.
    val schedulingHorizonYears: Long = 1,
    // Antiguedad maxima que un reporte puede consultar hacia atras.
    val reportHistoryYears: Long = 10,
)