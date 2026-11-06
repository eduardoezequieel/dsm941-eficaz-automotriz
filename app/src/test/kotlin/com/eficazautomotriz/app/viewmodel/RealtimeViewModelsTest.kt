package com.eficazautomotriz.app.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eficazautomotriz.app.data.ImageUploader
import com.eficazautomotriz.app.data.auth.InMemoryAuthGateway
import com.eficazautomotriz.app.demo.MockupSeedData
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.di.DataBackend
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.staff.StaffPanel
import com.eficazautomotriz.app.viewmodel.staff.StaffPanelViewModel
import com.eficazautomotriz.data.Repositories
import com.eficazautomotriz.data.VehicleRepository
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Each screen collects the in-memory repository flow. A later save, with no second load,
 * must show up in UiState. That is the demo stand-in for a Firestore snapshot.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class RealtimeViewModelsTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun clientAppointmentsUpdateWhenTheStoredStatusChanges() {
        val graph = graph()
        val vm = AppointmentsViewModel(graph, graph.repositories.users.findById(MockupSeedData.CLIENT_ID)!!)
        vm.load()
        val before = readyAppointments(vm).upcoming.first { it.appointment.id == "CT-1051" }
        assertEquals(AppointmentStatus.PENDING, before.appointment.status)

        val stored = graph.repositories.appointments.findById("CT-1051")!!
        graph.repositories.appointments.save(stored.copy(status = AppointmentStatus.CONFIRMED))

        val after = readyAppointments(vm).upcoming.first { it.appointment.id == "CT-1051" }
        assertEquals(AppointmentStatus.CONFIRMED, after.appointment.status)
    }

    @Test
    fun staffPanelShowsARequestAsSoonAsItIsSaved() {
        val graph = graph()
        val vm = StaffPanelViewModel(graph, graph.repositories.users.findById(MockupSeedData.STAFF_ID)!!)
        vm.load()
        val before = readyPanel(vm)
        assertTrue(before.pending.none { it.appointment.id == "CT-2000" })

        val sample = graph.repositories.appointments.findById("CT-1051")!!
        graph.repositories.appointments.save(sample.copy(id = "CT-2000", status = AppointmentStatus.PENDING))

        val after = readyPanel(vm)
        assertEquals(before.counts.pendingConfirmation + 1, after.counts.pendingConfirmation)
        assertEquals(AppointmentStatus.PENDING, after.pending.first { it.appointment.id == "CT-2000" }.appointment.status)
    }

    @Test
    fun serviceDetailFollowsStatusAndEvidenceChanges() {
        val graph = graph()
        val vm = ServiceDetailViewModel(
            graph,
            graph.repositories.users.findById(MockupSeedData.CLIENT_ID)!!,
            "SV-2104",
        )
        assertEquals(ServiceStatus.IN_PROGRESS, readyDetail(vm).order.detail.order.status)
        assertEquals(0, readyDetail(vm).order.detail.evidences.size)

        val order = graph.repositories.serviceOrders.findById("SV-2104")!!
        graph.repositories.serviceOrders.save(order.copy(status = ServiceStatus.COMPLETED, closedAt = order.receivedAt))
        assertEquals(ServiceStatus.COMPLETED, readyDetail(vm).order.detail.order.status)

        graph.repositories.evidences.save(
            Evidence(
                id = "EVD-900",
                serviceOrderId = "SV-2104",
                stage = EvidenceStage.DIAGNOSIS,
                imageUrl = "https://demo.invalid/evidencias/nuevo.jpg",
                uploadedAt = order.receivedAt,
            ),
        )
        val after = readyDetail(vm)
        assertEquals(ServiceStatus.COMPLETED, after.order.detail.order.status)
        assertEquals(listOf("EVD-900"), after.order.detail.evidences.map { it.id })
    }

    @Test
    fun clientAppointmentsKeepTheLastListWhenTheVehicleReadFails() {
        val gate = ReadGate()
        val graph = graph(gate)
        val vm = AppointmentsViewModel(graph, graph.repositories.users.findById(MockupSeedData.CLIENT_ID)!!)
        vm.load()
        assertEquals(AppointmentStatus.PENDING, readyAppointments(vm).upcoming.first { it.appointment.id == "CT-1051" }.appointment.status)

        gate.failReads = true
        val stored = graph.repositories.appointments.findById("CT-1051")!!
        graph.repositories.appointments.save(stored.copy(status = AppointmentStatus.CONFIRMED))

        val still = readyAppointments(vm)
        assertEquals(AppointmentStatus.PENDING, still.upcoming.first { it.appointment.id == "CT-1051" }.appointment.status)
        assertEquals(SYSTEM_READ_MESSAGE, vm.message.value)
    }

    @Test
    fun clientAppointmentsDoNotReportCancellationFromTheVehicleRead() {
        val gate = ReadGate()
        val graph = graph(gate)
        val vm = AppointmentsViewModel(graph, graph.repositories.users.findById(MockupSeedData.CLIENT_ID)!!)
        vm.load()
        assertEquals(
            AppointmentStatus.PENDING,
            readyAppointments(vm).upcoming.first { it.appointment.id == "CT-1051" }.appointment.status,
        )
        assertNull(vm.message.value)

        gate.cancelled = true
        val stored = graph.repositories.appointments.findById("CT-1051")!!
        try {
            graph.repositories.appointments.save(stored.copy(status = AppointmentStatus.CONFIRMED))
        } catch (e: CancellationException) {
            // flowOn delivers this to catch while the collector is still active, and the
            // rethrow can surface on the emitter under the unconfined test dispatcher.
        }

        assertEquals(
            AppointmentStatus.PENDING,
            readyAppointments(vm).upcoming.first { it.appointment.id == "CT-1051" }.appointment.status,
        )
        assertTrue(vm.state.value is UiState.Ready)
        assertNull(vm.message.value)
    }

    @Test
    fun staffPanelKeepsTheLastPanelWhenTheVehicleReadFails() {
        val gate = ReadGate()
        val graph = graph(gate)
        val vm = StaffPanelViewModel(graph, graph.repositories.users.findById(MockupSeedData.STAFF_ID)!!)
        vm.load()
        val before = readyPanel(vm).counts.pendingConfirmation

        gate.failReads = true
        val sample = graph.repositories.appointments.findById("CT-1051")!!
        graph.repositories.appointments.save(sample.copy(id = "CT-2000", status = AppointmentStatus.PENDING))

        assertEquals(before, readyPanel(vm).counts.pendingConfirmation)
        assertTrue(readyPanel(vm).pending.none { it.appointment.id == "CT-2000" })
        assertEquals(SYSTEM_READ_MESSAGE, vm.message.value)
    }

    @Test
    fun serviceDetailKeepsTheLastOrderWhenTheVehicleReadFails() {
        val gate = ReadGate()
        val graph = graph(gate)
        val vm = ServiceDetailViewModel(
            graph,
            graph.repositories.users.findById(MockupSeedData.CLIENT_ID)!!,
            "SV-2104",
        )
        assertEquals(ServiceStatus.IN_PROGRESS, readyDetail(vm).order.detail.order.status)

        gate.failReads = true
        val order = graph.repositories.serviceOrders.findById("SV-2104")!!
        graph.repositories.serviceOrders.save(order.copy(status = ServiceStatus.COMPLETED, closedAt = order.receivedAt))

        assertEquals(ServiceStatus.IN_PROGRESS, readyDetail(vm).order.detail.order.status)
        assertTrue(vm.state.value is UiState.Ready)
    }

    private fun graph(vehicles: ReadGate? = null): AppGraph {
        val repositories = Repositories()
        MockupSeedData.load(repositories, LocalDate.of(2026, 11, 2))
        val wired = if (vehicles == null) {
            repositories
        } else {
            repositories.copy(vehicles = GatedVehicleRepository(repositories.vehicles, vehicles))
        }
        val backend = DataBackend(
            repositories = wired,
            authGateway = InMemoryAuthGateway(
                repositories.users,
                MockupSeedData.accounts,
                MockupSeedData.CLIENT_ID,
                dispatcher,
            ),
            imageUploader = ImageUploader { _, fileName -> "https://demo.invalid/evidencias/$fileName" },
            imageUploaderConfigured = true,
            io = dispatcher,
            staffBackfill = null,
            demoMode = true,
        )
        val context = ApplicationProvider.getApplicationContext<Application>()
        return AppGraph(context, backend)
    }

    private fun readyAppointments(vm: AppointmentsViewModel): ClientAppointments {
        val state = vm.state.value
        assertTrue("expected Ready, was $state", state is UiState.Ready)
        return (state as UiState.Ready).data
    }

    private fun readyPanel(vm: StaffPanelViewModel): StaffPanel {
        val state = vm.state.value
        assertTrue("expected Ready, was $state", state is UiState.Ready)
        return (state as UiState.Ready).data
    }

    private fun readyDetail(vm: ServiceDetailViewModel): ServiceDetail {
        val state = vm.state.value
        assertTrue("expected Ready, was $state", state is UiState.Ready)
        return (state as UiState.Ready).data
    }
}

/** Turns the next vehicle read into the failure a Firestore get would throw after sign-out. */
private class ReadGate {
    var failReads: Boolean = false
    var cancelled: Boolean = false
}

private class GatedVehicleRepository(
    private val inner: VehicleRepository,
    private val gate: ReadGate,
) : VehicleRepository {
    private fun <T> read(block: () -> T): T {
        if (gate.cancelled) throw CancellationException("load cancelled")
        if (gate.failReads) throw IllegalStateException("PERMISSION_DENIED")
        return block()
    }

    override fun findById(id: String): Vehicle? = read { inner.findById(id) }
    override fun findAll(): List<Vehicle> = read { inner.findAll() }
    override fun save(entity: Vehicle): Vehicle = inner.save(entity)
    override fun deleteById(id: String): Boolean = inner.deleteById(id)
    override fun nextId(): String = inner.nextId()
    override fun findByOwner(ownerId: String): List<Vehicle> = read { inner.findByOwner(ownerId) }
    override fun findByPlate(plate: String): Vehicle? = read { inner.findByPlate(plate) }
    override fun plateExists(plate: String): Boolean = read { inner.plateExists(plate) }
}

private const val SYSTEM_READ_MESSAGE =
    "No se pudo completar la operación. Revise su conexión e intente de nuevo."
