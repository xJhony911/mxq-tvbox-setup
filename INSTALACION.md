# Instalar MXQ Inicio en Windows

La TV Box de este proyecto ya está configurada. Esta guía sirve para reinstalar o probar el launcher en otro equipo. Se probó en una MXQ con Rockchip RK3229 y API 25; no se ha validado en otros modelos. El APK requiere Android 7.1/API 25 o superior. No incluye SmartTube, YouTube, Kodi ni Xuper.

## 1. Descargar y conectar

Desde [Releases](https://github.com/xJhony911/mxq-tvbox-setup/releases), descarga `MXQ-Inicio-v1.0.0.apk` y `SHA256SUMS.txt`. Descarga también el código fuente ZIP y extráelo para tener los scripts. Instala las [Platform Tools oficiales](https://developer.android.com/tools/releases/platform-tools).

Coloca el APK junto a los scripts extraídos. Abre PowerShell en esa carpeta, comprueba el APK y compara su hash con `SHA256SUMS.txt`:

```powershell
Get-FileHash -LiteralPath '.\MXQ-Inicio-v1.0.0.apk' -Algorithm SHA256
$tv = 'DIRECCION_DE_TU_TV:5555'
adb connect $tv
adb devices -l
adb -s $tv shell getprop ro.build.version.sdk
```

Autoriza la conexión en la TV. Sustituye el destino de ejemplo y usa siempre `-s` para actuar solo sobre esa caja.

## 2. Guardar el estado original

En la carpeta de los scripts:

```powershell
.\Backup-TvBox.ps1 -Serial $tv
```

El script guarda el launcher original, ajustes e inventario en `local/baseline/` y se niega a sobrescribir un estado existente. En la caja original del proyecto conserva su backup previo; no lo reemplaces con un backup del sistema ya modificado. No es una copia de las cuentas ni de los datos internos de las apps.

## 3. Instalar y probar sin cambiar Home

```powershell
adb -s $tv install -r '.\MXQ-Inicio-v1.0.0.apk'
adb -s $tv shell am start -W -n ec.mxq.inicio/.MainActivity
```

Comprueba en pantalla la hora, el clima y la navegación D-Pad/OK. Las apps presentes aparecen automáticamente. Esta versión muestra Ambato y prioriza SmartTube, YouTube y Kodi en recientes. El clima usa Internet y conserva el último dato si no hay conexión. No usa GPS. El botón «Menú original» está preparado para `com.droidlogic.mboxlauncher`, el launcher de esta MXQ; en otro modelo usa su launcher real mediante la restauración.

## 4. Dar acceso a recientes y activar Home

Solo después de probar el menú. Registra primero el valor anterior del permiso y la intención de cambiar Home para que la restauración pueda revertirlo:

```powershell
$statePath = Join-Path $PWD 'local\baseline\state.json'
$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$opText = (adb -s $tv shell appops get ec.mxq.inicio GET_USAGE_STATS) -join "`n"
$oldMode = 'default'
if ($opText -match 'GET_USAGE_STATS:\s*(allow|ignore|deny|default|foreground)') {
    $oldMode = $Matches[1]
}
if (@($state.appOpsChanged | Where-Object { $_.package -eq 'ec.mxq.inicio' }).Count -eq 0) {
    $state.appOpsChanged = @($state.appOpsChanged) + [pscustomobject]@{
        package = 'ec.mxq.inicio'; operation = 'GET_USAGE_STATS'; original = $oldMode
    }
}
$state.homeChanged = $true
$state | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $statePath -Encoding UTF8
adb -s $tv shell appops set ec.mxq.inicio GET_USAGE_STATS allow
adb -s $tv shell cmd package set-home-activity --user 0 ec.mxq.inicio/.MainActivity
adb -s $tv shell input keyevent KEYCODE_HOME
adb -s $tv shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

La última consulta debe devolver `ec.mxq.inicio/.MainActivity`. Si tu ROM no admite `set-home-activity`, elige MXQ Inicio y «Siempre» en el selector de Home del sistema. Conserva habilitado el launcher de fábrica. El acceso al historial se usa localmente para ordenar apps; no descubre el capítulo ni el segundo exacto de un video.

## 5. Recuperar el menú original

Con el backup original disponible:

```powershell
.\restore_tvbox.ps1 -Serial $tv -EmergencyHome
```

Restaura los ajustes registrados, reactiva los paquetes deshabilitados por el proyecto y recupera el Home original. Conserva las apps instaladas y sus datos. Para quitar únicamente el launcher, cambia primero al Home original y después usa `adb -s $tv uninstall ec.mxq.inicio`; esa desinstalación elimina las preferencias del launcher.

## Limpieza e instalación de SmartTube

Son pasos separados; instalar el launcher no hace debloat. `Apply-LightCleanup.ps1` deshabilita solo dos herramientas de pruebas de esta ROM y pone las animaciones a 0.5. No ejecutes esa lista en otro modelo sin revisar sus paquetes. `Install-SmartTube.ps1` descarga la estable del proyecto oficial para la ABI detectada; `adb install -r` no borra sus datos.

No uses limpiadores periódicos ni límites rígidos de procesos para intentar mantener RAM vacía: pueden provocar más recargas de las apps. Las medidas de inicio de esta sesión variaron y no demuestran una aceleración uniforme ni 60 fps sostenidos.

## Compilar

Consulta `Build-Launcher.ps1` y el README. El código Java usa exclusivamente APIs del SDK. El APK de esta release está firmado para desarrollo local; la clave permanece privada. Si compilas con otra clave, Android no permite actualizar sobre esta instalación sin cambiar la firma o desinstalar antes, lo que elimina sus preferencias.

## ADB al terminar

La configuración original mantiene ADB activo para continuar el trabajo. Cuando el propietario cierre la configuración, puede desactivar la depuración desde las opciones de desarrollador. No expongas el puerto ADB a Internet ni publiques los backups completos o las claves de firma.


