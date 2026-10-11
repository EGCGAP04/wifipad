# Protocolo wifipad — versión 1

## Transporte

- UDP sobre la red local. Puerto por defecto del servidor: **7654**.
- El cliente manda el **estado completo** del control a **60 Hz** (cada ~16 ms), haya cambios o no.
- Un paquete perdido no importa: el siguiente trae el estado completo.

## Formato del paquete

20 bytes, **little-endian**, sin padding (`ByteBuffer.order(ByteOrder.LITTLE_ENDIAN)`).

| Offset | Tamaño | Campo     | Tipo | Descripción                                     |
|-------:|-------:|-----------|------|-------------------------------------------------|
| 0      | 2      | `magic`   | —    | ASCII `"GP"` (`0x47 0x50`)                      |
| 2      | 1      | `version` | u8   | `1`                                             |
| 3      | 1      | `flags`   | u8   | Reservado, enviar `0`                           |
| 4      | 4      | `seq`     | u32  | Número de secuencia, +1 por paquete, da vuelta  |
| 8      | 2      | `buttons` | u16  | Máscara de bits (ver abajo)                     |
| 10     | 2      | `lx`      | i16  | Stick izquierdo X, −32768..32767 (derecha +)    |
| 12     | 2      | `ly`      | i16  | Stick izquierdo Y, −32768..32767 (**arriba −**) |
| 14     | 2      | `rx`      | i16  | Stick derecho X                                 |
| 16     | 2      | `ry`      | i16  | Stick derecho Y (**arriba −**)                  |
| 18     | 1      | `lt`      | u8   | Gatillo izquierdo, 0..255                       |
| 19     | 1      | `rt`      | u8   | Gatillo derecho, 0..255                         |

El cliente limita cada stick al círculo unitario. La zona muerta la maneja cada juego.

## Botones (`buttons`)

| Bit | Botón       | Bit | Botón        |
|----:|-------------|----:|--------------|
| 0   | A           | 8   | Guide / Home |
| 1   | B           | 9   | L3           |
| 2   | X           | 10  | R3           |
| 3   | Y           | 11  | D-pad arriba |
| 4   | LB          | 12  | D-pad abajo  |
| 5   | RB          | 13  | D-pad izq.   |
| 6   | Back/Select | 14  | D-pad der.   |
| 7   | Start       | 15  | Reservado    |

Si llegan dos direcciones opuestas del D-pad a la vez, el servidor las trata como neutro en ese eje.

## Reglas del servidor

1. Descartar en silencio paquetes con tamaño ≠ 20, `magic` incorrecto o `version` desconocida.
2. Aceptar un paquete solo si `seq` es más nuevo que el último aceptado, usando aritmética de números de serie:
   `((seq - last) mod 2^32)` tiene que estar en `1..2^31-1`.
3. Un solo cliente a la vez: el primer emisor válido queda "enganchado" hasta el timeout.
4. **Timeout**: 250 ms sin paquetes válidos → soltar todos los botones, centrar sticks, gatillos en 0 y liberar el cliente. Al reengancharse, aceptar cualquier `seq`.

## Mapeo a Linux (`uinput`)

El dispositivo virtual imita un control de Xbox 360 como lo expone el driver `xpad`, para que SDL y Steam lo reconozcan sin configuración:
`bustype = BUS_USB`, `vendor = 0x045e`, `product = 0x028e`, `version = 0x0110`.

| Protocolo      | Código evdev                          | Rango               |
|----------------|---------------------------------------|---------------------|
| A / B / X / Y  | `BTN_A` / `BTN_B` / `BTN_X` / `BTN_Y` | 0/1                 |
| LB / RB        | `BTN_TL` / `BTN_TR`                   | 0/1                 |
| Back / Start   | `BTN_SELECT` / `BTN_START`            | 0/1                 |
| Guide          | `BTN_MODE`                            | 0/1                 |
| L3 / R3        | `BTN_THUMBL` / `BTN_THUMBR`           | 0/1                 |
| `lx` / `ly`    | `ABS_X` / `ABS_Y`                     | −32768..32767       |
| `rx` / `ry`    | `ABS_RX` / `ABS_RY`                   | −32768..32767       |
| `lt` / `rt`    | `ABS_Z` / `ABS_RZ`                    | 0..255              |
| D-pad          | `ABS_HAT0X` / `ABS_HAT0Y`             | −1..1 (arriba = −1) |

## Vector de prueba

`:protocol` tiene que codificar y decodificar exactamente esto:

- `seq = 1`, botones `A + Start` (`0x0081`)
- `lx = 32767`, `ly = -32768`, `rx = 0`, `ry = 1000`, `lt = 255`, `rt = 0`

```
47 50 01 00 01 00 00 00 81 00 FF 7F 00 80 00 00 E8 03 FF 00
```

## Historial

- **v1**: versión inicial.
