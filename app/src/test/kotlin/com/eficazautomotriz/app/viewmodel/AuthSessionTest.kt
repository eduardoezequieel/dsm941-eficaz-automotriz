package com.eficazautomotriz.app.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eficazautomotriz.app.data.ImageUploader
import com.eficazautomotriz.app.data.auth.AuthGateway
import com.eficazautomotriz.app.demo.MockupSeedData
import com.eficazautomotriz.app.demo.demoBackend
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.di.DataBackend
import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.data.ServiceTypeRepository
import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.UserRole
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper
import java.net.UnknownHostException
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException

/**
 * Password reset, offline notices and sign-out cleanup on the JVM (Robolectric).
 * The login control itself and a real Firebase reset email stay on the emulator list.
 */
@RunWith(AndroidJUnit4::class)
class AuthSessionTest {

    private val today = LocalDate.of(2026, 11, 2)

    @Test
    fun `password reset confirms a known email and reports an unknown one in Spanish`() {
        val graph = graph()
        val vm = AuthViewModel(graph)
        vm.openPasswordReset()
        vm.updateResetEmail(MockupSeedData.CLIENT_EMAIL)
        vm.sendPasswordReset()
        idleUntil { vm.state.value.notice != null }
        assertNull(vm.state.value.reset)
        assertTrue(vm.state.value.notice!!.contains(MockupSeedData.CLIENT_EMAIL))
        assertTrue(vm.state.value.notice!!.contains("restablecer"))

        vm.openPasswordReset()
        vm.updateResetEmail("nadie@demo.test")
        vm.sendPasswordReset()
        idleUntil { vm.state.value.reset?.error != null }
        assertEquals("No existe una cuenta con ese correo.", vm.state.value.reset?.error)
        assertFalse(vm.state.value.reset!!.error!!.contains("ERROR_"))
    }

    @Test
    fun `password reset keeps the notice until the screen consumes it`() {
        val graph = graph()
        val vm = AuthViewModel(graph)
        vm.openPasswordReset()
        vm.updateResetEmail(MockupSeedData.CLIENT_EMAIL)
        vm.sendPasswordReset()
        idleUntil { vm.state.value.notice != null }
        val sent = vm.state.value.notice
        assertNull(vm.state.value.reset)
        vm.closePasswordReset()
        assertEquals(sent, vm.state.value.notice)
        vm.consumeNotice()
        assertNull(vm.state.value.notice)

        vm.openPasswordReset()
        vm.updateResetEmail("nadie@demo.test")
        vm.sendPasswordReset()
        idleUntil { vm.state.value.reset?.error != null }
        val error = vm.state.value.reset?.error
        vm.closePasswordReset()
        assertNull(vm.state.value.reset)
        assertEquals(error, vm.state.value.notice)
        vm.consumeNotice()
        assertNull(vm.state.value.notice)
    }

    @Test
    fun `firebase exceptions use the code and hide the English detail`() {
        val wrong = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "The password is invalid")
        assertEquals("Correo o contraseña incorrectos.", authErrorMessage(wrong))
        assertFalse(authErrorMessage(wrong).contains("password"))

        val weak = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "weak")
        assertEquals("La contraseña es demasiado débil. Use al menos 6 caracteres.", authErrorMessage(weak))

        val collision = FirebaseAuthUserCollisionException(
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL",
            "account exists",
        )
        assertTrue(authErrorMessage(collision).contains("otro método"))

        val disabled = FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "disabled")
        assertEquals("Esta cuenta está deshabilitada. Contacte al taller.", authErrorMessage(disabled))

        val wrapped = RuntimeException("wrap", FirebaseNetworkException("Unable to resolve host"))
        assertEquals("Sin conexión. Revise su red e intente de nuevo.", authErrorMessage(wrapped))
        assertFalse(authErrorMessage(wrapped).contains("Unable"))

        assertEquals(
            "Demasiados intentos. Espere un momento e intente de nuevo.",
            authErrorMessage(FirebaseTooManyRequestsException("too many")),
        )
    }

    @Test
    fun `sign-in without a network shows a notice and leaves the form usable`() {
        val graph = graph(gateway = OfflineAuthGateway())
        val vm = AuthViewModel(graph)
        vm.updateLogin { it.copy(email = MockupSeedData.CLIENT_EMAIL, password = MockupSeedData.DEMO_PASSWORD) }
        vm.signIn()
        idleUntil { vm.state.value.message != null }
        assertEquals("Sin conexión. Revise su red e intente de nuevo.", vm.state.value.message)
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.message!!.contains("Unable"))
        assertNull(vm.state.value.authenticatedUid)
    }

    @Test
    fun `opening with a session and no network shows a profile notice instead of crashing`() {
        val graph = offlineProfileGraph()
        val vm = SessionViewModel(graph)
        idleUntil { vm.state.value is SessionState.ProfileError }
        val message = (vm.state.value as SessionState.ProfileError).message
        assertEquals("No se pudo cargar su perfil. Revise su conexión e intente de nuevo.", message)
        assertFalse(message.contains("offline"))
        assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
    }

    @Test
    fun `sign-out changes the navigation scope and keeps the caller's message`() {
        val graph = graph(signedInUid = MockupSeedData.CLIENT_ID)
        val vm = SessionViewModel(graph)
        idleUntil { vm.state.value is SessionState.SignedIn }
        assertEquals(MockupSeedData.CLIENT_ID, vm.scopeId.value)

        vm.signOut("Hasta luego")
        val signedOut = vm.state.value as SessionState.SignedOut
        assertEquals("Hasta luego", signedOut.message)
        assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
        assertNull(graph.authGateway.currentUid())
    }

    @Test
    fun `sign-out during the catalog load does not restore the signed-in session`() {
        val gate = BlockingServiceTypes()
        val graph = graph(signedInUid = MockupSeedData.CLIENT_ID, serviceTypes = gate)
        val vm = SessionViewModel(graph)
        try {
            idleUntil { gate.entered.count == 0L }
            vm.signOut("Hasta luego")
        } finally {
            gate.release.countDown()
        }
        pumpMain(1_000_000_000L)
        val signedOut = vm.state.value as SessionState.SignedOut
        assertEquals("Hasta luego", signedOut.message)
        assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
        assertNull(graph.authGateway.currentUid())
    }

    @Test
    fun `an external account switch clears the previous scope and loads the new profile`() {
        val graph = graph(signedInUid = MockupSeedData.CLIENT_ID)
        val vm = SessionViewModel(graph)
        idleUntil { vm.state.value is SessionState.SignedIn && vm.scopeId.value == MockupSeedData.CLIENT_ID }

        Thread {
            kotlinx.coroutines.runBlocking {
                graph.authGateway.signInWithEmail(MockupSeedData.STAFF_EMAIL, MockupSeedData.DEMO_PASSWORD)
            }
        }.apply {
            start()
            join(5_000)
        }
        assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
        assertTrue(vm.state.value is SessionState.Checking)

        idleUntil {
            val state = vm.state.value
            state is SessionState.SignedIn && state.user.id == MockupSeedData.STAFF_ID
        }
        assertEquals(MockupSeedData.STAFF_ID, vm.scopeId.value)
        assertEquals(MockupSeedData.STAFF_ID, (vm.state.value as SessionState.SignedIn).user.id)
    }

    @Test
    fun `a uid published while signed out waits until onAuthenticated`() {
        val gateway = ScriptedAuthGateway(null)
        val graph = graph(gateway = gateway)
        val vm = SessionViewModel(graph)
        assertEquals(SessionState.SignedOut(), vm.state.value)

        gateway.emit(MockupSeedData.CLIENT_ID)
        pumpMain(400_000_000L)
        assertEquals(SessionState.SignedOut(), vm.state.value)
        assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
        assertEquals(MockupSeedData.CLIENT_ID, graph.authGateway.currentUid())

        vm.onAuthenticated(MockupSeedData.CLIENT_ID)
        idleUntil { vm.state.value is SessionState.SignedIn }
        assertEquals(MockupSeedData.CLIENT_ID, (vm.state.value as SessionState.SignedIn).user.id)
        assertEquals(MockupSeedData.CLIENT_ID, vm.scopeId.value)
    }

    @Test
    fun `ensureCatalogs rethrows cancellation instead of reporting a catalog failure`() {
        val gate = BlockingServiceTypes()
        val graph = graph(serviceTypes = gate)
        val outcome = AtomicReference<String>()
        val jobRef = AtomicReference<Job>()
        val worker = Thread {
            runBlocking {
                val job = launch {
                    try {
                        outcome.set(if (graph.ensureCatalogs()) "true" else "false")
                    } catch (_: CancellationException) {
                        outcome.set("cancelled")
                    }
                }
                jobRef.set(job)
                job.join()
            }
        }
        worker.start()
        try {
            idleUntil { jobRef.get() != null && gate.entered.count == 0L }
            jobRef.get().cancel()
        } finally {
            gate.release.countDown()
        }
        worker.join(8_000)
        assertEquals("cancelled", outcome.get())
    }

    @Test
    fun `a null uid while the profile is blocked signs out and drops the late profile`() {
        val users = GatedUsers().also { it.hold(MockupSeedData.CLIENT_ID) }
        val graph = graph(signedInUid = MockupSeedData.CLIENT_ID, users = users)
        val vm = SessionViewModel(graph)
        val published = mutableListOf<String>()
        try {
            idleUntil { users.seen(MockupSeedData.CLIENT_ID) }
            graph.authGateway.signOut()
            val signedOut = vm.state.value as SessionState.SignedOut
            assertEquals("Su sesión ya no es válida. Inicie sesión de nuevo.", signedOut.message)
            assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
        } finally {
            users.release(MockupSeedData.CLIENT_ID)
        }
        pumpMain(1_000_000_000L) { vm.noteSignedIn(published) }
        val signedOut = vm.state.value as SessionState.SignedOut
        assertEquals("Su sesión ya no es válida. Inicie sesión de nuevo.", signedOut.message)
        assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
        assertTrue(published.isEmpty())
    }

    @Test
    fun `a blocked profile followed by an account switch publishes only the new uid`() {
        val users = GatedUsers().also {
            it.hold(MockupSeedData.CLIENT_ID)
            it.hold(MockupSeedData.STAFF_ID)
        }
        val gateway = ScriptedAuthGateway(MockupSeedData.CLIENT_ID)
        val graph = graph(gateway = gateway, users = users)
        val vm = SessionViewModel(graph)
        val published = mutableListOf<String>()
        try {
            idleUntil { users.seen(MockupSeedData.CLIENT_ID) }
            gateway.emit(MockupSeedData.STAFF_ID)
            assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
            assertTrue(vm.state.value is SessionState.Checking)
            users.release(MockupSeedData.CLIENT_ID)
            idleUntil { users.seen(MockupSeedData.STAFF_ID) }
            vm.noteSignedIn(published)
            assertTrue(vm.state.value !is SessionState.SignedIn)
        } finally {
            users.release(MockupSeedData.CLIENT_ID)
            users.release(MockupSeedData.STAFF_ID)
        }
        idleUntil {
            vm.noteSignedIn(published)
            val state = vm.state.value
            state is SessionState.SignedIn && state.user.id == MockupSeedData.STAFF_ID
        }
        assertEquals(listOf(MockupSeedData.STAFF_ID), published.distinct())
        assertEquals(MockupSeedData.STAFF_ID, vm.scopeId.value)
        assertEquals(listOf(MockupSeedData.CLIENT_ID, MockupSeedData.STAFF_ID), users.calls)
    }

    @Test
    fun `retry after a profile failure loads the same uid once more and signs in`() {
        val users = GatedUsers().also { it.failNext(1) }
        val graph = graph(signedInUid = MockupSeedData.CLIENT_ID, users = users)
        val vm = SessionViewModel(graph)
        idleUntil { vm.state.value is SessionState.ProfileError }
        val message = (vm.state.value as SessionState.ProfileError).message
        assertEquals("No se pudo cargar su perfil. Revise su conexión e intente de nuevo.", message)
        assertFalse(message.contains("offline"))
        assertEquals(listOf(MockupSeedData.CLIENT_ID), users.calls)

        vm.retry()
        idleUntil { vm.state.value is SessionState.SignedIn }
        assertEquals(MockupSeedData.CLIENT_ID, (vm.state.value as SessionState.SignedIn).user.id)
        assertEquals(MockupSeedData.CLIENT_ID, vm.scopeId.value)
        assertEquals(listOf(MockupSeedData.CLIENT_ID, MockupSeedData.CLIENT_ID), users.calls)
    }

    @Test
    fun `the same uid loads the profile once`() {
        val users = GatedUsers()
        val gateway = ScriptedAuthGateway(MockupSeedData.CLIENT_ID)
        val graph = graph(gateway = gateway, users = users)
        val vm = SessionViewModel(graph)
        idleUntil { vm.state.value is SessionState.SignedIn }
        assertEquals(listOf(MockupSeedData.CLIENT_ID), users.calls)

        gateway.emit(MockupSeedData.CLIENT_ID)
        pumpMain(400_000_000L)
        assertEquals(listOf(MockupSeedData.CLIENT_ID), users.calls)
        assertEquals(MockupSeedData.CLIENT_ID, (vm.state.value as SessionState.SignedIn).user.id)
        assertEquals(MockupSeedData.CLIENT_ID, vm.scopeId.value)
    }

    @Test
    fun `registration does not load the profile before the user document exists`() {
        val users = GatedUsers()
        val gateway = ScriptedAuthGateway(null)
        val graph = graph(gateway = gateway, users = users)
        val vm = SessionViewModel(graph)
        assertEquals(SessionState.SignedOut(), vm.state.value)

        val newId = "USR-NEW"
        gateway.emit(newId)
        pumpMain(400_000_000L)
        assertTrue(users.calls.isEmpty())
        assertEquals(SessionState.SignedOut(), vm.state.value)
        assertEquals(newId, gateway.currentUid())

        graph.repositories.users.save(
            User(newId, "Nueva Persona", "nueva@demo.test", "70000000", UserRole.CLIENT),
        )
        vm.onAuthenticated(newId)
        idleUntil { vm.state.value is SessionState.SignedIn }
        assertEquals(newId, (vm.state.value as SessionState.SignedIn).user.id)
        assertEquals(newId, vm.scopeId.value)
        assertEquals(listOf(newId), users.calls)
    }

    @Test
    fun `a revoked session returns to signed-out with a Spanish notice`() {
        val graph = graph(signedInUid = MockupSeedData.CLIENT_ID)
        val vm = SessionViewModel(graph)
        idleUntil { vm.state.value is SessionState.SignedIn }
        graph.authGateway.signOut()
        val signedOut = vm.state.value as SessionState.SignedOut
        assertEquals("Su sesión ya no es válida. Inicie sesión de nuevo.", signedOut.message)
        assertEquals(SIGNED_OUT_SCOPE, vm.scopeId.value)
    }

    private fun graph(
        gateway: AuthGateway? = null,
        signedInUid: String? = null,
        serviceTypes: BlockingServiceTypes? = null,
        users: GatedUsers? = null,
    ): AppGraph {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val demo = demoBackend(today, signedInUid)
        var repositories = demo.repositories
        if (serviceTypes != null) {
            repositories = repositories.copy(serviceTypes = serviceTypes.wrapping(repositories.serviceTypes))
        }
        if (users != null) {
            repositories = repositories.copy(users = users.wrapping(repositories.users))
        }
        val backend = if (gateway == null && serviceTypes == null && users == null) {
            demo
        } else {
            DataBackend(
                repositories = repositories,
                authGateway = gateway ?: demo.authGateway,
                imageUploader = demo.imageUploader,
                imageUploaderConfigured = demo.imageUploaderConfigured,
                io = demo.io,
                staffBackfill = null,
                demoMode = true,
            )
        }
        return AppGraph(context, backend)
    }

    private fun offlineProfileGraph(): AppGraph {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val users = object : UserRepository {
            override fun findById(id: String): User? = throw UnknownHostException("offline")
            override fun findAll(): List<User> = throw UnknownHostException("offline")
            override fun save(entity: User): User = throw UnknownHostException("offline")
            override fun deleteById(id: String): Boolean = throw UnknownHostException("offline")
            override fun nextId(): String = throw UnknownHostException("offline")
        }
        val backend = DataBackend(
            repositories = Repositories(users = users),
            authGateway = com.eficazautomotriz.app.data.auth.InMemoryAuthGateway(
                users,
                emptyMap(),
                signedInUid = "uid-offline",
            ),
            imageUploader = ImageUploader { _, name -> "https://demo.invalid/$name" },
            imageUploaderConfigured = false,
            io = Dispatchers.IO.limitedParallelism(1),
            staffBackfill = null,
            demoMode = true,
        )
        return AppGraph(context, backend)
    }

    private fun idleUntil(condition: () -> Boolean) {
        pumpMain(8_000_000_000L, condition)
        assertTrue(condition())
    }

    /** Lets a stale profile result publish if the session failed to ignore it. */
    private fun pumpMain(nanos: Long, condition: () -> Boolean = { false }, sample: () -> Unit = {}) {
        val deadline = System.nanoTime() + nanos
        while (!condition() && System.nanoTime() < deadline) {
            sample()
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        sample()
    }
}

private fun SessionViewModel.noteSignedIn(published: MutableList<String>) {
    val state = state.value
    if (state is SessionState.SignedIn) published.add(state.user.id)
}

/**
 * Holds [ServiceTypeRepository.findAll] so a test can sign out while catalogs are loading.
 * [wrapping] binds the real repository after the demo backend exists.
 */
private class BlockingServiceTypes : ServiceTypeRepository {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    private var inner: ServiceTypeRepository? = null

    fun wrapping(real: ServiceTypeRepository): ServiceTypeRepository {
        inner = real
        return this
    }

    override fun findAll(): List<ServiceType> {
        entered.countDown()
        check(release.await(8, TimeUnit.SECONDS)) { "catalog read was not released" }
        return inner!!.findAll()
    }

    override fun findById(id: String): ServiceType? = inner!!.findById(id)
    override fun save(entity: ServiceType): ServiceType = inner!!.save(entity)
    override fun deleteById(id: String): Boolean = inner!!.deleteById(id)
    override fun nextId(): String = inner!!.nextId()
    override fun findActive(): List<ServiceType> = inner!!.findActive()
    override fun nameOf(serviceTypeId: String): String = inner!!.nameOf(serviceTypeId)
}

/**
 * Counts and, when asked, blocks [UserRepository.findById]. The first failing calls throw
 * [UnknownHostException] so a retry can be observed without a clock.
 */
private class GatedUsers : UserRepository {
    val calls = CopyOnWriteArrayList<String>()
    private val entered = ConcurrentHashMap<String, CountDownLatch>()
    private val release = ConcurrentHashMap<String, CountDownLatch>()
    private val failuresLeft = AtomicInteger(0)
    private var inner: UserRepository? = null

    fun wrapping(real: UserRepository): UserRepository {
        inner = real
        return this
    }

    fun hold(id: String) {
        entered.putIfAbsent(id, CountDownLatch(1))
        release.putIfAbsent(id, CountDownLatch(1))
    }

    fun seen(id: String): Boolean = entered[id]?.count == 0L

    fun release(id: String) {
        release[id]?.countDown()
    }

    fun failNext(times: Int) {
        failuresLeft.set(times)
    }

    override fun findById(id: String): User? {
        calls.add(id)
        entered[id]?.countDown()
        if (failuresLeft.getAndUpdate { left -> if (left > 0) left - 1 else 0 } > 0) {
            throw UnknownHostException("offline")
        }
        val latch = release[id]
        if (latch != null) {
            check(latch.await(8, TimeUnit.SECONDS)) { "profile read was not released: $id" }
        }
        return inner!!.findById(id)
    }

    override fun findAll(): List<User> = inner!!.findAll()
    override fun save(entity: User): User = inner!!.save(entity)
    override fun deleteById(id: String): Boolean = inner!!.deleteById(id)
    override fun nextId(): String = inner!!.nextId()
}

/** Auth gateway whose uid the test changes without going through login or registration. */
private class ScriptedAuthGateway(initial: String?) : AuthGateway {
    private val listeners = mutableListOf<(String?) -> Unit>()
    private var uid: String? = initial

    override fun currentUid(): String? = uid

    fun emit(next: String?) {
        uid = next
        listeners.toList().forEach { it(next) }
    }

    override suspend fun signInWithEmail(email: String, password: String): String =
        throw UnsupportedOperationException("scripted")

    override suspend fun registerWithEmail(name: String, phone: String, email: String, password: String): String =
        throw UnsupportedOperationException("scripted")

    override suspend fun signInWithGoogle(idToken: String): String =
        throw UnsupportedOperationException("scripted")

    override suspend fun sendPasswordReset(email: String) = Unit

    override fun signOut() = emit(null)

    override fun observeUid(onChange: (String?) -> Unit): () -> Unit {
        listeners.add(onChange)
        onChange(uid)
        return { listeners.remove(onChange) }
    }
}

/** Email sign-in fails the way Firebase does when the device is offline. */
private class OfflineAuthGateway : AuthGateway {
    override fun currentUid(): String? = null
    override suspend fun signInWithEmail(email: String, password: String): String =
        throw FirebaseNetworkException("Unable to resolve host")

    override suspend fun registerWithEmail(name: String, phone: String, email: String, password: String): String =
        throw FirebaseNetworkException("Unable to resolve host")

    override suspend fun signInWithGoogle(idToken: String): String =
        throw FirebaseNetworkException("Unable to resolve host")

    override suspend fun sendPasswordReset(email: String) {
        throw FirebaseNetworkException("Unable to resolve host")
    }

    override fun signOut() = Unit
    override fun observeUid(onChange: (String?) -> Unit): () -> Unit = {}
}
