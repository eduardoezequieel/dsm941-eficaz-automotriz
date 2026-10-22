package com.eficazautomotriz.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.BoxedTextField
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.PhoneVisualTransformation
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.SegmentedTabs
import com.eficazautomotriz.app.ui.components.StatusBarBand
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.AuthViewModel
import com.eficazautomotriz.app.viewmodel.LoginForm
import com.eficazautomotriz.app.viewmodel.PasswordResetForm
import com.eficazautomotriz.app.viewmodel.RegisterForm
import com.eficazautomotriz.app.viewmodel.validation.ProfileFields

/** Segments of the P1 control: the register form lives in the same screen as the login. */
enum class AuthTab { LOGIN, REGISTER }

/**
 * P1 (f1): logo, segmented "Iniciar sesión / Crear cuenta", boxed fields, inline errors,
 * Google button, password reset and the Firebase footer. `sessionMessage` carries session
 * notices (e.g. "Cuenta sin perfil").
 */
@Composable
fun LoginScreen(
    sessionMessage: String?,
    onAuthenticated: (String) -> Unit,
    initialTab: AuthTab = AuthTab.LOGIN,
    vm: AuthViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    LaunchedEffect(state.authenticatedUid) {
        state.authenticatedUid?.let { uid ->
            vm.consumeAuthenticated()
            onAuthenticated(uid)
        }
    }
    MessageSnackbar(state.notice, vm::consumeNotice)

    AuthFrame(tab = tab, onTab = { tab = it }, enabled = !state.busy) {
        when (tab) {
            AuthTab.LOGIN -> LoginFields(
                form = state.login,
                busy = state.busy,
                message = state.message ?: sessionMessage,
                enabled = true,
                onChange = vm::updateLogin,
                onSubmit = vm::signIn,
                onForgotPassword = vm::openPasswordReset,
                google = {
                    val clientId = vm.googleWebClientId
                    if (clientId != null) {
                        GoogleSignInButton(
                            webClientId = clientId,
                            enabled = !state.busy,
                            onIdToken = vm::signInWithGoogle,
                            onError = vm::showMessage,
                        )
                    } else {
                        OutlinedNavyButton(
                            "Continuar con Google",
                            onClick = { vm.showMessage("El inicio con Google no está configurado en esta compilación.") },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
            AuthTab.REGISTER -> RegisterFields(
                form = state.register,
                busy = state.busy,
                message = state.message,
                onChange = vm::updateRegister,
                onSubmit = vm::register,
            )
        }
    }

    state.reset?.let { reset ->
        PasswordResetDialog(
            form = reset,
            onEmail = vm::updateResetEmail,
            onSend = vm::sendPasswordReset,
            onDismiss = vm::closePasswordReset,
        )
    }
}

/** Without `google-services.json` there is no Firebase: the disabled form is shown with a warning. */
@Composable
fun FirebaseMissingScreen() {
    AuthFrame(tab = AuthTab.LOGIN, onTab = {}, enabled = false) {
        InfoBanner(
            "Firebase no configurado: falta app/google-services.json. " +
                "Descárguelo de la consola de Firebase y vuelva a compilar.",
            tone = StatusTone.Danger,
        )
        LoginFields(
            form = LoginForm(),
            busy = false,
            message = null,
            enabled = false,
            onChange = {},
            onSubmit = {},
            onForgotPassword = {},
            google = null,
        )
    }
}

/** Shared frame: navy status bar band, vertically centered column with logo, title and tabs. */
@Composable
private fun AuthFrame(
    tab: AuthTab,
    onTab: (AuthTab) -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(EficazColors.Background)) {
        StatusBarBand()
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).imePadding()) {
            val minHeight = maxHeight
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight)
                    .padding(horizontal = EficazDimens.FormPadding, vertical = EficazDimens.SpaceLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(EficazDimens.SectionGap, Alignment.CenterVertically),
            ) {
                BrandBlock()
                SegmentedTabs(
                    options = listOf("Iniciar sesión", "Crear cuenta"),
                    selectedIndex = tab.ordinal,
                    onSelect = { onTab(AuthTab.entries[it]) },
                    enabled = enabled,
                    modifier = Modifier.padding(top = EficazDimens.SpaceSm),
                )
                content()
            }
        }
    }
}

@Composable
private fun BrandBlock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(EficazColors.Navy),
            contentAlignment = Alignment.Center,
        ) {
            Text("EA", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(EficazDimens.SectionGap))
        Text("Eficaz Automotriz", style = MaterialTheme.typography.headlineMedium, color = EficazColors.Navy)
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        Text(
            "Gestión de citas y mantenimiento vehicular",
            style = MaterialTheme.typography.bodyMedium,
            color = EficazColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoginFields(
    form: LoginForm,
    busy: Boolean,
    message: String?,
    enabled: Boolean,
    onChange: ((LoginForm) -> LoginForm) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    google: (@Composable () -> Unit)?,
) {
    val fieldsEnabled = enabled && !busy
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd)) {
        BoxedTextField(
            label = "Correo electrónico",
            value = form.email,
            onValueChange = { v -> onChange { it.copy(email = v) } },
            placeholder = "cliente@correo.com",
            isError = form.errors["email"] != null,
            enabled = fieldsEnabled,
            keyboardType = KeyboardType.Email,
            maxLength = ProfileFields.EMAIL.maxLength,
            inputFilter = InputFilters.Email,
        )
        BoxedTextField(
            label = "Contraseña",
            value = form.password,
            onValueChange = { v -> onChange { it.copy(password = v) } },
            isError = form.errors["password"] != null,
            enabled = fieldsEnabled,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            maxLength = ProfileFields.PASSWORD.maxLength,
        )
        ErrorLines(listOfNotNull(form.errors["email"], form.errors["password"], message))
        PrimaryButton("Iniciar sesión", onClick = onSubmit, enabled = fieldsEnabled, busy = busy, modifier = Modifier.fillMaxWidth())
        if (google != null) {
            OrDivider()
            google()
        }
        Text(
            "¿Olvidó su contraseña?",
            style = MaterialTheme.typography.bodyMedium,
            color = EficazColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = fieldsEnabled, onClick = onForgotPassword)
                .padding(vertical = EficazDimens.SpaceSm),
        )
    }
}

@Composable
private fun RegisterFields(
    form: RegisterForm,
    busy: Boolean,
    message: String?,
    onChange: ((RegisterForm) -> RegisterForm) -> Unit,
    onSubmit: () -> Unit,
) {
    val enabled = !busy
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd)) {
        BoxedTextField(
            "Nombre completo", form.name, { v -> onChange { it.copy(name = v) } },
            isError = form.errors["name"] != null, enabled = enabled,
            maxLength = ProfileFields.NAME.maxLength,
            inputFilter = InputFilters.PersonName,
        )
        BoxedTextField(
            "Teléfono", form.phone, { v -> onChange { it.copy(phone = v) } },
            placeholder = "7000-0000", isError = form.errors["phone"] != null, enabled = enabled,
            keyboardType = KeyboardType.Phone,
            maxLength = ProfileFields.PHONE.maxLength,
            inputFilter = InputFilters.Phone,
            visualTransformation = PhoneVisualTransformation,
        )
        BoxedTextField(
            "Correo electrónico", form.email, { v -> onChange { it.copy(email = v) } },
            placeholder = "cliente@correo.com", isError = form.errors["email"] != null, enabled = enabled,
            keyboardType = KeyboardType.Email,
            maxLength = ProfileFields.EMAIL.maxLength,
            inputFilter = InputFilters.Email,
        )
        BoxedTextField(
            "Contraseña", form.password, { v -> onChange { it.copy(password = v) } },
            placeholder = "Mínimo 6 caracteres", isError = form.errors["password"] != null, enabled = enabled,
            isPassword = true, keyboardType = KeyboardType.Password,
            maxLength = ProfileFields.PASSWORD.maxLength,
        )
        BoxedTextField(
            "Confirmar contraseña", form.confirmation, { v -> onChange { it.copy(confirmation = v) } },
            isError = form.errors["confirmation"] != null, enabled = enabled,
            isPassword = true, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
            maxLength = ProfileFields.PASSWORD.maxLength,
        )
        ErrorLines(
            listOfNotNull(
                form.errors["name"], form.errors["phone"], form.errors["email"],
                form.errors["password"], form.errors["confirmation"], message,
            ),
        )
        PrimaryButton("Crear cuenta", onClick = onSubmit, enabled = enabled, busy = busy, modifier = Modifier.fillMaxWidth())
        Text(
            "Las cuentas nuevas se registran con el rol Cliente.",
            style = MaterialTheme.typography.bodySmall,
            color = EficazColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorLines(errors: List<String>) {
    if (errors.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXs)) { errors.distinct().forEach { FieldError(it) } }
}

@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = EficazColors.CardBorder)
        Text("o", color = EficazColors.TextSecondary, modifier = Modifier.padding(horizontal = EficazDimens.ScreenPadding))
        HorizontalDivider(Modifier.weight(1f), color = EficazColors.CardBorder)
    }
}

@Composable
private fun PasswordResetDialog(
    form: PasswordResetForm,
    onEmail: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!form.busy) onDismiss() },
        containerColor = Color.White,
        title = { Text("Restablecer contraseña", style = MaterialTheme.typography.titleMedium, color = EficazColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                Text(
                    "Le enviaremos un enlace para crear una nueva contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                )
                BoxedTextField(
                    "Correo electrónico", form.email, onEmail,
                    isError = form.error != null, enabled = !form.busy,
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Send,
                    maxLength = ProfileFields.EMAIL.maxLength,
                    inputFilter = InputFilters.Email,
                )
                form.error?.let { FieldError(it) }
            }
        },
        confirmButton = { PrimaryButton("Enviar enlace", onClick = onSend, busy = form.busy, compact = true) },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !form.busy) { Text("Cancelar", color = EficazColors.Navy) }
        },
    )
}
