# MXQ TV Box: diagnóstico y personalización por ADB

Proyecto en PowerShell para documentar y personalizar una MXQ-4K 5G de forma reversible. **Estado: Fase 1 completada; todavía no se ha instalado, deshabilitado ni eliminado ninguna aplicación.**

## Antes

Captura real del launcher de fábrica, tomada por ADB el 31 de octubre de 2026:

![Menú de fábrica antes de los cambios](before.png)

## Después

Pendiente. Se añadirá una captura real cuando el nuevo launcher esté instalado, probado con el mando y configurado como predeterminado. La imagen anterior no representa una optimización terminada.

## Hallazgos

| Dato | Resultado observado |
| --- | --- |
| Android declarado por la ROM | `10.1` |
| SDK reportado | `25`, correspondiente a Android 7.1 |
| Procesador | Rockchip RK3229, 4 núcleos ARMv7 |
| ABI de aplicaciones | `armeabi-v7a` / `armeabi`, 32 bits |
| RAM | `free -m`: 995 MiB; `/proc/meminfo`: 4096 MiB, lectura contradictoria |
| Almacenamiento de usuario | `/data`: 3.8 GiB totales, aproximadamente 1.0 GiB libres |
| Características de la ROM | `box` |
| Launcher predeterminado | `com.droidlogic.mboxlauncher/.Launcher` |

El SDK, el identificador de compilación y el parche de seguridad son consistentes con una base antigua de Android 7.1. La memoria observada por `free` y las zonas de memoria sugieren aproximadamente 1 GiB de RAM. Estos datos proceden del firmware; no constituyen una medición física independiente del hardware. Ver [el diagnóstico completo](DIAGNOSTICO.md).

## Uso en Windows PowerShell

Instala las [Platform Tools oficiales](https://developer.android.com/tools/releases/platform-tools) y habilita la depuración en tu propia TV Box. Sustituye el destino de ejemplo por la dirección que corresponda:

```powershell
adb connect DIRECCION_DE_TU_TV:5555
.\Get-TvBoxDiagnostics.ps1 -Serial 'DIRECCION_DE_TU_TV:5555'
.\Capture-TvBox.ps1 -Serial 'DIRECCION_DE_TU_TV:5555'
```

Los resultados nuevos se guardan en `local/`, excluido de Git. El script de diagnóstico solo consulta datos. El script de captura fotografía la pantalla actual; no pulsa Home ni cambia ajustes. Revisa cada captura antes de publicarla. La captura PNG usa un flujo binario para evitar alteraciones por la redirección de Windows PowerShell 5.1.

## Fases pendientes

1. Backup de paquetes y propiedades, y script de restauración.
2. Evaluación individual del bloatware y cambios reversibles en lotes pequeños.
3. Verificación de compatibilidad e instalación de aplicaciones oficiales.
4. Elección y prueba del launcher, con hora, clima y recientes.
5. Captura del después y verificación del botón Home.
6. Decisión del propietario sobre apagar ADB por red.

Se conservarán Google Play Services, Play Store, Kodi y Xuper TV. El launcher de fábrica permanecerá habilitado hasta que exista un reemplazo funcional y probado. Las apps recientes permiten volver a una aplicación; no garantizan retomar un video en su posición exacta. Eso depende de cada app.

## Referencias

- [ADB: documentación oficial](https://developer.android.com/tools/adb).
- [Correspondencia entre versión de Android y API](https://developer.android.com/tools/releases/platforms).
- [CleanYourTV](https://github.com/Gathin23/cleanyourtv), inspiración para el flujo reversible. Este repositorio contiene scripts propios; no es un fork ni una distribución del código de CleanYourTV.

No se publican las propiedades completas, direcciones de red, números de serie, credenciales ADB ni APK de terceros.
