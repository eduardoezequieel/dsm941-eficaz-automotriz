package com.eficazautomotriz.domain.model

import com.eficazautomotriz.domain.model.enums.UserRole

data class User(
    override val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
) : Identifiable