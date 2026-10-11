# CLAUDE.md

Guía para Claude Code en este repo. Leela completa antes de proponer cambios.

## Qué es

**wifipad**: usar un celular Android como gamepad de la PC por Wi-Fi. Todo en **Kotlin**, en un solo build de Gradle:

- `:protocol`: módulo Kotlin/JVM puro con el estado del control y el encode/decode de paquetes. Lo usan los otros dos módulos.
- `:app`: app Android cliente. Dibuja botones, sticks y gatillos, y manda el estado por UDP.
- `:server`: app Kotlin/JVM para **Linux**. Recibe los paquetes y crea un gamepad virtual con `uinput` (vía **libevdev + JNA**) que los juegos ven como un control real.
- `docs/protocol.md`: especificación del protocolo. Es la fuente de verdad.

Fuera de alcance por ahora (no agregar sin que lo pida): Bluetooth/HID, soporte para Windows, iOS, Kotlin Multiplatform.

## Cómo trabajamos

- Claude Code escribe el código. Enzo revisa y hace ajustes chicos a mano.
- **Antes de editar un archivo, releelo**: puede tener cambios manuales de Enzo. Respetalos; si te parece que alguno está mal, decilo en vez de pisarlo.
- Antes de cambios grandes (un módulo nuevo o más de ~3 archivos), mostrá un plan corto y esperá el OK.
- Trabajá en pasos chicos y revisables. Después de cada paso, corré los tests y el build del módulo afectado.
- Al terminar, resumí qué cambió y explicá brevemente cualquier decisión no obvia (sobre todo en el interop nativo y el multitouch), para que la revisión sea fácil.
- No hagas `git commit` ni `git push`, ni abras PRs, sin que te lo pida. Cuando te lo pida, usá Conventional Commits.
- No agregues dependencias sin preguntar. Las aprobadas: JNA (`:server`), kotlinx-coroutines (`:app`), kotlin-test/JUnit y, en el build, el plugin `foojay-resolver-convention` (baja los JDK de las toolchains).

## Git y GitHub

- Repo: `github.com/EGCGAP04/wifipad` (público, MIT). Usar `gh` para todo lo de GitHub. Si `gh` tiene más de una cuenta logueada, la activa tiene que ser `EGCGAP04` (`gh auth switch`).
- `main` está protegida con un ruleset: **no se puede pushear directo**, ni hacer force-push, ni borrarla. Todo entra por PR (sin aprobaciones requeridas).
- **Commits firmados en todas las ramas** (ruleset `signed-commits`): GitHub rechaza el push si hay un commit sin firmar. La firma ya está configurada en el `~/.gitconfig` de Enzo (SSH, `commit.gpgsign true`). Nunca usar `--no-gpg-sign`; si un commit sale sin firmar, rehacerlo firmado antes de pushear.
- **CI** (`.github/workflows/ci.yml`): el job `Build and test` corre `./gradlew build` en cada PR y en `main`, y es check obligatorio del ruleset de `main`. Si se cambia el `name:` del job, actualizar el ruleset. Los módulos nuevos entran solos al CI por estar en `build`.
- **CodeQL** (`.github/workflows/codeql.yml`): configuración avanzada, porque el default setup no soporta Kotlin (usa `build-mode: none`). Analiza `java-kotlin` compilando con Gradle y `actions`. El default setup tiene que quedar desactivado, porque si no, GitHub rechaza los resultados del workflow.
- Las actions de los workflows van fijadas por SHA completo con la versión en un comentario (`@<sha> # v7.0.1`). Dependabot las actualiza.
- Dependabot abre PRs de actualización (`build(deps): ...`, `ci(deps): ...`, con label `dependencies`). Los revisa y mergea Enzo; no tocarlos salvo que lo pida. Que Dependabot actualice versiones no cuenta como "agregar dependencias".
- Flujo: rama desde `main` actualizada → commits → `git push -u origin <rama>` → `gh pr create` → squash merge. La rama remota se borra sola al mergear; después, `git switch main && git pull` y borrar la rama local.
- Nombre de rama: `<tipo>/<descripcion-corta>` en inglés, por ejemplo `feat/protocol-encoder` o `fix/server-timeout`.
- Solo se permite **squash merge**: el **título del PR** pasa a ser el mensaje del commit en `main`, así que tiene que seguir Conventional Commits. Tipos: `feat`, `fix`, `docs`, `test`, `refactor`, `build`, `ci`, `chore`. Scopes: `protocol`, `server`, `app`, `docs`. Por ejemplo, `feat(protocol): add packet encoder and decoder`.
- El cuerpo del PR queda como cuerpo del commit: qué cambió, por qué y cómo se verificó (tests y build corridos).
- Labels por módulo: `protocol`, `server`, `app` y `documentation`. Ponerlas al crear el PR (`gh pr create --label ...`).
- Un PR por paso revisable. Los commits dentro de la rama pueden ser chicos; lo que importa es el título del PR.
- **Merge**: Claude mergea solo si Enzo lo pide explícitamente para ese PR (por ejemplo, "mergeá el #9"; puede ser desde el celular). Antes, revisar que el PR esté `MERGEABLE` y que pasen los checks del CI. Usar `gh pr merge <N> --squash --match-head-commit <SHA completo>`, para no mergear commits que nadie revisó. Después, actualizar `main` local y borrar la rama.
- Milestones: una por hito del roadmap (`M0: Setup inicial` … `M8: Windows`). Cada issue y PR va en la milestone del hito al que aporta. Las milestones las cierra Enzo.
- Proyecto: [wifipad](https://github.com/users/EGCGAP04/projects/5) (número 5, dueño `EGCGAP04`). Cada issue y PR va al proyecto. Al empezar algo, pasarlo a `In Progress`; al cerrar un issue o mergear un PR pasa a `Done` solo.
- Todos los issues y PRs se **asignan a Enzo** (`EGCGAP04`). Al crear, asignar también label, milestone y proyecto (lo mismo con `gh issue create`, y con `gh pr edit`/`gh issue edit` para cambiarlos después):
  ```bash
  gh pr create --assignee @me --label protocol --milestone "M1: Protocolo" --project wifipad
  ```
- Lo que se agrega al proyecto pasa a `Todo` solo, aunque tarda unos segundos. Para cambiar el estado:
  ```bash
  gh project item-edit 5 --owner EGCGAP04 --url <url del issue o PR> --field Status --value "In Progress"
  ```
- Un PR que resuelve un issue lleva `Closes #N` en el cuerpo. Las tareas que salen de un issue grande se crean como sub-issues de ese issue (`gh api repos/EGCGAP04/wifipad/issues/<padre>/sub_issues -F sub_issue_id=<id del hijo>`, donde el id es el `.id` del issue, no el número).

## Protocolo

@docs/protocol.md

Reglas:
- Cualquier cambio al formato del paquete se hace **primero** en `docs/protocol.md`, después en `:protocol`, y sube `version`.
- Todo el encode/decode vive en `:protocol`. `:app` y `:server` nunca arman ni leen bytes por su cuenta.
- El vector de prueba de `docs/protocol.md` es un test obligatorio de `:protocol`.
- El cliente manda el **estado completo** a frecuencia fija, nunca eventos sueltos.

## Entorno

- PC: Linux Mint (base Ubuntu, `apt`). JDK 21 (el de Android Studio sirve).
- Android: Android Studio, Gradle con Kotlin DSL y version catalog (`gradle/libs.versions.toml`), `minSdk 26`.
- Las versiones de dependencias y plugins van **siempre** en el version catalog. Única excepción: los plugins del bloque `plugins {}` de `settings.gradle.kts` (foojay), porque ahí el catálogo todavía no está disponible.
- Las toolchains (`jvmToolchain(17)` en `:protocol`) las resuelve Gradle con foojay: alcanza con tener un JDK 21 para correr Gradle.

## Comandos

```bash
./gradlew :protocol:test                    # tests del protocolo
./gradlew :server:test
./gradlew :server:run --args="--port 7654"  # correr el servidor
./gradlew :server:runFakeClient             # cliente de prueba sin celular (manda un patrón animado)
./gradlew :server:installDist               # binario en server/build/install/server/bin/
./gradlew :app:assembleDebug
./gradlew :app:installDebug                 # celular por USB o depuración inalámbrica
./gradlew :app:testDebugUnitTest
./gradlew :app:lint
```

Verificar el gamepad virtual en la PC: `evtest` (eventos crudos) y `jstest-gtk` (ejes y botones).

## Arquitectura

### `:protocol`
- `GamepadState`: data class inmutable con el estado completo, todo en `Int`. Valida los rangos en `init` (tira `IllegalArgumentException`): quien lo construye tiene que limitar los valores antes. Constantes de bits de botones (`GamepadState.A`, `DPAD_UP`, ...), `isPressed(button)` y `GamepadState.NEUTRAL`.
- `Protocol`: `encode(state, seq: UInt): ByteArray` y `decode(bytes, length = bytes.size): Packet?` con `ByteBuffer` en `LITTLE_ENDIAN`. `decode` devuelve `null` si el paquete es inválido; `length` permite pasarle el buffer de recepción del servidor sin copiarlo. `Packet(seq: UInt, state)`.
- `Protocol.isNewer(seq, last)`: comparación con aritmética de números de serie.
- Sin dependencias, sin I/O, sin APIs que no existan en Android API 26. Compila con `jvmToolchain(17)`.

### `:server`
- `Evdev.kt`: bindings JNA mínimas de libevdev (solo las funciones que se usan) e `InputAbsinfo`. Cargar con el nombre `"evdev"` y, si falla, con la ruta `libevdev.so.2`, para no exigir el paquete `-dev`. `InputCodes.kt`: constantes de `<linux/input-event-codes.h>` con los nombres de C.
- `GamepadEvents.kt`: el mapeo de `docs/protocol.md` (`BUTTONS`, `AXES`, D-pad a `ABS_HAT0X/Y`) y `forEachChange(old, new, emit)`, que calcula los eventos que cambiaron. Es puro e `inline` (no reserva memoria en el loop) y es lo que se testea, porque el CI no tiene `/dev/uinput`.
- `VirtualGamepad`: `create()` arma el dispositivo con `AXES` y `BUTTONS`; `update(state)` emite lo que cambió, seguido de `SYN_REPORT`. Implementa `AutoCloseable` y destruye el dispositivo al cerrar (también en Ctrl+C, con un shutdown hook). Si falta permiso sobre `/dev/uinput`, el error apunta a la regla de udev.
- `Main.kt`: loop UDP con `DatagramSocket` y `soTimeout` para chequear el timeout de 250 ms, enganche de un cliente, CLI mínima parseada a mano.
- `FakeClient.kt`: segundo `main` que manda paquetes a 60 Hz con un patrón (stick girando, botones alternando). Expuesto como tarea `runFakeClient`.
- Reutilizar buffers en el loop. No hace falta optimizar más.

### `:app`
- `GamepadView`: `View` custom que dibuja en `Canvas` y maneja multitouch por **pointer ID**: cada dedo "captura" el control donde empezó el toque hasta que lo levanta (`ACTION_POINTER_DOWN/UP`, `ACTION_MOVE` recorriendo todos los pointers, `ACTION_CANCEL` suelta todo).
- El estado se publica en un `StateFlow<GamepadState>`.
- `UdpSender`: corrutina en `Dispatchers.IO` que cada ~16 ms toma el último estado, lo codifica con `:protocol` y lo manda con `DatagramSocket`. No encola estados.
- Pantalla: landscape, fullscreen inmersivo, `FLAG_KEEP_SCREEN_ON`, vibración corta al apretar.
- Al salir de la app o ir a segundo plano, mandar un estado neutro y parar el envío.

## Convenciones

- Código, identificadores y commits en inglés. Documentación en español.
- Paquete base: `io.github.egcgap04.wifipad`, con un subpaquete por módulo (`.protocol`, `.server`, `.app`). El `applicationId` de Android es `io.github.egcgap04.wifipad.app`.
- Estilo oficial de Kotlin. Nada de `!!` salvo que esté justificado en un comentario.
- Cambios enfocados; no mezclar `:app` y `:server` en el mismo PR salvo cambios de protocolo.

## Gotchas conocidos

- **Permisos de `/dev/uinput`**: por defecto es solo root. Instalar `server/udev/70-wifipad-uinput.rules` (ver README). No usar `sudo` como solución. El número del archivo tiene que ser menor a 73: si no, la etiqueta `uaccess` llega tarde y el permiso no se aplica.
- **Structs nativos**: con libevdev no hace falta mapear `input_event` ni `uinput_setup`. Lo único que se pasa por JNA es `input_absinfo` (6 × `int`) para los ejes.
- **Eje Y**: arriba es **negativo** (convención de Linux). Coincide con las coordenadas de pantalla de Android, así que no hay que invertir nada.
- **`seq` da la vuelta** a los 2³²: usar `Protocol.isNewer`, nunca `>`. En Kotlin, `seq` es `UInt`.
- **`ByteBuffer` en `:protocol`**: usar solo los accesos absolutos (`getInt(index)`, `put(index, b)`). Los métodos de `Buffer` (`position`, `flip`, `limit`...) compilados con JDK 9+ apuntan a sobrecargas que las versiones viejas de Android pueden no tener.
- **Timeout**: 250 ms sin paquetes válidos → soltar todo y centrar ejes. Nunca dejar un botón trabado.
- **Emulador de Android**: la PC host es `10.0.2.2`, no `127.0.0.1`.
- **Firewall**: si `ufw` está activo, `sudo ufw allow 7654/udp`.
- **Reconocimiento en juegos**: el servidor imita un control de Xbox 360 (IDs y códigos de `xpad`) para que SDL/Steam lo mapeen solo.

## Roadmap

- [x] M0: proyecto Android Studio, repo, protocolo y documentación.
- [x] M1: módulo `:protocol` con tests (incluye el vector de prueba). CI con GitHub Actions como check obligatorio de `main` y CodeQL para Kotlin y Actions.
- [ ] M2: `:server` funcionando con `runFakeClient`. Verificado con `evtest` y `jstest-gtk`.
- [ ] M3: `:app` MVP: campo IP/puerto, stick izquierdo, A/B/X/Y y Start, envío a 60 Hz.
- [ ] M4: layout completo (dos sticks, gatillos, D-pad, bumpers), vibración, pantalla siempre encendida.
- [ ] M5: descubrimiento automático de la PC en la red local.
- [ ] M6: layouts configurables.
- [ ] M7: giroscopio.
- [ ] M8: evaluar soporte para Windows (ViGEmClient vía JNA).

Al terminar un hito, marcarlo acá (en el último PR del hito) y actualizar el README si cambió cómo se usa. Enzo cierra la milestone en GitHub.
