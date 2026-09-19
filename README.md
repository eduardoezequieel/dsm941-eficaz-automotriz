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

## Cómo está construido

Capas. Cada una solo habla con la de abajo:

```
cli  →  application  →  domain
                    →  data  →  domain
```

Código en `src/main/kotlin/com/eficazautomotriz/`:

- **`domain/`** — entidades, las tres reglas de negocio, validación y `Outcome` (éxito o error sin excepciones).
- **`data/`** — repositorios en memoria y datos de demostración (`SeedData`).
- **`application/`** — casos de uso: vehículos, citas, órdenes, mantenimiento y reportes. Ahí se revisan los permisos.
- **`cli/`** — consola: lee el teclado, muestra menús e invoca casos de uso. No decide reglas.
- **`logging/`** — errores en `logs/errors.log`.
- **`Main.kt`** — arma las piezas a mano y arranca.

Las pruebas están en `src/test/`, con el mismo orden de paquetes.

## Cómo ejecutar

Hace falta cualquier JDK en el `PATH` e internet la primera vez. Gradle y el JDK 17 se resuelven solos.

```bash
./gradlew run     # arranca la consola
./gradlew test    # 146 pruebas
./gradlew build   # compila y prueba todo
```

Al entrar se elige un usuario; el menú cambia según el rol (cliente o personal).
Los datos cargados son de demostración.
