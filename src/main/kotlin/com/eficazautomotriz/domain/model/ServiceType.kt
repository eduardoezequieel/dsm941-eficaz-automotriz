package com.eficazautomotriz.domain.model

data class ServiceType(
    override val id: String,
    val name: String,
    val description: String,
    val active: Boolean,
) : Identifiable