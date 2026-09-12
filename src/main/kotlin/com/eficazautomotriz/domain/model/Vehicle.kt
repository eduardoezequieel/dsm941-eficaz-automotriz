package com.eficazautomotriz.domain.model

import java.time.LocalDate

data class Vehicle(
    override val id: String,
    val ownerId: String,
    val plate: String,
    val make: String,
    val model: String,
    val year: Int,
    val currentMileage: Int,
    val mileageUpdatedAt: LocalDate,
) : Identifiable