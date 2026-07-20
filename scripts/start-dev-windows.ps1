param(
  [switch]$SkipInstall,
  [switch]$ShowWindow,
  [switch]$WaitForReady,
  [string]$EnvFile = ""
)

$ErrorActionPreference = 'Stop'

# Some terminals inherit both PATH and Path. Start-Process rejects that duplicate
# environment key on Windows, so normalize it before spawning background processes.
$processPath = [Environment]::GetEnvironmentVariable('Path', 'Process')
if ([string]::IsNullOrWhiteSpace($processPath)) {
  $processPath = [Environment]::GetEnvironmentVariable('PATH', 'Process')
}
[Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
if (-not [string]::IsNullOrWhiteSpace($processPath)) {
  [Environment]::SetEnvironmentVariable('Path', $processPath, 'Process')
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $repoRoot 'logs'
$stateFile = Join-Path $logsDir 'dev-processes.json'
$backendLog = Join-Path $logsDir 'backend.log'
$backendErrLog = Join-Path $logsDir 'backend.err.log'
$frontendLog = Join-Path $logsDir 'frontend.log'
$frontendErrLog = Join-Path $logsDir 'frontend.err.log'
$credentialsFile = Join-Path $repoRoot 'credentials.txt'

New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

function Import-DotEnv {
  param([Parameter(Mandatory = $true)][string]$Path)

  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) {
      return
    }

    $parts = $line.Split('=', 2)
    if ($parts.Count -ne 2) {
      return
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    if ($value.Length -ge 2) {
      if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  }
}

function Import-IniFile {
  param([Parameter(Mandatory = $true)][string]$Path)

  $result = @{}
  $section = ''
  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#') -or $line.StartsWith(';')) {
      return
    }

    if ($line -match '^\[(.+)\]$') {
      $section = $matches[1].Trim().ToLowerInvariant()
      if (-not $result.ContainsKey($section)) {
        $result[$section] = @{}
      }
      return
    }

    $parts = $line.Split('=', 2)
    if ($parts.Count -ne 2) {
      if ($section -and -not $result[$section].ContainsKey('token')) {
        # Also support credential sections that contain a single raw token line.
        $result[$section]['token'] = $line
      }
      return
    }

    if (-not $result.ContainsKey($section)) {
      $result[$section] = @{}
    }

    $key = $parts[0].Trim().ToLowerInvariant()
    $value = $parts[1].Trim()
    if ($value.Length -ge 2) {
      if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }
    $result[$section][$key] = $value
  }

  return $result
}

function Set-DefaultEnv {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value
  )

  if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($Name, 'Process'))) {
    [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
  }
}

function Test-PortListening {
  param([Parameter(Mandatory = $true)][int]$Port)

  return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Get-PortProcessId {
  param([Parameter(Mandatory = $true)][int]$Port)

  $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
  if ($null -eq $connection) {
    return $null
  }
  return [int]$connection.OwningProcess
}

function Wait-PortReady {
  param(
    [Parameter(Mandatory = $true)][int]$Port,
    [Parameter(Mandatory = $true)][int]$TimeoutSeconds,
    [int]$WrapperProcessId = 0
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  do {
    if (Test-PortListening -Port $Port) {
      return $true
    }
    if ($WrapperProcessId -gt 0 -and -not (Get-Process -Id $WrapperProcessId -ErrorAction SilentlyContinue)) {
      return $false
    }
    Start-Sleep -Seconds 1
  } while ((Get-Date) -lt $deadline)

  return $false
}

$credentialMap = @{}
if (Test-Path $credentialsFile) {
  $credentialMap = Import-IniFile -Path $credentialsFile
}

$resolvedEnvFile = if ($EnvFile) {
  (Resolve-Path $EnvFile).Path
} else {
  Join-Path $repoRoot '.env'
}

if (Test-Path $resolvedEnvFile) {
  Import-DotEnv -Path $resolvedEnvFile
}

Set-DefaultEnv -Name 'DB_TARGET' -Value 'remote'
Set-DefaultEnv -Name 'SERVER_PORT' -Value '8090'
Set-DefaultEnv -Name 'FRONTEND_HOST' -Value '127.0.0.1'
Set-DefaultEnv -Name 'FRONTEND_PORT' -Value '5174'
Set-DefaultEnv -Name 'CORS_ALLOWED_ORIGINS' -Value "http://localhost:$($env:FRONTEND_PORT),http://127.0.0.1:$($env:FRONTEND_PORT)"

$dbTargetValue = [Environment]::GetEnvironmentVariable('DB_TARGET', 'Process')
if ([string]::IsNullOrWhiteSpace($dbTargetValue)) {
  $dbTargetValue = 'remote'
}
$dbTarget = $dbTargetValue.Trim().ToLowerInvariant()
$dbSection = if ($dbTarget -eq 'local') { 'mysql.local' } else { 'mysql.remote' }

if ($credentialMap.ContainsKey($dbSection)) {
  $dbConfig = $credentialMap[$dbSection]
  if ($dbConfig.ContainsKey('host')) { Set-DefaultEnv -Name 'MYSQL_HOST' -Value $dbConfig['host'] }
  if ($dbConfig.ContainsKey('port')) { Set-DefaultEnv -Name 'MYSQL_PORT' -Value $dbConfig['port'] }
  if ($dbConfig.ContainsKey('database')) { Set-DefaultEnv -Name 'MYSQL_DATABASE' -Value $dbConfig['database'] }
  if ($dbConfig.ContainsKey('user')) { Set-DefaultEnv -Name 'MYSQL_USER' -Value $dbConfig['user'] }
  if ($dbConfig.ContainsKey('password')) { Set-DefaultEnv -Name 'MYSQL_PASSWORD' -Value $dbConfig['password'] }
}

function Set-CredentialEnv {
  param(
    [Parameter(Mandatory = $true)][string[]]$Sections,
    [Parameter(Mandatory = $true)][string[]]$Keys,
    [Parameter(Mandatory = $true)][string]$EnvName
  )

  foreach ($sectionName in $Sections) {
    $normalizedSection = $sectionName.ToLowerInvariant()
    if (-not $credentialMap.ContainsKey($normalizedSection)) { continue }
    foreach ($keyName in $Keys) {
      $normalizedKey = $keyName.ToLowerInvariant()
      if ($credentialMap[$normalizedSection].ContainsKey($normalizedKey)) {
        Set-DefaultEnv -Name $EnvName -Value $credentialMap[$normalizedSection][$normalizedKey]
        return
      }
    }
  }
}

Set-CredentialEnv -Sections @('deepseek.api', 'deepseek') -Keys @('api_key', 'token', 'key') -EnvName 'DEEPSEEK_API_KEY'
Set-CredentialEnv -Sections @('rakuten.api', 'rakuten') -Keys @('application_id', 'appid') -EnvName 'RAKUTEN_APPLICATION_ID'
Set-CredentialEnv -Sections @('rakuten.api', 'rakuten') -Keys @('access_key', 'accesskey') -EnvName 'RAKUTEN_ACCESS_KEY'
Set-CredentialEnv -Sections @('rakuten.api', 'rakuten') -Keys @('affiliate_id', 'affiliateid') -EnvName 'RAKUTEN_AFFILIATE_ID'
Set-CredentialEnv -Sections @('yahoo.shopping', 'yahoo.api') -Keys @('client_id', 'appid') -EnvName 'YAHOO_SHOPPING_CLIENT_ID'
Set-CredentialEnv -Sections @('rainforest.api', 'rainforest') -Keys @('api_key', 'key', 'token') -EnvName 'RAINFOREST_API_KEY'
Set-CredentialEnv -Sections @('apify.api', 'apify') -Keys @('api_token', 'token') -EnvName 'APIFY_TOKEN'

if ($dbTarget -eq 'local') {
  Set-DefaultEnv -Name 'MYSQL_HOST' -Value '127.0.0.1'
  Set-DefaultEnv -Name 'MYSQL_PORT' -Value '3306'
  Set-DefaultEnv -Name 'MYSQL_DATABASE' -Value 'crossborder_trend_demo'
  Set-DefaultEnv -Name 'MYSQL_USER' -Value 'root'
  Set-DefaultEnv -Name 'MYSQL_PASSWORD' -Value ''
} else {
  Set-DefaultEnv -Name 'MYSQL_HOST' -Value '101.132.78.217'
  Set-DefaultEnv -Name 'MYSQL_PORT' -Value '3306'
  Set-DefaultEnv -Name 'MYSQL_DATABASE' -Value 'crossborder_trend_demo'
  Set-DefaultEnv -Name 'MYSQL_USER' -Value 'cross_demo'
  Set-DefaultEnv -Name 'MYSQL_PASSWORD' -Value ''
}

if (Test-PortListening -Port ([int]$env:SERVER_PORT)) {
  throw "Port $($env:SERVER_PORT) is already in use. Run scripts\\stop-dev-windows.ps1 first."
}

if (Test-PortListening -Port ([int]$env:FRONTEND_PORT)) {
  throw "Port $($env:FRONTEND_PORT) is already in use. Run scripts\\stop-dev-windows.ps1 first."
}

if (-not $SkipInstall) {
  Push-Location (Join-Path $repoRoot 'frontend')
  try {
    if (-not (Test-Path 'node_modules')) {
      npm.cmd ci
    }
  } finally {
    Pop-Location
  }
}

$windowStyle = if ($ShowWindow) { 'Normal' } else { 'Hidden' }
$shellExecutable = (Get-Command pwsh -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source)
if ([string]::IsNullOrWhiteSpace($shellExecutable)) {
  $shellExecutable = 'powershell.exe'
}

if (Test-Path $backendLog) { Remove-Item $backendLog -Force }
if (Test-Path $backendErrLog) { Remove-Item $backendErrLog -Force }
if (Test-Path $frontendLog) { Remove-Item $frontendLog -Force }
if (Test-Path $frontendErrLog) { Remove-Item $frontendErrLog -Force }

$backendCommand = @"
`$ErrorActionPreference = 'Stop'
Set-Location '$($repoRoot.Replace("'", "''"))\backend'
`$env:MYSQL_HOST = '$($env:MYSQL_HOST.Replace("'", "''"))'
`$env:MYSQL_PORT = '$($env:MYSQL_PORT.Replace("'", "''"))'
`$env:MYSQL_DATABASE = '$($env:MYSQL_DATABASE.Replace("'", "''"))'
`$env:MYSQL_USER = '$($env:MYSQL_USER.Replace("'", "''"))'
`$env:MYSQL_PASSWORD = '$($env:MYSQL_PASSWORD.Replace("'", "''"))'
`$env:SERVER_PORT = '$($env:SERVER_PORT.Replace("'", "''"))'
`$env:CORS_ALLOWED_ORIGINS = '$($env:CORS_ALLOWED_ORIGINS.Replace("'", "''"))'
mvn spring-boot:run
"@

$backendWrapper = Start-Process $shellExecutable `
  -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $backendCommand `
  -WindowStyle $windowStyle `
  -RedirectStandardOutput $backendLog `
  -RedirectStandardError $backendErrLog `
  -PassThru

if ($WaitForReady -and -not (Wait-PortReady -Port ([int]$env:SERVER_PORT) -TimeoutSeconds 90 -WrapperProcessId $backendWrapper.Id)) {
  throw "Backend did not start within 90 seconds. Check $backendLog and $backendErrLog."
}

$frontendCommand = @"
`$ErrorActionPreference = 'Stop'
Set-Location '$($repoRoot.Replace("'", "''"))\frontend'
`$env:VITE_HOST = '$($env:FRONTEND_HOST.Replace("'", "''"))'
`$env:VITE_PORT = '$($env:FRONTEND_PORT.Replace("'", "''"))'
npm.cmd run dev -- --host `$env:VITE_HOST --port `$env:VITE_PORT
"@

$frontendWrapper = Start-Process $shellExecutable `
  -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $frontendCommand `
  -WindowStyle $windowStyle `
  -RedirectStandardOutput $frontendLog `
  -RedirectStandardError $frontendErrLog `
  -PassThru

if ($WaitForReady -and -not (Wait-PortReady -Port ([int]$env:FRONTEND_PORT) -TimeoutSeconds 60 -WrapperProcessId $frontendWrapper.Id)) {
  throw "Frontend did not start within 60 seconds. Check $frontendLog and $frontendErrLog."
}

$backendPid = if ($WaitForReady) { Get-PortProcessId -Port ([int]$env:SERVER_PORT) } else { $null }
$frontendPid = if ($WaitForReady) { Get-PortProcessId -Port ([int]$env:FRONTEND_PORT) } else { $null }

$state = [ordered]@{
  startedAt = (Get-Date).ToString('s')
  repoRoot = $repoRoot
  backend = [ordered]@{
    wrapperPid = $backendWrapper.Id
    servicePid = $backendPid
    port = [int]$env:SERVER_PORT
    url = "http://localhost:$($env:SERVER_PORT)"
    log = $backendLog
    errLog = $backendErrLog
  }
  frontend = [ordered]@{
    wrapperPid = $frontendWrapper.Id
    servicePid = $frontendPid
    port = [int]$env:FRONTEND_PORT
    url = "http://$($env:FRONTEND_HOST):$($env:FRONTEND_PORT)"
    log = $frontendLog
    errLog = $frontendErrLog
  }
}

$state | ConvertTo-Json -Depth 5 | Set-Content -Path $stateFile -Encoding UTF8

Write-Output "Backend starting:  http://localhost:$($env:SERVER_PORT)"
Write-Output "Frontend starting: http://$($env:FRONTEND_HOST):$($env:FRONTEND_PORT)"
if (-not $WaitForReady) { Write-Output "The command has returned. Run .\scripts\status-dev-windows.ps1 to check readiness, or add -WaitForReady to wait for both ports." }
Write-Output "State file: $stateFile"
