# Entrega Final · Eficaz Automotriz

Cierre de la **Entrega Final: Proyecto Completo** (DSM941, UDB Virtual), preparado sobre `main` a partir de `0378bd3` siguiendo `docs/etapa3/PROMPT-FINAL.md`.

> **Pendiente para Eduardo (mockups):** necesito los PNG originales de los mockups de la Etapa 1 (`f1`–`f9`) y el enlace público de Figma para completar la columna «Mockup (Figma)» del `README.md`.

---

## 1. Qué se hizo por fase

| Fase | Rama / Origen | Commits clave | Resultado |
|---|---|---|---|
| **0 · Auditoría** | `final/auditoria` (`fa2947f`) | `ec21597`, `718031a` | Línea base en verde (`171 + 187`), sin secretos, auditoría en `docs/etapa3/final/AUDITORIA-FINAL.md` y `PROMPT-FINAL.md` integrado. |
| **1 · Licencia** | `final/licencia` (`523a7b0`) | `69f1136` | `LICENSE` en raíz con texto legal completo CC BY-NC-SA 4.0 (440 líneas) y atribución al equipo; sección «Licencia» e insignia en `README.md`. |
| **2 · Mockups** | `final/mockups` (`0c9377a`) | `dace1e6` | Generadas 9 capturas reales de la app (`docs/mockups/app/p1-login.png` … `p9-reportes.png`, 720 px de ancho) y tabla de 3 columnas en `README.md`. Faltan los PNG de Figma (`f1`–`f9`). |
| **3 · README** | `final/readme` (`6dc6748`) | `d30eaca` | `README.md` actualizado con entrega 22 de noviembre de 2026, problema de Etapa 1 (solo hechos de `eficazautomotriz.com`), funciones por rol, versiones de `libs.versions.toml`, enlaces e integrantes. |
| **4 · Pulido UX/UI** | `final-ui` + `main` (`47b8a38`) | `90f7bb1`, `b5a9523`, `fe57408`, `c79eaaa`, `6b840ed`, `d8911bc`, `47b8a38` | `docs/etapa3/final/DIFERENCIAS-UI.md` sin filas sin decidir; `ContrastTest` y `LargeFontShots` (130 %); contraste de barra inferior corregido con `TextSecondary`; capturas de `docs/mockups/app/` regeneradas tras los cambios. |
| **5 · Calidad de código** | `final-calidad` | `a236700`, `4345282`, `b822cd0` | KDoc en inglés en reglas (citando `REGLAS-Y-DECISIONES.md`), servicios, repositorios/mappers Firestore y ViewModels (`data/auth/` ya tenía KDoc en todas sus clases públicas y no cambió en esta fase); sin imports sin usar, sin logs de depuración y sin warnings nuevos. |
| **6 · Verificación E2E** | Emulador `Pixel_8_API_36` | — | **Parcial.** Firebase real (`demo.inMemory=false`): solo ingreso y navegación de ambos roles, sin modificar datos. Modo demo (`demo.inMemory=true`): pasos 1, 2, 3, 5 y 6 del guion; pasos 4 (fotos), 7 (comprobación del cliente) y 8 (reportes) pendientes (ver §2). |
| **7 · Reparto y defensa** | `final-defensa` + `main` (`2beb583`+) | `2cd92a6`, `2beb583` | `docs/etapa3/final/GUIA-DEFENSA.md`, `docs/etapa3/REPARTO-COPIA-FINAL.md` (12 bloques parejos probados en limpio sobre `0378bd3`) y este cierre `ENTREGA-FINAL.md`. |

---

## 2. Verificación en emulador (Fase 6)

Ejecutado en el emulador **`Pixel_8_API_36`** (API 36, `emulator-5554`), con `app/google-services.json` y `local.properties` presentes (`demo.inMemory=false` para Firebase real, y `demo.inMemory=true` para recorrer parte del guion en memoria, restaurando `demo.inMemory=false` al terminar). Sin excepciones fatales en `logcat` (`AndroidRuntime:E`).

### Guion de demo (README §5)

- **Firebase real (`demo.inMemory=false`):** comprobado inicio de sesión de cliente (`cliente.demo@eficaz.test`: Inicio, Mis citas `#APT-003`, Historial y cierre de sesión) y personal (`staff.demo@eficaz.test`: Panel del personal con solicitud pendiente, Reportes). No se mutaron datos compartidos ni se subieron fotos reales a Cloudinary desde el emulador.
- **Modo demostración (`demo.inMemory=true`):** ejecutados los pasos 1, 2, 3, 5 y 6 sin errores ni crashes: Inicio de Diego → Nueva cita (`Nissan Frontier 2019` · `Mantenimiento preventivo` · `Mié 18 nov` · `10:00`) → `#APT-001` creada → cierre de sesión → entrada como `personal@demo.test` → confirmación de `#APT-001` (`POR CONFIRMAR` 3 → 2) → apertura de orden `#SV-2107` → `Iniciar` (`En proceso`) → `Finalizar` (`44,300 km`, `Cambio de aceite y filtro`) → estado `Finalizado`.
- **Pasos del guion sin comprobar (pendiente: Eduardo):** 4 (foto de cámara y de galería), 7 (el cliente ve la cita Atendida, el historial con fotos y el cambio de aceite Vigente) y 8 (gráficas de Reportes). Con Firebase real, el guion completo con escrituras (pasos 1–8) también queda pendiente.
- **Evidencia:** este registro es el reporte de quien corrió el emulador; no se guardaron capturas ni logs en el repositorio.

### Tabla «Verificar en emulador» (`docs/etapa3/foro2/ENTREGA.md`)

| Punto | Resultado en `Pixel_8_API_36` |
|---|---|
| Mensaje en español por cada código de Auth, sin el código | **Comprobado (parcial):** contraseña incorrecta con `cliente.demo@eficaz.test` muestra «Correo o contraseña incorrectos.» (sin `ERROR_` ni código). Correo ya registrado y cuenta deshabilitada: **pendiente: Eduardo**. |
| «¿Olvidó su contraseña?» visible, y el correo real de restablecimiento | **Parcial:** botón visible y accesible (48 dp) en `LoginScreen`; envío y recepción en bandeja real: **pendiente: Eduardo**. |
| Modo avión: aviso, sin pantalla en blanco ni crash | **Comprobado:** con modo avión activo (`cmd connectivity airplane-mode enable`), pulsar «Iniciar sesión» muestra «Sin conexión. Revise su red e intente de nuevo.» sin crash. |
| Cierre de sesión limpia los ViewModels | **Comprobado:** cierre desde Perfil de dos cuentas cliente de prueba (una de ellas `cliente.demo@eficaz.test`) y entrada a otra cuenta (`cliente.demo` / `staff.demo`) sin datos residuales. |
| Cerrar sesión con Mis citas, el Panel o el Detalle abiertos | **Parcial:** cierre desde Perfil tras visitar Mis citas e Historial vuelve limpio a Login. Cerrar sesión con esas pantallas abiertas (y con el Panel o el Detalle): **pendiente: Eduardo**. |
| Cierre durante la carga no restaura la sesión anterior | **Pendiente: Eduardo** (requiere red lenta manual durante apertura). |
| Cambio de cuenta A→B sin pasar por el login | **Comprobado en unitarias (`AuthSessionTest`); en Firebase real: pendiente: Eduardo.** |
| Cambio de cuenta con Firebase real | **Comprobado** vía cierre de sesión y entrada A → B (`cliente.demo@eficaz.test` ↔ `staff.demo@eficaz.test`): cada rol ve solo sus pantallas y datos. |
| Dos sesiones a la vez, tiempo real | **Pendiente: Eduardo** (requiere dos emuladores/dispositivos simultáneos). |
| Mensaje de sistema sin texto técnico | **Comprobado en unitarias (`SessionScopeTest`); provocación de fallo de reglas en vivo: pendiente: Eduardo.** |
| Revocación del token con la app abierta | **Pendiente: Eduardo** (requiere deshabilitar la cuenta en Firebase Console con la app abierta). |
| Selector de Google | **Pendiente: Eduardo** (requiere cuenta Google iniciada en el emulador o dispositivo físico). |

---

## 3. Verificación final (`PROMPT-FINAL.md` §5)

- [x] `./gradlew clean :core:test :app:testDebugUnitTest :app:assembleDebug` en verde: **171** pruebas en `:core` + **190** pruebas en `:app` (0 fallos, 0 errores, 0 omitidas), `BUILD SUCCESSFUL`.
- [x] `grep -rl "com.google.firebase" app/src/main/kotlin/com/eficazautomotriz/app/ui` vacío.
- [x] `grep -rl "android\." core/src` vacío.
- [x] `git log --all -- app/google-services.json local.properties` vacío y sin claves reales en el código.
- [x] `LICENSE` con CC BY-NC-SA 4.0 y enlazada en el `README.md`.
- [ ] **PARCIAL:** `README.md` con descripción, problema, funciones por rol, instalación, arquitectura, integrantes y licencia. Mockups: solo las 9 capturas de la app; **faltan los 9 PNG de Figma y el enlace público** (pendiente: Eduardo).
- [x] Todas las imágenes enlazadas en `README.md` (`docs/mockups/app/p1-login.png` … `p9-reportes.png`) existen y cargan con rutas relativas.
- [x] Tabla «Verificar en emulador» con resultado en cada fila (en §2); varias filas son parciales o «pendiente: Eduardo».
- [x] `docs/etapa3/REPARTO-COPIA-FINAL.md` probado en limpio sobre `0378bd3`, con 12 bloques parejos (3 por integrante).
- [x] `docs/etapa3/final/GUIA-DEFENSA.md` completa con la parte de cada integrante.
- [x] `git status` limpio y todo integrado en `main`.

---

## 4. Qué queda para Eduardo

1. Pasar los **PNG originales de los mockups** (`f1`–`f9`) a `docs/mockups/` y colocar el **enlace público de Figma** en `README.md`.
2. Completar las filas marcadas **`pendiente: Eduardo`** en la tabla «Verificar en emulador» (correo real de restablecimiento, Google Sign-In con cuenta activa, dos dispositivos en tiempo real, revocación de token en consola, entre otras) y los pasos 4, 7 y 8 del guion de demo, más el guion completo con Firebase real.
3. Coordinar a los 4 integrantes para copiar los 12 bloques de `docs/etapa3/REPARTO-COPIA-FINAL.md` del jueves 19 al sábado 21 de noviembre.
4. Hacer **público** el repositorio de entrega en GitHub, confirmar los 4 colaboradores y las 4 ramas `dev/<apellido>`, y verificar la autoría de cada commit.
5. Subir el enlace del repositorio público al Aula Digital antes del **domingo 22 de noviembre de 2026 a las 11:00 pm**.
6. Compartir `docs/etapa3/final/GUIA-DEFENSA.md` con el equipo y ensayar la demo una vez antes de las defensas (23–27 de noviembre).
