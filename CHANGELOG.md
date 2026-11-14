# Historial de versiones de Cenotes

Cada versión se guarda en Google Drive (`Mi unidad/Notas app/Cenotes-x.y.z.apk`, sin reemplazar la anterior) y, desde la 0.11.0, también como *release* de GitHub (`vx.y.z`, con el APK), que es de donde la app avisa de versiones nuevas. Lo que ve el usuario en Ajustes → Actualizaciones está en `app/src/main/res/raw*/changelog.txt` (inglés, español y alemán).

## 0.16.0 — 2026-11-14 (fase 5 del plan)
- Orden a mano (`NoteSort.MANUAL`, `Note.position`; los mayores primero, las notas nuevas arriba): mantener pulsada una tarjeta la selecciona como siempre y, si se mueve el dedo, se arrastra (`ui/CardReorder.kt`); las demás se apartan en vivo, la lista se desplaza sola en los bordes y al soltar se guarda (`NotesViewModel.applyOrder`: las notas visibles se reparten sus posiciones, así que las de otras secciones no cambian). Fijadas y no fijadas se ordenan por separado. TalkBack: acciones «Subir» y «Bajar».
- Historial (`NoteVersion`, tabla `note_versions`): al cerrar una nota cuyo texto cambió se guarda el texto anterior con su fecha; se guardan las 20 últimas (`MaxVersions`) y se borran con la nota. ⋮ → Historial (`ui/HistoryViews.kt`) las enseña y «Restaurar» devuelve una al editor, guardando antes en el historial lo que había en pantalla.
- Tabletas (≥ 840 dp, el mismo código que Cemail: `ui/Panes.kt` con `TwoPane`, `Centered` y `Dockable`): lista y nota lado a lado, Ajustes en una columna movible con flechas, y Aspecto → Lista de notas a la izquierda o a la derecha (`LocalListOnRight`, `floatingCorner`, menú lateral desde ese lado).
- La animación de abrir y cerrar una nota recorta con las esquinas del diseño (`LocalSkin.shape`) en vez de 24 dp fijos: en Retro 98 ya no se ven redondas.
- TalkBack: interruptores con `toggleable`, opciones con `selectable` (dicen si están elegidas), muestras de color con nombre, tarjetas con «Abrir»/«Seleccionar», y elementos de las listas con «Subir»/«Bajar».
- Base de datos v7 (`position` empieza con la fecha de edición, así que el orden actual se mantiene); copia formato 7 (`position`; el historial no se exporta).
- Notas de voz grabadas: descartadas por decisión de César (basta con dictar).

## 0.15.0 — 2026-11-14 (fase 4 del plan)
- Bloques desplegables (`BlockType.TOGGLE`): se guardan como HTML estándar de Markdown (`<details open>` / `<details>`, `<summary>Título</summary>`, sus líneas, `</details>`), así que la nota sigue siendo Markdown legible. En el editor cada bloque tiene `depth` (dentro de cuántos desplegables está, hasta `MaxToggleDepth` = 3) y el título `collapsed`; `normalizeDepths()` mantiene la estructura válida. Intro en el título escribe dentro (o detrás si está cerrado), Intro en una línea vacía de dentro sale un nivel, y borrar al principio del título lo deja como texto normal (sus líneas salen). Las tarjetas y los widgets enseñan ▸/▾ y esconden lo de los cerrados; compartir y exportar enseñan todo.
- Barra de formato: iconos de bloque desplegable, bloque de código (`{ }`) y línea separadora. Todo se hace con iconos; no hay menú «/» ni comandos escritos.
- Descartado (decisión de César): menú «/», «[[», enlaces entre notas, plantillas y dibujo a mano.

## 0.14.0 — 2026-11-14 (fase 3 del plan)
- Notas protegidas (`Note.locked`, sección `Section.Locked` «Protegidas»): se ocultan de la lista, búsqueda, etiquetas, recordatorios, widgets y atajos; se abren con el bloqueo del móvil (`androidx.biometric`, huella/cara o PIN; `ui/Unlock.kt`). Un desbloqueo abre todas hasta salir de Cenotes (`MainActivity.onStop`) o pulsar «Bloquear». Las notas nuevas creadas dentro de Protegidas nacen protegidas; las que llegan de fuera (compartir, mosaico, atajos) nunca. En la papelera, tapadas mientras estén bloqueadas. Es un bloqueo de privacidad: el contenido no se cifra en el móvil (sigue en la base de datos privada de la app).
- Exportar a PDF (A4 con imágenes, `PdfDocument`), Markdown (con las imágenes) o texto, por el menú de compartir (`ui/Export.kt`).
- Dictar en la barra de formato (`RecognizerIntent`).
- Copiar el texto de una imagen con ML Kit de Google Play services (`play-services-mlkit-text-recognition`, modelo descargado por Android; el incluido en la app hacía el APK de 17 a 60 MB).
- `MainActivity` es ahora `FragmentActivity` (lo pide la ventana de huella). Base de datos v6, copia formato 6.

## 0.13.0 — 2026-11-14 (fase 2 del plan)
- Recordatorios (`Note.reminderAt`, `Note.reminderRepeat` con `Repeat`): una alarma exacta (`AlarmManager.setExactAndAllowWhileIdle`, permiso `USE_EXACT_ALARM`) para el más próximo; al saltar se muestran todos los vencidos y los que se repiten pasan a la siguiente vez (`notify/Reminders.kt`). Notificación con «Hecho» y «Dentro de una hora» (solo los de una vez); se reprograman tras reiniciar, actualizar la app o cambiar la hora. Diálogo con las mismas opciones rápidas que «Posponer» de Cemail (`ReminderDialog`); chip en la nota y en la tarjeta (corto, con ↻); sección Recordatorios en el menú. Permiso de notificaciones al poner el primero (Android 13+).
- Compartir a Cenotes (texto, enlaces e imágenes, `LaunchAction.Shared`): crea una nota con el asunto como título.
- Mosaico de Ajustes rápidos «Nota nueva» (`NewNoteTile`).
- Base de datos v5 y copia formato 5 (recordatorios). Cemail usa WorkManager para posponer; aquí se usa AlarmManager porque un recordatorio tiene que sonar a su hora.

## 0.12.1 — 2026-11-14
- Actualizaciones: «Instalar» usa el APK descargado en la carpeta de la app (por el FileProvider, `external-files-path` Download/) cuando el gestor de descargas no da su URI, y registra cada fallo en el log (`CenotesUpdates`). Probado en el emulador: de 0.11.x a 0.12.0 desde el aviso, con el instalador de Android, y las notas se mantienen. Cemail tiene el mismo código en `update/Updates.kt`.

## 0.12.0 — 2026-11-14 (fase 1 del plan)
- Papelera: borrar envía a la papelera (`Note.trashedAt`), con deshacer; se vacía sola a los 7 días (`TrashDays`) al abrir la app; restaurar, eliminar para siempre (pregunta antes) y vaciar. La papelera no entra en widgets, atajos ni copias.
- Color de nota (`Note.color`, colores de las etiquetas) como velo suave sobre la tarjeta y el editor (`noteTint`, sirve en todos los diseños); desde el menú de la nota y desde la selección.
- Listas: marcadas al final (Ajustes → Notas, activado por defecto) y reordenar arrastrando por el asa (`BlockEditorState.moveItem`).
- Orden de las notas: última edición, creación (`Note.createdAt`) o título.
- Base de datos v4 (migración 3→4) y copia de seguridad formato 4 (color y fecha de creación). Borrar desde el editor ya no pregunta: va a la papelera.

## 0.11.1 — 2026-11-13
- Botón de nota nueva abajo a la derecha (60 dp, como el de redactar de Cemail) y búsqueda como botón redondo arriba.
- Barra inferior con las medidas de Cemail (56 dp de alto, iconos de 52 dp).
- Retro 98: casillas de Windows 98 (`ui/glass/TaskCheckbox.kt`).
- Plan de desarrollo en `docs/ROADMAP.md`.

## 0.11.0 — 2026-11-13
- Cenotes se parece a Cemail: el sistema de diseño (tema, diseños, cristal, movimiento, interruptor y deslizador) es el mismo código que en Cemail.
- Ajustes es un árbol de pantallas con filas grandes: Apariencia, Colores de etiquetas, Copia de seguridad, Copia de ajustes y Actualizaciones.
- Apariencia: los seis diseños de Cemail, claro/oscuro/sistema (también para los iconos de la barra de estado), seis estilos de fondo, sombra 3D, los ocho colores de Celander y color personalizado, transparencia con vista previa.
- Copia de ajustes en un archivo .json (mismo formato que Cemail).
- Actualizaciones desde las *releases* de GitHub: aviso arriba en la lista de notas, descarga e instalación, y novedades de cada versión. Necesita que el repositorio sea público.
- Idiomas: inglés (por defecto), español y alemán, con selector de idioma por app en Android 13+.
- Los colores automáticos de las etiquetas salen ahora de los ocho acentos nuevos, así que pueden cambiar de tono.

## 0.10.2 — 2026-11-09
- El botón atrás ya no se ignora al reabrir una nota muy rápido.

## 0.10.1
- El resaltado de la píldora inferior sigue al dedo.

## 0.10
- Animación de nota como Google Keep y arrastre por la píldora.

## 0.9.1 – 0.9.5
- Colores propios para etiquetas, menú lateral estrecho y translúcido, icono de etiqueta con su color (hueco sin etiquetas).

## 0.2 – 0.9
- Primera versión, casillas y Markdown, editor visual, subtareas, enlaces e imágenes, nombre Cenotes, copia de seguridad, widgets y atajos, etiquetas, archivo, selección múltiple y colores de etiquetas.
