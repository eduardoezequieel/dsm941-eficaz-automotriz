# Guion del video · Foro 2, Etapa 3

Presenta: Eduardo Ezequiel López Rivera (LR230061). Duración objetivo: **12:30** (máximo 14:00).
Equipo: Christian Gustavo Crespín Lozano, Diego Guillermo Esnard Romero, Diego René López Martínez y Eduardo Ezequiel López Rivera.

Convenciones:
- **Acción**: lo que haces en pantalla.
- Las citas son lo que dices.
- `app/…` = `app/src/main/kotlin/com/eficazautomotriz/app/`
- `core/…` = `core/src/main/kotlin/com/eficazautomotriz/`
- En Android Studio: **⌘⇧O** abre un archivo y **⌘L** salta a una línea.

Suma de los tramos (el video dura lo que suma esta tabla):

| Tramo | Duración |
|---|---|
| 1. Presentación | 0:25 |
| 2. Registro con correo | 1:40 |
| 3. Documento `usuarios/{uid}` | 0:40 |
| 4. Contraseña incorrecta | 0:40 |
| 5. Recuperación de contraseña | 0:55 |
| 6. Inicio con Google | 1:05 |
| 7. Cita nueva y documento `citas` | 1:50 |
| 8. Tiempo real en dos emuladores | 2:30 |
| 9. Reglas: escritura denegada | 1:50 |
| 10. Cierre | 0:55 |
| **Suma** | **12:30** |

12:30 son 750 segundos. El máximo del foro es 14:00.

---

## Antes de grabar

**Entorno**
- [ ] Dos emuladores abiertos (Pixel 8 u otro), **sin modo avión** y con wifi. En el video se ven a la vez.
- [ ] Los dos tienen la app instalada desde este repositorio con `demo.inMemory=false` (`./gradlew installDebug`). El modo demostración no sirve para las capturas de la consola.
- [ ] `app/google-services.json` está en su sitio y `local.properties` trae `google.webClientId`. El SHA-1 de debug (`./gradlew :app:signingReport`) está registrado en el proyecto Firebase. Si falta, «Continuar con Google» muestra «El inicio con Google no está configurado en esta compilación.»
- [ ] Android Studio está abierto en la rama que se va a mostrar, ya sincronizada.
- [ ] El navegador tiene la consola del proyecto `eficaz-automotriz`: Authentication, Firestore (datos y Rules Playground) y la bandeja del correo de prueba.
- [ ] Si aparece el tutorial de stylus de Gboard, ciérralo; escribe con el teclado de la Mac.

**Datos en Firebase**
1. Authentication → Sign-in method: **Correo/contraseña** y **Google** habilitados.
2. Firestore en modo producción, con `firestore.rules` publicado (el modo de prueba desactivado).
3. El personal ya entró una vez, para que existan `tiposServicio`, `tiposMantenimiento` y `configFranjas`. Sin catálogo, Nueva cita dice «Catálogo no configurado: el taller aún no cargó los tipos de servicio y horarios. Intente más tarde.»
4. `staff.demo@eficaz.test` tiene `role = STAFF` en `usuarios/{uid}`. `cliente.demo@eficaz.test` tiene `role = CLIENT` y al menos un vehículo.
5. Una cuenta de Google que puedas elegir en el selector y que aún no esté registrada en el proyecto, o una que ya tenga `usuarios/{uid}`.
6. Un correo nuevo, todavía sin cuenta, para el registro en cámara. Ejemplo: `foro2.video@eficaz.test`. Anota la contraseña (mínimo 6 caracteres).
7. Una franja libre en un día que el taller atiende, para la cita que vas a crear en el tramo 7.
8. El cliente de demostración no tiene una solicitud pendiente en esa misma franja: el tramo 8 usa la cita que acabas de crear.

**Cuentas**

| Rol | Correo | Contraseña | Dónde |
|---|---|---|---|
| Cliente de demostración | `cliente.demo@eficaz.test` | `Demo1234` | Ya existe; personal en la consola |
| Personal | `staff.demo@eficaz.test` | `Demo1234` | `usuarios/{uid}` con `role = STAFF` |
| Registro en cámara | `foro2.video@eficaz.test` | la que anotes (6 o más) | Se crea en el tramo 2 |
| Google | la cuenta del selector | la de esa cuenta | Tramo 6 |

El modo demostración, si hubiera que usarlo fuera de cámara, cambia las cuentas a `cliente@demo.test` y `personal@demo.test`, ambas con `Demo1234`. Este video se graba con Firebase.

---

## 1. Presentación · 0:00 – 0:25

**Acción:** emulador del cliente en el login. Se leen «Eficaz Automotriz», «Gestión de citas y mantenimiento vehicular», «Iniciar sesión» / «Crear cuenta» y «Continuar con Google».

> «Hola, soy Eduardo López Rivera, del equipo de Eficaz Automotriz, con Christian Crespín, Diego Esnard y Diego López. En este foro muestro Firebase sobre la app del taller: registro con correo, Google, la contraseña, el documento del usuario, una cita en tiempo real entre dos teléfonos y una regla que niega una escritura.»

## 2. Registro con correo · 0:25 – 2:05

**Acción:** pulsa el segmento «Crear cuenta».

> «El registro solo pide correo. No hay Facebook ni teléfono.»

**Acción:** completa «Nombre completo», «Teléfono» (placeholder «7000-0000», 8 dígitos), «Correo electrónico» con `foro2.video@eficaz.test`, «Contraseña» (placeholder «Mínimo 6 caracteres») y «Confirmar contraseña». Bajo el botón se lee «Las cuentas nuevas se registran con el rol Cliente.» Pulsa «Crear cuenta».

> «La cuenta nueva entra con el rol Cliente. El perfil queda en Firestore con el mismo uid de Authentication.»

**Acción:** la app abre el inicio del cliente. Si el correo ya existía, el aviso es «Ya existe una cuenta con ese correo.» En ese caso usa otro correo y repite este tramo fuera de cámara antes de seguir.

## 3. Documento `usuarios/{uid}` · 2:05 – 2:45

**Acción:** en la consola, Authentication → Users. Abre el usuario recién creado y copia el User UID. Luego Firestore → `usuarios` → el documento con ese id. En cámara se ven `role` = `CLIENT` y que el id del documento es el uid.

> «El documento se llama como el uid. El rol es CLIENT. Desde la app el cliente no puede pasarse a personal: la regla de actualización exige conservar el rol.»

## 4. Contraseña incorrecta · 2:45 – 3:25

**Acción:** en Perfil, «Cerrar sesión». En «Iniciar sesión» escribe `foro2.video@eficaz.test` y una contraseña distinta. Pulsa «Iniciar sesión».

> «La contraseña no coincide.»

**Acción:** deja en pantalla el aviso «Correo o contraseña incorrectos.» No aparece `ERROR_WRONG_PASSWORD` ni otro código.

> «El texto es ese. El código de Firebase se queda en el registro, no en la pantalla.»

## 5. Recuperación de contraseña · 3:25 – 4:20

**Acción:** pulsa «¿Olvidó su contraseña?». Se abre el diálogo «Restablecer contraseña», con el texto «Le enviaremos un enlace para crear una nueva contraseña.» El campo es «Correo electrónico». Deja `foro2.video@eficaz.test` y pulsa «Enviar enlace».

> «Pedimos el enlace al correo con el que acabamos de registrar la cuenta.»

**Acción:** el diálogo se cierra y queda el aviso «Le enviamos un enlace para restablecer su contraseña a foro2.video@eficaz.test.» Abre la bandeja de ese correo y muestra el mensaje de Firebase, sin leer la contraseña en voz alta. «Cancelar» cierra el diálogo si hiciera falta regrabar.

> «El correo llegó. Con la protección de enumeración, un correo que no existe puede mostrar el mismo aviso de éxito: por eso uso el correo que acabo de crear.»

## 6. Inicio con Google · 4:20 – 5:25

**Acción:** sigue en el login. Pulsa «Continuar con Google». Elige la cuenta preparada en el selector.

> «Google devuelve el token. La app crea `usuarios/{uid}` solo la primera vez, también con rol Cliente.»

**Acción:** entra al inicio. Si cierras el selector sin elegir cuenta, la pantalla no muestra error: el código 12501 no se enseña. Si el botón respondiera «El inicio con Google no está configurado en esta compilación.», falta el SHA-1 o `google.webClientId`: corrige eso antes de grabar, no durante el tramo.

> «Cancelar el selector no deja un aviso. Un fallo distinto dice «No se pudo iniciar sesión con Google. Intente de nuevo.», también sin el número.»

## 7. Cita nueva y documento `citas` · 5:25 – 7:15

**Acción:** cierra sesión y entra otra vez con `foro2.video@eficaz.test` y la contraseña correcta (o la nueva, si ya cambiaste la del enlace). En Inicio pulsa «Nueva cita». Si no hay vehículo, la pantalla dice «Primero registre un vehículo para poder solicitar una cita.» Pulsa «Agregar vehículo».

**Acción:** en «Datos del vehículo» completa «Placa», «Marca», «Modelo», «Año» y «Kilometraje actual». Pulsa «Guardar vehículo». Vuelve a «Nueva cita». Elige vehículo, servicio, una fecha hábil y una franja libre. Pulsa «Solicitar cita».

> «La cita nace pendiente. El texto de confirmación dice que quedó pendiente de confirmación por el taller.»

**Acción:** se lee «Su cita #… quedó pendiente de confirmación por el taller.» Pulsa «Ver mis citas». El encabezado es «Mis citas» / «Solicitudes y confirmaciones» y el chip dice «Pendiente de confirmación».

**Acción:** en Firestore abre la colección `citas` y el documento nuevo. En cámara, `clientId` es el mismo uid del tramo 3 y `status` es `PENDING`.

> «`clientId` es el uid de quien la pidió. La regla de lista solo le deja ver las citas con ese `clientId`, o se las deja ver al personal.»

## 8. Tiempo real en dos emuladores · 7:15 – 9:45

**Acción:** deja el primer emulador en «Mis citas», sin salir de esa pantalla. En el segundo, inicia sesión con `staff.demo@eficaz.test`. El encabezado es «Panel del personal». En el segmento «Citas» se ve «Solicitudes pendientes» y la cita de hace un momento, con el botón «Confirmar».

> «El cliente dejó Mis citas abierta. El personal ve la misma solicitud en el panel. Las dos pantallas escuchan Firestore: el cliente con las citas de su uid, el personal con las pendientes y con el listado completo.»

**Acción:** en el panel pulsa «Confirmar». Sin tocar el emulador del cliente, el chip pasa de «Pendiente de confirmación» a «Confirmada».

> «No hubo que volver a entrar. El listener entregó el documento nuevo y la pantalla cambió.»

**Acción:** opcional y corto, si el reloj va bien: en el panel pulsa «Rechazar» sobre otra solicitud de prueba, o deja esta en «Confirmada» y dilo. No hace falta el detalle del servicio en este tramo. El detalle también escucha la orden y sus evidencias; el panel no escucha la colección `servicios`.

> «Mis citas, el panel y el detalle del servicio son las tres pantallas con listener. El inicio, los vehículos, el historial, el perfil y los reportes se leen al abrir.»

## 9. Reglas: escritura denegada · 9:45 – 11:35

**Acción:** di primero que la app no ofrece esa escritura. El cliente no tiene un botón para crear una orden de servicio: `servicios` lo escribe solo el personal (`allow write: if isStaff()`). Para mostrarlo de forma repetible se usa el Rules Playground, no un toque escondido.

> «La interfaz no deja al cliente escribir el historial. Lo provoco en el Rules Playground, con el uid que acabamos de crear, para que cualquiera pueda repetir los mismos campos.»

**Acción:** consola → Firestore → Rules → Rules Playground. Completa así:
- Tipo de simulación: **create**
- Ubicación: `servicios/SRV-VIDEO`
- Autenticado: sí
- Firebase UID: el uid del tramo 3 (el de `foro2.video@eficaz.test`, rol CLIENT)
- Datos del documento: un mapa mínimo, por ejemplo `vehicleId` = `VEH-001` y `status` = `OPEN`

Pulsa **Run**. El resultado es denegado (*Simulated write denied*).

> «La simulación se niega. Da igual el contenido: la regla de `servicios` solo deja escribir al personal, y este uid es Cliente.»

**Acción:** si quieres un segundo rechazo en la misma pantalla, cambia a **update**, ubicación `usuarios/` más ese uid, y en los datos pon `role` = `STAFF` (el resto igual al documento real). Vuelve a pulsar **Run**. También se niega: la actualización del propio usuario exige conservar el rol.

> «El mismo uid no puede nombrarse personal. Crear el usuario sí está permitido, y solo con rol CLIENT, que es lo que hizo el registro.»

## 10. Cierre · 11:35 – 12:30

**Acción:** vuelve a los dos emuladores, cliente en «Mis citas» y personal en «Panel del personal».

> «En resumen: el registro crea `usuarios` con el uid y el rol Cliente; Google entra por el mismo perfil; la contraseña mal puesta y el restablecimiento avisan en español; la cita guarda ese uid; Mis citas y el panel se actualizan solos; y las reglas niegan la escritura de `servicios` a quien no es personal. El detalle está en el PDF del README. Gracias.»

---

## Si te pasas de tiempo

Recorta en este orden. Los ocho demostrativos obligatorios se quedan: registro, Google, contraseña incorrecta, recuperación, documento de la cita con el uid, los dos emuladores, y el Playground.

1. Tramo 10: deja solo la última frase (ahorras unos 25 s).
2. Tramo 1: entra directo al segmento «Crear cuenta» (ahorras unos 15 s).
3. Tramo 8: quita la frase de las pantallas sin listener (ahorras unos 15 s).
4. Tramo 9: no hagas la segunda simulación de `usuarios` (ahorras unos 25 s).

Con esos cuatro cortes el video queda cerca de 11:10 y sigue completo.

## Si algo falla en vivo

- No lo repitas en cámara: sigue y regraba solo ese tramo.
- **«Ya existe una cuenta con ese correo.»** Usa otro correo. Borra el usuario de prueba en Authentication antes de la toma buena, para que el documento se vea recién creado.
- **«Correo o contraseña incorrectos.» en el registro bueno.** Revisa mayúsculas. La contraseña de demostración de las otras cuentas es `Demo1234`; la del correo nuevo es la que anotaste.
- **Google:** si el selector no abre, di la frase que esté en pantalla y sigue con el correo. Regraba el tramo 6 cuando el SHA-1 y `google.webClientId` estén bien.
- **La cita no aparece en el panel.** Confirma que el segundo emulador es `staff.demo@eficaz.test` y que el segmento es «Citas». Una cuenta CLIENT no pasa esa consulta.
- **El chip no cambia.** Los dos emuladores tienen que seguir en la pantalla. Salir de «Mis citas» o del panel cancela el listener; al volver se abre otro.
- **El Playground permite la escritura.** El UID es el del cliente, no el del personal, y la ubicación es `servicios/…`. Si el modo de prueba sigue activo, publícalo desactivado y corre otra vez.
- **Sin internet, fuera de esta grabación:** `demo.inMemory=true` y `./gradlew installDebug`. Cuentas `cliente@demo.test` y `personal@demo.test`, contraseña `Demo1234`. También se actualizan Mis citas y el panel, porque el repositorio en memoria emite al guardar. La consola de Firestore no va a mostrar esos documentos.
