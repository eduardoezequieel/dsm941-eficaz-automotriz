# Eficaz Automotriz · Núcleo funcional (Etapa 2)

Aplicación de consola en **Kotlin puro sobre JVM** para la gestión de citas y mantenimiento
vehicular. Es la Etapa 2 del proyecto de cátedra: construye el núcleo funcional que la Etapa 3
reutilizará tal cual, reemplazando únicamente la interfaz (Jetpack Compose) y la persistencia
(Cloud Firestore).

| | |
|---|---|
| **Asignatura** | DSM941 — Desarrollo de Software para Móviles |
| **Sección** | G01T |
| **Docente** | Ing. Alexander Alberto Siguenza Campos |
| **Institución** | Universidad Don Bosco |
| **Ciclo** | II · 2026 |
| **Entrega** | 20 de septiembre de 2026 |

### Integrantes

| Nombre completo | Carné | Rama de trabajo |
|---|---|---|
| Christian Gustavo Crespín Lozano | CL060107 | `dev/crespin` |
| Diego Guillermo Esnard Romero | ER231474 | `dev/esnard` |
| Diego René López Martínez | LM231893 | `dev/diego` |
| Eduardo Ezequiel López Rivera | LR230061 | `dev/lopez-rivera` |

> Los cuatro integrantes deben figurar como colaboradores del repositorio, cada uno con su
> propia rama y commits distribuidos en el tiempo. Quien no aparezca como colaborador y no
> tenga commits obtiene cero automático.

---

## 1. Cómo compilar y ejecutar

Requisitos: **cualquier JDK en el `PATH`** (el wrapper de Gradle necesita una JVM para arrancar) y
conexión a internet la primera vez. **No hace falta que sea el JDK 17.**

Gradle no se instala: viene el wrapper. Y el JDK 17 concreto tampoco: `settings.gradle.kts` aplica
el plugin `org.gradle.toolchains.foojay-resolver-convention`, así que el `jvmToolchain(17)` que
declara `build.gradle.kts` se resuelve **descargando** el JDK 17 si la máquina no lo tiene. El
proyecto siempre compila y corre sobre 17, sea el que estaba instalado o el que Gradle bajó.

La primera ejecución tarda más porque descarga el JDK y las dependencias; después ambos quedan en
la caché de Gradle.

```bash
# Ejecutar la aplicación
./gradlew run

# Correr la batería de pruebas
./gradlew test

# Compilar y probar todo
./gradlew build
```

La aplicación arranca con datos de demostración ya cargados. La primera pantalla pide elegir el
usuario de la sesión; según su rol se abre el menú de cliente o el de personal.

Los errores quedan registrados en `logs/errors.log`, que se crea al primer evento.

---

## 2. Arquitectura

```
cli  →  application  →  domain
                    →  data  →  domain
```

- **`domain`** — Kotlin puro. Entidades, enumeraciones, configuración, errores y **las tres
  reglas de negocio**. No importa nada fuera de `kotlin.*` y `java.time.*`, de modo que el
  paquete completo se copia a la app Android de la Etapa 3 sin tocar una línea.
- **`data`** — persistencia en memoria. Se sustituye por Firestore en la Etapa 3.
- **`application`** — casos de uso. Orquestan reglas y repositorios, verifican permisos y
  registran los errores.
- **`cli`** — única capa que imprime y lee del teclado. No contiene lógica de negocio.
- **`logging`** — registro de errores en archivo.

`Main.kt` es el punto de ensamblaje: construye el grafo de dependencias a mano y lo inyecta por
constructor. No hay singletons mutables globales.

### Estructura de paquetes

```
src/main/kotlin/com/eficazautomotriz/
├── Main.kt
├── domain/
│   ├── model/          Identifiable, User, Vehicle, Appointment, ServiceOrder,
│   │   └── enums/      Evidence, ServiceType, MaintenanceType, MaintenanceRecord, TimeSlot
│   ├── config/         SystemConfig
│   ├── rules/          AppointmentAvailabilityRule, MaintenanceStatusRule, ServiceTransitionRule
│   ├── validation/     TextField, TextFields
│   └── error/          DomainError, ValidationError, Outcome
├── data/               Repository, InMemoryRepository, repositorios concretos, SeedData
├── application/        AuthService, VehicleService, AppointmentService,
│                       ServiceOrderService, MaintenanceService, ReportService
├── cli/                ConsoleApp, AppContext, menu/, io/, format/
└── logging/            ErrorLogger, FileErrorLogger
```

### Programación orientada a objetos

- **Interfaces** — `Repository<T>`, `ErrorLogger`, `Identifiable`.
- **Herencia** — `InMemoryRepository<T>` es la clase abstracta base de los nueve repositorios
  concretos; `Menu` es la base de todos los menús de consola; `ApplicationService` es la base de
  los casos de uso.
- **Clases selladas** — `DomainError`, `ValidationError`, `Outcome<T>`, `AvailabilityResult`,
  `TransitionResult`.
- **Genéricos** — `Repository<T>`, `Outcome<T>`, `Column<T>`, el bucle de lectura validada de
  `ConsoleReader` y los ayudantes compartidos de `Menu` (`report`, `handle`, `showTable`,
  `select`), que es lo que evita repetir el mismo bloque en cada menú.

---

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

### 5.9 El personal configura las franjas; los intervalos de mantenimiento no

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

Los **intervalos de mantenimiento (`MaintenanceType.intervalKm`) siguen siendo de solo lectura**,
y por eso la sección 3.2 mantiene su `require`: `intervalKm <= 0` sigue siendo imposible de
producir desde la interfaz, así que el `require` protege contra un error de programación, no
contra un error del usuario. Si en una etapa posterior se abre la edición de intervalos, esa
validación debe convertirse en un `ValidationError` **antes** de llegar a la regla, igual que se
hizo con la capacidad de la franja.

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

---

## 7. Registro de errores

`FileErrorLogger` escribe en modo append en `logs/errors.log`, creando el directorio si no
existe. Una línea por evento:

```
timestamp | nivel | contexto | mensaje | datos
```

```
2026-09-14T21:03:11 | WARN | AppointmentService.request | SlotUnavailable(CAPACITY_REACHED) | clientId=USR-002 slotId=SLT-004 date=2026-09-18
```

- `WARN` — errores de negocio esperados. Se registran **todos** los `DomainError` que devuelvan
  los casos de uso.
- `ERROR` — excepciones no previstas.

La escritura del log va dentro de su propio `try/catch`: si falla el log, el programa sigue. Un
logger que tumba la aplicación es peor que no tener logger.

Hay **un único** `catch (e: Exception)` en todo el sistema, en `Main.kt`, alrededor del arranque
de `ConsoleApp`. Registra la excepción, muestra un mensaje breve y termina con código 1.

---

## 8. Reportes

`ReportService` devuelve **estructuras de datos**; el dibujo de barras y tablas es
responsabilidad de `cli/format`. Los agregados se construyen con `groupBy`/`groupingBy` y
`eachCount`, nunca con contadores manuales.

**1. Citas por estado en un período.**

```
CITAS POR ESTADO - 01/08/2026 a 31/12/2026

Confirmada            ████████████████████    2   (25.0 %)
Atendida              ████████████████████    2   (25.0 %)
Pendiente             ██████████              1   (12.5 %)
Reprogramada          ██████████              1   (12.5 %)
Rechazada             ██████████              1   (12.5 %)
Cancelada             ██████████              1   (12.5 %)
TOTAL                                         8
```

**2. Servicios finalizados por tipo.** Solo `ServiceStatus.COMPLETED` cerrados dentro del
período, agrupados por `ServiceType`.

**3. Resumen del sistema.** Total de vehículos, citas por estado, órdenes por estado y cuántos
vehículos hay en cada `MaintenanceStatus`. Se recalcula en cada consulta, así que refleja los
cambios de inmediato.

---

## 9. Pruebas

```bash
./gradlew test
```

**146 pruebas**, todas en verde.

| Suite | Cubre |
|---|---|
| `MaintenanceStatusRuleTest` | Cada rama de la regla 2, `null` vs `0`, los dos valores exactamente en el umbral, intervalo inválido |
| `AppointmentAvailabilityRuleTest` | Los cuatro motivos de indisponibilidad, su precedencia, qué estados ocupan cupo, capacidad por franja y por omisión, exclusión de la propia cita al confirmar |
| `ServiceTransitionRuleTest` | Las dos transiciones válidas, retrocesos, saltos, estado terminal, transición al mismo estado, las tres condiciones de cierre |
| `AppointmentServiceTest` | Solicitud con conflicto de franja, permisos por rol y por propietario, confirmación con revalidación, reprogramación, cancelación que libera el cupo, motivo vacío o fuera de límite al rechazar y reprogramar, configuración de franjas (habilitar, deshabilitar, ampliar capacidad, capacidad `0`, negativa, por encima del techo, permiso y franja inexistente) |
| `ServiceOrderServiceTest` | Ciclo completo de la orden, cierre que actualiza el odómetro y reinicia el ciclo de mantenimiento, rechazo de kilometraje decreciente, evidencias con dirección inválida o fuera de límite, notas de recepción vacías y fuera de límite, la cita que se marca `atendida` al cerrar y no al recibir |
| `VehicleServiceTest` | CRUD completo, placa duplicada, con espacios, demasiado larga o sin formato salvadoreño, placa alfanumérica válida, marca y modelo fuera de límite o con caracteres no permitidos, año y kilometraje fuera de rango en ambos extremos, edición con los mismos límites que el alta, acceso a vehículo ajeno |
| `MaintenanceAndReportServiceTest` | Las cuatro clasificaciones en los datos semilla, recálculo tras cambiar el odómetro, agregados de los reportes y actualización dinámica del resumen |
| `ConsoleReaderTest` | Formato y límites: fechas imposibles (`31/02`, `29/02` de año no bisiesto, mes `13`) y fuera del rango pedido, letras donde va un número, separador de miles bien y mal puesto, desbordamiento de `Int`, texto en el límite exacto y uno más allá, caracteres no permitidos, línea desmedida, texto opcional, placa y dirección de imagen, confirmación, intentos agotados, entrada agotada y selección sobre lista |

---

## 10. Alcance

### Incluido en esta etapa

Gestión de vehículos y citas (CRUD completo), clasificación del mantenimiento preventivo,
ciclo de las órdenes de servicio con evidencias, detalle de la orden con sus notas, historial,
configuración de las franjas horarias por parte del personal, los dos reportes, el resumen del
sistema, validación en dos niveles, permisos por rol y registro de errores en archivo.

### Deliberadamente fuera de esta etapa

Android, Jetpack Compose, Firebase, Firestore, Cloudinary, subida real de imágenes,
autenticación con contraseña, gráficas, facturación, cobros, inventario de repuestos, nómina,
cotización con precios, mensajería interna y persistencia en base de datos o en archivos más
allá del log.

Las entidades y las reglas quedan listas para recibir todo eso en la Etapa 3. En consola no hay
cámara: `Evidence` registra la URL de la imagen como texto, con validación de formato, y la
entidad queda lista para la captura real.
