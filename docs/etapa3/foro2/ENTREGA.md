# Entrega · Foro 2

Todas las fases están integradas en `main`. Pruebas: **171** de `:core` y **187** de `:app`, más `assembleDebug`, en verde tras `./gradlew clean`.

Cada fase pasó por una verificación cruzada independiente antes de integrarse. Los hallazgos (carreras de sesión, excepciones fuera del `catch` en los flujos, cancelación, fechas APA) se corrigieron en commits propios.

## Qué se hizo por fase

### Fase 0 — Auditoría

| Hash | Mensaje |
|---|---|
| `c5edfb8` | `docs: audit auth and Firestore against the Foro 2 rubric` |

Documento: `docs/etapa3/foro2/AUDITORIA.md`. La lista «Falta» de ese archivo quedó atrás: el tiempo real ya está en `main` y el PDF está en `foro2/documento`. La tabla «Verificación de la Fase 2» sigue vigente y se copia abajo.

### Fase 1 — Tiempo real

| Hash | Mensaje |
|---|---|
| `3f5c003` | `feat(app): stream appointments, orders and evidence` |
| `004140b` | `fix(app): catch realtime row builds that fail after a snapshot` |
| `9722bae` | `fix(app): rethrow cancellation from realtime catches` |
| `f534008` | `Merge branch 'foro2/tiempo-real'` |

Mis citas escucha `observeByClient`. El panel combina `observePending` y `observeAll`. El detalle del servicio combina `observeById` y `observeByOrder`. El `catch` arma el `UiState` antes y vuelve a lanzar `CancellationException`. `firestore.rules` no se modificó.

### Fase 2 — Autenticación, sesión y reglas

| Hash | Mensaje |
|---|---|
| `1571a98` | `fix(auth): close password, offline and sign-out gaps` |
| `18a05c6` | `fix(auth): drop a stale profile load and follow an account switch` |
| `4b1187f` | `fix(app): hide technical exception text from user messages` |
| `41a76bf` | `Merge branch 'foro2/auth'` |
| `25d9dd1` | `test(app): expect the plain system message after merging auth` |

`25d9dd1` es el `main` sobre el que se abre `foro2/readme`. El mensaje de sistema queda en «No se pudo completar la operación. Revise su conexión e intente de nuevo.», sin el texto de la excepción.

### Fase 3 — Documento PDF

Rama `foro2/documento`. Todavía no está en `main`. Al integrarse, el PDF queda en `docs/etapa3/foro2/INVESTIGACION-FIREBASE.pdf`.

| Hash | Mensaje |
|---|---|
| `104f847` | `docs: add the Firebase investigation for forum 2` |
| `dc5e482` | `docs: correct APA dates, indexes, and captures in the forum 2 paper` |

`dc5e482` es la punta commiteada (markdown, docx, pdf, `img/` y `tools/build_foro2.py`). El worktree de esa rama tiene cambios sin commit en `INVESTIGACION-FIREBASE.md` y en `tools/build_foro2.py`. No entran en este borrador.

### Fase 4 — README, guion y reparto

Rama `foro2/readme`, tomada de `main` `25d9dd1`.

| Hash | Mensaje |
|---|---|
| `75595e0` | `docs: add the forum 2 readme, script and delivery draft` |

Archivos de esta fase: `README.md` (entrega **8 de noviembre de 2026**, sección «7. Firebase», pruebas 171 + 187), `docs/etapa3/GUION-VIDEO-FORO2.md` y este borrador. El guion suma **12:30** (máximo 14:00).

### Cierre

| Hash | Mensaje |
|---|---|
| `b216e96`, `b63e758` | `fix(app): rethrow cancellation while seeding catalogs` · `test(app): lock session races around the profile load` |
| `38b42bb` | `docs/etapa3/REPARTO-COPIA-FORO2.md`: cinco bloques desde `f62fd39`, probados en orden (171/152 → 159 → 166 → 186 → 186) y verificados |
| `de9e106`, `2189c0b` | Las 11 capturas reales (emulador Pixel 8, API 36, y consola del proyecto), incrustadas como figuras APA. PDF de 42 páginas |
| `f5b4ef5`, `d2d89e5` | El aviso «Le enviamos un enlace…» no se veía: `MessageSnackbar` consumía el mensaje y cancelaba su propio snackbar. Afectaba a todos los snackbars de la app |

Datos de prueba creados para las capturas (ficticios): usuario `foro2.captura@eficaz.test` (uid `GSTzlr6bbkfObRanSmUT9KgjLQE3`), vehículo `P864-213` (`VEH-004`) y cita `APT-004`, confirmada por `staff.demo@eficaz.test`.

## Qué queda para Eduardo

1. Grabar el video con `docs/etapa3/GUION-VIDEO-FORO2.md` (suma 12:30, máximo 14:00).
2. Copiar los bloques de `docs/etapa3/REPARTO-COPIA-FORO2.md` al repositorio de entrega, cada integrante en su día.
3. Subir el PDF `docs/etapa3/foro2/INVESTIGACION-FIREBASE.pdf` y los enlaces: repositorio, video y PDF.
4. Opcional: repetir la Figura 4 para que salga el aviso de envío, ahora que el bug está corregido. El pie actual describe solo el diálogo.

## Verificar en emulador

Lista de `docs/etapa3/foro2/AUDITORIA.md` (tabla «Verificación de la Fase 2»), más los puntos del cierre. La prueba de unidad no sustituye la fila del emulador.

| Punto | Prueba ya hecha | Verificar en emulador |
|---|---|---|
| Mensaje en español por cada código de Auth, sin el código | `AuthErrorsTest` | Un correo ya registrado, una contraseña incorrecta y una cuenta deshabilitada. No aparece `ERROR_` ni un número de Google. El texto de la contraseña mal puesta es «Correo o contraseña incorrectos.» |
| «¿Olvidó su contraseña?» visible, y el correo real de restablecimiento | `AuthSessionTest` (correo conocido y desconocido contra el gateway en memoria) | Pulsar «¿Olvidó su contraseña?», «Enviar enlace», y recibir el correo real de restablecimiento. Con protección de enumeración, un correo inexistente puede mostrar el mismo éxito: «Le enviamos un enlace para restablecer su contraseña a …» |
| Modo avión: aviso, sin pantalla en blanco ni crash | `AuthSessionTest` (ingreso y perfil) y `SessionScopeTest` (texto del perfil) | Modo avión con sesión guardada al abrir, y modo avión al pulsar «Iniciar sesión». El aviso de ingreso es «Sin conexión. Revise su red e intente de nuevo.» |
| Cierre de sesión limpia los ViewModels | `SessionScopeTest` y `AuthSessionTest` | Entrar con la cuenta A, abrir vehículos, cerrar sesión, entrar con la cuenta B y confirmar que no quedan datos de A |
| Cerrar sesión con Mis citas, el Panel o el Detalle abiertos | `SessionScopeTest` (el store suelta el ViewModel al cambiar `scopeId`) | Con Mis citas, el Panel del personal o el Detalle del servicio en pantalla, «Cerrar sesión». No queda la lista de esa cuenta, el listener se va y la siguiente cuenta no la hereda |
| Cierre durante la carga no restaura la sesión anterior | `AuthSessionTest` («sign-out during the catalog load does not restore the signed-in session») | Cerrar sesión justo al abrir, con la red lenta, y confirmar que no reaparece el inicio de la cuenta anterior |
| Cambio de cuenta A→B sin pasar por el login | `AuthSessionTest` (cambio de cuenta y uid con la sesión cerrada) | Con la app abierta, cambiar el usuario en Firebase y confirmar que las pantallas pasan a la cuenta nueva |
| Cambio de cuenta con Firebase real | La fila anterior cubre el gateway en memoria | Repetir el cambio con el proyecto Firebase real (no con `demo.inMemory=true`): la cuenta B no muestra citas, vehículos ni el detalle de A |
| Dos sesiones a la vez, tiempo real | `RealtimeViewModelsTest` | Dos emuladores. Cliente en «Mis citas» y personal en «Panel del personal», segmento «Citas». Al pulsar «Confirmar», el chip del cliente pasa de «Pendiente de confirmación» a «Confirmada» sin salir de la pantalla. En el detalle, un cambio de la orden o una evidencia nueva se ve con el cliente todavía en esa pantalla |
| Mensaje de sistema sin texto técnico | `SessionScopeTest` | Provocar un fallo de reglas o de red fuera del login. El aviso es «No se pudo completar la operación. Revise su conexión e intente de nuevo.» y no trae la excepción |
| Revocación del token con la app abierta | `AuthSessionTest` | Deshabilitar la cuenta en la consola con la app abierta en una pantalla de cliente. El aviso es «Su sesión ya no es válida. Inicie sesión de nuevo.» |
| Selector de Google | `AuthErrorsTest` (el código 12501 no produce mensaje) | Pulsar «Continuar con Google», elegir una cuenta en el selector, y entrar. Cerrar el selector sin elegir: no aparece aviso ni un número. Si falta el SHA-1 o `google.webClientId`, el botón dice «El inicio con Google no está configurado en esta compilación.» |

También, la primera vez con un cliente real: el inicio, el historial y el detalle cargan. `citas.findByVehicle` sigue sin filtro por `clientId` y ningún caso de uso de cliente lo llama. Registrar un vehículo a nombre de otro dueño no es una pantalla de hoy.

## Verificación final

Corrida por el coordinador sobre `main`:

- [x] `./gradlew clean :core:test :app:testDebugUnitTest :app:assembleDebug`: BUILD SUCCESSFUL, 171 + 187 pruebas, 0 fallos.
- [x] `grep -rl "com.google.firebase" app/src/main/kotlin/com/eficazautomotriz/app/ui`: vacío.
- [x] `grep -rl "android\." core/src`: vacío.
- [x] `git log --all -- app/google-services.json local.properties`: vacío.
- [x] Ningún commit del foro lleva `Co-Authored-By` ni «Generated with».
- [x] Las 11 capturas sustituyen los marcadores; no queda ningún `[CAPTURA` en el documento.
- [ ] Video grabado y enlaces publicados (Eduardo).
- [ ] La tabla «Verificar en emulador» marcada por quien la corra.
