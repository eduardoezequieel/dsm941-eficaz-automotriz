package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.model.Vehicle

class InMemoryVehicleRepository : InMemoryRepository<Vehicle>("VEH"), VehicleRepository {

    override fun findByOwner(ownerId: String): List<Vehicle> = findAll().filter { it.ownerId == ownerId }

    override fun findByPlate(plate: String): Vehicle? =
        findAll().firstOrNull { it.plate.equals(plate, ignoreCase = true) }

    override fun plateExists(plate: String): Boolean = findByPlate(plate) != null
}
