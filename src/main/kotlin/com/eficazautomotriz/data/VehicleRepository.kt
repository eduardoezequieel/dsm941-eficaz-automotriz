package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Vehicle

class VehicleRepository : InMemoryRepository<Vehicle>("VEH") {

    fun findByOwner(ownerId: String): List<Vehicle> = findAll().filter { it.ownerId == ownerId }

    fun findByPlate(plate: String): Vehicle? =
        findAll().firstOrNull { it.plate.equals(plate, ignoreCase = true) }
}
