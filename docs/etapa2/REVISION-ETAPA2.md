# Revisión Etapa 2 — pendientes (sin GitHub)

Estado: compila, **112 pruebas** en verde, 5/5 requisitos funcionales.
Los puntos 1 a 4 quedaron cerrados; el 5 es preparación de cada integrante.

## 1. Bloqueantes para la entrega

- [x] **README con datos reales.** Sección G01T, docente Ing. Alexander Alberto Siguenza Campos e
  integrantes (Crespín Lozano CL060107, Esnard Romero ER231474, López Martínez LM231893,
  López Rivera LR230061) ya están en `README.md`. La columna «Rama de trabajo» sigue vacía: la
  completa cada integrante cuando exista el repositorio.

- [x] **Que corra sin JDK 17 instalado.** `settings.gradle.kts` aplica
  `org.gradle.toolchains.foojay-resolver-convention` 0.8.0.
  Verificado: con `jvmToolchain` apuntando a una versión ausente, Gradle descargó el JDK solo
  (`~/.gradle/jdks/eclipse_adoptium-…`) y compiló sin intervención.
  Matiz que conviene saber en la defensa: **el wrapper de Gradle sigue necesitando alguna JVM en el
  `PATH`** para arrancar; lo que el plugin elimina es la exigencia de que sea *exactamente* el 17.
  `README.md` §1 lo dice así.

## 2. Código vs. documento de decisiones

- [x] **Cita → `atendida`.** Se alineó **el código al pacto**: `markAppointmentAttended` salió de
  `ServiceOrderService.create` y ahora se invoca en `complete()`.
  Justificación en `README.md` §5.8: `ATTENDED` es final y **libera el cupo**, así que marcarla al
  recibir el vehículo dejaría la franja libre con el auto todavía en el taller. Mientras el
  servicio está abierto la cita permanece `CONFIRMED` ocupando su franja.
  Verificado en consola: APT-005 sigue `[Confirmada]` tras crear la orden y pasa a `[Atendida]`
  solo al cerrarla.

- [x] **`reprogramada` ocupa cupo.** Sin cambios de código; sigue justificado en `README.md` §5.1.
  Cada integrante debe poder explicarlo en 2 frases (ver §5 de este documento).

- [x] **Personal «configura catálogos».** `AppointmentService.setSlotEnabled` y `setSlotCapacity`,
  más dos opciones nuevas en `CatalogMenu`. Son exactamente los dos parámetros que lee
  `AppointmentAvailabilityRule`, así que configurar la agenda no toca ninguna regla.
  Ninguna de las dos operaciones cancela citas ya agendadas: solo frenan las nuevas.
  `capacity = 0` sigue significando «usar `config.defaultSlotCapacity`»; negativo se rechaza con
  `ValidationError.NegativeSlotCapacity`. Decisión documentada en `README.md` §5.9.

## 3. Bugs y cabos sueltos

- [x] **Notas de la orden nunca se ven.** Nueva opción `Ver detalle de una orden` en
  `ServiceOrderMenu`, sobre `detailOf()`. Imprime datos, `notes` y evidencias.
  Las evidencias se listan **sin tabla de ancho fijo**: es la única vista donde se lee la dirección
  completa y la columna de 52 la recortaba. La pista de `ServiceHistoryMenu` ahora apunta a esa
  opción, que sí existe.

- [x] **Título «Mis vehiculos» para el personal.** `VehicleMenu.title` depende de `actor.role`:
  «Mis vehiculos» para CLIENT, «Vehiculos» para STAFF.

- [x] **`object Cancelled` sin uso.** Borrado de `cli/io/ConsoleReader.kt`.

- [x] **`MaintenanceStatusRule` lanza excepción.** **Se dejó el `require` a propósito.**
  La configuración que se abrió en §2 es la de *franjas*, no la de intervalos de mantenimiento:
  `MaintenanceType.intervalKm` sigue siendo de solo lectura, así que `intervalKm <= 0` continúa
  siendo imposible de producir desde la interfaz y el `require` protege contra un error de
  programación, no del usuario. Lo que sí se hizo fue aplicar el criterio del pendiente al
  parámetro que ahora *sí* es editable: la capacidad de la franja se valida con `ValidationError`
  **antes** de llegar al repositorio. `README.md` §5.9 deja escrito que si en una etapa posterior
  se abre la edición de intervalos, esa validación debe convertirse igualmente en `ValidationError`.

### Encontrado durante la verificación manual (no estaba en la lista)

- [x] **`31/02/2026` no se rechazaba.** `ConsoleReader` parseaba con `dd/MM/yyyy` y
  `ResolverStyle.SMART` por omisión, que **ajusta en silencio** la fecha imposible al `28/02/2026`.
  No se caía, pero el usuario terminaba agendando en un día que nunca escribió (y veía «la fecha ya
  paso», un mensaje que no explicaba nada).
  Corregido a `dd/MM/uuuu` con `ResolverStyle.STRICT` (el estilo estricto exige el año
  independiente de era). Cubierto por `ConsoleReaderTest`.

- [x] **El motivo de indisponibilidad salía recortado.** La columna `DISPONIBILIDAD` era de 34 y
  «No seleccionable: la franja alcanzo su capacidad» ocupa 48: se leía «…la franja alcan.», sin la
  palabra *capacidad*. Ahora el prefijo es `[X] ` (mismo vocabulario que `[OK]` / `[!]` de
  `ConsoleWriter`) y la columna mide 50, el ancho del motivo más largo.

## 4. Verificación manual antes de entregar

Recorrido ejecutado sobre `build/install/eficaz-automotriz/bin/eficaz-automotriz` con los datos
semilla y `today = 08/09/2026`. Todos los puntos pasaron.

- [x] Cliente Ana → Mis citas → cita en fecha pasada → rechazada; la tabla marca
  `[X] la fecha ya paso` en cada franja.
- [x] Cliente Ana → franja ya ocupada → `SLT-010  1/1  [X] la franja alcanzo su capacidad`
  (tras el arreglo del ancho de columna).
- [x] Personal → confirmar APT-006 y APT-007 → ambas OK, franjas distintas (SLT-010 martes,
  SLT-017 miércoles).
- [x] Personal → finalizar SRV-003 con 90 000 → rechazado
  (`El kilometraje no puede decrecer: 90000 es menor que 96500 km`) y registrado:
  `WARN | ServiceOrderService.complete | MileageDecrease(90000<96500) | orderId=SRV-003 …`
- [x] Personal → finalizar SRV-003 con 97 000 marcando «Cambio de aceite y filtro» →
  VEH-003 pasa de `[Vencido]` (96 500 km) a `[Al dia]` (97 000 km).
- [x] Personal → Reportes → 09/08/2026 a 18/09/2026 → ambas gráficas con barras.
  Nota: en ese período aparecen 7 citas, no 8. APT-008 (`Reprogramada`) cae el 19/09 porque
  `alignToSlotDay` la mueve al sábado, y queda fuera del rango. No es un fallo; si en la defensa
  quieren mostrar las seis clasificaciones a la vez, usen un período más amplio (el ejemplo de
  `README.md` §8 usa 01/08/2026 a 31/12/2026).
- [x] Letras donde piden número → `[!] Escriba un numero entero.`; fuera de rango →
  `[!] El valor debe estar entre 0 y N.`; `31/02/2026`, `30/02/2026`, `29/02/2026`, `ayer` y
  `17/9/2026` → `[!] Fecha invalida. Use el formato dd/mm/aaaa…`. No se cae en ningún caso y
  cerrar la entrada equivale a cancelar.
- [x] `./gradlew clean build` → **112 pruebas en verde**, sin advertencias del compilador.

### Pruebas añadidas (92 → 112)

| Suite | Añadido |
|---|---|
| `ServiceOrderServiceTest` | +3: la cita **no** se marca al recibir, sí al cerrar, y cerrar una orden sin cita no toca ninguna |
| `AppointmentServiceTest` | +7: deshabilitar/rehabilitar franja, ampliar capacidad, capacidad `0`, capacidad negativa, permiso de cliente, franja inexistente |
| `ConsoleReaderTest` (nueva) | +11: `31/02`, `29/02` no bisiesto, mes 13, texto por fecha, letras por número, fuera de rango, entrada agotada, `readChoice` |

## 5. Para la defensa (cada integrante)

Lo único que queda. Explicar sin leer:

1. `AppointmentAvailabilityRule.check` — las 4 comprobaciones en orden y por qué la lista excluye
   la propia cita al confirmar.
2. `MaintenanceStatusRule.evaluate` — `kmRecomendado = último + intervalo`; por qué `null ≠ 0`.
3. `ServiceTransitionRule` — por qué `finalizado` es terminal y qué dispara `complete()`
   (odómetro, ciclo de mantenimiento, historial **y la cita que pasa a `atendida`**).
4. Por qué `Outcome<T>` y no excepciones para errores de negocio.

Añadidos por los cambios de esta revisión:

5. Por qué `atendida` se marca al **cerrar** y no al recibir (`README.md` §5.8): `ATTENDED` es
   final y libera el cupo.
6. Por qué el personal puede configurar franjas pero no intervalos de mantenimiento, y por qué la
   capacidad se valida con `ValidationError` mientras el intervalo sigue con `require`
   (`README.md` §5.9).
