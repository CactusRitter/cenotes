# Historial de versiones de Cenotes

Cada versión se guarda en Google Drive (`Mi unidad/Ce Apps/Cenotes/Cenotes-x.y.z.apk`, sin reemplazar la anterior) y, desde la 0.11.0, también como *release* de GitHub (`vx.y.z`, con el APK), que es de donde la app avisa de versiones nuevas. Lo que ve el usuario en Ajustes → Actualizaciones está en `app/src/main/res/raw*/changelog.txt` (inglés, español y alemán).

## Sin publicar
- Diagnóstico: los botones (compartir, copiar y borrar el registro; el del último cierre) pasan a FlowRow del kit y bajan de línea si no caben (en la OPPO «Borrar» se partía en dos).

## 2.4.1 — 2026-11-22
- Arreglo: las novedades de la versión actual salían vacías en Actualizaciones (el changelog empezaba con BOM y la versión más nueva no se leía). El kit ignora el BOM y release.ps1 escribe sin BOM. Probado en emulador.

## 2.4.0 — 2026-11-22
- Copia de seguridad y ajustes (F6 de CeKit): el contenedor del ZIP (manifiesto, límites contra zip-bombs, excepciones), el JSON tipado de ajustes y la lectura validada de los ajustes comunes pasan al kit; mismas claves y mismo formato. Probado en emulador con la copia real de Cenotes (Cenotes-notes-2026-11-17.zip): 63 notas, 17 imágenes y ajustes restaurados.
- R8 (minify + shrinkResources) en la versión release: APK de 13,8 MB a 4,2 MB. Reglas en `proguard-rules.pro`: Glance (callbacks y widgets), receptores/tiles/workers, registradores de ML Kit y, nueva, los `InputMerger` de WorkManager (sin ella los widgets se quedaban cargando para siempre). Probado en emulador con la release R8: arranque, crear nota, widgets (se dibujan y sus botones abren el editor), recordatorio a una hora (notificación con sus botones), lugar con «Estoy aquí» (ubicación y dirección), copia de seguridad (crear, importar la copia real de 64 notas y 17 imágenes sin duplicar, restaurar ajustes). En el S26 con la release R8: el texto de una foto se lee bien y el lugar buscado se registra en Google Play («watching 1 place(s)»). Sin probar: que el aviso salte al llegar de verdad.
- Tests de migración (`MigrationTest`, Robolectric): las bases de las versiones 1 a 8 de Room se reconstruyen, se llenan con las notas reales de César (copia de `G:Mi unidadCe AppsCenotesCopias` o `CENOTES_REAL_BACKUP`; nunca entran en el repositorio) más notas de prueba (papelera, recordatorios, lugares, historial, colores de etiqueta, texto raro) y se abren con la app de hoy: nada se pierde y el esquema final coincide con las entidades. También: restaurar la copia real dos veces no duplica y una copia hecha ahora se restaura igual. Room exporta su esquema a `app/schemas/` (base para probar futuras migraciones); la lista de migraciones vive en `NotesDatabase.AllMigrations`. `gradlew testDebugUnitTest`: 63 notas reales, todo en verde.
- Avisos, menús y diálogos con desenfoque: opacidad mínima 0,92 (kit). Sin probar.
- Campos de texto de etiquetas, enlaces y lugares pasan a GlassTextField del kit. Sin probar en emulador.
- Arreglo: el cajón vuelve a ser cristal con desenfoque (con 2.3.0 quedó blanco opaco por el suelo de opacidad de los menús). Probado en emulador.

## 2.3.0 — 2026-11-21
- Pasada de temas: el menú ⋮ (nota e imagen) usa GlassMenu del kit; botones de diálogos y pantallas (SoftButton/AccentButton), casilla de etiquetas (TaskCheckbox) y formas de controles siguen el diseño elegido (Retro 98, Brutalista…). Sin probar en emulador.
- Actualizaciones (aviso, Novedades, página Actualizaciones) y página Diagnóstico pasan al kit (CeKit F3b), con sus textos; los textos hablan de «tus datos» en lugar de «tus notas» para valer en todas las apps.
- Página Apariencia y componentes de Ajustes (SettingsCard, ChoiceChip, SwitchRow, IconBubble, BigRow) pasan al kit (CeKit F3a), con sus textos. Los diálogos se alojan en la raíz de la pantalla (copia de seguridad, editor de color) para que el fondo atenuado cubra toda la pantalla. Menús y avisos nunca más claros que 0,92 de opacidad (informe de UI 2026-11-21) y, en Retro 98, el interruptor es una casilla biselada.
- Todos los diálogos (11) pasan de `AlertDialog` de Material a `GlassDialog` del kit (cristal, centrado, animado, toque fuera = cancelar). Los informativos ganan un botón Cancelar. Probado en emulador (claro y oscuro, con teclado): editar etiquetas.
- Actualizador y Diagnóstico pasan al kit (CeKit F2a): `AppLog`, `CrashLog`, `Updates`, `ReleaseNotes` (paquetes `com.ce.kit.log`/`update`), con su FileProvider `${applicationId}.kit` y los permisos INTERNET/REQUEST_INSTALL_PACKAGES. La app solo da nombre, versión y repositorio (`CeKit.init` en `CenotesApp`). Probado en emulador: comprobar actualización y compartir registro.
- Módulo común CeKit (submódulo git `CeKit/`, módulo `:kit`, paquete `com.ce.kit`): `ui/glass`, `Skin`, `Theme` (`CeTheme`, `CeAppearance`: `AppSettings` la implementa) y los textos de acento/estilo/diseño pasan al kit; se borran las copias locales. Mismo comportamiento (probado en emulador: arranque, cajón). gradle.properties como Cemail.

## 2.2.0 — 2026-11-18
- Ajustes → Diagnóstico, copiado de Cemail: `AppLog` (registro `activity.log` en `filesDir`, 200 KB, sin texto de notas ni secretos) y `CrashLog` (último error, también de las corrutinas de `appScope`). Registra arranque, copias de seguridad (exportar/importar, con recuentos), actualizaciones y recordatorios (las llamadas `Log` de `Updates`, `Reminders` y `Places` pasan por `AppLog`). Pantalla con Compartir registro, Copiar y Borrar, y el último error si lo hay (`DiagnosticsPage`); `cache-path shared/` en `file_paths.xml`. Textos en en/es/de. Probado en el emulador (pantalla y registro escrito).


## 2.1.1 — 2026-11-18
- Arreglo grave del menú lateral con Cristal V2: el panel del cajón (hermano del contenido) no recibía el `HazeState` (`LocalHazeState` era null) y no desenfocaba ni rellenaba: se veía la lista a través. Ahora `NotesListScreen` provee `LocalHazeState` a todo el `ModalNavigationDrawer`. Comprobado en el emulador (Cristal, cajón abierto). Detectado en el OPPO de César; mismo fallo y causa que Cemail 2.5.0.

## 2.1.0 — 2026-11-18
- Cristal V2, igual que Celander: Haze 2.0.1 (`hazeBlur`, capas `GlassLayers`, `LocalHazeState`), curva de transparencia con contraste AA (`GlassTokens`), sombra suave `softShadow`, `glassTile`, `fadingTopEdge`. `ui/glass` es idéntico al de Celander salvo el paquete; el ajuste de transparencia (0–100 %) se conserva. Probado en el emulador (arranque y lista, claro y oscuro); revisados los 6 diseños (lista, editor y Ajustes; claro, y oscuro en Cristal y Brutalista) y transparencia 0 y 99 %; sin probar la tableta.
- Arreglo en Cristal V2: la lista cerraba la app (SIGSEGV de `libhwui` unos segundos después de abrir) porque las tarjetas, dentro de la fuente de haze «contenido», desenfocaban la fuente «fondo», que era su propio padre. Ahora `ScreenBackground` (fuente BACKGROUND) y el contenido (fuente CONTENT) son hermanos en `NotesListScreen`.

## 2.0.1 — 2026-11-17
- Se quita la página «Copia de ajustes» (ruta `SETTINGS_BACKUP`, `SettingsBackupPage`, `SettingsBackupIcon`, `Backup.exportSettings` y los textos `settings_backup_*`, `settings_section_backup`, `settings_row_backup_body` en en/es/de): con el botón único «Crear copia de seguridad» (casillas Notas / Con imágenes / Ajustes) sobraba y confundía. Ajustes queda con una sola entrada «Copia de seguridad» (Crear + Importar); un zip de solo ajustes se restaura con el Importar normal. Probado en el emulador: crear y restaurar notas, ajustes y ambos desde la única entrada.

## 2.0.0 — 2026-11-17
- Copia de seguridad en un solo zip: en Ajustes → Copia queda UN botón «Crear copia de seguridad» (se quitan los interruptores «Incluir imágenes» e «Incluir ajustes» y el botón «Exportar ajustes»). Abre un diálogo con casillas (mismo estilo que el de restaurar): Notas (con «Con imágenes» debajo) y Ajustes, todas marcadas; hace falta al menos Notas o Ajustes. Un solo zip: `manifest.json` (`contents` refleja lo elegido), `notes.json` e `images/` solo si Notas, `settings.json` solo si Ajustes. Nombre según el contenido: `Cenotes-notas-…`, `Cenotes-ajustes-…` o `Cenotes-copia-…` (en: notes/settings/backup; de: notizen/einstellungen/sicherung). `Backup.export(uri, includeNotes, includeImages, includeSettings)` sustituye a `exportNotes`; `exportSettings` (Ajustes → Copia de ajustes) lo llama con solo ajustes. Restaurar no cambia.
- Cenotes pasa a firmarse con una clave propia (`keystore/cenotes-release.jks`, RSA 4096, alias `cenotes`; la clave y `keystore.properties` están fuera de git y copiadas en Drive, `Ce Apps/Común`). Las instalaciones con la firma antigua (debug) no pueden actualizarse a esta versión: hay que reinstalar una vez (copia de seguridad de notas y ajustes → desinstalar → instalar → restaurar). `assembleRelease` firma con ella (sin R8).

## 1.6.1 — 2026-11-17
- Brutalista: la sombra dura de la tarjeta que vuelve a la lista al cerrar una nota seguía sin verse mientras viajaba y aparecía de golpe al terminar (el arreglo de `ShadowRoomShape` de 1.5.1/1.6.0 no bastaba en un dispositivo real). Causa: el hueco de la sombra era un `padding` FUERA del elemento compartido (`noteSharedBounds`), así que el overlay que dibuja la tarjeta durante la transición solo incluía la tarjeta y dejaba fuera la sombra, que se dibuja fuera de sus límites. Ahora el `padding(end, bottom = hardShadow)` va DESPUÉS de `noteSharedBounds` (dentro del elemento compartido: el overlay dibuja tarjeta y sombra juntas), y el recorte del overlay es el rectángulo del elemento (`noteSharedBounds(id, clipRect = hardShadow > 0)`); se elimina `ShadowRoomShape`. Con otros diseños (`hardShadow` 0) no cambia nada. Verificado en el emulador a velocidad normal grabando con `screenrecord` y extrayendo fotogramas de la grabación (antes: fotograma de llegada sin sombra y la sombra aparece en el siguiente; después: la sombra está en el fotograma de llegada), en Brutalista oscuro y claro.
- Lista bajo la fila superior: con la lista desplazada, las tarjetas pasaban por debajo del título, los botones redondos y la barra de estado (que no tienen fondo) y el texto se mezclaba. Nuevo componente común `TopScrim(height, modifier, hazeState, visible)` en `ui/glass/Glass.kt`: una franja detrás de la barra de estado y la fila superior, que se desvanece en sus últimos 24 dp y no recibe toques. Cristal: Haze (blur 20 dp, velo del fondo al 60 %, desvanecimiento progresivo de arriba abajo, el mismo `hazeState` que los botones); resto de skins (sin blur): degradado sólido del color de fondo del skin (`glass.base`) que se desvanece. Solo se ve cuando la lista se ha desplazado (`visible = gridState.canScrollBackward`, aparece con `glassSpring`), así que en reposo todo queda como antes. En `NotesListScreen`, `height = topPadding + 4.dp`, entre la lista y la fila superior. Probado en el emulador con la lista desplazada en Brutalista oscuro y claro, Cristal claro, Retro 98, Papel y AMOLED.

## 1.6.0 — 2026-11-17
- Preparación para sincronizar (sin Drive todavía): Room 8→9 (`Migration8To9`, en una transacción, cuenta las notas antes y después y exige un uuid distinto en cada una; si no cuadra lanza error y la base queda como estaba). `notes.uuid` (UUID v4 estable, índice único) y `notes.modifiedAt` (última modificación de cualquier cosa: texto, fijar, color, archivo, etiquetas, bloqueo, papelera, orden; `updatedAt` sigue siendo la última edición del texto y ordena «editadas»). Tabla `deleted_notes` (uuid, deletedAt): `NoteDao.deleteNotes` anota cada borrado definitivo (papelera vaciada, caducada o borrado para siempre) y purga las de más de `TombstoneDays` = 90 (decisión a revisar si hay sincronización: borrarlas demasiado pronto puede resucitar notas desde un móvil que lleva meses sin sincronizar). `setReminder` (avance automático de un recordatorio repetido) no cambia `modifiedAt`.
- Copias de seguridad en un contenedor .zip (el mismo que copiará Cemail), solo el formato nuevo: `manifest.json` {"app":"cenotes","container":1,"appVersion","createdAt","contents":["notes"|"settings"]} + `notes.json` (formato 9) y `images/<archivo>` (opcionales) y/o `settings.json` (lo que escribe `SettingsRepository.exportJson`: {"app":"cenotes","format":1,"settings":{clave:{"s"|"b"|"i"|"l"|"f"|"set":valor}}}). Tres exportaciones en Ajustes → Copia: «Exportar notas» (zip con notas e imágenes si se marca; casilla «Incluir ajustes», desactivada por defecto, añade `settings.json`), «Exportar ajustes» (zip solo con `settings.json`) y, en Ajustes → Copia de ajustes (sigue existiendo aparte), «Guardar copia» (el mismo zip de ajustes). Nombres traducidos: `Cenotes-notas-AAAA-MM-DD.zip`, `Cenotes-ajustes-AAAA-MM-DD.zip` (en: notes/settings; de: notizen/einstellungen). Importar: un solo selector (cualquier archivo, `*/*`); `Backup.inspect` abre el zip, exige el manifiesto con `app` = cenotes (un zip de otra app, un .json suelto, una copia de formato 8 sin manifest o cualquier otra cosa dan «Ese archivo no es una copia de Cenotes»; `container` mayor que 1 da «viene de un Cenotes más nuevo») y pregunta qué restaurar con interruptores solo de lo que lleva (Notas / Ajustes; los ajustes empiezan apagados si hay notas). Seguridad: solo se leen `manifest.json`, `notes.json`, `settings.json` e `images/<nombre simple>` (`SafeName`); tope de 64 MB por entrada JSON y por imagen, 20 000 entradas; una imagen que se pasa del tope no deja un archivo a medias. Se quitan los formatos antiguos del importador: ya no se aceptan zips sin manifest ni el .json suelto de la copia de ajustes (César reinstala si algo se queda atrás y sus notas viejas se migran con Room). Notas de `notes.json` formato 9: cada nota lleva `uuid`, `modifiedAt` (más `title`, `content`, `pinned`, `archived`, `updatedAt`, `createdAt`, `color`, `reminderAt`, `reminderRepeat`, `locked`, `position`, `labels`, `place`); la raíz lleva `app` "Cenotes", `format` 9, `exportedAt`, `includesImages`, `labels`, `labelColors`, `notes` y `deleted` [{uuid,deletedAt}]. Importación de notas: por uuid, gana el mayor `modifiedAt`, lo borrado para siempre no reaparece ni se borra nada local; el resumen muestra añadidas, actualizadas y las que siguen borradas. `NoteSerializer` común para copia y futura sincronización. Nunca se exporta ni se restaura una clave con pinta de secreto (`SettingsRepository.isSecretKey`; test en `SettingsSecretsTest`). Textos en en/es/de.
- Actualizaciones: la descarga de la nueva versión ya no usa el gestor de descargas de Android: se hace dentro de la app (OkHttp, copiado de Cemail 5c4ca99) a `filesDir/updates/Cenotes-x.y.z.apk.part`, que se renombra al terminar, comprueba `Content-Length` y el tamaño del asset de GitHub, y reanuda con `Range` tras un corte. `Updates` expone `download: StateFlow<DownloadState>` (Idle, Downloading, Ready, Failed) y `startDownload` y `cancelDownload`; `install()` usa el FileProvider (`files-path updates/`). El aviso (lista y Ajustes → Actualizaciones) muestra `GlassProgressBar` (nuevo `ui/glass/GlassProgress.kt`, idéntico al de Cemail) con porcentaje y megas, Cancelar, error con Reintentar e Instalar. Textos nuevos en en/es/de (`update_progress`, `update_progress_unknown`, `update_ready`, `update_failed_network`, `update_failed_incomplete`, `update_cancel`, `update_retry`); se quitan `update_started`, `update_failed` y `update_downloading`. Diferencias con Cemail: dependencia `okhttp` añadida; el JSON de GitHub se lee con `org.json` (Cenotes no usa el plugin de serialización) y los avisos van a `Log` con la etiqueta `CenotesUpdates`. Probado en el emulador con una debug temporal 1.5.0 contra la release v1.5.1 real: detección, barra, Cancelar, corte de red y Reintentar (reanuda), instalador.
- Brutalista: al cerrar una nota, la tarjeta que vuelve a su sitio perdía la sombra dura (abajo y a la derecha) hasta acabar la transición. Causa: `noteSharedBounds` recorta la tarjeta durante el viaje (`OverlayClip`) a su forma, y la sombra queda fuera de esa forma. Ahora el recorte de la tarjeta de la lista deja sitio a la sombra (`shadowRoom` = `hardShadow`, forma `ShadowRoomShape` en `NoteTransition.kt`); el editor no cambia. Reproducido y comprobado en el emulador con animaciones 10×.
- Selección de tarjetas por skin (sistema compartido con Cemail): en `Skin.kt`, tokens nuevos de `SkinLook` (`selectedEdgeWidth`, `badgeFill`, `badgeInk`, `badgeEdge`, `badgeEdgeWidth`, `badgeShadow`, `badgeBevel`); en `Glass.kt`, `Modifier.selectedOutline(shape)` y `SelectionBadge()`. Cristal y Moderno: borde de acento de 2,5 dp + círculo con ✓; Brutalista: borde de acento de 4 dp, insignia casi cuadrada con borde negro de 2 dp y sombra dura de 2 dp; Retro 98: borde de 2 dp, casilla cuadrada blanca con bisel hundido y ✓ negro; AMOLED y Papel: borde de 2 dp e insignia con borde fino. La sombra dura de la tarjeta sigue entera al seleccionar. Probado los seis skins en el emulador.
- Diálogo «¿Qué quieres restaurar?»: casillas de verificación (las mismas que las de las listas, con el aspecto de cada diseño) en vez de interruptores.
- Limpieza de código sin cambios de comportamiento: imports y funciones sin uso (`NotesViewModel.togglePin`, `VisibilityIcon`), la comprobación de API < 26 en `createChannel`, la limpieza de APKs del gestor de descargas antiguo en `Updates.cleanUp`, y `junit` pasa al catálogo de versiones.
- Esta versión lleva la migración Room 8→9 (irreversible): probada con la copia real de César (64 notas, 17 imágenes) y con la build firmada sobre una 1.5.1 con datos.

## 1.5.1 — 2026-11-16
- Tema Brutalista: la sombra dura de la barra de búsqueda, del chip de enlace y de la barra de formato del editor ya no se corta por la derecha durante el despliegue (`AnimatedVisibility` recorta al tamaño del contenido): cada uno lleva `padding(end = hardShadow * 1.25)` dentro. Cenotes no tiene etiquetas anidadas en el menú lateral; los desplegables de checklists no llevan sombra. Comprobado en el emulador con animaciones 10× (búsqueda); chip de enlace y barra de formato, mismo arreglo, sin probar a mano.
- Tema Brutalista: al archivar, borrar o añadir una nota, la sombra dura de la tarjeta se recortaba durante el fundido de `animateItem` (el fundido recorta lo que sobresale de la fila, y `animateItem` no admite `ModulateAlpha`). Cada tarjeta lleva ahora `padding(end, bottom = hardShadow)` y la lista resta lo mismo en `contentPadding` y en los huecos, así que la distancia visible no cambia; con otros temas (`hardShadow` 0) no cambia nada. Reproducido en el emulador con animaciones 10× y comprobado después del arreglo.

## 1.5.0 — 2026-11-16
- Listas de casillas: una casilla con hijas (sangradas) es ahora un desplegable. Una flecha a la derecha la pliega y despliega (mismo resorte que los bloques plegables) y, plegada, muestra las hechas (p. ej. 2/5). Arrastrar una casilla por su asa sobre la mitad de un desplegable plegado la mete como última hija (arriba o abajo del todo sigue siendo antes/después). Marcar el padre sigue marcando todas las hijas, y el padre se marca solo al completarse todas. Estado plegado: se guarda en el propio Markdown con un comentario HTML invisible al final de la línea (`- [ ] texto <!--fold-->`), solo si tiene hijas; las notas antiguas sin sangrías se ven igual y las copias/importaciones no cambian. Intro en una casilla plegada añade la nueva después de sus hijas ocultas.
- Lista de notas: al abrir una nota y volver, la lista conserva exactamente su posición (antes volvía arriba en el móvil, porque abrir una nota quita la lista y se perdía su `LazyStaggeredGridState`). El estado del scroll vive ahora en `MainActivity` (`rememberLazyStaggeredGridState` + `gridKey` guardable), así que también sobrevive a girar la pantalla, y `NotesListScreen` lo recibe y solo lo lleva arriba cuando cambia la lista (`section`, `pinnedOnly` o `query`). Probado en el emulador: móvil (abrir/cerrar nota y giro de pantalla conservan la posición; cambiar a otra sección o etiqueta y volver empieza arriba) y tamaño tableta con dos paneles.

## 1.0.2 — 2026-11-16
- Etiquetas: al abrir una etiqueta se ven también sus notas archivadas (como en Keep), y los contadores del menú lateral las cuentan (`NotesViewModel`: `Section.ByLabel` y `labelCounts` ya no descartan `archived`; siguen fuera las protegidas y la papelera). Las archivadas llevan un icono pequeño de archivo junto al título (`NoteCard.showArchived`, texto `archived_badge`), en todas las vistas menos Archivo y Papelera. Probado en el emulador con la copia de César: Notas no muestra archivadas, Archivo igual y sin icono, y la etiqueta Blog muestra sus 23 con icono.

## 1.0.1 — 2026-11-16
- Copia de seguridad y copia de ajustes: Importar acepta cualquier archivo (`arrayOf("*/*")` en `importLauncher` y `open`); el filtro por tipo dejaba en gris el .zip en el móvil de César. `Backup.import` ya dice «no es de Cenotes» si no lo es. Probado en el emulador: un .zip con otra extensión se importa (64 notas, 17 imágenes) y un .txt da el mensaje sin colgarse.

## 1.0.0 — 2026-11-16 (primera versión completa, publicada por indicación de César a través de la sesión Ce)
- Retro 98: el botón de nueva nota tiene el mismo color que los demás botones (plateado), no el resaltado blanco (`fabKind()`/`fabTint()`, `SkinLook.fabPinned`). Compartido con Cemail.
- Pasa a 1.0.0: Cenotes ya tiene todo lo del plan (fases 1 a 5 y recordatorios en un lugar).

## 0.18.2 — 2026-11-15
- Barra de selección: se sigue viendo solo el número (como Cemail 2.2.9, para que no se parta en Retro 98 con cinco botones; comprobado en el emulador), y TalkBack lee «N seleccionadas» (`selected_count_spoken`, en/es/de).
- Compartido con Cemail: misma regla que su barra de selección en 2.2.9.

## 0.18.1 — 2026-11-15
- Ajustes: el margen lateral va dentro del `verticalScroll` (`padding(start = 12.dp, end = 12.dp, bottom = 24.dp)`) y la barra superior lleva su propio `padding(horizontal = 12.dp)`; el scroll recortaba la sombra lateral de las tarjetas en su borde. Revisados los demás scrolls (editor, historial, diálogos de etiquetas y lugares, lista con `contentPadding`): no tienen el problema.
- Compartido con Cemail: mismo arreglo que Cemail 2.2.8.

## 0.18.0 — 2026-11-15
- Menú lateral como el de Cemail (`MailApp.DrawerContent`): tarjeta de cristal flotante (`GlassKind.Menu`, 28 dp, 82 % del ancho) con Ajustes como botón redondo arriba, las secciones (Notas, Recordatorios, Archivo, Protegidas, Papelera) en el lugar de las carpetas y las etiquetas debajo con el título «Etiquetas» en color de acento, su color y su número, y «Editar etiquetas». Píldoras de cristal (la elegida, `GlassKind.Pinned`), aparición escalonada con `popIn` y cierre tocando fuera de la tarjeta. Sustituye al panel estilo Keep (`NavigationDrawerItem`, 280 dp).
- Compartido con Cemail: misma colocación que su menú; Cenotes no cambia ningún archivo compartido.

## 0.17.2 — 2026-11-15
- `popIn` (`ui/glass/Motion.kt`) usa `CompositingStrategy.ModulateAlpha`: con alpha < 1 la capa se dibujaba en un búfer del tamaño del elemento y cortaba la sombra exterior (la suave y la de Brutalista), que aparecía de golpe al terminar.
- Compartido con Cemail: arreglo de Cemail 2.2.7, copiado igual.

## 0.17.1 — 2026-11-15
- Sombra 3D del cristal suave, copiada de Cemail 2.2.6 (`ui/glass/Glass.kt`, `ui/theme/Skin.kt`, `ui/theme/Theme.kt` iguales que en Cemail salvo el paquete): `SkinLook.lift`/`liftColor`, `liftShadow`, `liftSegment`, `glassDepthColor` nuevo; `glassSegment` gana `wall`/`fillOverride` (Cenotes no los usa). Texto `depth_body` nuevo en en/es/de.
- Ajustes → Actualizaciones: mismo freno que Cemail para no pulsar dos veces «Buscar ahora».
- Compartido con Cemail: todo lo anterior viene de Cemail 2.2.6. Registro en `Cemail/docs/ECOSYSTEM.md`.

## 0.17.0 — 2026-11-14 (tras probar en la tableta de César)
- Recordatorios en un lugar (`Place`, tabla `places`; `Note.placeId`, `Note.placeLeave`; `Reminder` lleva el lugar): al llegar o al salir de un lugar guardado. El lugar se busca en el dispositivo con el `Geocoder` de Android o con «Estoy aquí» (`FusedLocationProviderClient`), sin mapa ni claves (`ui/PlaceViews.kt`). Las geocercas de Google Play (`play-services-location`, `notify/Places.kt`, radio 150 m, `PlaceReceiver`) avisan aunque Cenotes esté cerrado, con la misma notificación que los de hora; al saltar, el recordatorio de lugar se quita (como Keep). Se vuelven a registrar al reiniciar, al actualizar y al volver a la app (`MainActivity.onStart`). Permiso de ubicación «todo el tiempo» pedido solo al poner el primero, con una explicación antes. Probado en la tableta: búsqueda, permisos y geocerca registrada; el aviso al llegar no se pudo probar sin ir al sitio.
- Selector de fecha y hora propio (`ui/DateTimeDialog.kt`): tarjeta de cristal con el `DatePicker` y el `TimePicker` de Material 3 en los colores del tema, en vez de los diálogos de Android. Se avisó a la sesión de Cemail para que haga lo mismo en `ScheduleDialog`.
- Tabletas: un único fondo detrás de todo y `ScreenBackground`/`LocalSharedBackdrop` para que los paneles y Ajustes no pinten el suyo (había un corte visible entre lista y nota).
- Textos «teléfono/móvil/Handy/phone» → «dispositivo/Gerät/device». Arreglados caracteres estropeados en `ReminderViews.kt` (`Â·` en los recordatorios que se repiten).
- Base de datos v8, copia formato 8 (el lugar va con la nota, por nombre).
- Compartido con Cemail: `DateTimeDialog` (Cemail 2.2.5) y los textos «dispositivo» en vez de «teléfono». Registro en `Cemail/docs/ECOSYSTEM.md`.

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
