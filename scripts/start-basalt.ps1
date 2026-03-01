#!/usr/bin/env pwsh
# ─────────────────────────────────────────────────────────────────────────────
# Basalt — Full Stack Startup Script (PowerShell)
# Starts: Docker (Postgres + Ollama) → Backend → Frontend
# ─────────────────────────────────────────────────────────────────────────────

$ROOT = Split-Path -Parent $PSScriptRoot
$BACKEND = Join-Path $ROOT "basalt-backend"
$FRONTEND = Join-Path $ROOT "basalt-frontend"

Write-Host ""
Write-Host "██████╗  █████╗ ███████╗ █████╗ ██╗  ████████╗" -ForegroundColor DarkGray
Write-Host "██╔══██╗██╔══██╗██╔════╝██╔══██╗██║  ╚══██╔══╝" -ForegroundColor DarkGray
Write-Host "██████╔╝███████║███████╗███████║██║     ██║   " -ForegroundColor Cyan
Write-Host "██╔══██╗██╔══██║╚════██║██╔══██║██║     ██║   " -ForegroundColor DarkGray
Write-Host "██████╔╝██║  ██║███████║██║  ██║███████╗██║   " -ForegroundColor DarkGray
Write-Host "╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝╚═╝   " -ForegroundColor DarkGray
Write-Host ""
Write-Host "  Basalt AI Assistant — Startup Script" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host ""

# ── Step 1: Docker Compose ────────────────────────────────────────────────────
Write-Host "[1/4] Starting Docker services (Postgres + Ollama)..." -ForegroundColor Yellow
Set-Location $ROOT
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ✗ Docker Compose failed. Is Docker Desktop running?" -ForegroundColor Red
    exit 1
}
Write-Host "  ✓ Docker services started" -ForegroundColor Green

# ── Step 2: Wait for Postgres ─────────────────────────────────────────────────
Write-Host "[2/4] Waiting for PostgreSQL to be healthy..." -ForegroundColor Yellow
$maxWait = 30
$waited  = 0
do {
    Start-Sleep -Seconds 2
    $waited += 2
    $health = docker inspect --format "{{.State.Health.Status}}" basalt-postgres 2>$null
} while ($health -ne "healthy" -and $waited -lt $maxWait)

if ($health -eq "healthy") {
    Write-Host "  ✓ PostgreSQL is healthy" -ForegroundColor Green
} else {
    Write-Host "  ⚠ PostgreSQL health check timed out — continuing anyway" -ForegroundColor Yellow
}

# ── Step 3: Start Spring Boot Backend ────────────────────────────────────────
Write-Host "[3/4] Starting Spring Boot backend (port 8080)..." -ForegroundColor Yellow
$backendLog = Join-Path $ROOT "logs\backend.log"
New-Item -ItemType Directory -Force -Path (Join-Path $ROOT "logs") | Out-Null

$backendJob = Start-Job -Name "BasaltBackend" -ScriptBlock {
    param($dir, $log)
    Set-Location $dir
    mvn spring-boot:run 2>&1 | Tee-Object -FilePath $log
} -ArgumentList $BACKEND, $backendLog

Write-Host "  ⏳ Waiting for backend to start (up to 60s)..." -ForegroundColor DarkGray
$maxWait = 60
$waited  = 0
$started = $false
do {
    Start-Sleep -Seconds 3
    $waited += 3
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:8080/api/actuator/health" `
                                  -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($resp.StatusCode -eq 200) { $started = $true }
    } catch { }
} while (-not $started -and $waited -lt $maxWait)

if ($started) {
    Write-Host "  ✓ Backend is UP → http://localhost:8080/api" -ForegroundColor Green
} else {
    Write-Host "  ✗ Backend did not start in time. Check logs\backend.log" -ForegroundColor Red
    Write-Host "  Tail of log:" -ForegroundColor Yellow
    if (Test-Path $backendLog) { Get-Content $backendLog | Select-Object -Last 20 }
    exit 1
}

# ── Step 4: Start Angular Frontend ────────────────────────────────────────────
Write-Host "[4/4] Starting Angular frontend (port 4200)..." -ForegroundColor Yellow
$frontendLog = Join-Path $ROOT "logs\frontend.log"

Start-Job -Name "BasaltFrontend" -ScriptBlock {
    param($dir, $log)
    Set-Location $dir
    npm start 2>&1 | Tee-Object -FilePath $log
} -ArgumentList $FRONTEND, $frontendLog | Out-Null

Write-Host "  ⏳ Waiting for Angular dev server (up to 45s)..." -ForegroundColor DarkGray
$maxWait = 45
$waited  = 0
$started = $false
do {
    Start-Sleep -Seconds 3
    $waited += 3
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:4200" `
                                  -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($resp.StatusCode -eq 200) { $started = $true }
    } catch { }
} while (-not $started -and $waited -lt $maxWait)

if ($started) {
    Write-Host "  ✓ Frontend is UP → http://localhost:4200" -ForegroundColor Green
} else {
    Write-Host "  ⚠ Frontend still compiling. Check logs\frontend.log" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "─────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "  🪨 Basalt is running!" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Frontend : http://localhost:4200" -ForegroundColor White
Write-Host "  Backend  : http://localhost:8080/api" -ForegroundColor White
Write-Host "  Health   : http://localhost:8080/api/actuator/health" -ForegroundColor White
Write-Host "  Ollama   : http://localhost:11434" -ForegroundColor White
Write-Host ""
Write-Host "  Logs     : .\logs\backend.log  |  .\logs\frontend.log" -ForegroundColor DarkGray
Write-Host "─────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host ""
Write-Host "Press Ctrl+C to stop all services." -ForegroundColor DarkGray

# Keep alive — clean up jobs on exit
try {
    while ($true) { Start-Sleep -Seconds 5 }
} finally {
    Write-Host "`nStopping Basalt jobs..." -ForegroundColor Yellow
    Get-Job -Name "BasaltBackend","BasaltFrontend" -ErrorAction SilentlyContinue | Stop-Job
    Get-Job -Name "BasaltBackend","BasaltFrontend" -ErrorAction SilentlyContinue | Remove-Job
    Write-Host "Done." -ForegroundColor Green
}

