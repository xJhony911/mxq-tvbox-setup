# Diagnóstico de la MXQ-4K 5G

Fecha de consulta: 2026-10-31. Fase 1, sin modificaciones persistentes al dispositivo. Se pulsó Home para fotografiar el menú de fábrica.

## Android y CPU

| Consulta | Resultado |
| --- | --- |
| `ro.build.version.release` | `10.1` |
| `ro.build.version.sdk` | `25` |
| `ro.build.id` | `NHG47K` |
| `ro.build.version.security_patch` | `2017-04-05` |
| `ro.product.cpu.abi` | `armeabi-v7a` |
| `ro.product.cpu.abilist` | `armeabi-v7a,armeabi` |
| `ro.board.platform` | `rk322x` |
| `ro.hardware` | `rk30board` |
| `/proc/cpuinfo`, Hardware | `Rockchip RK3229` |
| `/proc/cpuinfo`, procesadores | `0`, `1`, `2`, `3` |
| `ro.build.characteristics` | `box` |
| Kernel | `3.10.104`, `armv7l`, compilado el 29 de abril de 2020 |

**Interpretación:** el SDK 25 corresponde a Android 7.1, mientras Android 10 corresponde a SDK 29 según la [documentación oficial](https://developer.android.com/tools/releases/platforms). La cadena `10.1` contradice la base de API que declara el propio sistema. Para seleccionar APK se debe comprobar API mínima y ABI de 32 bits. El valor `box` no demuestra certificación Android TV o Google TV; esa certificación no se ha verificado.

## RAM: lecturas contradictorias

```text
free -m
                 total  used  free
Mem:               995   800   194
Swap:                0     0     0

/proc/meminfo
MemTotal: 4194304 kB

/proc/zoneinfo
Normal:  present 130816 páginas
HighMem: present 131072 páginas
```

`free` observa aproximadamente 1 GiB y los contadores de zonas son consistentes con ese orden de magnitud suponiendo páginas de 4 KiB. `getconf PAGESIZE` no está disponible en esta ROM, por lo que ese tamaño de página no se verificó con ese comando. La declaración de 4 GiB en `meminfo` no concuerda con las demás lecturas; hay indicios de valores de RAM alterados por el firmware. No se ha realizado una prueba física ni un test de estrés. No se deben aplicar ajustes suponiendo 4 GiB.

## Almacenamiento observado

```text
Filesystem                 Size  Used Avail Use% Mounted on
/dev/block/rknand_system    2.4G  1.3G  1.1G  55% /system
/dev/block/rknand_cache     122M  380K  119M   1% /cache
/dev/block/rknand_metadata   12M   40K   11M   1% /metadata
/dev/block/rknand_userdata  3.8G  2.8G  1.0G  73% /data
/dev/fuse                  3.8G  2.8G  1.0G  73% /storage/emulated
```

`/data` y `/storage/emulated` representan el mismo espacio de usuario; no deben sumarse. Las particiones expuestas no respaldan la etiqueta comercial de 32 GB. No se ha comprobado la capacidad física completa ni la integridad del chip mediante escritura. El espacio libre cambiará con el uso.

## Interfaz

- Al comenzar estaba en foco `com.android.tv.settings/.MainSettings`: es Ajustes, no el launcher.
- La resolución de la actividad HOME devuelve `com.droidlogic.mboxlauncher/.Launcher` como predeterminada.
- Se capturó el menú de fábrica en `before.png`, con resolución 1280 × 720. Esto no determina la resolución máxima de salida HDMI.
- ADB detectó un único destino conectado y autorizado.

## Estado de cambios

Este documento conserva las observaciones de la **Fase 1**, anteriores a las modificaciones. Posteriormente se aplicaron la limpieza ligera, los ajustes y el nuevo launcher descritos en [CAMBIOS.md](CAMBIOS.md). El launcher original permanece instalado y habilitado. La captura actual se encuentra en `after.png`.
