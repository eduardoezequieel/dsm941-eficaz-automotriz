# Eficaz Automotriz · Etapa 3 — App Android

Entrega: **18 de octubre de 2026**.

App del taller hecha con Jetpack Compose y Firebase. Las reglas de negocio están en el módulo `:core` (Kotlin puro) y las pantallas en `:app`. Las rutas `ui/` y `viewmodel/` de este documento cuelgan de `app/src/main/kotlin/com/eficazautomotriz/app/`; las de `core/`, de `core/src/main/kotlin/com/eficazautomotriz/`.

| Integrante | Carnet |
|---|---|
| Christian Gustavo Crespín Lozano | CL060107 |
| Diego Guillermo Esnard Romero | ER231474 |
| Diego René López Martínez | LM231893 |
| Eduardo Ezequiel López Rivera | LR230061 |

Cada integrante trabajó en su rama `dev/<apellido>` y la integró a `main` por pull request.

## 1. Configuración

Ningún secreto se versiona: `app/google-services.json` y `local.properties` están en `.gitignore`. Las plantillas son `app/google-services.json.example` y `local.properties.example`.

**Firebase**

1. Crear el proyecto y habilitar Authentication con **Email/Password** y **Google**.
2. Crear Firestore en modo **producción** y publicar `firestore.rules`.
3. Crear el índice de `firestore.indexes.json` (`firebase deploy --only firestore:indexes`).
4. Descargar `google-services.json` a `app/`. Sin él la app compila, pero abre con el aviso «Firebase no configurado».
5. Registrar el SHA-1 de debug (`./gradlew :app:signingReport`) para Google Sign-In.

**Cloudinary.** Un upload preset **unsigned** con carpeta `eficaz/evidencias`. La app no necesita API secret.

**`local.properties`**

```
sdk.dir=/ruta/al/Android/sdk
cloudinary.cloudName=
cloudinary.uploadPreset=
google.webClientId=        # client ID web de Firebase, para Google Sign-In
demo.inMemory=false
```

**Personal (STAFF).** El registro siempre crea `role = "CLIENT"`. Para el personal, cambiar `role` a `"STAFF"` en `usuarios/{uid}` desde la consola. El primer ingreso del personal siembra los catálogos (`tiposServicio`, `tiposMantenimiento`, `configFranjas`) y reconstruye las colecciones de apoyo (`contadores`, `placas`, `ocupacion`, ver `data/firestore/SupportDocuments.kt`).

### Modo demostración (sin Firebase)

Con `demo.inMemory=true`, la compilación **debug** usa los repositorios en memoria de `core`, cargados con los datos de los mockups de la Etapa 1 (`app/src/debug/.../demo/MockupSeedData.kt`). No necesita `google-services.json` ni conexión.

- Arranca con la sesión de Diego abierta; el login aparece al cerrar sesión desde Perfil.
- Cuentas: `cliente@demo.test` y `personal@demo.test`, ambas con `Demo1234`.
- Los datos se pierden al cerrar la app, las evidencias reciben una URL ficticia y no hay Google Sign-In.
- Release nunca activa este modo.

## 2. Compilar y probar

JDK 17 y Android SDK 35 (`minSdk` 26).

```
./gradlew :core:test :app:testDebugUnitTest   # 171 + 152 pruebas, sin Firebase ni Cloudinary
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 3. Arquitectura

```
app (Android)                         core (JVM puro)
┌─────────────────────────────┐       ┌──────────────────────────────────┐
│ ui/        Composables      │       │ domain/      modelos + 3 reglas  │
│   ↓ observa StateFlow       │       │ application/ 7 servicios         │
│ viewmodel/ ViewModels       │ ───►  │ data/        interfaces + InMemory│
│ di/AppGraph  arma todo      │       │ logging/     ErrorLogger          │
│ data/firestore, cloudinary, │       └──────────────────────────────────┘
│      auth  (implementan las interfaces de core.data)
└─────────────────────────────┘
```

Ningún archivo de `ui/` importa Firebase ni OkHttp, y ningún archivo de `core` importa `android.*`. La navegación está en `ui/AppRoot.kt` y las rutas en `ui/navigation/Routes.kt`. Al arrancar, `SessionViewModel` abre el inicio del cliente o el panel del personal según el rol.

Las tres reglas de negocio, las decisiones de diseño y los límites de validación están en [docs/etapa2/REGLAS-Y-DECISIONES.md](docs/etapa2/REGLAS-Y-DECISIONES.md).

## 4. Requisitos mínimos

| Requisito | Dónde se cumple |
|---|---|
| 5 pantallas o más con navegación | 21 pantallas (`*Screen.kt`) en `ui/auth/`, `ui/client/` y `ui/staff/`; `NavHost` en `ui/AppRoot.kt` |
| Dos roles con permisos distintos | `UserRole`, `requireStaff` en `core/application/ApplicationService.kt` y `firestore.rules`. Solo el personal edita catálogos (`CatalogService`) |
| Cámara y galería | `ui/staff/evidence/EvidenceScreen.kt` (`TakePicture` y `PickVisualMedia`); compresión y subida en `data/cloudinary/` |
| Dos gráficas | `BarChart` y `DonutChart` en `ui/staff/reports/Charts.kt` (Canvas, sin librería) |
| Tres módulos con lógica real | `core/domain/rules/`: `AppointmentAvailabilityRule`, `MaintenanceStatusRule`, `ServiceTransitionRule` |
| Validación en todos los formularios | `viewmodel/validation/FormValidators.kt`, `core/domain/validation/` y `ValidationError` |
| Reglas de Firestore fuera del modo de prueba | `firestore.rules`: cada cliente lee solo lo suyo; el personal, todo |
| Sesión al arrancar y cierre en ambos roles | `viewmodel/SessionViewModel.kt` y Perfil |
| Sin credenciales en el repo | Solo las plantillas `.example` |

## 5. Guion de demo (5 minutos)

1. **Cliente.** Pide una cita: vehículo, tipo de servicio, fecha y franja libre.
2. **Personal.** Confirma la cita en el panel.
3. **Orden.** Desde la cita confirmada crea la orden de servicio.
4. **Fotos.** Sube una foto de cámara y una de galería.
5. **En proceso.** Avanza la orden.
6. **Cierre.** Finaliza con un kilometraje mayor o igual al actual y marca «Cambio de aceite».
7. **Cliente.** La cita figura Atendida, el historial muestra el servicio con fotos y el cambio de aceite queda Vigente.
8. **Reportes.** Barras de citas por estado y dona de servicios por tipo.

Cuentas en el proyecto Firebase `eficaz-automotriz` (ficticias): `cliente.demo@eficaz.test` y `staff.demo@eficaz.test` (STAFF), ambas con `Demo1234`.

## 6. Limitaciones conocidas

- Crear vehículos, citas e ids usa transacciones de Firestore: sin conexión fallan en lugar de quedar en cola.
- Las reglas limitan los contadores de `ocupacion` a ±1 por escritura, pero no los atan a una cita concreta. El personal revisa la disponibilidad de nuevo al confirmar.
- `GoogleSignIn` está marcado como deprecated por Google, aunque sigue funcionando.
