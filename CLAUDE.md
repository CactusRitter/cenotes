# Cenotes

App de notas para Android. Forma parte de las apps "Ce" de César Caballero y su amigo Jonathan, junto con **Celander** (calendario) y **Cemail** (correo): las tres comparten el mismo diseño y deben parecer una sola familia. No se publica en Play Store: es para uso propio y de amigos (APK).

- Paquete / applicationId: `com.cenotes.app`
- Kotlin + Jetpack Compose (Material 3), un único módulo `:app`
- minSdk 26 · compileSdk 37 · targetSdk 36 · Java 17
- AGP 9.5 (Kotlin integrado: **no** se aplica `org.jetbrains.kotlin.android`) · Kotlin 2.4 · Room · Haze (cristal) · Coil · Glance (widgets)
- Las versiones de las dependencias están en [gradle/libs.versions.toml](gradle/libs.versions.toml).

**Al empezar:** lee [docs/TRASPASO.md](docs/TRASPASO.md) (estado vivo, pendientes, lo no probado) y las normas comunes de las apps Ce en `D:\cesar\Documents\Claude Projects\Ce\CLAUDE.md` (turnos de emulador/S26/tableta con `emu-lock.ps1 acquire Cenotes [-Dev s26|tableta]`, burn-in, informes a Ce Líder).

Documentación: [docs/DESIGN.md](docs/DESIGN.md) (diseño compartido con Cemail y Celander, **leer antes de tocar la interfaz**), [docs/ROADMAP.md](docs/ROADMAP.md) (plan de desarrollo) y [CHANGELOG.md](CHANGELOG.md).

## Comandos

```bash
./gradlew assembleDebug          # compilar APK de depuración
./gradlew installDebug           # instalar en el emulador/dispositivo conectado
./gradlew lintDebug              # lint de Android
```

En Windows, `gradlew.bat`. El emulador es `Pixel_API_36`; [scripts/emulator.ps1](scripts/emulator.ps1) lo arranca sin ventana (`start`, `shot`, `stop`).

### Publicar una versión
**Solo cuando César lo diga** (o la sesión líder Ce, si lo dice ella). Mientras tanto, los cambios se acumulan: commit y push a `main`, y cada cambio anotado en la sección «Sin publicar» de arriba de `CHANGELOG.md` (sin subir la versión ni tocar `res/raw*/changelog.txt`). Un fallo grave (la app no arranca, se pierden datos, seguridad) sí se publica al momento, avisando. Al publicar, esa sección pasa a ser la entrada de la versión:

1. Subir `versionCode` y `versionName` en `app/build.gradle.kts`, añadir la entrada en `CHANGELOG.md` y en `res/raw*/changelog.txt` (inglés, español y alemán: es lo que muestra Ajustes → Actualizaciones).
2. Compilar y copiar el APK a `G:\Mi unidad\Ce Apps\Cenotes\Cenotes-x.y.z.apk` (nunca borrar ni sustituir los anteriores).
3. Commit y push a `main`.
4. Publicar la *release* en GitHub con el APK y notas con secciones `## en`, `## es`, `## de`: la app avisa de versiones nuevas leyendo la última *release* del repositorio (`update/Updates.kt`). Para que funcione, el repositorio tiene que ser público, como el de Cemail.

```bash
gh release create vx.y.z "G:/Mi unidad/Ce Apps/Cenotes/Cenotes-x.y.z.apk" --title "Cenotes x.y.z" --notes-file notas.md
```

## Reglas del proyecto

### Diseño (obligatorio)
- Misma identidad visual que Cemail y Celander: el sistema de diseño (`ui/theme/Theme.kt`, `ui/theme/Skin.kt`, `ui/glass/*`) es **el mismo código que en Cemail**, con los mismos valores. Si se cambia en una app, se cambia en las otras.
- Usa `CenotesTheme`, `Modifier.glass(...)`, `GlassBackground`, `GlassPill`, `PillIcon`, `GlassSwitch`, `GlassSlider`, `pressScale`, `popIn`. No inventes colores, radios ni animaciones nuevas.
- Nada de colores fijos en las pantallas: siempre `MaterialTheme.colorScheme` y `LocalGlass`. El texto dibujado directamente sobre el fondo usa `onBackdrop(...)` (el escritorio verde de Retro 98).

### Idiomas (obligatorio)
- La app usa el idioma del sistema. Ningún texto visible al usuario en el código: todo en `res/values*/strings.xml`.
- `values/` es inglés (por defecto), `values-es/` español y `values-de/` alemán (con «du»). Cada clave nueva se añade a la vez en los tres.

### Arquitectura
- Simple: sin Hilt; las dependencias viven en `CenotesApp`; navegación con estado de Compose. Room es la fuente de verdad de las notas; los ajustes, en `SettingsRepository` (SharedPreferences).

## Ahorro de tokens (norma de César para todas las apps Ce)
Lee `D:\cesar\Documents\Claude Projects\Ce\docs\AHORRO.md`: subagentes para leer/comparar/buscar (resumen de 10-15 líneas), salidas siempre recortadas, mensajes cortos entre sesiones, modelo barato en tareas mecánicas, y mantener `docs/TRASPASO.md` de esta app al día (aviso a César «conviene /clear» al cerrar un bloque grande).

**Informar a la líder:** al cerrar cada bloque, `SendMessage` a «Ce Líder» con un resumen de máximo 6 líneas (qué hiciste y commit, decisiones de diseño que tomaste, qué no probaste, qué le pides a César). César no debe ser el único que se entera.

**Consolas y cuentas:** lo ya hecho (Google Cloud, Entra, claves, webs, dispositivos) está en `D:\cesar\Documents\Claude Projects\Ce\docs\NUBE.md`; mírelo antes de abrir una consola o preguntar a César, y anota allí lo nuevo.

**Reutilizar antes de inventar:** antes de decir «no se puede» o diseñar algo, busca si otra app Ce ya lo resuelve (`Ce/docs/NUBE.md`, `Ce/docs/ECOSYSTEM.md`, `docs/` de las otras apps; un subagente barato con resumen de 5 líneas). Lo que una app ya sabe hacer, las demás lo copian.

## Dispositivos reales y mensajes entre sesiones (César, 2026-11-21: vale sin volver a preguntar)
- Se puede instalar y probar en el **S26** `RFGL107KERL` (`--user 0`) y en la **tableta** `R52Y806MWSP` con `adb -s <serie> install -r` (nunca desinstalar ni borrar datos; nunca `installDebug` ni `connectedAndroidTest` sin `ANDROID_SERIAL`). Con la pantalla SIEMPRE encendida durante la prueba: `adb -s <serie> shell svc power stayon true` y `false` al terminar. El **OPPO** `3B165C00LH900000` es el móvil principal de César: NO tocarlo sin que él lo pida. Correos de prueba: solo entre cuentas propias de César, con asunto de prueba. Si dos sesiones usan el mismo móvil, mirar antes `topResumedActivity` y esperar.
- Enviar mensajes a «Ce Líder» y a las demás sesiones Ce con `SendMessage` está permitido y esperado (resúmenes, preguntas, avisos): no hace falta pedir permiso a César.

## Normas comunes, turnos y traspaso (César, 2026-11-22)
- Las normas de toda la familia Ce están en `D:\cesar\Documents\Claude Projects\Ce\CLAUDE.md`; léelo al empezar. El estado y los pendientes de esta app, en `docs/TRASPASO.md`.
- **Turnos**: emulador, S26 y tableta son de uno solo a la vez: `powershell -NoProfile -ExecutionPolicy Bypass -File "D:\cesar\Documents\Claude Projects\Ce\scripts\emu-lock.ps1" acquire Cenotes` (añade `-Dev s26` o `-Dev tableta` para esos), y `release Cenotes` al acabar.
- **Burn-in**: pantalla del móvil encendida solo mientras se prueba; al acabar `stayon false` y `input keyevent KEYCODE_SLEEP`.
- **Sesiones bajo demanda**: la sesión se cierra al terminar la tarea (deja `docs/TRASPASO.md` al día) y se abre otra nueva cuando haga falta.

## Inventario de funciones
`docs/FUNCIONES.md` (y `CeKit/docs/FUNCIONES.md` para lo común): leerlo antes de proponer o programar algo y actualizarlo en el mismo commit que añada, cambie o quite una función.
