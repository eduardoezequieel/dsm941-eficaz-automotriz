package com.eficazautomotriz.app.shots

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eficazautomotriz.app.demo.MockupSeedData
import com.eficazautomotriz.app.demo.demoBackend
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.AppRoot
import com.eficazautomotriz.app.ui.theme.EficazTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * The main screens with the system font at 130 %, on a narrow 360dp phone, to check that no
 * label is clipped. Opt-in like [MockupShots]; PNGs in app/build/shots with the `f130-` prefix.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi", application = android.app.Application::class)
class LargeFontShots {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val dir = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }

    private fun start(signedInUid: String?) {
        val graph = AppGraph(rule.activity, demoBackend(LocalDate.now(), signedInUid))
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                EficazTheme { AppRoot(graph) }
            }
        }
        repeat(20) {
            Thread.sleep(100)
            ShadowLooper.idleMainLooper()
            rule.waitForIdle()
        }
    }

    private fun snap(name: String) {
        rule.waitForIdle()
        Thread.sleep(300)
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(Canvas(bitmap)) }
        FileOutputStream(File(dir, "f130-$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitText(text: String) {
        rule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            rule.onAllNodes(hasText(text, substring = true, ignoreCase = true), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
    }

    private fun tab(label: String) {
        rule.onAllNodes(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[0].performClick()
        rule.waitForIdle()
    }

    private fun scroll() {
        rule.onAllNodes(hasScrollAction())[0].performTouchInput { swipeUp() }
        rule.waitForIdle()
    }

    @Test
    fun login() {
        start(signedInUid = null)
        waitText("Continuar con Google")
        snap("p1")
    }

    @Test
    fun client() {
        start(MockupSeedData.CLIENT_ID)
        waitText("Mantenimiento preventivo")
        snap("p2")
        scroll(); snap("p2b")
        tab("Vehículos"); waitText("Nissan Frontier 2019"); snap("p3")
        tab("Citas"); waitText("CT-1028"); snap("p5")
    }

    @Test
    fun staff() {
        start(MockupSeedData.STAFF_ID)
        waitText("Solicitudes pendientes")
        snap("p8")
        tab("Reportes")
        waitText("Citas por estado")
        Thread.sleep(500)
        snap("p9")
    }
}
