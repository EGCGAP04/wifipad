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

Si usás `ufw`: `sudo ufw allow 7654/udp`.

## App Android

Abrí el proyecto en Android Studio y corré el módulo `app` en el celular. En la app, ingresá la IP local de la PC (`hostname -I`) y el puerto.

La PC y el celular tienen que estar en la misma red Wi-Fi.

## Protocolo

Ver [`docs/protocol.md`](docs/protocol.md).

## Licencia

MIT
