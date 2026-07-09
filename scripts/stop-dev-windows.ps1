param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $repoRoot 'logs'
$stateFile = Join-Path $logsDir 'dev-processes.json'

function Get-PortProcessIds {
  param([Parameter(Mandatory = $true)][int]$Port)

  @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique)
}

function Wait-PortClosed {
  param(
    [Parameter(Mandatory = $true)][int]$Port,
    [Parameter(Mandatory = $true)][int]$TimeoutSeconds
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  do {
    if (-not (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) {
      return $true
    }
    Start-Sleep -Seconds 1
  } while ((Get-Date) -lt $deadline)

  return $false
}

$targets = @()
if (Test-Path $stateFile) {
  $state = Get-Content -Raw $stateFile | ConvertFrom-Json
  $targets += [pscustomobject]@{ Name = 'backend'; Port = [int]$state.backend.port; Pids = @($state.backend.servicePid, $state.backend.wrapperPid) }
  $targets += [pscustomobject]@{ Name = 'frontend'; Port = [int]$state.frontend.port; Pids = @($state.frontend.servicePid, $state.frontend.wrapperPid) }
} else {
  $targets += [pscustomobject]@{ Name = 'backend'; Port = 8090; Pids = @() }
  $targets += [pscustomobject]@{ Name = 'frontend'; Port = 5174; Pids = @() }
}

foreach ($target in $targets) {
  $allPids = @($target.Pids + (Get-PortProcessIds -Port $target.Port)) |
    Where-Object { $_ } |
    Sort-Object -Unique

  foreach ($procId in $allPids) {
    if (Get-Process -Id $procId -ErrorAction SilentlyContinue) {
      Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
  }

  [void](Wait-PortClosed -Port $target.Port -TimeoutSeconds 20)
}

if (Test-Path $stateFile) {
  Remove-Item $stateFile -Force
}

Write-Host 'Stop command finished.'
