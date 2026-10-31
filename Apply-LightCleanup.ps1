#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9_.:\-]+$')]
    [string]$Serial,
    [string]$StatePath = (Join-Path $PSScriptRoot 'local\baseline\state.json')
)

$ErrorActionPreference = 'Stop'
$adb = (Get-Command adb -ErrorAction Stop).Source
if (-not (Test-Path -LiteralPath $StatePath)) { throw 'Primero se necesita el backup y state.json original.' }
$state = Get-Content -LiteralPath $StatePath -Raw | ConvertFrom-Json
function Save-State {
    $state | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $StatePath -Encoding UTF8
}
function Run-Adb {
    param([string[]]$Command)
    $result = & $adb -s $Serial @Command 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join [Environment]::NewLine) }
    return (($result | ForEach-Object { [string]$_ }) -join [Environment]::NewLine).Trim()
}
if ((Run-Adb -Command @('get-state')) -ne 'device') { throw 'Destino ADB no autorizado.' }
$originalDisabledPath = Join-Path (Split-Path -Parent $StatePath) 'disabled-before.txt'
if (-not (Test-Path -LiteralPath $originalDisabledPath)) { throw 'Falta la lista original de paquetes deshabilitados.' }
$originalDisabled = @(Get-Content -LiteralPath $originalDisabledPath)
# Lista cerrada para esta ROM. No se incluye ningun launcher ni servicio de Google.
$batch = @('com.cghs.stresstest', 'com.factorytools.factorystability')
foreach ($package in $batch) {
    if ($originalDisabled -contains "package:$package") { continue }
    $path = Run-Adb -Command @('shell','pm','path',$package)
    if ($path -notmatch '^package:') { throw "No esta instalado $package" }
    if (@($state.disabledByProject) -notcontains $package) {
        $state.disabledByProject = @($state.disabledByProject) + $package
        Save-State
    }
    $response = Run-Adb -Command @('shell','pm','disable-user','--user','0',$package)
    if ($response -notmatch 'disabled-user') { throw "No se confirmo la deshabilitacion: $response" }
    Write-Host $response
}
foreach ($name in @('window_animation_scale','transition_animation_scale','animator_duration_scale')) {
    if (@($state.globalsChanged) -notcontains $name) {
        $state.globalsChanged = @($state.globalsChanged) + $name
        Save-State
    }
    $null = Run-Adb -Command @('shell','settings','put','global',$name,'0.5')
    $value = Run-Adb -Command @('shell','settings','get','global',$name)
    if ($value -notmatch '^0\.5(?:0*)?$') { throw "No se confirmo $name = 0.5" }
    Write-Host "$name = $value"
}
Write-Host 'Lote terminado. El launcher original sigue habilitado.'
