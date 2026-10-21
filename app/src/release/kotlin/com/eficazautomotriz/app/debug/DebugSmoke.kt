package com.eficazautomotriz.app.debug

import com.eficazautomotriz.app.di.AppGraph

/** Release builds have no smoke test; this signature only lets MainActivity compile. */
@Suppress("UNUSED_PARAMETER")
suspend fun runSmoke(graph: AppGraph, email: String, password: String) = Unit
