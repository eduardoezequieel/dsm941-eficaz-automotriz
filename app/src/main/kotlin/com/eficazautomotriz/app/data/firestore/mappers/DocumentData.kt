package com.eficazautomotriz.app.data.firestore.mappers

import com.eficazautomotriz.app.data.firestore.MalformedDocumentException
import com.google.firebase.firestore.DocumentSnapshot

/** Mappers work on the raw map, so they can be tested on the JVM without Firestore. */
internal fun requireData(d: DocumentSnapshot): Map<String, Any?> =
    d.data ?: throw MalformedDocumentException("(documento)", "${d.reference.path} no existe")
