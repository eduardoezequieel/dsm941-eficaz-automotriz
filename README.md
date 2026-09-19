# Eficaz Automotriz · Etapa 2

Aplicación de consola en Kotlin para citas y mantenimiento vehicular.
Núcleo funcional de DSM941 (G01T), Universidad Don Bosco. Ciclo II 2026.

**Docente:** Ing. Alexander Alberto Siguenza Campos

## Integrantes

| Nombre completo | Carné | Rama |
|---|---|---|
| Christian Gustavo Crespín Lozano | CL060107 | `dev/crespin` |
| Diego Guillermo Esnard Romero | ER231474 | `dev/esnard` |
| Diego René López Martínez | LM231893 | `dev/diego` |
| Eduardo Ezequiel López Rivera | LR230061 | `dev/lopez-rivera` |

## Cómo ejecutar

Hace falta cualquier JDK en el `PATH` e internet la primera vez. Gradle y el JDK 17 se resuelven solos.

```bash
./gradlew run     # arranca la consola
./gradlew test    # 146 pruebas
./gradlew build   # compila y prueba todo
```

La consola carga datos de demostración. Al entrar se elige un usuario; el menú cambia según el rol (cliente o personal). Los errores quedan en `logs/errors.log`.
