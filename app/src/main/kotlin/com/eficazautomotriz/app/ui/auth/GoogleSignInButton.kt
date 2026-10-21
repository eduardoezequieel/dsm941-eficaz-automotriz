@file:Suppress("DEPRECATION")

package com.eficazautomotriz.app.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * "Continuar con Google" button (play-services-auth). It only gets the idToken from the
 * account picker; AuthViewModel signs in to Firebase through FirebaseAuthGateway.
 *
 * GoogleSignIn is deprecated in favor of Credential Manager (hence the @file:Suppress); it is
 * kept because it is the API the plan prescribes and it works on API 26+.
 */
@Composable
fun GoogleSignInButton(
    webClientId: String,
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val client = remember(webClientId) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build(),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK && result.data == null) return@rememberLauncherForActivityResult
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val token = account?.idToken
            if (token != null) onIdToken(token) else onError("Google no devolvió un token de identidad.")
        } catch (e: ApiException) {
            // 12501 = the user closed the picker: not an error worth showing.
            if (e.statusCode != USER_CANCELLED) onError("No se pudo iniciar sesión con Google (código ${e.statusCode}).")
        }
    }
    OutlinedNavyButton(
        "Continuar con Google",
        onClick = { client.signOut().addOnCompleteListener { launcher.launch(client.signInIntent) } },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val USER_CANCELLED = 12501
