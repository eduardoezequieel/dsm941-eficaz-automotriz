# Instrucciones para cerrar la Etapa 2 (sin GitHub)

Lo de GitHub se hace después. Esto es lo demás, en orden.

## 1. Un arreglo pendiente en el README

La tabla de integrantes dice «Nombre completo» pero solo tiene apellidos. Reemplazar por:

| Nombre completo | Carné | Rama de trabajo |
|---|---|---|
| Christian Gustavo Crespín Lozano | CL060107 | |
| Diego Guillermo Esnard Romero | ER231474 | |
| Diego René López Martínez | LM231893 | |
| Eduardo Ezequiel López Rivera | LR230061 | |

La columna «Rama de trabajo» se deja vacía hasta que exista el repositorio.

## 2. Verificación final (5 minutos)

En la carpeta del proyecto:

```bash
./gradlew clean build      # debe decir BUILD SUCCESSFUL y 146 pruebas
./gradlew run              # arranca la consola
```

Recorrido corto para confirmar que nada se rompió:

1. Entrar como **Jose Antonio Mendoza (Personal)** → Catálogos → Franjas horarias. Debe listar las franjas con capacidad.
2. Ordenes de servicio → Ver detalle de una orden → SRV-003. Deben verse las notas y la evidencia.
3. Ordenes de servicio → Finalizar orden → SRV-003 → km `90000`. Debe rechazar por kilometraje menor.
4. Cerrar sesión, entrar como **Ana Beatriz Melara (Cliente)** → Mis citas → Solicitar → fecha `31/02/2026`. Debe decir «Fecha invalida».
5. Salir y abrir `logs/errors.log`. Debe tener la línea `MileageDecrease(90000<96500)` del paso 3.

Si los 5 pasan, el código está listo para entregar.

## 3. Qué se entrega

- El enlace del repositorio (cuando exista).
- Nada más: el README ya explica cómo compilar, ejecutar y qué hace cada regla.

Antes de subir, borrar `build/`, `.gradle/`, `.kotlin/` y `logs/` (el `.gitignore` ya los excluye, pero así el zip o la carpeta quedan limpios):

```bash
./gradlew clean
rm -rf .gradle .kotlin logs
```

## 4. Preparación de cada integrante para la defensa

Cada uno debe poder explicar **sin leer** estas 6 cosas. Donde está el código para estudiarlo:

| # | Pregunta | Archivo | Respuesta en una frase |
|---|---|---|---|
| 1 | ¿Cómo decide el sistema si cabe una cita? | `domain/rules/AppointmentAvailabilityRule.kt` | Revisa en orden: franja habilitada, fecha no pasada, día correcto, cupo libre. Al confirmar excluye la propia cita para no contarse a sí misma. |
| 2 | ¿Cómo calcula el estado del mantenimiento? | `domain/rules/MaintenanceStatusRule.kt` | `recomendado = último km + intervalo`. Vencido si ya llegó, próximo si falta menos que el umbral, al día si no. Sin registro previo no es cero: es «no sabemos». |
| 3 | ¿Por qué el servicio no puede retroceder? | `domain/rules/ServiceTransitionRule.kt` | Recibido → En proceso → Finalizado. Finalizado es definitivo porque dispara efectos: actualiza odómetro, reinicia el ciclo de mantenimiento, entra al historial y marca la cita como atendida. |
| 4 | ¿Por qué `Outcome` y no excepciones? | `domain/error/Outcome.kt` | Que una franja esté llena no es un error del programa, es un resultado esperado. Las excepciones se reservan para fallos reales y van al log. |
| 5 | ¿Por qué la cita pasa a atendida al cerrar y no al recibir? | `README.md` §5.8 | Atendida es estado final y libera el cupo. Si se marcara al recibir, la franja quedaría libre con el auto adentro del taller. |
| 6 | ¿Por qué «reprogramada» ocupa cupo? | `README.md` §5.1 | Una cita reprogramada tiene fecha y franja nuevas y reales; si no ocupara cupo, otro cliente podría tomar el mismo espacio. |

Repartir: cada integrante domina las 6, pero se asigna una como «suya» para responder primero.

## 5. Lo que queda para después

- GitHub: crear repo, 4 colaboradores, una rama por persona, commits repartidos en el tiempo.
- Llenar la columna «Rama de trabajo» del README con el nombre de cada rama.
