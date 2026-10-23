# wifipad

Usá tu celular Android como gamepad de la PC por Wi-Fi.

La app de Android manda el estado del control por UDP 60 veces por segundo, y un servidor en la PC crea un gamepad virtual en Linux con `uinput`, que los juegos reconocen como un control de Xbox 360. Todo está escrito en Kotlin.

> Estado: en desarrollo. Ver el roadmap en [`CLAUDE.md`](CLAUDE.md).

## Estructura

```
protocol/   Protocolo compartido (Kotlin/JVM)
server/     Servidor para Linux (Kotlin/JVM + libevdev vía JNA)
app/        App Android
docs/       Especificación del protocolo
```

## Servidor (Linux)

### 1. Dependencias

```bash
sudo apt install libevdev2 evtest jstest-gtk
```

Necesitás un JDK 21 (sirve el que trae Android Studio).

### 2. Permisos para `/dev/uinput` (una sola vez)

```bash
sudo cp server/udev/70-wifipad-uinput.rules /etc/udev/rules.d/
echo uinput | sudo tee /etc/modules-load.d/uinput.conf
sudo modprobe uinput
sudo udevadm control --reload-rules && sudo udevadm trigger
```

Para comprobarlo, `getfacl /dev/uinput` tiene que mostrar tu usuario con `rw-`. Si no, cerrá sesión y volvé a entrar.

Si instalaste la regla con su nombre viejo (`99-wifipad-uinput.rules`), no tiene efecto: borrala de `/etc/udev/rules.d/` e instalá la nueva.

### 3. Correr

```bash
./gradlew :server:run --args="--port 7654"
```

El servidor crea un gamepad virtual llamado `wifipad` y espera paquetes. Se corta con Ctrl+C, que también borra el dispositivo. Si dejan de llegar paquetes durante 250 ms, suelta todos los botones y centra los sticks.

Si usás `ufw`: `sudo ufw allow 7654/udp`.

Para tener el servidor sin Gradle: `./gradlew :server:installDist` deja el binario en `server/build/install/server/bin/server`.

### 4. Probar sin celular

Con el servidor corriendo, en otra terminal:

```bash
./gradlew :server:runFakeClient                                      # a 127.0.0.1:7654
./gradlew :server:runFakeClient --args="--host 192.168.0.10 --port 7654"
```

Manda un patrón a 60 Hz: los sticks giran, los gatillos suben y bajan, y los botones y el D-pad se apretan de a uno. Para verlo:

```bash
evtest        # elegí "wifipad" en la lista: muestra los eventos crudos
jstest-gtk    # ejes y botones en una ventana
```

## App Android

Necesita Android 8.0 o más nuevo. Para instalarla, abrí el proyecto en Android Studio y corré el módulo `app`, o, con el celular conectado por USB o depuración inalámbrica:

```bash
./gradlew :app:installDebug
```

Con el servidor corriendo:

1. En la app, ingresá la IP local de la PC (`hostname -I`) y el puerto, y tocá **Conectar**.
2. En Android 17 o más nuevo, la app pide permiso para encontrar y conectarse a **dispositivos cercanos**: hace falta para mandar paquetes a la PC por la red local. Si lo negaste, habilitalo en los ajustes de la app.
3. Se abre el control en pantalla completa y horizontal, con la distribución de un control de Xbox. Se pueden usar varios dedos a la vez, y la pantalla no se apaga mientras el control está abierto.

| Control | Dónde está | Cómo se usa |
|---|---|---|
| Stick izquierdo | Arriba a la izquierda | Arrastrá el dedo. **Doble toque** rápido y dejá el dedo apoyado para apretar L3. |
| Stick derecho | Abajo a la derecha | Igual que el izquierdo; el doble toque aprieta R3. |
| D-pad | Abajo a la izquierda | Sigue al dedo: deslizalo para cambiar de dirección o ir a una diagonal sin levantarlo. |
| A, B, X, Y | Arriba a la derecha | Quedan apretados hasta que levantás el dedo, aunque se salga del botón. |
| LB/RB y LT/RT | Esquinas de arriba | Los gatillos van a fondo o sueltos: la pantalla no mide presión. |
| Back (⧉), Guide (⌂) y Start (≡) | Arriba en el centro | Más chicos, para no tocarlos sin querer. |

Al apretar un control, el celular vibra un instante si tenés activada la respuesta táctil en los ajustes del sistema.

La app manda el estado 60 veces por segundo mientras el control está en pantalla. Si salís de la app o la mandás a segundo plano, suelta todo y deja de mandar.

La PC y el celular tienen que estar en la misma red Wi-Fi. Para probar en el emulador de Android, la IP de la PC es `10.0.2.2`.

Las redes de invitados o públicas (de bares, facultades u oficinas) suelen aislar a los dispositivos entre sí: los dos tienen internet, pero el celular no llega a la PC y el servidor no recibe nada. Para comprobarlo, hacé ping desde la PC a la IP del celular (está en los ajustes del celular, dentro de Acerca del teléfono). Si no responde, usá otra red:

- **Hotspot del celular**: conectá la PC a la zona Wi-Fi del celular.
- **Anclaje USB**: con el celular conectado por cable, activá el anclaje USB en los ajustes de red del celular (en Samsung: Conexiones → Zona Wi-Fi y Anclaje).

En los dos casos la PC navega con los datos del celular y su IP cambia: volvé a mirarla con `hostname -I`.

## Protocolo

Ver [`docs/protocol.md`](docs/protocol.md).

## Licencia

MIT
