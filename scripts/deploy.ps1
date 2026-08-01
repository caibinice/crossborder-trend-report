param(
  [switch]$BuildOnly
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

& mvn -q -f (Join-Path $root 'backend\pom.xml') package
if ($LASTEXITCODE -ne 0) { throw '跨境项目 Maven 打包失败。' }

Push-Location (Join-Path $root 'frontend')
try {
  npm ci --no-audit --no-fund
  if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败。' }
  npm test
  if ($LASTEXITCODE -ne 0) { throw '前端测试失败。' }
  npm run build
  if ($LASTEXITCODE -ne 0) { throw '前端生产构建失败。' }
} finally {
  Pop-Location
}

if ($BuildOnly) {
  Write-Host '跨境项目独立构建完成，未连接远程服务器。' -ForegroundColor Green
  return
}

$python = Join-Path $root '.venv-deploy\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python)) {
  python -m venv (Join-Path $root '.venv-deploy')
  if ($LASTEXITCODE -ne 0) { throw '创建部署虚拟环境失败。' }
  & $python -m pip install --disable-pip-version-check -r (
    Join-Path $root 'scripts\remote\requirements.txt'
  )
  if ($LASTEXITCODE -ne 0) { throw '安装部署依赖失败。' }
}
& $python (Join-Path $root 'scripts\remote\deploy_crossborder.py')
if ($LASTEXITCODE -ne 0) { throw '跨境项目远程发布失败。' }
