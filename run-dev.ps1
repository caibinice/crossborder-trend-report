param(
  [switch]$SkipInstall,
  [switch]$ShowWindow,
  [switch]$WaitForReady,
  [string]$EnvFile = ""
)

$ErrorActionPreference = 'Stop'

$scriptPath = Join-Path $PSScriptRoot 'scripts\start-dev-windows.ps1'
& $scriptPath -SkipInstall:$SkipInstall -ShowWindow:$ShowWindow -WaitForReady:$WaitForReady -EnvFile $EnvFile
