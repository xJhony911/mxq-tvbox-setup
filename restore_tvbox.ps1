#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9_.:\-]+$')]
    [string]$Serial,
    [string]$StatePath = (Join-Path $PSScriptRoot 'local\baseline\state.json'),
    [ValidatePattern('^[a-zA-Z0-9_.]+$')]
    [string[]]$Packages = @(),
    [switch]$EmergencyHome
)

$ErrorActionPreference = 'Stop'
$adb = (Get-Command adb -ErrorAction Stop).Source
function Invoke-AdbRestore {
    param([string[]]$Command)
    $result = & $adb -s $Serial @Command 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join [Environment]::NewLine) }
    $result | Write-Output
}

if ($Packages.Count -gt 0) {
    foreach ($package in $Packages) {
        Invoke-AdbRestore -Command @('shell','pm','enable','--user','0',$package)
    }
    if (-not $EmergencyHome) { return }
}

if (-not (Test-Path -LiteralPath $StatePath)) {
    throw 'No existe el estado original. Usa -StatePath o -Packages para reactivar paquetes concretos.'
}
$state = Get-Content -LiteralPath $StatePath -Raw | ConvertFrom-Json
foreach ($package in @($state.disabledByProject)) {
    if ($package -notmatch '^[a-zA-Z0-9_.]+$') { throw 'Paquete no valido en el registro.' }
    Invoke-AdbRestore -Command @('shell','pm','enable','--user','0',$package)
}
foreach ($name in @($state.globalsChanged)) {
    if ($name -notmatch '^[a-zA-Z0-9_]+$') { throw 'Ajuste no valido en el registro.' }
    $original = [string]$state.globals.$name
    if ($original -eq 'null') {
        Invoke-AdbRestore -Command @('shell','settings','delete','global',$name)
    } else {
        Invoke-AdbRestore -Command @('shell','settings','put','global',$name,$original)
    }
}
foreach ($name in @($state.secureChanged)) {
    if ($name -notmatch '^[a-zA-Z0-9_]+$') { throw 'Ajuste no valido en el registro.' }
    $original = [string]$state.secure.$name
    if ($original -eq 'null') {
        Invoke-AdbRestore -Command @('shell','settings','delete','secure',$name)
    } else {
        Invoke-AdbRestore -Command @('shell','settings','put','secure',$name,$original)
    }
}
foreach ($operation in @($state.appOpsChanged)) {
    if ($operation.package -notmatch '^[a-zA-Z0-9_.]+$' -or
        $operation.operation -notmatch '^[A-Z_]+$' -or
        $operation.original -notmatch '^(allow|ignore|deny|default|foreground)$') {
        throw 'Operacion de permisos no valida en el registro.'
    }
    Invoke-AdbRestore -Command @('shell','appops','set',$operation.package,$operation.operation,$operation.original)
}
if ($state.homeChanged -or $EmergencyHome) {
    $component = [string]$state.factoryHome
    if ($component -notmatch '^[a-zA-Z0-9_.]+/[a-zA-Z0-9_.]+$') { throw 'Launcher original no valido.' }
    $package = $component.Split('/')[0]
    Invoke-AdbRestore -Command @('shell','pm','enable','--user','0',$package)
    Invoke-AdbRestore -Command @('shell','cmd','package','set-home-activity','--user','0',$component)
    Invoke-AdbRestore -Command @('shell','am','start','-n',$component)
}
Write-Host 'Restauracion terminada. Las aplicaciones nuevas conservan sus datos y siguen instaladas.'
