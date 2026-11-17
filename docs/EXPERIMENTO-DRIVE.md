# Experimento: sincronización cifrada con Google Drive

**Estado: cerrado (17-11-2026). Nunca se ha publicado ni fusionado en `main`.** Vive solo en la rama `experimento-drive`
(partida de Cenotes 2.0.1). César decidió dejarlo para el futuro, «quizás nunca»; las pruebas quedan aquí para retomarlo.

Todo funciona de extremo a extremo con dos dispositivos reales (Galaxy Tab SM-X930 y Galaxy S26 Ultra SM-S948B), pero es un
esqueleto de laboratorio: no hay sincronización automática ni interfaz definitiva.

## Qué se probó

Sincronizar las notas entre dispositivos usando la carpeta oculta de la app en Drive (`appDataFolder`, permiso
`drive.appdata`, sin acceso al resto del Drive del usuario), con **cifrado en el cliente**: Google solo ve archivos opacos.

## Diseño final

### Inicio de sesión y acceso
- `play-services-auth` (`Identity.getAuthorizationClient().authorize(...)`) con un único scope:
  `https://www.googleapis.com/auth/drive.appdata`. Devuelve un *access token* (dura 1 h; se pide otro llamando de nuevo a
  `authorize`, sin pantalla si ya se concedió). Sin AppAuth ni SDK de Drive.
- API REST de Drive v3 con OkHttp (`sync/DriveApi.kt`): `list` (con `appProperties`), `put` (multipart, crear o sustituir),
  `get`, `delete`. Todo dentro de `appDataFolder`.

### Claves (`sync/DriveCrypto.kt`, `sync/DriveKeyring.kt`)
- Una **clave de datos (DEK)** aleatoria de 256 bits cifra todos los archivos con **AES-256-GCM** (IV de 12 bytes delante;
  el nombre del archivo va como AAD, así que un archivo no puede cambiarse por otro).
- `keyring.json` (único por cuenta, en claro porque solo contiene claves envueltas):
  `{v, generation, kdf: pbkdf2-sha256, iter: 600000, salt, wrapPass, wrapRecovery}`.
  - `wrapPass`: la DEK cifrada con una clave derivada de la **frase** (PBKDF2-HMAC-SHA256, 600 000 iteraciones, sal
    aleatoria de 16 bytes, AAD `keyring:pass:<generation>`).
  - `wrapRecovery`: la DEK cifrada con una **clave de recuperación** aleatoria de 256 bits (se muestra una sola vez al
    crear el keyring, en hexadecimal).
- Reglas: solo existe **un** keyring por cuenta (crear la frase mira Drive justo antes y no crea nada si ya hay uno); una
  frase equivocada solo falla al descifrar (etiqueta GCM) y no cambia nada; desbloquear acepta la frase o la clave de
  recuperación.
- **Cambiar la frase**: pide la actual (la de recuperación no vale), relee el keyring de Drive y, si `generation` ha cambiado
  mientras tanto, no sobrescribe; si coincide, reenvuelve la **misma DEK** con sal nueva, `generation + 1`, y sube. Las
  notas no se recifran. Los demás dispositivos lo detectan en la siguiente sincronización (`generation` distinto), se
  bloquean y avisan «The passphrase was changed on another device», y se desbloquean con la frase nueva.
- **PBKDF2 frente a Argon2id**: PBKDF2 viene en el JDK y evita una dependencia, pero Argon2id (resistente a GPU) es lo
  adecuado para una frase que protege datos en un servidor ajeno. Pendiente: añadir una librería (libsodium o Bouncy Castle).
  Un cambio de KDF encajaría en el campo `kdf` del keyring.

### Archivos en Drive (`sync/DriveSync.kt`)
No hay manifest aparte: el **listado** de `appDataFolder`, con la propiedad `appProperties.m` de cada archivo, hace de
manifest (una petición da el estado de todo).

| Archivo | Contenido | `appProperties.m` |
|---|---|---|
| `n-<uuid>` | nota cifrada (JSON de `NoteSerializer` + `trashedAt`), AAD = nombre | `modifiedAt` |
| `t-<uuid>` | tombstone (0 bytes): nota borrada para siempre | `deletedAt` |
| `i-<archivo de imagen>` | imagen cifrada (se sube antes que la nota que la usa) | — |
| `keyring.json` | cabecera de claves (ver arriba) | — |

Los nombres (uuid, nombres de imagen) y `m` son visibles para Google; el contenido, el título, las etiquetas y el lugar de
las notas no.

### Algoritmo de sincronización («Sync now»)
1. Lista Drive y lee las notas locales (también las de la papelera, `NoteDao.allForSync`), los tombstones locales y el
   estado de la última sincronización (`drive_base`: por nota, el `modifiedAt` que tenían ambos lados).
2. Por cada uuid, hasta 6 en paralelo:
   - Tombstone remoto: si es más reciente o igual que la última modificación local, se borra la nota aquí; si la nota local
     se editó después, **gana la edición** y la nota vuelve a Drive.
   - Tombstone local y nota remota: gana el más reciente (borra en Drive y sube el tombstone, o restaura desde Drive).
   - Solo local → sube; solo remota → baja.
   - En ambos lados con distinto `modifiedAt`: si solo cambió uno desde la última sincronización, sube o baja; si
     **cambiaron los dos** es un **conflicto**: gana la copia con `modifiedAt` mayor y la otra se conserva como una nota
     nueva «… (conflicto)» (se sube también). Nunca se pierde texto.
3. Aplicar una nota remota: inserta/actualiza por uuid, vuelve a enlazar etiquetas y lugar por nombre, y deja el
   `modifiedAt` original (`setModified`).

## Cómo probarlo
- Ajustes → «Drive lab» (solo existe en la rama). Botones: **Sign in**, **Check keyring**, **Create passphrase** /
  **Unlock** / **Change passphrase** / **Lock**, **Upload / Download / Delete test** (archivo de prueba cifrado con la DEK),
  **Sync now**, **List**, **Reset keyring (lab)** (borra TODO `appDataFolder`) y **Lab: simulate change elsewhere** (sube un
  keyring con otra frase y `generation + 1`).
- Herramientas de notas de laboratorio: **seed 5 notes** (A–E; una con imagen y otra con lista de casillas), **new note**,
  **edit / trash / delete note \<letra\>**, **show notes**. Todas las acciones escriben su resultado en el cuadro grande, en un
  aviso y en `logcat` (etiqueta `DriveLab`), útil para automatizar con `adb`.
- Hay que instalar la build **release** firmada con `cenotes-release.jks` y el paquete `com.cenotes.app`: el cliente OAuth
  solo vale para esa firma y ese paquete (una build *debug* o con otro `applicationId` no puede iniciar sesión).
- El estado del laboratorio (token, DEK desbloqueada) vive en memoria del proceso: se pierde al cerrar la app.

## Resultados (tableta y S26 reales, notas de prueba)

| Prueba | Resultado |
|---|---|
| Subir 6 notas + 1 imagen (~150 KB) | 8 peticiones, 2,9 s |
| Segundo dispositivo desbloquea con la frase y baja todo | 6 notas + imagen, 2,1 s, 8 peticiones |
| Editar en un dispositivo y sincronizar en el otro | 1 nota, 1,2 s, 2 peticiones |
| Misma nota editada en ambos | conflicto: gana la más reciente, la otra queda «(conflicto)»; ambos convergen (4,9 s, 4 peticiones) |
| Editar (antes) y borrar en el otro (después) | el tombstone gana, se borra en ambos |
| Borrar en uno y editar en el otro después | gana la edición; la nota vuelve |
| Papelera, notas nuevas en cada lado, lista de casillas | se propagan y convergen |
| Imagen | baja y se descifra una vez; no se vuelve a subir al editar el texto |
| Cambio de frase real (generación 2) | el otro dispositivo avisa, se bloquea; frase vieja falla, nueva y clave de recuperación abren |
| Carga: 30 notas | subida 7,5 s / 31 peticiones; bajada 4,7 s / 31 peticiones |
| Sincronización sin cambios | 0,5 s, 1 petición |
| Keyring: repetición distinta, frase mala, actual mala, recuperación como «actual», cambio concurrente | todo rechazado sin cambiar nada |

Sin fallos de lógica encontrados. Tamaño por nota cifrada: unos 360 bytes para una nota corta.

## Limitaciones del esqueleto
- No hay sincronización automática ni reintentos (WorkManager); hay que pulsar «Sync now».
- El token dura 1 h; el laboratorio lo pide al pulsar «Sign in» y no lo renueva solo.
- Los recordatorios y alarmas de lugar no se reprograman al bajar una nota.
- Las etiquetas viajan por nombre dentro de la nota; sus **colores** no.
- No se purgan los tombstones de Drive (en local se purgan a los 90 días: un dispositivo que lleve más tiempo sin
  sincronizar podría resucitar notas borradas).
- Nombres de archivo y `modifiedAt` visibles en Drive (fuga de metadatos, el contenido va cifrado).
- Las notas bloqueadas (`locked`) se sincronizan como las demás: su contenido ya está en claro en la base local y el bloqueo
  es solo de interfaz; el indicador `locked` viaja en el JSON.
- Sin interfaz definitiva (la frase se escribe en un campo de prueba).

## Lo que faltaría para hacerlo de verdad
1. Sincronización automática: WorkManager con restricción de red, disparo al guardar/abrir, reintentos con retroceso, y
   un estado visible («sincronizado hace…»).
2. Renovación del token (y manejo de 401 / revocación del permiso).
3. Google Cloud: pasar el consentimiento de **«En pruebas» a «En producción»** (en pruebas los permisos caducan a los 7 días
   y solo entran los usuarios de prueba); revisar si `drive.appdata` exige verificación de la app.
4. Interfaz definitiva: asistente para crear la frase (dos veces) y enseñar la clave de recuperación (con aviso de que sin
   frase ni clave los datos de Drive son irrecuperables), desbloquear, cambiar frase y desconectar; un solo punto de entrada
   en Ajustes.
5. **Argon2id** en lugar de PBKDF2 (dependencia nueva).
6. Colores de etiquetas, reprogramar recordatorios y lugares tras bajar notas, purga de tombstones en Drive, borrado
   total de la nube desde la app.
7. Pruebas automáticas (la criptografía y el algoritmo de sincronización se pueden probar en JVM con un Drive falso) y
   decidir el comportamiento con muchas notas (paginación ya implementada en `list`) y con imágenes grandes.
8. Evaluar el límite de cuota y de peticiones por segundo de la API de Drive con cuentas con miles de notas.

## Proyecto de Google Cloud (sin secretos)
- Proyecto **Cenotes**, id `cenotes-515011`; **Drive API** activada.
- Pantalla de consentimiento en **modo prueba**, con la cuenta de César como usuario de prueba (los permisos caducan a los
  7 días); único permiso solicitado: `drive.appdata`.
- Cliente OAuth de tipo **Android**: `899426999969-uaka95edmlrueg43rs716jjmsspd9qu5.apps.googleusercontent.com`
  (paquete `com.cenotes.app`, huella SHA-1 de `cenotes-release.jks`:
  `F7:B3:61:D2:4A:27:40:3D:61:06:64:B7:08:8D:1F:BB:45:5A:CC:58`). Un cliente Android no lleva secreto.
- La clave de firma (`cenotes-release.jks` y `keystore.properties`) está fuera de git (copia en Drive, `Ce Apps/Común`).
- Las compilaciones *debug* llevan otra firma y no pueden iniciar sesión con ese cliente (haría falta un segundo cliente
  con el SHA-1 de debug).

## Código
Todo en la rama `experimento-drive`: `sync/DriveCrypto.kt`, `sync/DriveKeyring.kt`, `sync/DriveApi.kt`, `sync/DriveSync.kt`,
`ui/DriveLabPage.kt`, `ui/DriveLabTools.kt`, `NoteDao.allForSync()`, dependencia `play-services-auth` y la ruta «Drive lab» en
`SettingsScreen.kt`.
