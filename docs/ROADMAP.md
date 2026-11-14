# Plan de desarrollo de Cenotes

**Idea guía:** Cenotes es una app de notas **sencilla, como Google Keep**, con el aspecto de las apps "Ce". Se copia lo útil de Keep, Samsung Notes y Notion, pero solo lo que se entiende sin explicación. Cada función nueva tiene que poder usarse sin leer nada y no puede añadir botones a la pantalla principal si no se usa a menudo (lo poco frecuente va en el menú «⋮» de la nota o en Ajustes).

## Qué tiene cada app y qué nos sirve

| | Google Keep | Samsung Notes | Notion | ¿Para Cenotes? |
|---|---|---|---|---|
| Listas con casillas, subtareas | ✅ | ✅ | ✅ | **Ya está** |
| Etiquetas, fijar, archivo | ✅ | carpetas | páginas | **Ya está** |
| Imágenes, enlaces, compartir | ✅ | ✅ | ✅ | **Ya está** |
| Widgets, atajos del icono | ✅ | ✅ | — | **Ya está** |
| **Papelera** (recuperar 7 días) | ✅ | ✅ (15 días) | ✅ | **Ya está** (0.12) |
| **Color de fondo de la nota** | ✅ | ✅ | — | **Ya está** (0.12) |
| Casillas marcadas al final, reordenar arrastrando | ✅ | — | ✅ | **Ya está** (0.12) |
| Ordenar notas (editada, creada, a mano) | a mano | ✅ | ✅ | **Ya está** salvo a mano (0.12) |
| **Recordatorios** con notificación | ✅ | ✅ | ✅ | **Ya está** (0.13) |
| **Compartir hacia Cenotes** desde otras apps | ✅ | ✅ | ✅ | **Ya está** (0.13) |
| Bloquear notas con huella o PIN | — | ✅ | — | **Ya está** (0.14, sección Protegidas) |
| Notas de voz (con texto transcrito) | ✅ | ✅ | — | Dictado **ya está** (0.14); grabar audio, fase 5 |
| Copiar texto de una foto (OCR) | ✅ | ✅ | — | **Ya está** (0.14) |
| Dibujo a mano | ✅ | ✅ (S Pen, muy completo) | — | **No**: en Cenotes se escribe con el teclado |
| Bloques desplegables | — | — | ✅ | **Ya está** (0.15), con un icono |
| Enlaces entre notas, menú «/», comandos escritos | — | — | ✅ | **No**: poco intuitivo |
| Plantillas | — | ✅ | ✅ | **No** |
| Exportar a PDF / Markdown / texto | Docs | ✅ PDF, Word | ✅ | **Ya está** (0.14) |
| Historial de versiones de una nota | — | — | ✅ | Más adelante |
| Tabletas: lista y nota a la vez | ✅ | ✅ | ✅ | Como Cemail (fase 5) |
| Fuentes, tamaños y colores de texto, páginas, maquetación | — | ✅ (como Word) | — | **No**: lo hace difícil |
| Anotar PDF, S Pen avanzado | — | ✅ | — | **No** |
| Bases de datos, tablas, wiki de equipo | — | — | ✅ | **No**: no es para notas rápidas |
| Colaborar en tiempo real, sincronizar con nube propia | ✅ | ✅ | ✅ | **No** por ahora (necesita servidor); la copia de seguridad cubre el cambio de móvil |
| Resúmenes con IA | ✅ (Gemini) | ✅ (Galaxy AI) | ✅ | **No** por ahora |

## Fases

### Fase 1 · 0.12 — Lo básico que falta (como Keep) ✅ hecha en 0.12.0
- ✅ **Papelera**: borrar manda la nota a la Papelera (en el menú lateral, debajo de Archivo); se borra sola a los 7 días; desde allí, restaurar, eliminar para siempre o vaciar.
- ✅ **Color de la nota**: los 10 colores de las etiquetas como velo suave sobre la tarjeta y el editor, en todos los diseños y en claro/oscuro. Menú «⋮» de la nota y selección múltiple.
- ✅ **Listas**: «Marcadas al final» (Ajustes → Notas) y reordenar elementos arrastrándolos por su asa.
- ✅ **Orden de las notas**: última edición, creación o título. Ajustes → Notas.
- ⏳ **Orden a mano** (arrastrar tarjetas en la lista, como Keep): pendiente; choca con «mantener pulsado = seleccionar» y con la cuadrícula escalonada, así que necesita su propio diseño. Pasa a la fase 5.

### Fase 2 · 0.13 — Recordatorios y entrada rápida ✅ hecha en 0.13.0
- ✅ **Recordatorios**: fecha y hora (hoy, mañana, la semana que viene, otra), repetición (diaria, semanal, mensual) y notificación con «Hecho» y «Dentro de una hora». Sección «Recordatorios» en el menú.
- ✅ **Compartir a Cenotes**: texto, enlaces e imágenes de cualquier app crean una nota (con su título y su enlace).
- ✅ **Mosaico de Ajustes rápidos** para una nota nueva.

### Fase 3 · 0.14 — Privacidad y más formas de capturar ✅ hecha en 0.14.0 (salvo grabar audio)
- ✅ **Notas protegidas**: sección «Protegidas» que se abre entera con la huella, la cara o el bloqueo del móvil y se cierra al salir de Cenotes.
- ✅ **Dictar**: el texto dicho se escribe en la nota (reconocimiento de voz de Android).
- ⏳ **Notas de voz grabadas** (guardar el audio en la nota para escucharlo): pasa a la fase 5.
- ✅ **Texto de una imagen** (OCR en el móvil con ML Kit de Google Play, sin enviar la foto).
- ✅ **Exportar** una nota a PDF, Markdown o texto.
- Posible mejora: cifrar el contenido de las notas protegidas con una clave del Android Keystore.

### Fase 4 · 0.15 — Bloques desplegables ✅
- **Bloque desplegable** (como el «toggle» de Notion): una línea con una flecha que esconde o enseña lo que hay debajo. Se crea con un **icono** de la barra de formato, nunca escribiendo comandos.
- **Todo con iconos**: la barra de formato tiene un botón para cada tipo de línea (casilla, viñetas, numerada, título, cita, código, separador, desplegable).

Descartado por decisión de César (2026-11-14): menú «/» y «[[» (comandos escritos: poco intuitivos), enlaces entre notas, plantillas y dibujo a mano. En Cenotes se escribe con el teclado.

### Fase 5 · 0.16 — Tabletas y pulido
- Dos paneles desde 840 dp como Cemail (lista a la izquierda y nota a la derecha), Ajustes movibles.
- Accesibilidad con TalkBack revisada en todas las pantallas.
- Historial de versiones de una nota (las últimas 20 ediciones, para deshacer un borrado grande).
- Orden a mano de las notas arrastrando las tarjetas (viene de la fase 1).
- Notas de voz grabadas, con reproductor en la nota (viene de la fase 3).

## Futuro (no se hace todavía)
- **Enlaces entre las apps "Ce"**: «Crear evento en Celander» desde una nota con fecha, y «Guardar en Cenotes» un correo desde Cemail. Se hará cuando las tres apps estén claras y se decida junto con Cemail y Celander; hoy ninguna enlaza con otra.

## Cómo se decide qué entra
1. ¿Lo usaría alguien cada semana? Si no, no entra o va escondido en «⋮».
2. ¿Se entiende sin explicación? Si necesita un tutorial, se simplifica o no entra.
3. ¿Encaja con Cemail y Celander? Mismo diseño, mismos componentes, mismos textos en inglés, español y alemán.
