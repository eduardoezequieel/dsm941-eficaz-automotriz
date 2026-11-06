# Auditoría · Foro 2 · Autenticación, sesión y Firestore

Contraste con la rúbrica del Foro 2 y con la sección «Fase 0» de `docs/etapa3/PROMPT-FORO2.md`. La base auditada es `f62fd39`. Este documento, en la rama `foro2/auth`, ya incorpora el cierre de la Fase 2. El hallazgo previo al arreglo está en el commit de `foro2/auditoria`.

La lista **falta** está ordenada por impacto en la nota.

## Cumple

### Autenticación y sesión

- Correo y Google están conectados. `FirebaseAuthGateway` implementa `signInWithEmail`, `registerWithEmail` (siempre `role = CLIENT` y, si el perfil no se guarda, borra la cuenta) y `signInWithGoogle` (crea `usuarios/{uid}` solo en el primer ingreso). No hay proveedores nuevos.
- El formulario valida correo, contraseña (mínimo 6), nombre, teléfono de 8 dígitos y confirmación, en `AuthFormValidator` y otra vez en `AuthService`, antes de llamar a Firebase. Los errores de campo se muestran bajo el control.
- La navegación depende de la sesión. Con `google-services.json` ausente se muestra `FirebaseMissingScreen` (aviso, formulario deshabilitado). Al abrir, `SessionViewModel` lee `currentUid()`: sin uid va al login; con uid carga el perfil con `AuthService.signIn` y `AppRoot` entra a inicio de cliente o al panel según el rol. Una cuenta sin documento `usuarios/{uid}` cierra la sesión con «Cuenta sin perfil; contacte al taller».
- «¿Olvidó su contraseña?» está en el login (`LoginScreen`, texto «¿Olvidó su contraseña?»), abre el diálogo y llama a `sendPasswordReset`. El éxito avisa en español. Prueba: `AuthSessionTest.password reset confirms a known email and reports an unknown one in Spanish`. El correo real de Firebase queda en «verificar en emulador».
- Cada código de Firebase Auth que pueden devolver el ingreso, el registro, Google y el restablecimiento tiene un mensaje en español. El código no aparece en el texto. Un código desconocido cae en «Ocurrió un error. Intente de nuevo.» Cerrar el selector de Google no muestra nada; el resto de estados de Google tampoco muestran el número. Prueba: `AuthErrorsTest`.
- Sin red, el ingreso muestra «Sin conexión. Revise su red e intente de nuevo.» y el formulario sigue usable. Al abrir con sesión guardada y sin poder leer el perfil, la pantalla queda cubierta por el aviso, «Reintentar» y «Cerrar sesión»: no queda en blanco ni se cae, y el texto no incluye la excepción. Pruebas: `AuthSessionTest` (sign-in sin red, perfil sin red) y `SessionScopeTest` (el mensaje de perfil). El modo avión en un emulador queda en «verificar en emulador».
- Si el uid desaparece con la app abierta (revocación o cierre externo), la sesión vuelve al login con «Su sesión ya no es válida. Inicie sesión de nuevo.» y cambia el ámbito de navegación. Un cierre pedido por el usuario conserva su propio mensaje. Prueba: `AuthSessionTest`. El token que Firebase refresca solo, con red, queda en «verificar en emulador».
- Cerrar sesión o perder el uid mientras se lee el perfil o se siembran los catálogos cancela ese trabajo. Una generación por petición descarta el resultado si la consulta igual termina después: no se vuelve a publicar `SignedIn` ni el ámbito de la cuenta anterior. Prueba: `AuthSessionTest` («sign-out during the catalog load does not restore the signed-in session»).
- Un cambio de uid A→B avisado por Firebase limpia el ámbito de A y carga el perfil de B. Un uid nuevo con la sesión cerrada no adelanta esa carga: el registro y el primer ingreso con Google publican el uid antes de crear `usuarios/{uid}`, y `onAuthenticated` corre solo cuando ese documento ya existe. Pruebas: `AuthSessionTest` (cambio de cuenta y uid con la sesión cerrada).
- El cierre de sesión cambia `scopeId` (`navigationScopeId`). `AppRoot` crea un `NavHost` y un `SessionViewModelStoreOwner` por ese id y llama `clear()` al salir de la clave, así que los ViewModels de pantalla se descartan al cerrar sesión o al cambiar de cuenta. `SessionViewModel` sigue en el store de la actividad. Pruebas: `SessionScopeTest` (el store suelta el ViewModel anterior) y `AuthSessionTest` (el ámbito pasa al de «sin sesión», y el de A no sobrevive al cambio a B). Que la cuenta siguiente no vea listas de la anterior en pantalla queda en «verificar en emulador».
- En cualquier pantalla, un fallo de sistema muestra «No se pudo completar la operación. Revise su conexión e intente de nuevo.» El texto de la excepción no se agrega: `EficazViewModel.run` ya lo escribió en el registro. Un error de negocio conserva su mensaje. Prueba: `SessionScopeTest`.

### Firestore: datos del usuario y reglas

Consultas que un cliente ejecuta de verdad, y la regla que las exige:

| Consulta del cliente | Filtro | Regla |
|---|---|---|
| `vehiculos` `findByOwner` | `ownerId == uid` | `list` solo si `ownerId == uid` o personal |
| `citas` `findByClient` | `clientId == uid` | `list` solo si `clientId == uid` o personal |
| `usuarios/{uid}` al entrar | id del documento = uid | `get` propio o personal |
| catálogos (`tiposServicio`, `tiposMantenimiento`, `configFranjas`) | sin uid; son compartidos | `read` si hay sesión |
| `ocupacion` por fecha y `placas/{placa}` | no son datos de otro cliente | `read` si hay sesión |

Escrituras que la app hace hoy, y por qué la regla no las niega:

- Crear `usuarios/{uid}` con `role = CLIENT` (registro y primer Google). Actualizar el propio documento solo si el rol no cambia. Borrar usuarios está prohibido.
- Crear vehículo con `ownerId == uid` y el reclamo `placas/{PLACA}` en la misma transacción (`getAfter`). Actualizar o borrar el vehículo propio. Al borrar se libera la placa.
- Crear cita con `clientId == uid` y `status = PENDING`, y el contador `ocupacion` en ±1. El cliente cancela (`CANCELLED`) y responde una reprogramación (`RESCHEDULED` → `CONFIRMED` o `CANCELLED`). El personal confirma, rechaza, reprograma y marca atendida (`isStaff()`).
- `servicios` y `evidencias` solo los escribe el personal. El cliente los lee por `vehicleId` o `serviceOrderId`.
- `estadosMantenimiento`: el cliente los borra con su vehículo; el personal los escribe al cerrar la orden. La regla deja crear, actualizar y borrar al dueño del vehículo o al personal.
- `contadores`: el cliente solo suma 1; el personal puede adelantar. `nextId` trata `PERMISSION_DENIED` como id ocupado, no como caída.
- Catálogos: cualquier sesión lee; solo el personal escribe. Si el cliente no puede sembrarlos, `ensureCatalogs` lo registra y sigue.

El personal usa `findAll`, `findByStatus` y `findByDateRange`. Esas consultas no llevan uid porque la regla las admite con `isStaff()`, que no depende del documento. El caso de uso ya exige rol personal antes de llamarlas.

Hay un índice compuesto para `estadosMantenimiento` (`vehicleId` + `maintenanceTypeId`), que es la única consulta de dos igualdades.

Ninguna escritura de la UI actual queda denegada por las reglas. No hizo falta cambiar `firestore.rules`.

## Cumple parcial

- **Consultas por vehículo, no por uid.** `servicios`, `evidencias` y `estadosMantenimiento` no guardan `ownerId` ni `clientId`. El cliente filtra por `vehicleId` o `serviceOrderId` y la regla hace `get()` del vehículo (o de la orden y luego del vehículo). Firebase admite ese `list` cuando la igualdad de la consulta fija la ruta del `get()`. Es el diseño del Foro 1. Confirmar en emulador que inicio, historial y detalle cargan con un cliente real. No se agregan campos: el dominio y `data/firestore/` no se tocan aquí.
- **`citas.findByVehicle` no filtra por `clientId`.** La regla de `list` de citas exige `clientId == uid` (o personal), así que esa consulta sería denegada a un cliente. Ningún caso de uso de cliente la llama: el detalle filtra en memoria el resultado de `findByClient`. Queda latente. El repositorio está en `data/firestore/`, que esta fase no edita.
- **Alta de vehículo a nombre de otro usuario.** `VehicleService.register` deja al personal registrar un vehículo con `ownerId` ajeno. La regla de `create` exige `ownerId == request.auth.uid`. Ninguna pantalla lo hace: el formulario es del cliente y usa su propio uid. No es una operación de la UI de hoy.

## Falta

1. **Documento PDF «Firebase aplicado al proyecto» (criterio 3, Fase 3).** No existe `INVESTIGACION-FIREBASE.md` ni el PDF. Sin ese entregable el foro deja fuera un criterio entero. Un APA 7 incorrecto fija la nota base en 5. No lo escribe este corte.
2. **Sincronización en tiempo real — en curso (Fase 1).** No hay `addSnapshotListener` ni `Flow` de citas, panel o detalle de servicio. Sin eso el criterio 2 (persistencia) no llega al nivel estratégico, aunque las reglas y el modelo por usuario ya estén. Lo está cerrando el otro worker. No se tocan `AppointmentsViewModel`, `StaffPanelViewModel`, `ServiceDetailViewModel` ni `data/firestore/`.
3. **Guion del video, sección Firebase del README y reparto (Fase 4).** Son entregables del foro, detrás del PDF y del tiempo real en impacto sobre las tres filas de la rúbrica.

## Verificación de la Fase 2

| Punto | Prueba | Verificar en emulador |
|---|---|---|
| Mensaje en español por cada código de Auth, sin el código | `AuthErrorsTest` | Un correo ya registrado, una contraseña incorrecta y una cuenta deshabilitada, y comprobar que no aparece `ERROR_` ni un número de Google |
| «Olvidé mi contraseña» visible y funcional | `AuthSessionTest` (correo conocido y desconocido contra el gateway en memoria). El enlace está en `LoginScreen` | Pulsar «¿Olvidó su contraseña?», enviar, y recibir el correo de Firebase. Con protección de enumeración, un correo inexistente puede igual mostrar éxito |
| Sin red: aviso, sin pantalla en blanco ni crash | `AuthSessionTest` (ingreso y perfil) y `SessionScopeTest` (texto del perfil) | Modo avión con sesión guardada al abrir, y modo avión al pulsar «Iniciar sesión» |
| Cierre de sesión limpia los ViewModels | `SessionScopeTest` (el store descarta el ViewModel) y `AuthSessionTest` (el ámbito y el mensaje de revocación) | Entrar con la cuenta A, abrir vehículos, cerrar sesión, entrar con la cuenta B y confirmar que no quedan datos de A |
| Cierre durante la carga no restaura la sesión anterior | `AuthSessionTest` («sign-out during the catalog load does not restore the signed-in session») | Cerrar sesión justo al abrir, con la red lenta, y confirmar que no reaparece el inicio de la cuenta anterior |
| Cambio de cuenta A→B sin pasar por el login | `AuthSessionTest` («an external account switch clears the previous scope and loads the new profile» y «a uid published while signed out waits until onAuthenticated») | Con la app abierta, cambiar el usuario en Firebase y confirmar que las pantallas pasan a la cuenta nueva. El registro sigue creando `usuarios/{uid}` antes de entrar |
| Mensaje de sistema sin texto técnico | `SessionScopeTest` («a system failure shows the generic Spanish message without the exception text») | Provocar un fallo de reglas o de red en una pantalla que no sea el login y confirmar que el aviso no trae el texto de la excepción |
| Revocación del token con la app abierta | `AuthSessionTest.a revoked session returns to signed-out with a Spanish notice` | Deshabilitar la cuenta en la consola con la app abierta en una pantalla de cliente |

## Qué no se cambia en este corte

- Proveedores (no Facebook ni teléfono) y el modelo de roles.
- Firmas de los métodos síncronos de repositorio y de los casos de uso. `observeUid` es un método nuevo del gateway.
- `:core` dominio y aplicación, y los archivos del worker de tiempo real.
- `FirestoreAppointmentRepository.findByVehicle`: no filtra por `clientId` y ningún cliente lo llama. Arreglarlo exige editar `data/firestore/`.
