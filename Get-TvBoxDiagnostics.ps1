#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9_.:\-]+$')]
    [string]$Serial,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'local')
)

$ErrorActionPreference = 'Stop'
$adb = (Get-Command adb -ErrorAction Stop).Source

function Read-Adb {
    param([string[]]$Command)
    $lines = & $adb -s $Serial @Command 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ('ADB fallo: ' + ($lines -join [Environment]::NewLine))
    }
    return (($lines | ForEach-Object { [string]$_ }) -join [Environment]::NewLine).Trim()
}

if ((Read-Adb -Command @('get-state')) -ne 'device') {
    throw 'El destino no esta conectado y autorizado. Ejecuta adb connect y acepta el aviso en la TV.'
}

$properties = @(
    'ro.build.version.release', 'ro.build.version.sdk',
    'ro.build.version.security_patch', 'ro.build.id',
    'ro.product.cpu.abi', 'ro.product.cpu.abilist',
    'ro.board.platform', 'ro.hardware', 'ro.build.characteristics',
    'ro.product.manufacturer', 'ro.product.model', 'ro.product.device'
)
$report = [ordered]@{ capturedAt = [DateTimeOffset]::Now.ToString('o') }
foreach ($property in $properties) {
    $report[$property] = Read-Adb -Command @('shell', 'getprop', $property)
}
# Se excluye el numero de serie de cpuinfo. No se recopila getprop completo.
$cpu = Read-Adb -Command @('shell', 'cat', '/proc/cpuinfo')
$report['cpuinfo'] = (($cpu -split '\r?\n' | Where-Object { $_ -notmatch '^Serial\s*:' }) -join [Environment]::NewLine)
$report['free_m'] = Read-Adb -Command @('shell', 'free', '-m')
$report['meminfo'] = Read-Adb -Command @('shell', 'cat', '/proc/meminfo')
$zones = Read-Adb -Command @('shell', 'cat', '/proc/zoneinfo')
$report['memory_zones'] = (($zones -split '\r?\n' | Where-Object { $_ -match 'Node |present |managed |spanned ' }) -join [Environment]::NewLine)
$report['df_h'] = Read-Adb -Command @('shell', 'df', '-h')
$report['kernel'] = Read-Adb -Command @('shell', 'uname', '-a')
$window = Read-Adb -Command @('shell', 'dumpsys', 'window')
$report['focus'] = (($window -split '\r?\n' | Where-Object { $_ -match 'mCurrentFocus|mFocusedApp' }) -join [Environment]::NewLine)
$report['home_activity'] = Read-Adb -Command @('shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')

$null = New-Item -ItemType Directory -Force -Path $OutputDirectory
$destination = Join-Path $OutputDirectory ('diagnostic-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
$report | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $destination -Encoding UTF8
$report | ConvertTo-Json -Depth 4
Write-Host "Diagnostico guardado en $destination. Revisa los datos antes de publicarlos."
