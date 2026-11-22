package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.UserMapper
import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Firestore-backed [UserRepository] keyed by Firebase Auth UID.
 * [nextId] is unsupported because user document IDs must match the authenticated Firebase UID.
 */
class FirestoreUserRepository(db: FirebaseFirestore) :
    FirestoreRepository<User>(db, UserMapper.COLLECTION, "USR", UserMapper::toMap, UserMapper::fromDoc),
    UserRepository {

    override fun nextId(): String = throw UnsupportedOperationException("The user id is the Firebase uid")
}