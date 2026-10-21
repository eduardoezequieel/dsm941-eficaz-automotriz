# Eficaz Automotriz · Etapa 3 — App Android

Entrega: **18 de octubre de 2026**.

App del taller (Jetpack Compose + Firebase). El núcleo de negocio sigue en el módulo `:core`; las pantallas están en `:app`. Las secciones 3 a 6, más abajo, son las reglas de la Etapa 2 y no cambiaron.

Las rutas `ui/` y `viewmodel/` de este documento cuelgan de `app/src/main/kotlin/com/eficazautomotriz/app/`. Las de `core/` cuelgan de `core/src/main/kotlin/com/eficazautomotriz/`.

### Integrantes

| Integrante | Carnet | Rama de trabajo |
|---|---|---|
| Christian Gustavo Crespín Lozano | CL060107 | `feature/firestore-data` |
| Diego Guillermo Esnard Romero | ER231474 | `feature/cliente-ui` |
| Diego René López Martínez | LM231893 | `feature/staff-ui-reportes` |
| Eduardo Ezequiel López Rivera | LR230061 | `feature/core-build-docs` |

Los cuatro deben ser colaboradores del repositorio. Cada rama se integra a `main` por pull request; el orden y los comandos están en `docs/etapa3/REPARTO-RAMAS.md`.

## 1. Configuración

Hacen falta un proyecto de Firebase, una cuenta de Cloudinary y un `local.properties` local. Ninguno de esos secretos se commitea.

### Firebase

1. Crear un proyecto en la consola de Firebase.
2. Habilitar Authentication con **Email/Password** y **Google**.
3. Crear Firestore en modo **producción** (no dejar `allow read, write: if true`).
4. Pegar el contenido de `firestore.rules` en las reglas de Firestore y publicarlo. Si el proyecto ya tenía datos de una versión anterior, **el primer ingreso de una cuenta STAFF reconstruye `contadores`, `placas` y `ocupacion`** (ver «Colecciones de apoyo» abajo); conviene hacerlo antes de que los clientes vuelvan a registrar vehículos o pedir citas.
5. Crear el índice compuesto de `firestore.indexes.json` (ver abajo).
6. Descargar `google-services.json` y dejarlo en `app/`. No se commitea. La plantilla de campos está en `app/google-services.json.example`. Sin ese archivo el proyecto compila igual (Gradle avisa que omite el plugin de Google Services), pero Firebase no arranca: la app abre en el login inactivo con el aviso «Firebase no configurado».
7. Registrar el SHA-1 de debug para que Google Sign-In funcione:

```
./gradlew :app:signingReport
```

Copiar el SHA-1 de la variante `debug` en la configuración de Android del proyecto Firebase.

### Índice de `estadosMantenimiento`

`FirestoreMaintenanceRecordRepository.findByVehicleAndType` filtra por `vehicleId` y `maintenanceTypeId` a la vez. Con dos igualdades Firestore suele resolver la consulta combinando los índices simples; el archivo `firestore.indexes.json` declara además el índice compuesto (ámbito `COLLECTION`, ambos campos ascendentes) para no depender de esa combinación.

Crearlo con la CLI, desde la raíz del repo y con el proyecto ya asociado:

```
firebase deploy --only firestore:indexes
```

O crearlo a mano en la consola: Firestore → Indexes → Composite → colección `estadosMantenimiento`, `vehicleId` Ascending, `maintenanceTypeId` Ascending, scope Collection.

Si alguna vez Firestore exige el índice, la consulta falla con `FAILED_PRECONDITION` y el log incluye un enlace que crea el mismo índice.

Las colecciones de apoyo no necesitan índices nuevos: `ocupacion` se consulta con una sola igualdad (`date`), y `contadores` y `placas` se leen por id de documento.

### Colecciones de apoyo

Las reglas solo dejan leer `vehiculos`, `citas`, `servicios`, `evidencias` y `estadosMantenimiento` al dueño (directamente o a través del vehículo relacionado) y al personal. Tres operaciones que antes recorrían la colección entera usan ahora documentos de apoyo, definidos en `data/firestore/SupportDocuments.kt`:

| Colección | Documento | Para qué | Quién escribe |
|---|---|---|---|
| `contadores/{colección}` | `{ value }` | Último número emitido para los ids legibles (`VEH-007`). `nextId()` lo incrementa en una transacción. | Cualquier sesión, solo de 1 en 1. El personal puede subirlo más (relleno y catálogos sembrados). Nunca baja. |
| `placas/{PLACA}` | `{ vehicleId, ownerId }` | Placa reservada. Se crea en la misma transacción que el vehículo y se borra con él. `plateExists` lo lee para avisar «placa ya registrada» sin leer el vehículo de otro. | El dueño (o el personal) al crearla. No se modifica. |
| `ocupacion/{yyyy-MM-dd_slotId}` | `{ date, slotId, count }` | Lugares ocupados por franja y fecha. Se actualiza en la misma transacción que la cita (crear, cancelar, rechazar, atender, reprogramar). `AppointmentService` calcula la disponibilidad con esto. | Cualquier sesión, solo ±1 por escritura y nunca negativo. El personal, libre. |

`FirestoreBackfill` reconstruye las tres colecciones a partir de los datos existentes. Lo lanza `AppGraph.ensureCatalogs` en segundo plano cuando entra una cuenta STAFF, sin retrasar el inicio de sesión. Es idempotente: solo sube los contadores y nunca sobrescribe una placa u ocupación que ya exista. Los errores van al log.

### Cloudinary

1. Cuenta gratuita en Cloudinary.
2. Upload preset con **Signing mode = Unsigned** y carpeta `eficaz/evidencias`.
3. Anotar el cloud name y el nombre del preset. No hace falta API secret en la app: la subida es unsigned.

### `local.properties`

Copiar la plantilla y rellenar las cuatro claves (el archivo real está en `.gitignore`):

```
cp local.properties.example local.properties
```

```
sdk.dir=/ruta/al/Android/sdk
cloudinary.cloudName=
cloudinary.uploadPreset=
google.webClientId=
demo.inMemory=false
```

`google.webClientId` es el client ID de tipo Web de Firebase (hace falta para Google Sign-In). Las dos de Cloudinary salen del preset del paso anterior.

### Modo demostración (sin Firebase)

Con `demo.inMemory=true` en `local.properties`, la compilación **debug** no usa Firebase ni Cloudinary: `AppGraph` recibe los repositorios en memoria de `core`, cargados con `app/src/debug/.../demo/MockupSeedData.kt`, que reproduce los datos de los mockups de la Etapa 1 (Diego con Toyota Corolla 2016, Nissan Frontier 2019 y Hyundai Accent 2013; citas en todos los estados; el servicio «Sistema de frenos» con tres evidencias; órdenes en proceso; actividad del mes para Reportes). Las fechas se calculan a partir del día actual. Sirve para comparar capturas con los mockups y para la demo del curso sin conexión; no hace falta `google-services.json`.

- La app arranca con la sesión de Diego abierta. Para ver el login (P1), cierre sesión desde Perfil.
- Cuentas: `cliente@demo.test` / `Demo1234` (Diego, Cliente) y `personal@demo.test` / `Demo1234` (Carlos, Personal).
- Los datos viven solo en memoria: se pierden al cerrar el proceso. Google Sign-In no está disponible y las evidencias «subidas» reciben una URL ficticia.
- Las compilaciones **release** nunca activan el modo: `BuildConfig.DEMO_IN_MEMORY` es `false` en release y además se exige `BuildConfig.DEBUG`.

### Convertir un usuario en STAFF

El registro desde la app siempre crea `role = "CLIENT"`. Para el personal:

1. La persona se registra en la app (o entra con Google).
2. En la consola de Firebase, abrir Firestore → `usuarios/{uid}`.
3. Cambiar el campo `role` a `"STAFF"` (el nombre del enum, en mayúsculas).

El primer STAFF que inicia sesión siembra los catálogos (`tiposServicio`, `tiposMantenimiento`, `configFranjas`) mediante `AppGraph.ensureCatalogs`, que `SessionViewModel` llama al entrar. Si un cliente abre la app antes de que exista ese personal, la nueva cita muestra «Catálogo no configurado» y no hay franjas ni tipos de servicio.

## 2. Compilar y probar

Hace falta JDK 17 y Android SDK 35. `minSdk` es 26.

```
./gradlew :core:test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`:core:test` (157 pruebas) y `:app:testDebugUnitTest` (125 pruebas: mappers y documentos de apoyo de Firestore, validadores, lógica de pantallas, tonos de estado, semilla de demo y cálculos de las gráficas; con `-Pshots` genera capturas JVM de las pantallas principales en `app/build/shots`) no necesitan `google-services.json` ni Cloudinary. El flujo en el dispositivo sí.

## Arquitectura

```
app (Android)                         core (JVM puro)
┌─────────────────────────────┐       ┌──────────────────────────────────┐
│ ui/        Composables      │       │ domain/      modelos + 3 reglas  │
│   ↓ observa StateFlow       │       │ application/ 7 servicios         │
│ viewmodel/ ViewModels       │ ───►  │ data/        interfaces + InMemory│
│   ↓ withContext(IO)         │       │ logging/     ErrorLogger          │
│ di/AppGraph  arma todo      │       └──────────────────────────────────┘
│ data/firestore/*Repository  │  implementa las interfaces de core.data
│ data/cloudinary/*Uploader   │
│ data/auth/FirebaseAuthGateway│
│ logging/AndroidErrorLogger  │
└─────────────────────────────┘
```

Regla de oro (verificable leyendo imports): **ningún archivo en `ui/` importa `com.google.firebase.*` ni `okhttp3.*`**. Ningún archivo en `core` importa `android.*`.

La navegación vive en `ui/AppRoot.kt`. Al arrancar, `SessionViewModel` lee `currentUid()`; si hay sesión, `AuthService.signIn` lleva a `client/home` o a `staff/panel` según `user.role`. La barra del cliente es Inicio · Vehículos · Citas · Historial · Perfil. La del personal es Citas · Servicios · Reportes · Perfil; Configuración se abre desde el Perfil del personal. Ambos cierran sesión desde Perfil o desde el avatar del encabezado.

### Pantalla, ruta, ViewModel y caso de uso

Todas las pantallas están en `etapa3/base`. Las del cliente usan `viewmodel/*ViewModel.kt`; las del personal, `viewmodel/staff/*ViewModel.kt`. Las rutas están en `ui/navigation/Routes.kt`.

| Pantalla | Ruta | ViewModel | Caso de uso | Archivo |
|---|---|---|---|---|
| P1 Login / Registro | `auth/login`, `auth/register` | `AuthViewModel` | `FirebaseAuthGateway`, `AuthService.signIn` | `ui/auth/LoginScreen.kt`, `ui/auth/RegisterScreen.kt`, `viewmodel/AuthViewModel.kt` |
| P2 Inicio cliente | `client/home` | `ClientHomeViewModel` | `appointments.listVisibleTo`, `appointments.cancel`, `maintenance.reportFor`, `serviceOrders.historyFor` | `ui/client/ClientHomeScreen.kt`, `viewmodel/ClientHomeViewModel.kt` |
| P3 Mis vehículos | `client/vehicles` | `VehiclesViewModel` | `vehicles.listVisibleTo`, `maintenance.buildReport`; filtro local por placa, marca o modelo | `ui/client/VehiclesScreen.kt`, `viewmodel/VehiclesViewModel.kt` |
| Alta de vehículo | `client/vehicles/new` | `VehicleFormViewModel` | `vehicles.register` | `ui/client/VehicleFormScreen.kt`, `viewmodel/VehicleFormViewModel.kt` |
| P4 Detalle y mantenimiento | `client/vehicles/{id}` | `VehicleDetailViewModel` | `maintenance.reportFor`, `serviceOrders.historyFor`, `vehicles.delete` | `ui/client/VehicleDetailScreen.kt`, `viewmodel/VehicleDetailViewModel.kt` |
| Actualizar km | `client/vehicles/{id}/mileage` | `MileageViewModel` | `vehicles.updateMileage` | `ui/client/MileageScreen.kt`, `viewmodel/MileageViewModel.kt` |
| P5 Mis citas (pestañas Próximas / Historial) | `client/appointments` | `AppointmentsViewModel` | `appointments.listVisibleTo`, `appointments.cancel`, `appointments.acceptReschedule`, `appointments.declineReschedule` | `ui/client/AppointmentsScreen.kt`, `viewmodel/AppointmentsViewModel.kt` |
| Detalle de cita (cliente) | `client/appointments/{id}` | `AppointmentDetailViewModel` | detalle y cancelación de esa cita | `ui/client/AppointmentsScreen.kt` (`AppointmentDetailScreen`), `viewmodel/AppointmentDetailViewModel.kt` |
| P6 Nueva cita | `client/appointments/new` | `NewAppointmentViewModel` | `vehicles.listVisibleTo`, `appointments.activeServiceTypes`, `appointments.slotsFor`, `appointments.request` | `ui/client/NewAppointmentScreen.kt`, `viewmodel/NewAppointmentViewModel.kt` |
| P7 Historial | `client/vehicles/{id}/history` | `ServiceHistoryViewModel` | `serviceOrders.historyFor` | `ui/client/ServiceHistoryScreen.kt`, `viewmodel/ServiceHistoryViewModel.kt` |
| Detalle de servicio | `client/services/{id}` | `ServiceDetailViewModel` | `serviceOrders.detailOf`, `appointments.listVisibleTo` | `ui/client/ServiceHistoryScreen.kt` (`ServiceDetailScreen`), `viewmodel/ServiceDetailViewModel.kt` |
| P8 Panel personal | `staff/panel` | `StaffPanelViewModel` | `appointments.listPending`, `appointments.listVisibleTo`, `serviceOrders.listAll`, `appointments.confirm`, `appointments.reject`, `serviceOrders.advance` (contadores del día, solicitudes y órdenes en proceso) | `ui/staff/StaffPanelScreen.kt`, `viewmodel/staff/StaffPanelViewModel.kt`, `viewmodel/staff/StaffPanelLogic.kt` |
| Detalle de cita (staff) | `staff/appointments/{id}` | `StaffAppointmentViewModel` | `confirm`, `reject`, `reschedule`, `slotsFor` | `ui/staff/StaffAppointmentScreen.kt`, `ui/staff/SlotGrid.kt`, `viewmodel/staff/StaffAppointmentViewModel.kt` |
| Nueva orden | `staff/orders/new?appointmentId={id}` | `NewOrderViewModel` | `serviceOrders.create` | `ui/staff/NewOrderScreen.kt`, `viewmodel/staff/NewOrderViewModel.kt` |
| Detalle de orden | `staff/orders/{id}` | `OrderDetailViewModel` | `detailOf`, `advance` a En proceso | `ui/staff/OrderDetailScreen.kt`, `viewmodel/staff/OrderDetailViewModel.kt` |
| Evidencias | `staff/orders/{id}/evidence` | `EvidenceViewModel` | `ImageCompressor.compress`, `CloudinaryUploader.upload` y `serviceOrders.addEvidence` | `ui/staff/evidence/EvidenceScreen.kt`, `ui/staff/evidence/CameraFiles.kt`, `viewmodel/staff/EvidenceViewModel.kt`, `data/cloudinary/CloudinaryUploader.kt`, `data/cloudinary/ImageCompressor.kt` |
| Finalizar orden | `staff/orders/{id}/complete` | `CompleteOrderViewModel` | `maintenance.activeTypes`, `serviceOrders.complete` | `ui/staff/CompleteOrderScreen.kt`, `viewmodel/staff/CompleteOrderViewModel.kt` |
| P9 Reportes | `staff/reports` | `ReportsViewModel` | `reports.appointmentsByStatus`, `reports.completedServicesByType`, `reports.systemSummary` (periodo Semana, Mes, Trimestre o personalizado) | `ui/staff/reports/ReportsScreen.kt`, `ui/staff/reports/Charts.kt`, `ui/staff/reports/ChartColors.kt`, `ui/staff/reports/ChartMath.kt`, `viewmodel/staff/ReportsViewModel.kt`, `viewmodel/staff/ReportPeriods.kt` |
| Franjas | `staff/slots` | `SlotsViewModel` | `slotCatalog`, `setSlotEnabled`, `setSlotCapacity` | `ui/staff/SlotsScreen.kt`, `viewmodel/staff/SlotsViewModel.kt` |
| Configuración | `staff/settings` | `CatalogViewModel`, `SlotsViewModel` | pestañas Franjas, Servicios y Mantenimiento | `ui/staff/settings/SettingsScreen.kt`, `ui/staff/SlotsScreen.kt`, `viewmodel/staff/CatalogViewModel.kt`, `viewmodel/staff/SlotsViewModel.kt` |
| Servicios | `staff/settings` (pestaña) | `CatalogViewModel` | `catalog.serviceTypes`, `catalog.saveServiceType`, `catalog.setServiceTypeActive` | `ui/staff/settings/ServiceTypesTab.kt`, `viewmodel/staff/CatalogViewModel.kt` |
| Mantenimiento | `staff/settings` (pestaña) | `CatalogViewModel` | `catalog.maintenanceTypes`, `catalog.saveMaintenanceType`, `catalog.setMaintenanceTypeActive` | `ui/staff/settings/MaintenanceTypesTab.kt`, `viewmodel/staff/CatalogViewModel.kt` |

## Cumplimiento de requisitos mínimos

| Requisito | Dónde se cumple |
|---|---|
| Mínimo 5 pantallas con navegación real | Cliente: `ui/auth/LoginScreen.kt`, `ui/auth/RegisterScreen.kt`, `ui/client/ClientHomeScreen.kt`, `ui/client/VehiclesScreen.kt`, `ui/client/VehicleFormScreen.kt`, `ui/client/VehicleDetailScreen.kt`, `ui/client/MileageScreen.kt`, `ui/client/AppointmentsScreen.kt`, `ui/client/NewAppointmentScreen.kt`, `ui/client/ServiceHistoryScreen.kt`. Personal: `ui/staff/StaffPanelScreen.kt`, `ui/staff/StaffAppointmentScreen.kt`, `ui/staff/NewOrderScreen.kt`, `ui/staff/OrderDetailScreen.kt`, `ui/staff/evidence/EvidenceScreen.kt`, `ui/staff/CompleteOrderScreen.kt`, `ui/staff/reports/ReportsScreen.kt`, `ui/staff/SlotsScreen.kt`. El `NavHost` está en `ui/AppRoot.kt` y las rutas en `ui/navigation/Routes.kt`. |
| Dos roles con permisos distintos | `core/domain/model/enums/UserRole.kt`, `requireStaff` en `core/application/ApplicationService.kt`, y `firestore.rules`. El personal, y solo el personal, configura el catálogo de servicios y los intervalos de mantenimiento: `core/application/CatalogService.kt` (`requireStaff` en cada escritura) y la pestaña Configuración (`staff/settings`). |
| Cámara y galería | `ui/staff/evidence/EvidenceScreen.kt` (`TakePicture` con el `Uri` de `ui/staff/evidence/CameraFiles.kt` y `PickVisualMedia` solo imágenes). La compresión y la subida están en `data/cloudinary/ImageCompressor.kt` y `data/cloudinary/CloudinaryUploader.kt`, invocadas desde `viewmodel/staff/EvidenceViewModel.kt`. El `FileProvider` está en `app/src/main/AndroidManifest.xml` (sin permiso `CAMERA`: `TakePicture` abre la app de cámara). La URL queda en la colección `evidencias`. |
| Dos gráficas | `BarChart` y `DonutChart` en `ui/staff/reports/Charts.kt` (Canvas, sin librería), mostradas en `ui/staff/reports/ReportsScreen.kt`. Los cálculos de ancho y ángulo están en `ui/staff/reports/ChartMath.kt` y tienen pruebas. |
| Tres módulos con lógica real | Citas: `core/domain/rules/AppointmentAvailabilityRule.kt`. Mantenimiento: `core/domain/rules/MaintenanceStatusRule.kt`. Servicios (transiciones y cierre): `core/domain/rules/ServiceTransitionRule.kt`. |
| Validación en todos los formularios | `viewmodel/validation/FormValidators.kt`, `core/domain/error/ValidationError.kt` y `core/domain/validation/InputParsers.kt` |
| Reglas de Firestore publicadas, fuera del modo de prueba | `firestore.rules`: cada cliente lee solo sus vehículos, citas, servicios, evidencias y estados de mantenimiento (el personal lee todo). La disponibilidad, la placa única y los ids usan `ocupacion`, `placas` y `contadores` (ver «Colecciones de apoyo»). |
| Sesión al arrancar y cierre en ambos roles | `viewmodel/SessionViewModel.kt` y el menú de `ui/AppRoot.kt` |
| Sin credenciales en el repo | `app/google-services.json` y `local.properties` no se versionan. Plantillas: `app/google-services.json.example`, `local.properties.example` |
| 4 colaboradores, 4 ramas, commits repartidos | Tabla de integrantes de arriba (`feature/firestore-data`, `feature/cliente-ui`, `feature/staff-ui-reportes`, `feature/core-build-docs`) |

## Guion de demo (5 minutos)

Cuentas de la sección siguiente, con la segunda ya promovida a STAFF y los catálogos sembrados (el staff entró al menos una vez).

1. **Cliente (0:00).** Entra con el correo de cliente. Pide una cita: elige su vehículo, el tipo de servicio, una fecha y una franja libre.
2. **Staff (1:30).** Cierra sesión, entra con la cuenta de personal. En el panel confirma esa cita.
3. **Orden (2:15).** Desde la cita confirmada crea la orden de servicio.
4. **Fotos (2:45).** Sube una foto de cámara y una de galería (recepción, diagnóstico o resultado).
5. **En proceso (3:20).** Avanza la orden a En proceso.
6. **Cierre (3:40).** Finaliza con un kilometraje mayor o igual al actual y marca «Cambio de aceite».
7. **Cliente de nuevo (4:15).** La cita figura Atendida. En el historial del vehículo se ve el servicio con las fotos. El mantenimiento de cambio de aceite queda Al día, con el kilometraje nuevo.
8. **Reportes (4:40).** En Reportes se ven las dos gráficas: barras de citas por estado y dona de servicios finalizados por tipo.

## Cuentas de prueba

Son ficticias. Ya existen en el proyecto Firebase `eficaz-automotriz` (la segunda con `role` = `"STAFF"`). Si se usa otro proyecto, hay que crearlas en la app (registro con correo y contraseña) y, en la consola de Firebase, poner `role` = `"STAFF"` solo en la segunda.

| Uso | Correo | Contraseña |
|---|---|---|
| Cliente | `cliente.demo@eficaz.test` | `Demo1234` |
| Personal | `staff.demo@eficaz.test` | `Demo1234` |

## Limitaciones conocidas

- Los repositorios de Firestore bloquean con `Tasks.await` en el hilo de IO. Crear ids, guardar vehículos y guardar citas usa transacciones, que necesitan red: sin conexión esas operaciones fallan con un error de sistema en lugar de quedar en cola.
- Los contadores de `ocupacion` los mantiene la app. Las reglas limitan cada escritura a ±1 y a valores no negativos, pero no la atan a una cita concreta: un cliente malicioso podría mover un contador de uno en uno. El personal revisa disponibilidad de nuevo al confirmar.
- El relleno inicial no corrige un documento de `ocupacion` que ya exista. Si un cliente pide una cita nueva antes del primer ingreso del personal, las citas viejas de esa misma franja y fecha quedan sin contar.
- El relleno lee todas las colecciones en cada ingreso del personal. Con el volumen de la demo no importa; con muchos datos convendría correrlo una sola vez.
- `GoogleSignIn` (usado en `ui/auth/GoogleSignInButton.kt`) está marcado como deprecated por Google. Sigue funcionando con el SHA-1 de debug y el client ID web.

## Verificación con servicios reales

El 11/10/2026 se configuró el proyecto Firebase `eficaz-automotriz` (Auth con correo/contraseña y Google, Firestore en modo producción con `firestore.rules` publicadas y el índice de `firestore.indexes.json`, SHA-1 de debug registrada) y se probó en el emulador Pixel 8 (API 36) con `app/google-services.json` real:

- Registro con correo del cliente y del personal; `usuarios/{uid}` creado con `role` CLIENT; el intento de sembrar catálogos de un cliente lo deniegan las reglas (esperado).
- Tras poner `role` = `"STAFF"` en la consola, el primer inicio de sesión del personal sembró `tiposServicio`, `tiposMantenimiento` y `configFranjas` (36 franjas), y abrió el Panel del taller.
- El cliente registró `vehiculos/VEH-001` y P4 mostró «Sin registro previo» en los 4 tipos de mantenimiento.
- El cliente pidió `citas/APT-001` (martes 13/10/2026, 09:00) y al intentar otra cita la misma franja apareció «Lleno».

También se probó el flujo del personal con Cloudinary real (cloud name y preset unsigned `eficaz_evidencias` en `local.properties`):

- El personal confirmó `APT-001`, creó `servicios/SRV-001` desde la cita, subió una evidencia desde la galería (`evidencias/EVD-001` con `secure_url` en la carpeta `eficaz/evidencias`), avanzó a En proceso y finalizó con 30,500 km marcando «Cambio de aceite y filtro» (`estadosMantenimiento/MRC-001`).
- Reportes con datos reales: barra «Atendida 1 (100 %)» al ampliar el período hasta el 31/10 y dona «Mantenimiento preventivo 100 %».
- El cliente vio la cita Atendida, el servicio Finalizado con 1 evidencia en el historial y el vehículo «Al día» con 30,500 km.
- Durante la prueba se corrigió un error que hacía fallar toda subida (`ImageCompressor` interpretaba el `null` de `inJustDecodeBounds` como «No se pudo abrir la imagen»).

El 11/10/2026 (tarde) se publicaron las reglas por propietario en el proyecto real y se repitió la prueba en el emulador con la APK fusionada (`3562e0b`):

- El personal entró primero: el relleno creó `contadores` (citas = 1, vehiculos = 1) y `placas/P123456` a partir de los datos existentes; el panel nuevo cargó sin ningún `PERMISSION_DENIED` en el log.
- El cliente registró `vehiculos/VEH-002` (placa P987321): apareció `placas/P987321` con su `ownerId` y el detalle mostró los cuatro mantenimientos «Sin registro previo».
- El cliente pidió `citas/APT-002` (martes 13/10/2026, 10:00): se creó `ocupacion/2026-10-13_SLT-009` con `count` 1, y al cancelarla desde Mis citas el contador bajó a 0 y la cita pasó a la pestaña Historial.

Queda pendiente con credenciales reales:

- Google Sign-In de extremo a extremo (el botón aparece; falta probar el inicio de sesión real).
- Foto tomada con la cámara (en el emulador se probó solo la galería).

Nota del emulador: al volver del selector de fotos del sistema, Android a veces muestra «La app no responde» por un evento de foco de ventana pendiente; la traza no muestra trabajo de la app en el hilo principal. Pulsar «Esperar» basta. No se reprodujo en el segundo intento.

`:core:test` y `:app:testDebugUnitTest` no cubren esa integración: no hablan con Firebase ni con Cloudinary.

---

Las secciones 3 a 6 conservan el texto de la Etapa 2 (reglas de negocio, equivalencias de campos, decisiones y validación).


## 3. Las tres reglas de negocio

Cada regla es una clase sin estado. Recibe todo lo que necesita por parámetro y devuelve un
resultado: no consulta repositorios, no imprime y no lanza excepciones para casos de negocio
esperados. Eso las hace deterministas y comprobables.

### 3.1 `AppointmentAvailabilityRule` — ¿cabe una cita más en esta franja?

```kotlin
fun check(
    slot: TimeSlot,
    date: LocalDate,
    existingAppointments: List<Appointment>,
    today: LocalDate,
    config: SystemConfig,
): AvailabilityResult
```

**Orden de evaluación** — devuelve en la primera condición que falla:

| # | Condición | Motivo devuelto |
|---|---|---|
| 1 | `!slot.enabled` | `SLOT_DISABLED` |
| 2 | `date < today` | `DATE_IN_PAST` |
| 3 | `date.dayOfWeek != slot.dayOfWeek` | `DAY_MISMATCH` |
| 4 | citas ocupantes `>=` capacidad efectiva | `CAPACITY_REACHED` |
| 5 | — | `Available` |

**Fórmula del cupo:**

```
ocupadas = citas donde date == fecha && slotId == franja && status.occupiesSlot
capacidad efectiva = slot.capacity > 0 ? slot.capacity : config.defaultSlotCapacity
```

**Por qué ese orden.** Cada condición es más cara de evaluar que la anterior y cada una vuelve
irrelevantes a las siguientes: si la franja está deshabilitada no importa cuántos cupos le
queden, y si la fecha ya pasó no tiene sentido preguntar por el día de la semana. Contar las
citas ocupantes recorre la colección, así que se deja de último.

`today` entra **por parámetro**: la regla nunca llama a `LocalDate.now()`. Esa decisión es lo que
permite probarla con fechas fijas y que la prueba no cambie de resultado según el día en que se
ejecute.

La regla se invoca **dos veces**: al solicitar la cita y otra vez al confirmarla. En la segunda
llamada la lista de citas existentes **excluye la cita que se está confirmando**; de lo contrario
se compararía consigo misma y siempre reportaría capacidad llena.

### 3.2 `MaintenanceStatusRule` — ¿cómo está el mantenimiento preventivo?

```kotlin
fun evaluate(
    currentMileage: Int,
    lastServiceMileage: Int?,
    intervalKm: Int,
    warningThresholdKm: Int,
): MaintenanceEvaluation
```

**Orden de evaluación:**

| # | Condición | Clasificación |
|---|---|---|
| 0 | `intervalKm <= 0` | `IllegalArgumentException` (error de configuración, no de negocio) |
| 1 | `lastServiceMileage == null` | `NO_PREVIOUS_RECORD`, sin calcular nada más |
| 2 | `currentMileage >= recomendado` | `OVERDUE` |
| 3 | `currentMileage >= recomendado - umbral` | `DUE_SOON` |
| 4 | — | `UP_TO_DATE` |

**Fórmulas:**

```
recommendedMileage = lastServiceMileage + intervalKm
differenceKm       = recommendedMileage - currentMileage     // negativo si ya se excedió
```

**Por qué ese orden.** Sin kilometraje base no hay nada sobre lo cual proyectar: `null` sale de
inmediato con `recommendedMileage` y `differenceKm` en `null`, y no se calcula un número que
sería mentira. Después se pregunta primero por lo vencido y luego por lo próximo, porque las dos
condiciones se solapan: todo lo vencido cae también dentro de la ventana de aviso. Invertir el
orden clasificaría como «próximo» un mantenimiento que en realidad ya se pasó.

`intervalKm <= 0` se valida antes que todo con `require`, porque no es un desenlace del negocio
sino un catálogo mal configurado: el programa debe romperse ruidosamente, no devolver una
clasificación inventada.

**Casos límite cubiertos por pruebas:**

- `lastServiceMileage = 0` (vehículo nuevo, kilometraje válido) **no** es lo mismo que `null`.
- `currentMileage == recommendedMileage` → `OVERDUE`, no `DUE_SOON`.
- `currentMileage == recommendedMileage - warningThresholdKm` → `DUE_SOON`.
- `intervalKm <= 0` → excepción.

### 3.3 `ServiceTransitionRule` — ¿puede avanzar esta orden?

```kotlin
fun canTransition(from: ServiceStatus, to: ServiceStatus): TransitionResult
fun validateClosure(mileageAtService: Int?, lastKnownMileage: Int): TransitionResult
```

**Transiciones permitidas, y ninguna otra:**

```
RECEIVED    →  IN_PROGRESS
IN_PROGRESS →  COMPLETED
```

Sin retrocesos. `COMPLETED` es terminal: no admite transición ni edición de sus datos. Una
transición al mismo estado también se deniega.

`canTransition` está implementada con un `when` **exhaustivo sobre `from`, sin `else`**: si
mañana se agrega un estado al enum, el compilador rompe la compilación en vez de dejar pasar
silenciosamente una transición no contemplada.

**Reglas de cierre** (`validateClosure`):

- `mileageAtService != null` — es obligatorio al finalizar.
- `mileageAtService >= lastKnownMileage` — **el odómetro no decrece**.

Esa segunda condición es la que protege el cálculo de mantenimiento: si se pudiera cerrar una
orden con un kilometraje menor al conocido, la clasificación de la sección 3.2 quedaría basada en
un dato imposible.

---

## 4. Mapeo conceptual: Etapa 1 → código

La Etapa 1 documentó el modelo en español porque describía el modelo conceptual. El código está
íntegramente en inglés. Esta tabla es la correspondencia entre ambos.

| Etapa 1 | Código | Etapa 1 | Código |
|---|---|---|---|
| `usuarios` | `User` | `citas` | `Appointment` |
| `vehiculos` | `Vehicle` | `servicios` | `ServiceOrder` |
| `evidencias` | `Evidence` | `tiposServicio` | `ServiceType` |
| `tiposMantenimiento` | `MaintenanceType` | `estadosMantenimiento` | `MaintenanceRecord` |
| `configFranjas` | `TimeSlot` | `kilometrajeActual` | `currentMileage` |
| `kmUltimoMantenimiento` | `lastServiceMileage` | `kmRecomendado` | `recommendedMileage` (calculado) |
| `intervaloKm` | `intervalKm` | `umbralAviso` | `warningThresholdKm` |
| `franjaHoraria` | `slotId` → `TimeSlot` | `clasificación` | `MaintenanceStatus` |

---

## 5. Decisiones tomadas y su justificación

### 5.1 `RESCHEDULED` ocupa franja

La Etapa 1 dice que el conflicto de agenda se evalúa sobre las citas en estado «pendiente o
confirmada». Aquí se incluye además **`RESCHEDULED`**.

El motivo: una cita reprogramada no desaparece, apunta a una **fecha y una franja nuevas**, que
quedan efectivamente ocupadas. Excluirla del conteo permitiría vender dos veces la misma franja.
La decisión está codificada en la propiedad `AppointmentStatus.occupiesSlot`, de modo que la
regla no necesita saber cuáles estados son cuáles: pregunta por la propiedad.

```
occupiesSlot = true   →  PENDING, CONFIRMED, RESCHEDULED
isFinal      = true   →  ATTENDED, REJECTED, CANCELLED
```

El cliente responde a una reprogramación desde P5: **Aceptar** lleva `RESCHEDULED → CONFIRMED` (conserva el motivo) y **Rechazar** lleva `RESCHEDULED → CANCELLED`, lo que libera la franja (`acceptReschedule` / `declineReschedule`).

### 5.2 Los valores derivados no se persisten

`MaintenanceRecord` almacena **únicamente** `lastServiceMileage`. Ni el kilometraje recomendado
ni la clasificación se guardan: los calcula `MaintenanceStatusRule` cada vez que se consultan.

Persistir un valor derivado abre la puerta a que quede desincronizado del dato que lo origina.
Si se guardara la clasificación y el cliente actualizara su odómetro, la clasificación almacenada
quedaría mintiendo hasta que algo la recalculara. Al calcularla siempre, ese error es imposible
por construcción.

`lastServiceMileage = null` significa **«sin registro previo»**, que es distinto de `0`. Cero es
un kilometraje válido, el de un vehículo nuevo.

### 5.3 Los datos semilla son de ejemplo

Los usuarios, vehículos, placas, citas y órdenes de `SeedData` son **datos inventados para la
demostración de la cátedra**. No representan clientes, vehículos ni operaciones reales de Eficaz
Automotriz. Están elegidos para que los cuatro estados de mantenimiento queden representados y
para que los reportes no salgan vacíos.

### 5.4 Los valores numéricos son parámetros configurables

Ningún número de negocio está escrito dentro de las reglas. Todos viven en `SystemConfig`:

```kotlin
data class SystemConfig(
    val defaultSlotCapacity: Int = 1,
    val warningThresholdKm: Int = 500,
    val errorLogPath: String = "logs/errors.log",
    val minVehicleYear: Int = 1950,
    val maxMileage: Int = 2_000_000,
    val maxSlotCapacity: Int = 20,
    val schedulingHorizonYears: Long = 1,
    val reportHistoryYears: Long = 10,
)
```

Los topes de rango viven aquí y **no** en los menús. Antes el menú de vehículos ofrecía desde
`1900` mientras el caso de uso exigía `1950`: el usuario llenaba el formulario completo y el
rechazo llegaba al final. Ahora la consola pide exactamente el rango que el dominio acepta.

Estos valores son **parámetros de configuración iniciales, no políticas de Eficaz Automotriz**.
La capacidad de `1` cita por franja es un valor de demostración: la Etapa 1 declara
explícitamente que el equipo no conoce la capacidad real del taller. Que el valor sea
configurable, y que la capacidad se lea primero del `TimeSlot` y solo caiga al valor del sistema
cuando la franja no la define, es precisamente lo que permite ajustarlo cuando el dato real se
conozca, sin tocar ninguna regla.

### 5.5 `Outcome<T>` en lugar de `kotlin.Result`

Los casos de uso devuelven un sealed propio, `Outcome<T>`, y no `kotlin.Result`. `Result` exige
un `Throwable` en la rama de falla, y en este sistema los errores de negocio esperados se
**devuelven como dato, no se lanzan**: que una franja esté llena no es una falla del programa,
es un desenlace previsto.

### 5.6 La clasificación del vehículo es la más severa de sus tipos

Un vehículo tiene un `MaintenanceRecord` por tipo de mantenimiento. Para el resumen y las tablas
se necesita un solo estado por vehículo, y se toma **el más severo**:
`OVERDUE > DUE_SOON > UP_TO_DATE`. Un vehículo sin ninguna línea evaluable queda como
`NO_PREVIOUS_RECORD`. El criterio es conservador a propósito: un vehículo con el aceite vencido
no está «al día» porque los frenos sí lo estén.

### 5.7 Qué mantenimientos reinician su ciclo al cerrar una orden

El modelo no vincula `ServiceType` con `MaintenanceType`, y la Etapa 2 no agrega ese vínculo. Al
finalizar una orden, el personal indica **cuáles mantenimientos preventivos se realizaron**; solo
esos reciben el nuevo kilometraje base. Cerrar una reparación de motor no reinicia el ciclo del
cambio de aceite.

### 5.8 La cita pasa a `atendida` al **finalizar** el servicio, no al recibir el vehículo

`ServiceOrderService.complete` es el único punto que marca la cita como `ATTENDED`; `create` la
deja intacta. La orden guarda el `appointmentId`, así que al cerrar se sabe de qué cita venía.

El motivo: `ATTENDED` es un estado **final** (`isFinal = true`) y por tanto **libera el cupo**
(`occupiesSlot = false`). Marcarla al recibir el vehículo liberaría la franja mientras el auto
todavía está en el taller, y la agenda vendería ese hueco dos veces. Mientras el servicio está
abierto, la cita sigue en `CONFIRMED` ocupando su franja, que es la verdad de los hechos.

Si al cerrar la cita ya estaba en un estado final (la canceló el cliente, por ejemplo), el fallo
**no invalida la orden ya cerrada**: queda un `WARN` en el log (`AppointmentAlreadyFinal`) y el
cierre sigue adelante. Un dato de agenda inconsistente no debe deshacer trabajo real ya hecho.

### 5.9 El personal configura las franjas y, desde la Etapa 3, también los catálogos

`CatalogMenu` permite al personal **habilitar o deshabilitar una franja** y **cambiar su
capacidad** (`AppointmentService.setSlotEnabled` y `setSlotCapacity`). Son exactamente los dos
parámetros que lee `AppointmentAvailabilityRule`, así que configurar la agenda no requiere tocar
ninguna regla.

Ninguna de las dos operaciones toca las citas ya agendadas: deshabilitar una franja o reducir su
capacidad solo **frena las solicitudes nuevas**. Cancelar citas ya aceptadas es una decisión de
negocio distinta, con su propio caso de uso (`cancel`).

`capacity = 0` es válido y significa «usar `config.defaultSlotCapacity`», tal como lo define la
fórmula de la sección 3.1. Una capacidad **negativa** no significa nada y se rechaza con
`ValidationError.NegativeSlotCapacity`, devuelta como `DomainError.Invalid`: es un valor que el
usuario tecleó mal, no un catálogo corrupto.

En la Etapa 2 los **intervalos de mantenimiento (`MaintenanceType.intervalKm`) eran de solo lectura**
y la sección 3.2 confiaba en su `require`. En la Etapa 3 el personal ya edita el catálogo de
servicios y los intervalos desde la pestaña «Configuración» (`CatalogService`, Tabla 6 de la
Etapa 1). Tal como se anticipó, la validación se hizo **antes** de llegar a la regla:
`CatalogService.saveMaintenanceType` rechaza un intervalo fuera de `1..config.maxMileage` con
`ValidationError.IntervalOutOfRange`, igual que se hizo con la capacidad de la franja, así que el
`require` de `MaintenanceStatusRule` sigue protegiendo solo contra errores de programación.

---

## 6. Validación de entradas

Dos niveles, **una sola fuente de verdad**. Los límites de cada campo se declaran una vez y los
leen tanto la consola como los casos de uso: la consola no puede aceptar algo que el dominio
rechace, ni al revés.

**El catálogo — `domain/validation/TextFields`.** Cada campo de texto declara su nombre técnico,
su etiqueta en español, su longitud máxima y, cuando aplica, los caracteres admitidos:

```kotlin
val MAKE = TextField("make", "marca", maxLength = 30, allowed = NAME_CHARACTERS, ...)
```

`ValidationError.validateText(field, value)` aplica esa regla. La llama `ConsoleReader.readText`
al teclear y también `VehicleService.register`: el segundo no confía en el primero, porque en la
Etapa 3 la interfaz será otra.

**Formato y límites — `cli/io/ConsoleReader`.** Un único bucle de lectura (`readValidated`)
recorta la entrada, descarta lo que ni siquiera merece validarse —una línea de más de 1 000
caracteres, o con caracteres no imprimibles— y reintenta **como máximo tres veces**. Agotar los
intentos cancela la operación con un aviso; no hay reintento infinito. El programa nunca se cae
porque alguien escribió «hola» donde iba un número.

Las fechas se leen con `ResolverStyle.STRICT` y el patrón `dd/MM/uuuu`. Con el estilo por omisión
(`SMART`) una fecha imposible como `31/02/2026` **no falla**: `java.time` la ajusta en silencio al
`28/02/2026` y el usuario acabaría agendando en un día que nunca escribió. En estricto se rechaza
y se vuelve a preguntar. El estilo estricto exige el año independiente de era (`uuuu`, no
`yyyy`); de ahí el patrón.

Los enteros se aceptan con separador de miles **solo si está bien puesto**: `30,000` vale
`30 000`, pero `1,2,3` se rechaza en lugar de convertirse en `123`. Un valor mayor que `Int`
también se rechaza en vez de desbordar.

**Reglas de negocio — `domain` y `application`.** Verifican que el valor tenga sentido.

| Campo | Regla | Límite |
|---|---|---|
| `plate` | Formato de El Salvador: 1 a 3 letras de tipo, 3 números y 3 caracteres alfanuméricos (ejemplo `P123456` o `P12300A`); sin espacios; única | 7 a 9 caracteres |
| `make` | Letras, números, espacios y `. , ' & / -` | 30 |
| `model` | Igual que `make` | 30 |
| `year` | Entre `minVehicleYear` y el año actual + 1 | 1950 a 2027 |
| `currentMileage` | Entero, nunca menor al último valor conocido | 0 a `maxMileage` |
| `mileageAtService` | Obligatorio al cerrar, `>=` último conocido | 0 a `maxMileage` |
| `reason` (rechazo, reprogramación) | Obligatorio | 200 |
| `notes` (recepción) | Opcional; si se escribe, se valida | 500 |
| `imageUrl` | Empieza con `http://` o `https://` | 300 |
| `date` (cita) | Igual o posterior a hoy | Hasta hoy + `schedulingHorizonYears` |
| `date` (reporte) | — | Últimos `reportHistoryYears` años |
| `capacity` (franja) | `0` significa «usar la capacidad por omisión del sistema» | 0 a `maxSlotCapacity` |

El mensaje de ayuda nombra el campo en español: `ValidationError` lleva el nombre técnico
(`plate`) para el log y la etiqueta (`placa`) para la pantalla.

### Permisos

Cada caso de uso recibe el usuario de la sesión y **verifica el permiso ahí**, no solo en el
menú. Un cliente solo alcanza sus propios vehículos, citas y servicios. La verificación en el
menú es comodidad; la de `application` es la que cuenta. Al fallar devuelve
`DomainError.Forbidden`.
