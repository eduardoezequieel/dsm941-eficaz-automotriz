package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.FirestoreCodecs
import com.eficazautomotriz.app.data.firestore.enumValue
import com.eficazautomotriz.app.data.firestore.string
import com.eficazautomotriz.domain.model.User
import com.google.firebase.firestore.DocumentSnapshot

/** The document id is the Firebase Auth uid. */
object UserMapper {
    const val COLLECTION = "usuarios"

    fun toMap(u: User): Map<String, Any?> = mapOf(
        "id" to u.id, "name" to u.name, "email" to u.email, "phone" to u.phone,
        "role" to FirestoreCodecs.encode(u.role),
    )

    fun fromMap(m: Map<String, Any?>): User = User(
        id = m.string("id"), name = m.string("name"), email = m.string("email"), phone = m.string("phone"),
        role = m.enumValue("role"),
    )

    fun fromDoc(d: DocumentSnapshot): User = fromMap(requireData(d))
}
