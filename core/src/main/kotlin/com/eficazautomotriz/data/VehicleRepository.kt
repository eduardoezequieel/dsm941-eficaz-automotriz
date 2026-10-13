package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.Vehicle

interface VehicleRepository : Repository<Vehicle> {
    fun findByOwner(ownerId: String): List<Vehicle>
    fun findByPlate(plate: String): Vehicle?

    /**
     * Whether any vehicle, of any owner, already uses the plate. Uniqueness is checked with
     * this and not with `findByPlate`: a client may not be allowed to read the other vehicle.
     */
    fun plateExists(plate: String): Boolean
}