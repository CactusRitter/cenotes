# Traspaso de Cenotes

Estado vivo de la app para retomar el trabajo tras un `/clear`. Mantenerlo al día al cerrar cada bloque.
Última actualización: 21-11-2026.

## Estado
- Versión publicada: **2.2.0** (versionCode 43), release `v2.2.0` en GitHub (`releases/latest`). Antes, el 18-11: 2.1.0 (Cristal V2), 2.1.1 (arreglo grave: el cajón no recibía `LocalHazeState` y salía transparente) y 2.2.0 (Ajustes → Diagnóstico).
- `main` = `origin/main`, limpio (último commit `b6a50f7`). «Sin publicar»: migración a **CeKit** hecha por otra sesión (submódulo `CeKit/`, módulo `:kit`, paquete `com.ce.kit`): glass/Skin/Theme, AppLog/CrashLog/Updates/ReleaseNotes, `GlassDialog` en todos los diálogos, Apariencia y componentes de Ajustes, aviso de actualización, Novedades y Diagnóstico. Lo común se toca en CeKit, no aquí.
- APKs en `G:\Mi unidad\Ce Apps\Cenotes\` (no borrar ninguno).
- Firma: clave propia `keystore/cenotes-release.jks` + `keystore/keystore.properties` (fuera de git; copia en `G:\Mi unidad\Ce Apps\Común\`). Desde 2.0.0, misma firma.
- Copia de seguridad: un solo botón «Crear copia» (zip con notas, imágenes y/o ajustes) + Importar.

## En curso
- Nada. Por orden de Ce Líder (18-11): no abrir trabajo nuevo hasta el reinicio del límite semanal ni sin encargo suyo o de César.

## Pendiente
- Probar en tableta/S26/OPPO las 2.1.x–2.2.0 y lo de CeKit (sin probar: instalar el release sobre la versión anterior, tableta).
- Ce debe devolver tableta (R52Y806MWSP) y S26 (RFGL107KERL) a la versión publicada: llevaban la build del experimento Drive.
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
- `emulator-5554` compartido: antes de usarlo `powershell -NoProfile -ExecutionPolicy Bypass -File "D:sarDocumentsClaude ProjectsCescriptsemu-lock.ps1" acquire Cenotes` (0 = mío, 1 = ocupado) y `release Cenotes` al acabar; prioridad Líder/CeKit > Cemail > Celander > Cenotes. Puede haber un móvil físico conectado: siempre `-s emulator-5554`. Si se cuelga: matar `qemu-system` y arrancarlo con `Start-Process emulator.exe -avd Pixel_API_36 -no-snapshot-save -no-audio -no-boot-anim -no-window -gpu swiftshader_indirect` (`scripts/emulator.ps1` falla con más de un dispositivo conectado, no usa `ANDROID_SERIAL`).
- No cambiar `wm size`/`wm density` del emulador: lo colgó.
- Ajustes de prueba: escribir `shared_prefs/settings.xml` con `run-as` (claves `skin`, `theme_mode`, `transparency`) en la build debug.
- Cuando termine un bloque: mensaje de máx. 6 líneas a «Ce Líder» (norma de César).
