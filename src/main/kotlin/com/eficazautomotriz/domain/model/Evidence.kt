package com.eficazautomotriz.domain.model

import com.eficazautomotriz.domain.model.enums.EvidenceStage
import java.time.LocalDateTime

data class Evidence(
    override val id: String,
    val serviceOrderId: String,
    val stage: EvidenceStage,
    val imageUrl: String,
    val uploadedAt: LocalDateTime,
) : Identifiable