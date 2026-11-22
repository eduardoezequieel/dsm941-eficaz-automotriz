# Diferencias de UI contra los mockups (Fase 4)

## Fuente y método

Los PNG de los mockups de la Etapa 1 (f1–f9) **no están en este repositorio**, así que no se compararon de nuevo a ojo. Las diferencias de diseño vienen de los reportes de la comparación anterior, que sí se hizo contra esos PNG:

- W1 base: `docs/etapa3/diseno/diseno-base-W1-report.md`
- W2 cliente: `docs/etapa3/diseno/diseno-cliente-W2-report.md`
- W3 personal: `docs/etapa3/diseno/diseno-personal-W3-report.md`
- W4 escala: `docs/etapa3/diseno/diseno-escala-W4-report.md`

A eso se suma una revisión del código actual de `ui/` y de las capturas del harness:

```
./gradlew :app:testDebugUnitTest -Pshots --tests '*shots*'
```

Las PNG quedan en `app/build/shots/`:

- `p*.png` y `s-*.png`: 412 dp con fuente al 100 %.
- `f130-*.png`: 360 dp con fuente al 130 %. Las agrega esta fase con `LargeFontShots`.

Ninguna fila describe cómo se ve un mockup si el reporte no lo dice.

Commits de esta fase:

| Commit | Contenido |
|---|---|
| `90f7bb1` | Estados de carga y error con los componentes del sistema de diseño |
| `b5a9523` | Contraste 4.5:1 en chips neutros y texto secundario |
| `fe57408` | Áreas táctiles de 48 dp y etiquetas habladas |
| `c79eaaa` | Textos en español llano, sin jerga de configuración |
| `6b840ed` | Nada cortado con la fuente al 130 % |

## Tabla de diferencias

| Pantalla | Diferencia | Decisión |
|---|---|---|
| P1 Login | El texto de error del correo no es el de f1. f1 dice «El correo debe tener un formato válido»; la app dice «Correo inválido. Ejemplo: nombre@dominio.com» (W1, desviación 4). | **Se deja**, porque el texto sale de `viewmodel/validation`, que está fuera del alcance de esta fase. Además, el texto actual da un ejemplo y es más útil. |
| P1 Login | El botón de Google siempre se dibuja. En modo demo o sin `google.webClientId`, al tocarlo avisa que no está configurado (W1, desviación 5). | **Se deja**, porque f1 muestra el botón. Ocultarlo cambiaría el mockup, y la configuración depende de la compilación. |
| P1 Login | El enlace «¿Olvidó su contraseña?» medía unos 33 dp de alto. | **Se corrige** en `fe57408`: ahora mide 48 dp y TalkBack lo anuncia como botón. |
| P1 Login | Sin `google-services.json`, la pantalla mostraba «Firebase no configurado: falta app/google-services.json…». | **Se corrige** en `c79eaaa`: ahora dice «Esta versión de la aplicación no está conectada al servidor del taller. Avise al administrador del sistema.» |
| P2 Inicio | f2 muestra el Frontier como «Próximo», pero la semilla lo dejaba «Vencido» (W1, desviación 1; W2, desviación 1). | **Ya corregido** antes de esta fase, en el commit W2 `4ef33c0`. La captura `p2.png` muestra la alerta del Frontier «Próximo» a 85,000 km. |
| P2 Inicio | El texto de la tarjeta «Último servicio» (vehículo · fecha · km) se cortaba con «…» y perdía el kilometraje con fuente grande. | **Se corrige** en `6b840ed`: ahora usa hasta dos líneas. |
| P2 Inicio | Muestra más filas de vehículos sobre el pliegue que f2 (W4, tabla de ajuste). | **Se deja**, porque se debe a la escala de texto acordada en W4 (opción B: texto un paso arriba del mockup). |
| P3 Mis vehículos | No hay campo de combustible. La línea es «Placa P123-456 · 2016» (W2, desviación 4). | **Se deja**, porque el modelo `Vehicle` no guarda combustible. Agregarlo cambiaría `:core`, que está prohibido. |
| P3 Mis vehículos | La placa se guarda como `P123456`, pero el mockup la muestra como P123-456 (W1, desviación 2). | **Ya corregido** en W2 con `Formats.plate`. Las capturas muestran «P123-456». |
| P3 Mis vehículos | El subtítulo era estático (W1, desviación 6). | **Ya corregido** en W2: ahora muestra el número de vehículos. |
| P4 Detalle de vehículo | f4 no muestra la flecha de regreso. La app la dibuja en todas las rutas que no son pestaña (W2, desviación 6). | **Se deja**, porque `onBack` lo decide la navegación (`AppRoot`/`LocalShell`), que está fuera del alcance. Además, la flecha es necesaria para volver. |
| P4 Detalle de vehículo | El aceite sale «Vigente» y la rotación «Próximo», en vez de lo que dice f4. Con 80,000 km de base y un umbral de 500 km, las cifras del brief no pueden dar el texto de f4 (W2, desviación 1). | **Se deja**, porque lo deciden la regla de dominio y los parámetros configurables, no la UI. |
| P5 Mis citas | No se puede dibujar la fecha original tachada de una cita reprogramada, porque `Appointment` no guarda la fecha anterior (W2, desviación 2). | **Se deja**, porque requiere cambiar el modelo de `:core`. La tarjeta muestra «Nueva fecha → …». |
| P5 Mis citas | La cita rechazada CT-1028 aparece en «Historial», no en «Próximas» como en f5 (W2, desviación 3). | **Se deja**, porque la separación la hace el ViewModel (`isFinal`), que está fuera del alcance. Además, una cita rechazada ya no es próxima. |
| P5 Mis citas | El chip «Reprogramada por el taller» (tono neutro) tenía contraste 4.1:1. | **Se corrige** en `b5a9523`: el texto neutro y secundario pasa a #5A6C77 y llega al menos a 4.5:1. |
| P6 Nueva cita | En f6 los días del calendario medían 40 dp de alto y las franjas 36 dp. | **Se corrige** en `fe57408`: ambos miden 48 dp. El calendario queda unos 8 dp más alto por semana. |
| P6 Nueva cita | Los enlaces «Cambiar» y «Listo» de cada paso eran más chicos que 48 dp y no decían qué cambiaban. | **Se corrige** en `fe57408`: miden 48 dp y TalkBack anuncia, por ejemplo, «Cambiar vehículo». |
| P6 Nueva cita | Sin catálogo, la pantalla decía «Catálogo no configurado: …». | **Se corrige** en `c79eaaa`: ahora dice «El taller aún no ha cargado los tipos de servicio ni los horarios. Intente más tarde.» |
| P6 Nueva cita | f6 no muestra la flecha de regreso (W2, desviación 6). | **Se deja**, por la misma razón que en P4. |
| P7 Detalle de servicio | La nota final decía «Las imágenes se almacenan en Cloudinary; el documento del servicio guarda únicamente la URL segura.» | **Se corrige** en `c79eaaa`: ahora dice «Las fotos las registra el personal del taller en cada etapa del servicio.» |
| P7 Detalle de servicio | f7 no muestra la flecha de regreso (W2, desviación 6). | **Se deja**, por la misma razón que en P4. |
| P8 Panel del personal | La sección «Agenda confirmada» no está en f8 (W3, desviación 1). | **Se deja**, porque es el único camino de una cita confirmada a su orden de servicio. Esa orden es la que marca la cita como Atendida. |
| P8 Panel del personal | «Reportes del mes» no queda anclado abajo (W3, desviación 2). | **Se deja**, porque es el último elemento de la lista. Con datos reales la pantalla siempre se desplaza. |
| P8 Panel del personal | «Reprogramar» desde el panel abre el detalle de la cita, no el formulario (W3, desviación 8). | **Se deja**, porque es navegación, que está fuera del alcance. |
| P8 Panel del personal | Con fuente al 130 %, «POR CONFIRMAR» y «Reprogramar» se cortaban. | **Se corrige** en `6b840ed`: `FitText` puede reducir el texto hasta su tamaño al 100 %, y los botones de tarjeta lo usan. Ver `f130-p8.png`. |
| P9 Reportes | Las etiquetas de las barras son Confirmada / Atendida / Pendiente-reprogramada / Rechazada-cancelada, no las de f9. La barra «Finalizada» de f9 no corresponde a ningún estado (W3, desviación 3). | **Se deja**, porque las etiquetas siguen los datos reales. |
| P9 Reportes | Se escribe «periodo», no «período» (W3, desviación 4). | **Se deja**, porque así está en el mockup y la RAE acepta las dos formas. |
| P9 Reportes | El pie de página sigue el brief, no la nota de prototipo de f9. El total VEHÍCULOS es histórico (W3, desviación 5). | **Se deja**, porque el texto del brief describe mejor de dónde salen los datos. |
| P9 Reportes | La tarjeta «Resumen del sistema» no está en f9 (W3, desviación 6). | **Se deja**, porque ya existía, queda debajo del pie y no tapa nada del mockup. |
| P9 Reportes | Con fuente al 130 %, las etiquetas de las barras se cortaban y la leyenda del donut salía con «…». | **Se corrige** en `6b840ed`: las etiquetas usan `FitText` y la leyenda usa dos líneas. Ver `f130-p9.png`. |
| Encabezado (todas) | El avatar con el menú de la cuenta medía 38 dp y no tenía descripción. | **Se corrige** en `fe57408`: el área táctil mide 48 dp, el cuadro visible sigue en 38 dp, y TalkBack dice «Cuenta, DE · Abrir menú de la cuenta». |
| Barra inferior | Las pestañas no seleccionadas usaban `TextMuted` (#8D9CA6, 2.8:1 sobre blanco). | **Se corrige** usando el token `EficazColors.TextSecondary` (#5A6C77, >= 4.5:1) en `EficazBottomBar` (`ui/AppRoot.kt`), sin cambiar la navegación. |
| Escala general | En W2 el texto y el espaciado eran unas 1.4 veces los del mockup (W2, desviación 7). | **Ya corregido** en W4 con `EficazDimens` y la tipografía calibrada. Todas las pantallas caben sobre el pliegue (W4, tabla de ajuste). |
| Botones (todas) | El área táctil mínima era de 44 dp. | **Se corrige** en `fe57408`: `EficazDimens.MinTouchTarget` pasa a 48 dp. El tamaño visible de los botones (40/36 dp) no cambia. |
| Evidencias (personal) | El aviso de carga de fotos sin configurar mencionaba `cloudinary.cloudName` y `local.properties`. | **Se corrige** en `c79eaaa` con un mensaje para el usuario. |
| Evidencias (personal) | Si fallaba la carga de la lista, solo aparecía un error en rojo, sin forma de reintentar. | **Se corrige** en `90f7bb1`: ahora usa `ErrorBox` con «Reintentar». |
| Evidencias (personal) | La lista de evidencias no muestra un indicador de carga. | **Se deja**, porque `EvidenceViewModel` no expone un estado de carga, y cambiar el ViewModel está fuera del alcance. |
| Configuración (personal) | Los catálogos vacíos decían «Catálogo … no configurado.», y el botón flotante solo decía «Agregar». | **Se corrige** en `c79eaaa`. Los mensajes ahora dicen qué hacer: «Agregue el primero con el botón +» o «Agréguelos en Perfil › Configuración». Los botones flotantes dicen «Agregar tipo de servicio» y «Agregar tipo de mantenimiento». |
| Perfil del personal | Es la misma pantalla `ProfileScreen` del cliente (W3, desviación 7). | **Se deja**, porque ya sigue el estilo del mockup (W2, desviación 5). |
| Franjas horarias | Los días de la semana usan las franjas del sábado cuando hoy es domingo (W1, desviación 3). | **Se deja**, porque es un detalle de los datos demo, no de la UI. Los domingos no hay servicio. |

## Accesibilidad

| Punto | Estado |
|---|---|
| `contentDescription` en iconos tocables | Los iconos tocables tienen descripción: regresar, mes anterior y siguiente, reducir y aumentar capacidad, los botones flotantes «Nueva orden», «Agregar vehículo», «Solicitar cita» y «Agregar tipo de …», y las fotos de evidencia con su etapa. El avatar se corrigió en `fe57408`. Los íconos que acompañan un texto visible (accesos rápidos, marcadores de foto) siguen con `null` a propósito, para que TalkBack no repita la palabra. |
| Áreas táctiles de 48 dp o más | Cumplen los botones compartidos y de tarjeta, el avatar, los enlaces del login y de Nueva cita, los días del calendario, las franjas, las opciones del personal y el campo de capacidad (`fe57408`). Los iconos de Material (`IconButton`) ya miden 48 dp. Los marcadores de foto miden 52 dp. |
| Contraste en chips de estado | Todos los tonos (Info, Warning, Success, Danger, Neutral) llegan al menos a 4.5:1, y la prueba `ContrastTest` lo verifica (`b5a9523`). Valores: Info 7.3; Warning 4.8; Success 5.1; Danger 6.0; Neutral 4.8. La barra inferior también usa `TextSecondary` (≥ 4.5:1). |
| Fuente del sistema al 130 % | `LargeFontShots` revisa P1, P2, P3, P5, P8 y P9 en 360 dp. Ya no se corta ningún texto (`6b840ed`). Los botones crecen en altura por encima de 1.3, como ya hacía W4. |
| Idioma y textos técnicos | Todos los textos visibles están en español. Los errores de sistema llegan como «No se pudo completar la operación. Revise su conexión e intente de nuevo.» (`DomainError.userMessage`), y no hay códigos a la vista. Se quitaron las menciones a Firebase, Cloudinary, `local.properties` y «catálogo no configurado» (`c79eaaa`). |

## Estados por pantalla

| Pantalla | Cargando | Vacío con mensaje útil | Error con «Reintentar» |
|---|---|---|---|
| P2 Inicio | `LoadingBox` | «Aún no registra vehículos.» / «Aún no hay servicios finalizados.» | `ErrorBox` |
| P3 Mis vehículos | `LoadingBox` | «Aún no registra vehículos. Agregue el primero con el botón +.» y resultado de búsqueda vacío | `ErrorBox` |
| P4 Detalle de vehículo | `LoadingBox` | Sin servicios, sin citas o sin plan de mantenimiento | `ErrorBox` |
| P5 Mis citas | `LoadingBox` | Tarjeta vacía por pestaña | `ErrorBox` |
| P6 Nueva cita | `LoadingBox` | Sin vehículos, sin fecha elegida o día sin atención | `ErrorBox` (y catálogo vacío) |
| P7 Detalle de servicio e historial | `LoadingBox` | «Este vehículo aún no tiene servicios registrados.» | `ErrorBox` |
| Historial del cliente | `LoadingBox` | «Sus vehículos aún no tienen servicios registrados.» | `ErrorBox` |
| P8 Panel / Servicios | `LoadingBox` | Mensajes por sección | `ErrorBox` |
| P9 Reportes | `LoadingBox` | «Sin datos en el periodo» en cada gráfica | `ErrorBox` |
| Detalle de cita y orden, nueva orden, finalizar orden, franjas, catálogos | `LoadingBox` | Mensajes con la acción a seguir (`c79eaaa`) | `ErrorBox` |
| Evidencias | **No hay** (ver «se deja») | «Aún no hay evidencias. Tome una foto o elija una de la galería y súbala.» | `ErrorBox` (`90f7bb1`) |

`ErrorBox` muestra «Reintentar» con `PrimaryButton`, y `LoadingBox` usa el color azul marino con la etiqueta hablada «Cargando» (`90f7bb1`). Antes, `ErrorBox` usaba un `Button` de Material sin el estilo del sistema de diseño.
