package com.eficazautomotriz.domain.model

/** Contrato minimo de toda entidad persistible: un identificador estable y unico. */
interface Identifiable {
    val id: String
}