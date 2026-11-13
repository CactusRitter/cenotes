# Historial de versiones de Cenotes

Cada versión se guarda en Google Drive (`Mi unidad/Notas app/Cenotes-x.y.z.apk`, sin reemplazar la anterior) y, desde la 0.11.0, también como *release* de GitHub (`vx.y.z`, con el APK), que es de donde la app avisa de versiones nuevas. Lo que ve el usuario en Ajustes → Actualizaciones está en `app/src/main/res/raw*/changelog.txt` (inglés, español y alemán).

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
