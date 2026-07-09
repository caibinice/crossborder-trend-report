param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$stateFile = Join-Path (Join-Path $repoRoot 'logs') 'dev-processes.json'

function Show-PortStatus {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][int]$Port
  )

  $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1

  if ($null -eq $connection) {
    Write-Host "$Name not running (port $Port is not listening)"
    return
  }

  $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
  $processName = if ($process) { $process.ProcessName } else { 'unknown' }
  Write-Host "$Name running: port=$Port PID=$($connection.OwningProcess) process=$processName"
}

if (Test-Path $stateFile) {
  Write-Host "State file: $stateFile"
  Get-Content -Raw $stateFile | Write-Host
  Write-Host ''
}

Show-PortStatus -Name 'backend' -Port 8090
Show-PortStatus -Name 'frontend' -Port 5174
