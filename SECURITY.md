# Política de seguridad

## Versiones soportadas

wifipad está en desarrollo y todavía no tiene releases. Solo se mantiene la rama `main`.

## Reportar una vulnerabilidad

**No abras un issue público.** Usá el reporte privado de GitHub: pestaña [Security](https://github.com/EGCGAP04/wifipad/security) → *Report a vulnerability*.

Incluí, si podés:

- Qué componente afecta (`:server`, `:app` o `:protocol`).
- Cómo reproducirlo y qué impacto tiene.

## Alcance

wifipad está pensado para usarse en una red local de confianza. El protocolo UDP **no tiene autenticación ni cifrado**: cualquier equipo de la red que llegue al puerto del servidor puede mandar paquetes válidos. Eso es una limitación conocida, no una vulnerabilidad. Igual son bienvenidos los reportes sobre, por ejemplo:

- Paquetes que hagan fallar o colgar al servidor, o que dejen botones trabados.
- Problemas con los permisos de `/dev/uinput` o con la regla de `udev`.
