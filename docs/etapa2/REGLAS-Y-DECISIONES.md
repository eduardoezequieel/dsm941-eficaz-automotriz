# Reglas de negocio y decisiones (Etapa 2)

Texto de la Etapa 2, sin cambios: las tres reglas, la equivalencia de nombres con la Etapa 1, las
decisiones tomadas y la validación de entradas. Las menciones a la consola (`ConsoleReader`,
`CatalogMenu`) son de esa etapa; en la Etapa 3 los mismos límites los aplican los formularios de la app.

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
