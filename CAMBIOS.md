# Cambios aplicados — 2026-10-31

## MXQ Inicio 1.0 / release v1.0.0

- Launcher nativo Java para API 25, sin bibliotecas externas ni servicios persistentes.
- Hora y fecha de Ecuador; clima del centro de Ambato mediante Open-Meteo, con caché y consulta cada 15 minutos mientras el menú está visible.
- Recientes para SmartTube, YouTube y Kodi mediante UsageStatsManager y registro local de aperturas. No se envía el historial al servicio meteorológico.
- Cuadrícula de aplicaciones TV y móviles, navegación D-Pad/OK, acceso a Ajustes y al menú original de esta MXQ.
- Iconos en caché, reutilización de la cuadrícula, consultas fuera del hilo de pantalla y foco animado sin sombras costosas.
- Home predeterminado cambiado a ec.mxq.inicio/.MainActivity; launcher de fábrica conservado y habilitado. No se necesitó accesibilidad.

## Limpieza y aplicaciones

- Inventario de paquetes, propiedades y ajustes originales guardados de forma privada antes de modificar el dispositivo.
- Deshabilitados con pm disable-user --user 0: com.cghs.stresstest y com.factorytools.factorystability. Son herramientas de pruebas de esta ROM; no se identificaron otros paquetes como malware sin evidencia.
- Animaciones en 0.5x; el valor original era 0, por lo que esto añade transiciones breves en lugar de acelerar animaciones existentes.
- SmartTube estable 32.68, ABI armeabi-v7a, obtenido del repositorio oficial. El SHA-256 coincidió con el publicado en la API de GitHub.
- Conservados Google Play Services, Play Store, YouTube TV 2.05.03 y Kodi.
- Compilación ART speed verificada para SmartTube, YouTube TV y MXQ Inicio. Una limpieza puntual de procesos en caché; sin límites permanentes de procesos ni cambios del kernel.
- Xuper pasó de 4.99.20 a 5.2.0 desde un APK aportado por el propietario, con la misma firma y mediante install -r. Se guardó el APK anterior. Después se pausó ese trabajo a petición del propietario. El APK no se redistribuye.

## Validación y límites

- Compilación y firma del launcher verificadas con herramientas de Android.
- Scripts PowerShell comprobados mediante el parser; diagnóstico, captura y limpieza ejecutados en la caja.
- Home resolvió al nuevo launcher y se verificó la ventana en foco. D-Pad y OK abrieron aplicaciones.
- SmartTube y YouTube mostraron sus pantallas de contenido; Kodi abrió. No se observaron errores AndroidRuntime en la comprobación final.
- Capturas reales: before.png, after-cleanup.png y after.png. No son renders ni propuestas visuales.
- Los tiempos de inicio ADB variaron. Las mediciones no demuestran una reducción uniforme del arranque ni 60 fps sostenidos. La carga de contenido depende también de la red y de cada aplicación.
- Solo se validó en esta MXQ RK3229, ARM de 32 bits, SDK 25 y aproximadamente 1 GiB de RAM. El firmware reporta datos comerciales contradictorios; véase DIAGNOSTICO.md.

## Restauración

restore_tvbox.ps1 usa el registro privado local/baseline/state.json para reactivar los paquetes cambiados, restaurar ajustes, revocar el acceso de uso registrado y recuperar el Home original. Puede reactivar paquetes concretos mediante -Packages. No desinstala aplicaciones nuevas, no revierte automáticamente la versión de Xuper y no restaura sus datos de cuenta. El inventario no es un backup completo del dispositivo.

La clave de firma de desarrollo, los backups, las direcciones de red y los APK de terceros no forman parte del repositorio ni de la release. La release incluye el launcher propio, su checksum y una guía; GitHub proporciona también los archivos de código fuente del tag.

## Pendiente

- Abordar la versión de Xuper/Hydra del Fire Stick en otra sesión.
- Validar el launcher en más modelos y ampliar su configuración.
- Decisión del propietario sobre apagar ADB al cerrar definitivamente el trabajo. Por ahora permanece activo.
