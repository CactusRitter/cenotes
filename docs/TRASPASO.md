# Traspaso de Cenotes

Estado vivo de la app para retomar el trabajo tras un `/clear`. Mantenerlo al día al cerrar cada bloque.
Última actualización: 18-11-2026.

## Estado
- Versión publicada: **2.0.1** (versionCode 40), release `v2.0.1` en GitHub (`releases/latest`, la que lee la app para avisar de actualizaciones).
- `main` = `origin/main`, limpio (último commit `657b18d`). «Sin publicar» lleva Cristal V2 y su arreglo; no publicado.
- APKs en `G:\Mi unidad\Ce Apps\Cenotes\` (no borrar ninguno).
- Firma: clave propia `keystore/cenotes-release.jks` + `keystore/keystore.properties` (fuera de git; copia en `G:\Mi unidad\Ce Apps\Común\`). Versiones ≤1.6.1 iban con la clave de depuración antigua; desde 2.0.0 con la propia (misma firma en adelante).
- Copia de seguridad: un solo botón «Crear copia» (zip con notas, imágenes y/o ajustes) + Importar; sin página aparte de ajustes.

## En curso
- Nada. Cristal V2 (subido por una sesión lateral) revisado en emulador: 6 diseños, oscuro en Cristal/Brutalista/AMOLED, transparencia 0 y 99 %. Arreglado un cierre de la app en Cristal (SIGSEGV libhwui): en `NotesListScreen` la fuente haze BACKGROUND era padre de la CONTENT y las tarjetas desenfocaban a su propio padre; ahora son hermanas. Resumen enviado a Ce Líder.

## Pendiente
- Probar Cristal V2 en tableta y S26 (lo primero: que la lista no se cierre). Sin probar: editor con imágenes/listas, etiquetas, papelera.
- Ce debe devolver tableta (R52Y806MWSP) y S26 (RFGL107KERL) a Cenotes 2.0.1: ahora llevan la build del experimento Drive.
- Sin verificar: actualización en el OPPO de 2.0.0 a 2.0.1 desde la propia app; restauración real en los dispositivos de César.

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
- `emulator-5554` compartido con Cemail; ahora encendido y libre. Si se cuelga: matar `qemu-system` y arrancarlo con `Start-Process emulator.exe -avd Pixel_API_36 -no-snapshot-save -no-audio -no-boot-anim -no-window -gpu swiftshader_indirect` (`scripts/emulator.ps1` falla con más de un dispositivo conectado, no usa `ANDROID_SERIAL`).
- No cambiar `wm size`/`wm density` del emulador: lo colgó.
- Ajustes de prueba: escribir `shared_prefs/settings.xml` con `run-as` (claves `skin`, `theme_mode`, `transparency`) en la build debug.
- Cuando termine un bloque: mensaje de máx. 6 líneas a «Ce Líder» (norma de César).
