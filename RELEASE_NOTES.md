# MXQ Inicio v1.0.0 — vista previa

Primera versión del launcher que ya está funcionando en la MXQ RK3229 del proyecto.

- Home con hora, clima de Ambato y recientes para SmartTube, YouTube y Kodi.
- Navegación D-Pad, cuadrícula de apps, iconos en caché y consultas en segundo plano.
- Código Java y scripts PowerShell de diagnóstico, backup, captura, limpieza y restauración.
- Capturas reales del antes y después en el README.

## Descargas

- **MXQ-Inicio-v1.0.0.apk**: launcher propio; paquete `ec.mxq.inicio`, versionName `1.0`, versionCode `1`. Requiere Android 7.1/API 25 o superior. Probado solo en esta MXQ ARMv7.
- **SHA256SUMS.txt**: checksum para verificar la descarga.
- **INSTALACION.md**: conexión ADB, backup, instalación, prueba, activación de Home y recuperación del launcher original.
- **Source code**: fuentes y scripts correspondientes a este tag.

[Guía de instalación](https://github.com/xJhony911/mxq-tvbox-setup/blob/v1.0.0/INSTALACION.md) · [Cambios y validación](https://github.com/xJhony911/mxq-tvbox-setup/blob/v1.0.0/CAMBIOS.md)

Es una compilación de desarrollo local, no una versión de tienda. La clave de firma permanece privada. No incluye APK de SmartTube, YouTube, Kodi o Xuper ni los backups del dispositivo.

Recientes reabre aplicaciones; la posición de reproducción depende de cada app. No se ha demostrado una mejora uniforme del tiempo de arranque ni 60 fps sostenidos. Conserva habilitado el launcher de fábrica hasta comprobar el nuevo Home.
