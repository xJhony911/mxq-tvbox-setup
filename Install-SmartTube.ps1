#requires -Version 5.1
[CmdletBinding()]
param([Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9_.:\-]+$')][string]$Serial)
$ErrorActionPreference='Stop'
$adb=(Get-Command adb -ErrorAction Stop).Source
$abi=((& $adb -s $Serial shell getprop ro.product.cpu.abi)-join '').Trim()
if($LASTEXITCODE -ne 0){throw 'No se pudo leer la arquitectura.'}
if($abi -notin @('armeabi-v7a','arm64-v8a','x86')){throw "ABI sin variante seleccionada: $abi"}
$release=Invoke-RestMethod -Uri 'https://api.github.com/repos/yuliskov/SmartTube/releases/latest' -Headers @{'User-Agent'='mxq-tvbox-setup'}
if($release.prerelease -or $release.draft){throw 'La version no es estable.'}
$asset=@($release.assets|Where-Object{$_.name -match ('_'+[regex]::Escape($abi)+'\.apk$')})
if($asset.Count -ne 1){throw 'No se encontro exactamente un APK oficial para esta ABI.'}
if($asset[0].browser_download_url -notlike 'https://github.com/yuliskov/SmartTube/releases/download/*'){throw 'Origen de APK inesperado.'}
$folder=Join-Path $PSScriptRoot 'local\downloads'; $null=New-Item -ItemType Directory -Force -Path $folder
$file=Join-Path $folder ([IO.Path]::GetFileName($asset[0].name))
Invoke-WebRequest -Uri $asset[0].browser_download_url -OutFile $file
$hash=(Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
if($asset[0].digest -like 'sha256:*' -and $hash -ne $asset[0].digest.Substring(7)){throw 'SHA256 no coincide con GitHub.'}
& $adb -s $Serial install -r $file
if($LASTEXITCODE -ne 0){throw 'ADB rechazo la instalacion. No se borraron datos.'}
Write-Host "Instalada version $($release.tag_name), ABI $abi, SHA256 $hash"
