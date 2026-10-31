#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$JavaHome,
    [Parameter(Mandatory=$true)][string]$AndroidJar,
    [Parameter(Mandatory=$true)][string]$BuildTools
)
$ErrorActionPreference='Stop'
$env:JAVA_HOME=(Resolve-Path -LiteralPath $JavaHome).Path
$AndroidJar=(Resolve-Path -LiteralPath $AndroidJar).Path
$BuildTools=(Resolve-Path -LiteralPath $BuildTools).Path
$build=Join-Path $PSScriptRoot 'local\build'
foreach($folder in @($build,(Join-Path $build 'classes'),(Join-Path $build 'dex'),(Join-Path $build 'res\drawable'))){$null=New-Item -ItemType Directory -Force -Path $folder}
foreach($file in @('icon.xml','banner.xml')){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination (Join-Path $build ('res\drawable\'+$file))}
function Check-Exit { if($LASTEXITCODE -ne 0){throw "Herramienta Android/Java fallo con codigo $LASTEXITCODE"} }
& (Join-Path $env:JAVA_HOME 'bin\javac.exe') -encoding UTF-8 --release 8 -classpath $AndroidJar -d (Join-Path $build 'classes') (Join-Path $PSScriptRoot 'MainActivity.java'); Check-Exit
Add-Type -AssemblyName System.IO.Compression.FileSystem
$classJar=Join-Path $build 'classes.jar'
if(Test-Path -LiteralPath $classJar){Remove-Item -LiteralPath $classJar}
[IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $build 'classes'),$classJar)
& (Join-Path $BuildTools 'd8.bat') --min-api 25 --lib $AndroidJar --output (Join-Path $build 'dex') (Join-Path $build 'classes.jar'); Check-Exit
$unsigned=Join-Path $build 'unsigned.apk'
& (Join-Path $BuildTools 'aapt.exe') package -f -M (Join-Path $PSScriptRoot 'AndroidManifest.xml') -S (Join-Path $build 'res') -I $AndroidJar -F $unsigned; Check-Exit
$archive=[IO.Compression.ZipFile]::Open($unsigned,[IO.Compression.ZipArchiveMode]::Update)
try{[void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,(Join-Path $build 'dex\classes.dex'),'classes.dex')}finally{$archive.Dispose()}
$aligned=Join-Path $build 'aligned.apk'
& (Join-Path $BuildTools 'zipalign.exe') -f 4 $unsigned $aligned; Check-Exit
$keystore=Join-Path $PSScriptRoot 'local\launcher-debug.keystore'
if(-not(Test-Path -LiteralPath $keystore)){
    & (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -genkeypair -keystore $keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=MXQ Local Development'; Check-Exit
}
$apk=Join-Path $PSScriptRoot 'local\MXQ-Inicio.apk'
& (Join-Path $BuildTools 'apksigner.bat') sign --ks $keystore --ks-pass pass:android --key-pass pass:android --out $apk $aligned; Check-Exit
& (Join-Path $BuildTools 'apksigner.bat') verify $apk; Check-Exit
Write-Host "Launcher compilado y firmado para desarrollo local: $apk"
