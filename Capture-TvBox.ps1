#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9_.:\-]+$')]
    [string]$Serial,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'local\screenshots')
)

$ErrorActionPreference = 'Stop'
$adb = (Get-Command adb -ErrorAction Stop).Source
$null = New-Item -ItemType Directory -Force -Path $OutputDirectory
$destination = Join-Path (Resolve-Path -LiteralPath $OutputDirectory).Path ('screen-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.png')
$temporary = $destination + '.tmp'
$info = New-Object Diagnostics.ProcessStartInfo
$info.FileName = $adb
$info.Arguments = "-s $Serial exec-out screencap -p"
$info.UseShellExecute = $false
$info.CreateNoWindow = $true
$info.RedirectStandardOutput = $true
$info.RedirectStandardError = $true
$process = New-Object Diagnostics.Process
$process.StartInfo = $info
$stream = $null
$started = $false
try {
    $null = $process.Start()
    $started = $true
    $errorTask = $process.StandardError.ReadToEndAsync()
    $stream = [IO.File]::Create($temporary)
    $copyTask = $process.StandardOutput.BaseStream.CopyToAsync($stream)
    if (-not $process.WaitForExit(30000)) {
        $process.Kill()
        $process.WaitForExit()
        throw 'ADB no termino la captura en 30 segundos.'
    }
    $null = $copyTask.GetAwaiter().GetResult()
    $stream.Dispose()
    $stream = $null
    $errorText = $errorTask.GetAwaiter().GetResult()
    if ($process.ExitCode -ne 0) { throw "Fallo la captura ADB: $errorText" }
    $signature = New-Object byte[] 8
    $check = [IO.File]::OpenRead($temporary)
    try { $count = $check.Read($signature, 0, 8) } finally { $check.Dispose() }
    if ($count -ne 8 -or [BitConverter]::ToString($signature) -ne '89-50-4E-47-0D-0A-1A-0A') {
        throw 'El resultado no tiene una cabecera PNG valida.'
    }
    Move-Item -LiteralPath $temporary -Destination $destination
    Write-Host "Captura guardada en $destination. Revisala antes de publicarla."
} finally {
    if ($stream) { $stream.Dispose() }
    if ($started -and -not $process.HasExited) { $process.Kill(); $process.WaitForExit() }
    $process.Dispose()
    if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary }
}
