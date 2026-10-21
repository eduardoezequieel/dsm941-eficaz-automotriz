package com.eficazautomotriz.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.eficazautomotriz.app.debug.runSmoke
import com.eficazautomotriz.app.ui.AppRoot
import com.eficazautomotriz.app.ui.theme.EficazTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light icons over the navy headers; dark icons over the white bottom bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
        )
        val app = application as EficazApp
        val graph = if (app.demoMode || app.firebaseConfigured) app.graph else null
        setContent { EficazTheme { AppRoot(graph) } }

        // Firestore/Auth smoke test: see DebugSmoke.kt (src/debug) to enable it.
        if (BuildConfig.DEBUG && SMOKE_ENABLED && !app.demoMode && graph != null && savedInstanceState == null) {
            lifecycleScope.launch {
                runSmoke(graph, "smoke+${System.currentTimeMillis()}@ejemplo.com", SMOKE_PASSWORD)
            }
        }
    }

    private companion object {
        /** Requires app/google-services.json; without it, creating the graph throws while starting Firebase. */
        const val SMOKE_ENABLED = false
        const val SMOKE_PASSWORD = "Prueba-123456"
    }
}
