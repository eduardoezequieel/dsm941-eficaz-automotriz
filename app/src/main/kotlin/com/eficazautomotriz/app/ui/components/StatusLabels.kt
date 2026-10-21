package com.eficazautomotriz.app.ui.components

import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus

/** Short chip text. Domain labels omit accents; these are the ones shown on screen. */
fun maintenanceShortLabel(status: MaintenanceStatus): String = when (status) {
    MaintenanceStatus.UP_TO_DATE -> "Vigente"
    MaintenanceStatus.DUE_SOON -> "Próximo"
    MaintenanceStatus.OVERDUE -> "Vencido"
    MaintenanceStatus.NO_PREVIOUS_RECORD -> "Sin registro previo"
}

/** Longer label used on the vehicle list cards. */
fun maintenanceCardLabel(status: MaintenanceStatus): String = when (status) {
    MaintenanceStatus.UP_TO_DATE -> "Mantenimiento vigente"
    MaintenanceStatus.DUE_SOON -> "Próximo a vencer"
    MaintenanceStatus.OVERDUE -> "Mantenimiento vencido"
    MaintenanceStatus.NO_PREVIOUS_RECORD -> "Sin registro previo"
}

fun evidenceStageLabel(stage: EvidenceStage): String = when (stage) {
    EvidenceStage.RECEPTION -> "Recepción"
    EvidenceStage.DIAGNOSIS -> "Diagnóstico"
    EvidenceStage.RESULT -> "Resultado"
}
