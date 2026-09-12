package com.eficazautomotriz.domain.model

/**
 * Guarda solo el kilometraje base del ultimo mantenimiento.
 * El kilometraje recomendado y la clasificacion no se almacenan: los calcula
 * MaintenanceStatusRule en cada consulta, para que nunca queden desincronizados.
 */
data class MaintenanceRecord(
    override val id: String,
    val vehicleId: String,
    val maintenanceTypeId: String,
    // null significa «sin registro previo», que es distinto de 0.
    val lastServiceMileage: Int?,
) : Identifiable