# Traspaso de Cenotes

Estado vivo de la app para retomar el trabajo tras un `/clear`. Mantenerlo al día al cerrar cada bloque.
Última actualización: 22-11-2026.

## Estado
- Versión publicada: **2.3.0** (versionCode 44), release `v2.3.0` en GitHub (`releases/latest`). Antes, el 18-11: 2.1.0 (Cristal V2), 2.1.1 (arreglo grave: el cajón no recibía `LocalHazeState` y salía transparente) y 2.2.0 (Ajustes → Diagnóstico).
- `main` = `origin/main` (último commit mío `71d7f6a`). OJO: en la carpeta hay cambios SIN COMMITEAR de otra sesión (CeKit F6: `Backup.kt`, `SettingsRepository.kt`, `SettingsScreen.kt`, `SettingsSecretsTest.kt` y puntero `CeKit` pasan a `com.ce.kit.backup`/`settings`); no tocarlos ni commitearlos sin saber de quién son. «Sin publicar»: R8, tests de migración y migración a **CeKit** hecha por otra sesión (submódulo `CeKit/`, módulo `:kit`, paquete `com.ce.kit`): glass/Skin/Theme, AppLog/CrashLog/Updates/ReleaseNotes, `GlassDialog` en todos los diálogos, Apariencia y componentes de Ajustes, aviso de actualización, Novedades y Diagnóstico. Lo común se toca en CeKit, no aquí.
- APKs en `G:\Mi unidad\Ce Apps\Cenotes\` (no borrar ninguno).
- Firma: clave propia `keystore/cenotes-release.jks` + `keystore/keystore.properties` (fuera de git; copia en `G:\Mi unidad\Ce Apps\Común\`). Desde 2.0.0, misma firma.
- Copia de seguridad: un solo botón «Crear copia» (zip con notas, imágenes y/o ajustes) + Importar.

## En curso
- Nada mío en marcha. Último encargo de Ce Líder cerrado (R8 probado en emulador y S26, resumen enviado).
- Hecho (21-11, encargo de Ce Líder): tests de migración con datos reales (`app/src/test/.../MigrationTest.kt`, `gradlew testDebugUnitTest`). Toda migración nueva (9→10…) se añade a `NotesDatabase.AllMigrations` y el test la cubre solo; el esquema 10 lo exporta Room a `app/schemas/`.
- R8: fusionado en main (21-11, aprobado por César vía Ce Líder), sin publicar. Probado con la release R8 en emulador (ver CHANGELOG). En el S26 (22-11): texto de foto OK y lugar registrado OK; falta solo ver el aviso al llegar de verdad. En el S26 queda una nota de prueba en la Papelera de Cenotes (se borra sola a los 7 días). Para probar una release en el emulador sin borrar datos: `assembleRelease` con `-Pandroid.injected.signing.*` apuntando a `~/.android/debug.keystore`.

## Pendiente
- Probar en tableta/S26/OPPO las 2.1.x–2.2.0 y lo de CeKit (sin probar: instalar el release sobre la versión anterior, tableta).
- El S26 lleva la release R8 (2.3.0 + lo sin publicar, firma real); la tableta no se ha tocado.
- Antes de publicar con R8: ver que el aviso de lugar salta al llegar de verdad (en el S26 el registro funciona).
- Sin verificar: actualización desde la propia app en el OPPO; restauración real en los dispositivos de César.

## Decisiones de César (no están en el código)
- Publicar solo cuando César o la sesión Ce lo digan; entretanto, commit + push y nota en «Sin publicar». Un fallo grave se publica al momento.
- Sin capas de compatibilidad hacia atrás, pero nunca perder datos de César.
- Todo texto visible en los tres idiomas a la vez (en, es, de con «du»). Responder siempre en español.
- Diseño compartido con Cemail/Celander: avisar a Ce antes de tocar Skin/Glass/Theme/Motion/GlassProgress/Updates.
- Dispositivos: emulador `emulator-5554` (compartido con Cemail), tableta y S26 (solo perfil principal) autorizados por César; el OPPO y otras apps, no. Siempre `ANDROID_SERIAL` en adb.
- Credenciales y logins de Google los hace César.

## Experimento Drive (cerrado, no fusionado)
Sincronización cifrada vía `appDataFolder` de Drive, en la rama `experimento-drive` (commit d056a0e). César lo deja para el futuro, «quizás nunca». Detalle completo en `docs/EXPERIMENTO-DRIVE.md` de esa rama. Solo se retoma si Ce lo ordena.

## Emulador (notas prácticas)
- `emulator-5554` compartido: antes de usarlo `powershell -NoProfile -ExecutionPolicy Bypass -File "D:\cesar\Documents\Claude Projects\Ce\scripts\emu-lock.ps1" acquire Cenotes` (0 = mío, 1 = ocupado) y `release Cenotes` al acabar; prioridad Líder/CeKit > Cemail > Celander > Cenotes. Puede haber un móvil físico conectado: siempre `-s emulator-5554`. Si se cuelga: matar `qemu-system` y arrancarlo con `Start-Process emulator.exe -avd Pixel_API_36 -no-snapshot-save -no-audio -no-boot-anim -no-window -gpu swiftshader_indirect` (`scripts/emulator.ps1` falla con más de un dispositivo conectado, no usa `ANDROID_SERIAL`).
- No cambiar `wm size`/`wm density` del emulador: lo colgó.
- Ajustes de prueba: escribir `shared_prefs/settings.xml` con `run-as` (claves `skin`, `theme_mode`, `transparency`) en la build debug.
- Cuando termine un bloque: mensaje de máx. 6 líneas a «Ce Líder» (norma de César).
