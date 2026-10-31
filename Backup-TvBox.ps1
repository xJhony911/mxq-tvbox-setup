#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9_.:\-]+$')][string]$Serial,
    [string]$OutputDirectory=(Join-Path $PSScriptRoot 'local\baseline')
)
$ErrorActionPreference='Stop'
$adb=(Get-Command adb -ErrorAction Stop).Source
$statePath=Join-Path $OutputDirectory 'state.json'
if(Test-Path -LiteralPath $statePath){throw 'El estado original ya existe. No se sobrescribe; usa otra carpeta para un nuevo backup.'}
$null=New-Item -ItemType Directory -Force -Path $OutputDirectory
function Read-AdbBackup {
    param([string[]]$Command)
    $result=& $adb -s $Serial @Command 2>&1
    if($LASTEXITCODE -ne 0){throw ($result -join [Environment]::NewLine)}
    return (($result|ForEach-Object{[string]$_})-join [Environment]::NewLine).Trim()
}
if((Read-AdbBackup -Command @('get-state')) -ne 'device'){throw 'ADB no autorizado.'}
$home=Read-AdbBackup -Command @('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
$component=($home -split '\r?\n'|Where-Object{$_ -match '^[a-zA-Z0-9_.]+/[a-zA-Z0-9_.]+$'}|Select-Object -Last 1)
if(-not $component){throw 'No se pudo identificar el launcher original.'}
Read-AdbBackup -Command @('shell','pm','list','packages','-f') | Set-Content -LiteralPath (Join-Path $OutputDirectory 'backup_packages.txt') -Encoding UTF8
Read-AdbBackup -Command @('shell','getprop') | Set-Content -LiteralPath (Join-Path $OutputDirectory 'backup_props.txt') -Encoding UTF8
Read-AdbBackup -Command @('shell','pm','list','packages','-d') | Set-Content -LiteralPath (Join-Path $OutputDirectory 'disabled-before.txt') -Encoding UTF8
$state=[ordered]@{createdAt=[DateTimeOffset]::Now.ToString('o');factoryHome=$component;globals=[ordered]@{};secure=[ordered]@{};disabledByProject=@();installedByProject=@();homeChanged=$false;globalsChanged=@();secureChanged=@();appOpsChanged=@()}
foreach($name in @('window_animation_scale','transition_animation_scale','animator_duration_scale')){$state.globals[$name]=Read-AdbBackup -Command @('shell','settings','get','global',$name)}
foreach($name in @('enabled_accessibility_services','accessibility_enabled')){$state.secure[$name]=Read-AdbBackup -Command @('shell','settings','get','secure',$name)}
$state | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $statePath -Encoding UTF8
Write-Host "Backup de inventario y ajustes guardado en $OutputDirectory. No contiene los datos privados de las aplicaciones."
