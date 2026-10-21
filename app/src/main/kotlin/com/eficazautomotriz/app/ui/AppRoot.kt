package com.eficazautomotriz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.auth.AuthTab
import com.eficazautomotriz.app.ui.auth.FirebaseMissingScreen
import com.eficazautomotriz.app.ui.auth.LoginScreen
import com.eficazautomotriz.app.ui.client.AppointmentDetailScreen
import com.eficazautomotriz.app.ui.client.AppointmentsScreen
import com.eficazautomotriz.app.ui.client.ClientHistoryScreen
import com.eficazautomotriz.app.ui.client.ClientHomeScreen
import com.eficazautomotriz.app.ui.client.MileageScreen
import com.eficazautomotriz.app.ui.client.NewAppointmentScreen
import com.eficazautomotriz.app.ui.client.ProfileScreen
import com.eficazautomotriz.app.ui.client.ServiceDetailScreen
import com.eficazautomotriz.app.ui.client.ServiceHistoryScreen
import com.eficazautomotriz.app.ui.client.VehicleDetailScreen
import com.eficazautomotriz.app.ui.client.VehicleFormScreen
import com.eficazautomotriz.app.ui.client.VehiclesScreen
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.navigation.Routes
import com.eficazautomotriz.app.ui.staff.CompleteOrderScreen
import com.eficazautomotriz.app.ui.staff.NewOrderScreen
import com.eficazautomotriz.app.ui.staff.OrderDetailScreen
import com.eficazautomotriz.app.ui.staff.SlotsScreen
import com.eficazautomotriz.app.ui.staff.StaffAppointmentScreen
import com.eficazautomotriz.app.ui.staff.StaffPanelScreen
import com.eficazautomotriz.app.ui.staff.StaffServicesScreen
import com.eficazautomotriz.app.ui.staff.evidence.EvidenceScreen
import com.eficazautomotriz.app.ui.staff.reports.ReportsScreen
import com.eficazautomotriz.app.ui.staff.settings.SettingsScreen
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.NavLabelStyle
import com.eficazautomotriz.app.viewmodel.EficazViewModelFactory
import com.eficazautomotriz.app.viewmodel.SessionState
import com.eficazautomotriz.app.viewmodel.SessionViewModel
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.UserRole
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI root. `graph` is null when google-services.json is missing: then only the disabled
 * login with the warning is shown, without creating ViewModels that would touch Firebase.
 */
@Composable
fun AppRoot(graph: AppGraph?) {
    if (graph == null) {
        FirebaseMissingScreen()
        return
    }
    val sessionFactory = remember(graph) { EficazViewModelFactory(graph, actor = null) }
    val session: SessionViewModel = viewModel(factory = sessionFactory)
    val state by session.state.collectAsStateWithLifecycle()

    // The NavHost is mounted only after the first session check, so its start destination is fixed.
    var resolved by rememberSaveable { mutableStateOf(false) }
    if (state !is SessionState.Checking) resolved = true
    if (!resolved) {
        LoadingBox()
        return
    }
    SessionScaffold(graph, session, state)
}

private fun homeOf(user: User?): String = when (user?.role) {
    null -> Routes.LOGIN
    UserRole.CLIENT -> Routes.CLIENT_HOME
    UserRole.STAFF -> Routes.STAFF_PANEL
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val clientTabs = listOf(
    Tab(Routes.CLIENT_HOME, "Inicio", Icons.Outlined.Home),
    Tab(Routes.CLIENT_VEHICLES, "Vehículos", Icons.Outlined.DirectionsCar),
    Tab(Routes.CLIENT_APPOINTMENTS, "Citas", Icons.Outlined.CalendarMonth),
    Tab(Routes.CLIENT_HISTORY, "Historial", Icons.Outlined.History),
    Tab(Routes.CLIENT_PROFILE, "Perfil", Icons.Outlined.Person),
)
private val staffTabs = listOf(
    Tab(Routes.STAFF_PANEL, "Citas", Icons.Outlined.CalendarMonth),
    Tab(Routes.STAFF_SERVICES, "Servicios", Icons.Outlined.Build),
    Tab(Routes.STAFF_REPORTS, "Reportes", Icons.Outlined.BarChart),
    Tab(Routes.STAFF_PROFILE, "Perfil", Icons.Outlined.Person),
)

private val tabRoutes = (Routes.CLIENT_TABS + Routes.STAFF_TABS).toSet()

/** Navigates leaving `route` as the only back stack entry (session change). */
private fun NavHostController.resetTo(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

@Composable
private fun SessionScaffold(graph: AppGraph, session: SessionViewModel, state: SessionState) {
    val user = (state as? SessionState.SignedIn)?.user
    val navController = rememberNavController()
    val startDestination = remember { homeOf(user) }
    val snackbar = remember { SnackbarHostState() }
    val vmFactory = remember(graph, user) { EficazViewModelFactory(graph, user) }

    // User change (login, logout, missing profile): the whole back stack is cleared.
    var shownUserId by rememberSaveable { mutableStateOf(user?.id) }
    LaunchedEffect(user?.id) {
        if (user?.id != shownUserId) {
            shownUserId = user?.id
            navController.resetTo(homeOf(user))
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = when (user?.role) {
        UserRole.CLIENT -> clientTabs
        UserRole.STAFF -> staffTabs
        null -> emptyList()
    }
    val onTab = tabs.any { it.route == route }
    val shell = ShellActions(
        user = user,
        onBack = null,
        onProfile = {
            val profile = if (user?.role == UserRole.STAFF) Routes.STAFF_PROFILE else Routes.CLIENT_PROFILE
            navController.navigateToTab(profile, homeOf(user))
        },
        onSignOut = { session.signOut() },
    )

    CompositionLocalProvider(
        LocalVmFactory provides vmFactory,
        LocalSnackbar provides snackbar,
        LocalShell provides shell,
    ) {
        Scaffold(
            containerColor = EficazColors.Background,
            // No top bar: each screen draws its navy header behind the status bar (edge-to-edge).
            // Without a bottom bar, content still clears the gesture/navigation bar.
            contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (user != null && onTab) {
                    EficazBottomBar(tabs, route) { navController.navigateToTab(it, homeOf(user)) }
                }
            },
        ) { padding ->
            // Edge-to-edge turns off adjustResize, so the keyboard inset is applied here once for
            // every screen: the scroll containers shrink above the keyboard and keep the focused
            // field in view. The bar padding is consumed first so it is not counted twice.
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            sessionMessage = (state as? SessionState.SignedOut)?.message,
                            onAuthenticated = session::onAuthenticated,
                        )
                    }
                    // Kept as a route (deep links, old callers): the same screen opened on "Crear cuenta".
                    composable(Routes.REGISTER) {
                        LoginScreen(
                            sessionMessage = null,
                            onAuthenticated = session::onAuthenticated,
                            initialTab = AuthTab.REGISTER,
                        )
                    }
                    clientDestinations(navController, user, onSignOut = { session.signOut() })
                    staffDestinations(navController, user, onSignOut = { session.signOut() })
                }
                // While the profile loads or fails, the current screen is covered.
                when (state) {
                    SessionState.Checking -> SurfaceCover { LoadingBox() }
                    is SessionState.ProfileError -> SurfaceCover {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            ErrorBox(state.message, onRetry = session::retry)
                            OutlinedNavyButton("Cerrar sesión", onClick = { session.signOut() })
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String, home: String) {
    // The home tab is the root of the stack: pop back to it. Navigating to it with restoreState
    // would bring back the stack saved under `home`, i.e. the tab the user is leaving.
    if (route == home && popBackStack(home, inclusive = false)) return
    navigate(route) {
        popUpTo(home) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * White bottom bar with a 1dp top border; selected item navy with bold label, others gray, no
 * pill. Drawn by hand because the Material NavigationBar is fixed at 80dp (the mockup's is ~56dp).
 */
@Composable
private fun EficazBottomBar(tabs: List<Tab>, route: String?, onSelect: (String) -> Unit) {
    Column(Modifier.background(Color.White)) {
        HorizontalDivider(color = EficazColors.CardBorder)
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(EficazDimens.BottomBarHeight)
                .selectableGroup(),
        ) {
            tabs.forEach { tab ->
                val selected = route == tab.route
                val color = if (selected) EficazColors.Navy else EficazColors.TextMuted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab, onClick = { if (!selected) onSelect(tab.route) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(EficazDimens.BottomBarIcon))
                    Spacer(Modifier.height(EficazDimens.SpaceXxs))
                    Text(
                        tab.label,
                        style = NavLabelStyle,
                        color = color,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SurfaceCover(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = EficazColors.Background) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Registers a destination. When [header] is given, the screen is framed by [ScreenScaffold]
 * with that header; pass `header = null` for screens that draw their own [AppHeader].
 * The back arrow ([ShellActions.onBack]) is enabled on every non-tab route.
 *
 * Next wave: to move a header into its screen, change that route's `header = { ... }` to
 * `header = null` and call `ScreenScaffold(header = { AppHeader(...) })` inside the screen.
 */
private fun NavGraphBuilder.screen(
    route: String,
    nav: NavHostController,
    header: (@Composable () -> Unit)?,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) = composable(route, arguments) { entry ->
    val shell = LocalShell.current
    val back: (() -> Unit)? = if (route in tabRoutes) {
        null
    } else {
        { if (nav.currentBackStackEntry?.id == entry.id) nav.popBackStack() }
    }
    CompositionLocalProvider(LocalShell provides shell?.copy(onBack = back)) {
        if (header == null) {
            content(entry)
        } else {
            ScreenScaffold(header = header) { content(entry) }
        }
    }
}

/** "Hola, Diego": first word of the signed-in user's name. */
@Composable
private fun greeting(): String =
    "Hola, " + (LocalShell.current?.user?.name?.trim()?.substringBefore(' ')?.ifBlank { null } ?: "bienvenido")

private val headerDate = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-SV"))

private fun NavGraphBuilder.clientDestinations(nav: NavHostController, user: User?, onSignOut: () -> Unit) {
    screen(Routes.CLIENT_HOME, nav, header = { AppHeader(greeting(), "Rol: Cliente") }) {
        ClientHomeScreen(
            onVehicle = { nav.navigate(Routes.vehicleDetail(it)) },
            onAppointment = { nav.navigate(Routes.appointmentDetail(it)) },
            onService = { nav.navigate(Routes.serviceDetail(it)) },
            onNewAppointment = { nav.navigate(Routes.CLIENT_APPOINTMENT_NEW) },
            onVehicles = { nav.navigate(Routes.CLIENT_VEHICLES) },
            onHistory = { vehicleId ->
                nav.navigate(vehicleId?.let(Routes::vehicleHistory) ?: Routes.CLIENT_VEHICLES)
            },
            onMileage = { vehicleId ->
                nav.navigate(vehicleId?.let(Routes::vehicleMileage) ?: Routes.CLIENT_VEHICLES)
            },
        )
    }
    screen(Routes.CLIENT_VEHICLES, nav, header = null) {
        VehiclesScreen(
            onVehicle = { nav.navigate(Routes.vehicleDetail(it)) },
            onNewVehicle = { nav.navigate(Routes.CLIENT_VEHICLE_NEW) },
        )
    }
    screen(Routes.CLIENT_VEHICLE_NEW, nav, header = { AppHeader("Nuevo vehículo", "Registro de vehículo", showAvatar = false) }) {
        VehicleFormScreen(onCreated = { id ->
            nav.navigate(Routes.vehicleDetail(id)) { popUpTo(Routes.CLIENT_VEHICLE_NEW) { inclusive = true } }
        })
    }
    screen(Routes.CLIENT_VEHICLE_DETAIL, nav, header = null) { entry ->
        val id = entry.arguments?.getString(Routes.ARG_ID).orEmpty()
        VehicleDetailScreen(
            onMileage = { nav.navigate(Routes.vehicleMileage(id)) },
            onHistory = { nav.navigate(Routes.vehicleHistory(id)) },
            onDeleted = { nav.popBackStack() },
            onNewAppointment = { nav.navigate(Routes.CLIENT_APPOINTMENT_NEW) },
            onService = { nav.navigate(Routes.serviceDetail(it)) },
            onAppointment = { nav.navigate(Routes.appointmentDetail(it)) },
        )
    }
    screen(Routes.CLIENT_VEHICLE_MILEAGE, nav, header = null) {
        MileageScreen(onSaved = { nav.popBackStack() })
    }
    screen(Routes.CLIENT_VEHICLE_HISTORY, nav, header = { AppHeader("Historial de servicios", showAvatar = false) }) {
        ServiceHistoryScreen(onService = { nav.navigate(Routes.serviceDetail(it)) })
    }
    screen(Routes.CLIENT_SERVICE_DETAIL, nav, header = null) {
        ServiceDetailScreen()
    }
    screen(Routes.CLIENT_HISTORY, nav, header = null) {
        ClientHistoryScreen(onService = { nav.navigate(Routes.serviceDetail(it)) })
    }
    screen(Routes.CLIENT_PROFILE, nav, header = null) {
        if (user != null) ProfileScreen(user, onSignOut = onSignOut)
    }
    screen(Routes.CLIENT_APPOINTMENTS, nav, header = null) {
        AppointmentsScreen(
            onAppointment = { nav.navigate(Routes.appointmentDetail(it)) },
            onNewAppointment = { nav.navigate(Routes.CLIENT_APPOINTMENT_NEW) },
        )
    }
    screen(Routes.CLIENT_APPOINTMENT_NEW, nav, header = null) {
        NewAppointmentScreen(
            onNewVehicle = { nav.navigate(Routes.CLIENT_VEHICLE_NEW) },
            onViewAppointments = {
                nav.navigate(Routes.CLIENT_APPOINTMENTS) {
                    popUpTo(Routes.CLIENT_HOME)
                    launchSingleTop = true
                }
            },
        )
    }
    screen(Routes.CLIENT_APPOINTMENT_DETAIL, nav, header = { AppHeader("Detalle de la cita", showAvatar = false) }) {
        AppointmentDetailScreen()
    }
}

private fun NavGraphBuilder.staffDestinations(nav: NavHostController, user: User?, onSignOut: () -> Unit) {
    screen(
        Routes.STAFF_PANEL,
        nav,
        header = { AppHeader("Panel del personal", "Rol: Personal · ${headerDate.format(LocalDate.now())}") },
    ) {
        StaffPanelScreen(
            onAppointment = { nav.navigate(Routes.staffAppointment(it)) },
            onOrder = { nav.navigate(Routes.orderDetail(it)) },
            onNewOrder = { nav.navigate(Routes.newOrder()) },
            onReschedule = { nav.navigate(Routes.staffAppointment(it)) },
            onEvidence = { nav.navigate(Routes.orderEvidence(it)) },
            onComplete = { nav.navigate(Routes.orderComplete(it)) },
            onReports = { nav.navigateToTab(Routes.STAFF_REPORTS, Routes.STAFF_PANEL) },
        )
    }
    screen(Routes.STAFF_APPOINTMENT_DETAIL, nav, header = null) {
        StaffAppointmentScreen(onCreateOrder = { nav.navigate(Routes.newOrder(appointmentId = it)) })
    }
    screen(
        Routes.STAFF_ORDER_NEW,
        nav,
        header = { AppHeader("Recepción de vehículo", "Nueva orden de servicio", showAvatar = false) },
        arguments = listOf(navArgument(Routes.ARG_APPOINTMENT_ID) { type = NavType.StringType; nullable = true }),
    ) {
        NewOrderScreen(onCreated = { id ->
            // The reception form leaves the back stack: back from the order goes to the panel.
            nav.navigate(Routes.orderDetail(id)) { popUpTo(Routes.STAFF_PANEL) }
        })
    }
    screen(Routes.STAFF_ORDER_DETAIL, nav, header = null) { entry ->
        val id = entry.arguments?.getString(Routes.ARG_ID).orEmpty()
        OrderDetailScreen(
            onEvidence = { nav.navigate(Routes.orderEvidence(id)) },
            onComplete = { nav.navigate(Routes.orderComplete(id)) },
        )
    }
    screen(Routes.STAFF_ORDER_EVIDENCE, nav, header = { AppHeader("Evidencias fotográficas", "Recepción, diagnóstico y resultado", showAvatar = false) }) {
        EvidenceScreen()
    }
    screen(Routes.STAFF_ORDER_COMPLETE, nav, header = { AppHeader("Finalizar orden", "Kilometraje y mantenimientos realizados", showAvatar = false) }) {
        CompleteOrderScreen(onCompleted = { nav.popBackStack() })
    }
    screen(Routes.STAFF_SERVICES, nav, header = null) {
        StaffServicesScreen(
            onOrder = { nav.navigate(Routes.orderDetail(it)) },
            onNewOrder = { nav.navigate(Routes.newOrder()) },
        )
    }
    screen(
        Routes.STAFF_REPORTS,
        nav,
        header = { AppHeader("Reportes", "Panel del personal · periodo configurable", showAvatar = false) },
    ) { ReportsScreen() }
    screen(Routes.STAFF_PROFILE, nav, header = null) {
        if (user != null) ProfileScreen(user, onSignOut = onSignOut, onSettings = { nav.navigate(Routes.STAFF_SETTINGS) })
    }
    screen(Routes.STAFF_SETTINGS, nav, header = { AppHeader("Configuración", "Catálogos y franjas horarias", showAvatar = false) }) {
        SettingsScreen()
    }
    screen(Routes.STAFF_SLOTS, nav, header = { AppHeader("Franjas horarias", "Capacidad por día y horario", showAvatar = false) }) { SlotsScreen() }
}
