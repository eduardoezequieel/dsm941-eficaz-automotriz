package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.TimeSlotMapper
import com.eficazautomotriz.data.TimeSlotRepository
import com.eficazautomotriz.domain.model.TimeSlot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek

class FirestoreTimeSlotRepository(db: FirebaseFirestore) :
    FirestoreRepository<TimeSlot>(db, TimeSlotMapper.COLLECTION, "SLT", TimeSlotMapper::toMap, TimeSlotMapper::fromDoc),
    TimeSlotRepository {

    /** Sorted in memory: avoids requiring a composite dayOfWeek + startTime index. */
    override fun findByDay(dayOfWeek: DayOfWeek): List<TimeSlot> =
        query("dayOfWeek" to dayOfWeek.name).sortedBy { it.startTime }
}