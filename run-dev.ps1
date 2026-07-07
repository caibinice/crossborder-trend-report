param([switch]$SkipInstall)
$ErrorActionPreference='Stop'
. D:\devtools\env-dev.ps1
. D:\devtools\start-mysql.ps1
$root='D:\codes\crossborder-trend-report'
if(-not $SkipInstall){ Push-Location "$root\frontend"; if(-not(Test-Path 'node_modules')){ npm install }; Pop-Location }
Start-Process powershell -ArgumentList '-NoExit','-Command',". D:\devtools\env-dev.ps1; cd '$root\backend'; mvn spring-boot:run" -WindowStyle Normal
Start-Sleep -Seconds 8
Start-Process powershell -ArgumentList '-NoExit','-Command',". D:\devtools\env-dev.ps1; cd '$root\frontend'; npm run dev" -WindowStyle Normal
Write-Host 'Backend:  http://localhost:8090'
Write-Host 'Frontend: http://localhost:5174'
