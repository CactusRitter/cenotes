# Diseño de Cenotes (familia "Ce")

Cenotes, **Cemail** y **Celander** comparten el mismo lenguaje visual para que se note que son del mismo ecosistema. La referencia completa del sistema de diseño es [`docs/DESIGN.md` de Cemail](https://github.com/CactusRitter/Cemail/blob/main/docs/DESIGN.md); aquí está lo que Cenotes usa y lo que es propio de una app de notas.

## Código compartido (igual que en Cemail)
| Archivo | Qué es |
|---|---|
| `ui/theme/Theme.kt` | Acentos (los ocho de Celander + color personalizado), claro/oscuro, estilos de fondo (`ThemeStyle`), colores del cristal, `CenotesTheme` |
| `ui/theme/Skin.kt` | Los diseños: Cristal, Moderno, AMOLED, Retro 98, Papel, Brutalista; sombra 3D |
| `ui/glass/Glass.kt` | `Modifier.glass`, `glassSegment`, `GlassBackground`, `GlassIconButton`, `GlassPill`, `PillIcon`, `onBackdrop` |
| `ui/glass/Motion.kt` | Resortes (`glassSpring`, `bounceSpring`, `softBounceSpring`), `pressScale`, `popIn`, `bumpOn` |
| `ui/glass/GlassSwitch.kt`, `GlassSlider.kt` | Interruptor y deslizador propios (sustituyen a los de Material) |
| `ui/SettingsComponents.kt` | `BigRow`, `SettingsCard`, `SwitchRow`, `ChoiceChip`, `IconBubble` |
| `update/Updates.kt`, `ui/UpdateViews.kt` | Versiones nuevas desde las *releases* de GitHub, aviso y novedades |

Si una de estas piezas cambia en Cemail, se copia aquí (y al revés), cambiando solo el paquete y el nombre del tema.

## Ajustes
Árbol corto de pantallas, como en Cemail: la principal tiene una fila grande (`BigRow`) por sección y cada sección es una pantalla con tarjetas (`SettingsCard`). Ir hacia dentro hace crecer la pantalla nueva; atrás, al revés.
- **Apariencia**: diseño, claro u oscuro, sombra 3D, estilo de fondo, color (ocho + barra arcoíris) y transparencia con vista previa.
- **Colores de etiquetas**: los colores propios para etiquetas (propio de Cenotes).
- **Copia de seguridad**: las notas (y sus imágenes) en un .zip (propio de Cenotes).
- **Copia de ajustes**: todos los ajustes en un .json (`"app": "cenotes"`, mismo formato que Cemail).
- **Actualizaciones**: buscar versiones nuevas, aviso de descarga, novedades de la versión actual, de la próxima y de las anteriores (`res/raw*/changelog.txt`).

## Propio de una app de notas
- **Lista**: tarjetas de cristal en cuadrícula o lista (`LazyVerticalStaggeredGrid`); fijadas = `GlassKind.Pinned`. Cada tarjeta se encoge al pulsarla (`pressScale(0.97f)`) y se recolocan con `softBounceSpring`.
- **Aviso de actualización**: primera fila de la lista, a todo el ancho (como arriba en Recibidos en Cemail).
- **Píldora inferior**: cuadrícula / lista y filtro de fijadas; el resaltado sigue al dedo y toma la forma del diseño elegido.
- **Editor**: el contenido va sobre una tarjeta de cristal; acciones en una píldora flotante arriba y barra de formato flotante abajo.
- **Abrir una nota**: la tarjeta crece hasta el editor (transición compartida, `NoteTransition.kt`).
