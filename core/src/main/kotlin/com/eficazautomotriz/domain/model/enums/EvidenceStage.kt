package com.eficazautomotriz.domain.model.enums

/** Stage of the service at which the photographic evidence was captured. */
enum class EvidenceStage(val label: String) {
    RECEPTION("Recepcion"),
    DIAGNOSIS("Diagnostico"),
    RESULT("Resultado"),
}
