package com.eficazautomotriz.app.data.firestore

import com.eficazautomotriz.app.data.firestore.mappers.UserMapper
import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreUserRepository(db: FirebaseFirestore) :
    FirestoreRepository<User>(db, UserMapper.COLLECTION, "USR", UserMapper::toMap, UserMapper::fromDoc),
    UserRepository {

    override fun nextId(): String = throw UnsupportedOperationException("The user id is the Firebase uid")
}
