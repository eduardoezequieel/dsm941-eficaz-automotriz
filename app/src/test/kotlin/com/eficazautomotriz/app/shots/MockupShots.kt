package com.eficazautomotriz.app.shots

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eficazautomotriz.app.demo.MockupSeedData
import com.eficazautomotriz.app.demo.demoBackend
import com.eficazautomotriz.app.ui.client.ClientText
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Renders the mockup screens (P1–P5, P8, P9) with the demo seed on a 412x915dp Pixel-8-like
 * window, to compare side by side with the f1–f9 PNGs. Opt-in (slow):
 * `./gradlew :app:testDebugUnitTest -Pshots --tests '*shots*'`, PNGs in app/build/shots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi", application = android.app.Application::class)
class MockupShots {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val dir = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }

    private fun start(signedInUid: String?) {
        val graph = AppGraph(rule.activity, demoBackend(LocalDate.now(), signedInUid))
        rule.setContent { EficazTheme { AppRoot(graph) } }
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
        FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitText(text: String, substring: Boolean = false) {
        rule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            rule.onAllNodes(hasText(text, substring = substring, ignoreCase = true), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
    }

    private fun click(text: String, substring: Boolean = false) {
        waitText(text, substring)
        rule.onAllNodes(hasText(text, substring = substring, ignoreCase = true), useUnmergedTree = true)[0].performClick()
        rule.waitForIdle()
    }

    private fun tab(label: String) {
        rule.onAllNodes(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[0].performClick()
        rule.waitForIdle()
    }

    private fun scroll(up: Boolean = true) {
        rule.onAllNodes(hasScrollAction())[0].performTouchInput { if (up) swipeUp() else swipeDown() }
        rule.waitForIdle()
    }

    private fun showing(text: String): Boolean =
        rule.onAllNodes(hasText(text, substring = true, ignoreCase = true), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()

    /** Swipes the list until [text] is composed. Lazy lists drop off-screen rows. */
    private fun scrollTo(text: String) {
        repeat(8) {
            if (showing(text)) return
            scroll()
        }
        waitText(text, substring = true)
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun login() {
        start(signedInUid = null)
        waitText("Continuar con Google", substring = true)
        snap("p1")
    }

    @Test
    fun client() {
        start(MockupSeedData.CLIENT_ID)
        waitText("Mantenimiento preventivo")
        snap("p2")
        scroll(); snap("p2b")
        tab("Vehículos"); waitText("Nissan Frontier 2019"); snap("p3")
        click("Nissan Frontier 2019"); waitText("Plan de mantenimiento preventivo", substring = true); snap("p4")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        tab("Citas"); waitText("CT-1028", substring = true); snap("p5")
        tab("Inicio"); waitText("Nueva cita"); click("Nueva cita")
        waitText("Toyota Corolla 2016", substring = true)
        click("Toyota Corolla 2016", substring = true)
        waitText("Mantenimiento preventivo"); click("Mantenimiento preventivo")
        val showcase = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY)).plusDays(5)
        waitText("Seleccione la fecha", substring = true)
        val monthTitle = ClientText.monthYear(showcase)
        if (rule.onAllNodes(hasText(monthTitle), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            rule.onNode(hasContentDescription("Mes siguiente")).performClick()
            rule.waitForIdle()
        }
        click(showcase.dayOfMonth.toString())
        waitText("1:00"); click("10:00")
        waitText("Pendiente"); snap("p6")
        scroll(); snap("p6b")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        waitText("Sistema de frenos"); click("Sistema de frenos")
        waitText("SV-2087", substring = true); snap("p7")
        scroll(); snap("p7b")
    }

    @Test
    fun staff() {
        start(MockupSeedData.STAFF_ID)
        waitText("Panel del personal")
        waitText("Solicitudes pendientes")
        snap("p8")
        scrollTo("Reportes del mes")
        snap("p8b")
        scrollTo("Agenda confirmada")
        snap("p8c")
        tab("Reportes")
        waitText("Citas por estado")
        Thread.sleep(500)
        snap("p9")
        scrollTo("Exportar resumen")
        snap("p9b")
    }

    /** Detail, form and settings screens that have no f-number of their own. */
    @Test
    fun staffSecondary() {
        start(MockupSeedData.STAFF_ID)
        waitText("Solicitudes pendientes")
        click("Diagnóstico por escáner")
        waitText("Solicitud de cita")
        snap("s-cita")
        click("Reprogramar")
        waitText("Nueva fecha")
        snap("s-reprogramar")
        back()

        waitText("Panel del personal")
        scrollTo("Reparación de motor")
        click("Reparación de motor", substring = true)
        waitText("Orden de servicio")
        snap("s-orden")
        click("Subir evidencia")
        waitText("Evidencias fotográficas")
        snap("s-evidencia")
        back()
        waitText("Finalizar")
        click("Finalizar")
        waitText("Finalizar orden")
        snap("s-finalizar")
        back()
        back()

        waitText("Panel del personal")
        tab("Servicios")
        waitText("En proceso")
        snap("s-servicios-segmento")
        rule.onNode(hasContentDescription("Nueva orden")).performClick()
        rule.waitForIdle()
        waitText("Recepción de vehículo")
        snap("s-nueva-orden")
        back()

        waitText("Panel del personal")
        rule.onAllNodes(hasText("Servicios") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[1]
            .performClick()
        rule.waitForIdle()
        waitText("órdenes en proceso", substring = true)
        snap("s-servicios")

        tab("Perfil")
        waitText("Carlos Cruz")
        snap("s-perfil")
        click("Configuración")
        waitText("Franjas")
        snap("s-config-franjas")
        click("Mantenimiento")
        waitText("Cada", substring = true)
        snap("s-config-mant")
        click("Servicios")
        waitText("Aire acondicionado")
        snap("s-config-servicios")
    }
}
