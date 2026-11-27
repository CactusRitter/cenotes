# Cenotes: funciones

Inventario de lo que la app YA hace (2026-11-27). Mirarlo antes de proponer o programar algo; actualizarlo en el mismo commit que añada, cambie o quite una función. Sacado del código y los changelogs, sin probar en dispositivo.
Rutas relativas a `app/src/main/java/com/cenotes/app/`. Lo compartido: `CeKit/docs/FUNCIONES.md`.

## Notas
- Markdown con editor visual: casillas, subtareas, desplegables, enlaces, imágenes, código (ui/markdown/, ui/NoteEditorScreen.kt)
- Color por nota, etiquetas, reordenar tarjetas, historial, archivo, papelera de 7 días con deshacer
- Notas protegidas con huella, ocultas en búsqueda y widgets (ui/Unlock.kt)
- Búsqueda; dictado por voz; texto de una imagen con ML Kit (ui/TextFromImage.kt); imágenes (data/ImageStore.kt)
- Exportar PDF, Markdown o texto (ui/Export.kt); compartir; recibir texto o imágenes como nota nueva (LaunchAction.kt)

## Recordatorios
- Fecha, hora y repetición con alarma exacta (notify/Reminders.kt); por lugar, al llegar o salir (notify/Places.kt)
- Posponer y acciones desde la notificación

## Accesos
- 2 widgets (widget/NotesWidgets.kt), 3 atajos, tile «Nota nueva» (NewNoteTile.kt)

## Ajustes y datos
- Apariencia (kit) y colores de etiquetas; copia .zip manual (data/Backup.kt); Diagnóstico y actualizaciones (kit)
- Sin cuentas ni sincronización: el dispositivo es el original

## No tiene
Copia automática, escanear documentos, imprimir, Wear OS, enlaces con las otras apps Ce.
Descartado: plantillas, dibujo a mano, notas de voz, menú «/», enlaces entre notas.
