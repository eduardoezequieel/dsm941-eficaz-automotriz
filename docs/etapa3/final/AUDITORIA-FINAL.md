# Auditoría · Entrega Final

Contraste del repositorio con la rúbrica de la Entrega Final y con los requisitos de la guía (sección 2 de `docs/etapa3/PROMPT-FINAL.md`), más la tabla de requisitos mínimos del README §4. La base auditada es `main` en `0378bd3` (`docs: note that block 2 only verifies core`), el 16 de noviembre de 2026.

Las rutas `ui/`, `viewmodel/`, `data/` y `di/` cuelgan de `app/src/main/kotlin/com/eficazautomotriz/app/`. Las rutas `core/…` cuelgan de `core/src/main/kotlin/com/eficazautomotriz/`.

## Línea base (antes de tocar nada)

| Comprobación | Resultado |
|---|---|
| `./gradlew clean :core:test :app:testDebugUnitTest :app:assembleDebug` | `BUILD SUCCESSFUL in 37s`, 52 tareas. `:core` **171** pruebas, `:app` **187** pruebas, 0 fallos, 0 errores, 0 omitidas (conteo de los XML de `build/test-results`). Sin líneas `w:` de warnings de Kotlin. |
| `git log --all -- app/google-services.json local.properties` | Vacío. |
| `git grep -nE "AIza\|cloudinary://\|api_secret\|client_secret"` | Una sola coincidencia: `PLAN-ETAPA3.md:618`, que es el propio comando anti-fugas escrito como texto. Ninguna clave. |
| `grep -rl "com.google.firebase" ui/` | Vacío. |
| `grep -rl "android\." core/src` | Vacío. |
| `AGENTS.md` | No existe en el repositorio ni en las carpetas superiores. Rigen `PROMPT-FINAL.md` y `PLAN-ETAPA3.md` §2 y §6. |

## Cumple

### Rúbrica y guía

| Requisito | Dónde se cumple |
|---|---|
| Autenticación con correo y red social (Google) | `data/auth/FirebaseAuthGateway.kt` (`signInWithEmail`, `registerWithEmail`, `signInWithGoogle`), `ui/auth/LoginScreen.kt`, `ui/auth/GoogleSignInButton.kt`, `viewmodel/AuthViewModel.kt`, `viewmodel/AuthErrors.kt` |
| Sesión persistente y navegación según sesión | `viewmodel/SessionViewModel.kt`, `ui/AppRoot.kt`, `ui/SessionViewModelStore.kt` |
| Módulos principales y secundarios funcionales | 21 pantallas `*Screen.kt` en `ui/auth/`, `ui/client/`, `ui/staff/`; servicios de `core/application/` |
| Persistencia en Firestore con reglas y tiempo real | `data/firestore/*`, `firestore.rules`, `firestore.indexes.json`; listeners `observeByClient`, `observePending`, `observeAll`, `observeById`, `observeByOrder` |
| Diseño UX/UI con Jetpack Compose y Material 3 | `ui/theme/`, `ui/components/`; comparación previa contra f1–f9 en `docs/etapa3/diseno/*-report.md` |
| Firebase documentado | README §7 y `docs/etapa3/foro2/INVESTIGACION-FIREBASE.pdf` |
| Sin secretos en el repositorio | `.gitignore` (`app/google-services.json`, `local.properties`), plantillas `.example` |

### Requisitos mínimos (README §4)

| Requisito | Dónde se cumple |
|---|---|
| 5 pantallas o más con navegación | 21 `*Screen.kt`; `NavHost` en `ui/AppRoot.kt`, rutas en `ui/navigation/Routes.kt` |
| Dos roles con permisos distintos | `UserRole`; `requireStaff` en `core/application/ApplicationService.kt`; `firestore.rules` (`isStaff()`); `CatalogService` |
| Cámara y galería | `ui/staff/evidence/EvidenceScreen.kt` (`TakePicture`, `PickVisualMedia`), `ui/staff/evidence/CameraFiles.kt`; `data/cloudinary/CloudinaryUploader.kt`, `data/cloudinary/ImageCompressor.kt` |
| Dos gráficas | `VerticalBarChart` y `DonutChart` en `ui/staff/reports/Charts.kt` (Canvas, sin librería). El README dice `BarChart`: se corrige en la Fase 3 |
| Tres módulos con lógica real | `core/domain/rules/AppointmentAvailabilityRule.kt`, `MaintenanceStatusRule.kt`, `ServiceTransitionRule.kt` |
| Validación en todos los formularios | `viewmodel/validation/FormValidators.kt`, `core/domain/validation/InputParsers.kt`, `core/domain/validation/TextField.kt` |
| Reglas de Firestore fuera del modo de prueba | `firestore.rules`: no existe `allow read, write: if true` |
| Sesión al arrancar y cierre en ambos roles | `viewmodel/SessionViewModel.kt`; Perfil (`ui/client/ProfileScreen.kt`) |
| Sin credenciales en el repo | Solo `app/google-services.json.example` y `local.properties.example` |

## Cumple parcial

| Requisito | Estado | Dónde |
|---|---|---|
| Criterio 1: Firebase «bien documentado» y verificado | Documentado y con pruebas unitarias; la tabla «Verificar en emulador» del Foro 2 sigue sin marcar | `docs/etapa3/foro2/ENTREGA.md` |
| Criterio 2: app completa y robusta | Sin una pasada completa de punta a punta en emulador registrada | README §5 «Guion de demo» |
| Criterio 3: UX/UI «completamente alineado con los mockups» y accesible | Comparado contra f1–f9 en los reportes de diseño; quedan diferencias anotadas y sin una revisión de accesibilidad global | `docs/etapa3/diseno/*-report.md` |
| Criterio 4: código documentado | Hay KDoc en parte de las clases (por ejemplo `CatalogService`), no en todas las públicas de reglas, servicios, datos y ViewModels | `core/domain/rules/`, `core/application/`, `data/firestore/`, `data/auth/`, `viewmodel/` |
| Requisito 7: README descriptivo | Tiene configuración, arquitectura, requisitos y guion. Faltan descripción del problema, funciones por rol, tecnologías con versión, integrantes por fase, mockups y licencia. Fecha: 8 de noviembre | `README.md` |
| Deuda conocida del Foro 2 | `citas.findByVehicle` sin filtro por `clientId` (ningún caso de uso de cliente lo llama); consultas de cliente por vehículo; alta de vehículo ajeno sin pantalla | `docs/etapa3/foro2/AUDITORIA.md` «Cumple parcial» |

## Falta (ordenado por impacto en la nota)

1. **Licencia Creative Commons** (requisito 6 y criterio 4): no existe `LICENSE`. Sin licencia el criterio 4 no llega a «Estratégico». → Fase 1.
2. **Mockups publicados en el README** (requisito 5 y criterio 4): los PNG f1–f9 de la Etapa 1 **no están en el repositorio** (solo se citan en `app/src/test/kotlin/com/eficazautomotriz/app/shots/MockupShots.kt` y en los reportes de diseño). No hay enlace de Figma. → Fase 2: se piden a Eduardo; las capturas reales de la app sí se pueden generar.
3. **Reparto de esta fase para los 4 integrantes** (criterio 5; sin commits la nota individual es cero): no existe `REPARTO-COPIA-FINAL.md`. → Fase 7.
4. **README profesional** (criterio 4): descripción, problema, funciones por rol, tecnologías con versión, integrantes, enlaces de investigación y fecha de la entrega final. → Fase 3.
5. **Guía de defensa** (código sin comprensión demostrable → base 5): no existe `GUIA-DEFENSA.md`. → Fase 7.
6. **Verificación de punta a punta en emulador** (criterios 1 y 2). → Fase 6.
7. **KDoc y limpieza** (criterio 4). → Fase 5.
8. **Registro de diferencias contra los mockups** (criterio 3): no existe `DIFERENCIAS-UI.md`. → Fase 4.
