param(
  [switch]$SkipInstall,
  [switch]$ShowWindow,
  [string]$EnvFile = ""
)

$ErrorActionPreference = 'Stop'

$scriptPath = Join-Path $PSScriptRoot 'scripts\start-dev-windows.ps1'
& $scriptPath -SkipInstall:$SkipInstall -ShowWindow:$ShowWindow -EnvFile $EnvFile
