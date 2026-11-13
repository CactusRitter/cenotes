# Cenotes

App de notas para Android. Forma parte de las apps "Ce" de César Caballero y su amigo Jonathan, junto con **Celander** (calendario) y **Cemail** (correo): las tres comparten el mismo diseño y deben parecer una sola familia. No se publica en Play Store: es para uso propio y de amigos (APK).

- Paquete / applicationId: `com.cenotes.app`
- Kotlin + Jetpack Compose (Material 3), un único módulo `:app`
- minSdk 26 · compileSdk 37 · targetSdk 36 · Java 17
- AGP 9.5 (Kotlin integrado: **no** se aplica `org.jetbrains.kotlin.android`) · Kotlin 2.4 · Room · Haze (cristal) · Coil · Glance (widgets)
- Las versiones de las dependencias están en [gradle/libs.versions.toml](gradle/libs.versions.toml).

Documentación: [docs/DESIGN.md](docs/DESIGN.md) (diseño compartido con Cemail y Celander, **leer antes de tocar la interfaz**) y [CHANGELOG.md](CHANGELOG.md).

## Comandos

```bash
./gradlew assembleDebug          # compilar APK de depuración
./gradlew installDebug           # instalar en el emulador/dispositivo conectado
./gradlew lintDebug              # lint de Android
```

En Windows, `gradlew.bat`. El emulador es `Pixel_API_36`; [scripts/emulator.ps1](scripts/emulator.ps1) lo arranca sin ventana (`start`, `shot`, `stop`).

### Publicar una versión
1. Subir `versionCode` y `versionName` en `app/build.gradle.kts`, añadir la entrada en `CHANGELOG.md` y en `res/raw*/changelog.txt` (inglés, español y alemán: es lo que muestra Ajustes → Actualizaciones).
2. Compilar y copiar el APK a `G:\Mi unidad\Notas app\Cenotes-x.y.z.apk` (nunca borrar ni sustituir los anteriores).
3. Commit y push a `main`.
4. Publicar la *release* en GitHub con el APK y notas con secciones `## en`, `## es`, `## de`: la app avisa de versiones nuevas leyendo la última *release* del repositorio (`update/Updates.kt`). Para que funcione, el repositorio tiene que ser público, como el de Cemail.

```bash
gh release create vx.y.z "G:/Mi unidad/Notas app/Cenotes-x.y.z.apk" --title "Cenotes x.y.z" --notes-file notas.md
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
