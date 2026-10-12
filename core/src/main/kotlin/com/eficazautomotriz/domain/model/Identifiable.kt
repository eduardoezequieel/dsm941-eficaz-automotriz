package com.eficazautomotriz.domain.model

/** Minimum contract of every persistable entity: one stable, unique identifier. */
interface Identifiable {
    val id: String
}
