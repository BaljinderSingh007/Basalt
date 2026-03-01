#!/usr/bin/env pwsh
# ─────────────────────────────────────────────────────────────────────────────
# Basalt — Stop Script
# Kills backend/frontend jobs and optionally stops Docker containers
# ─────────────────────────────────────────────────────────────────────────────

$ROOT = Split-Path -Parent $PSScriptRoot

Write-Host ""
Write-Host "  Stopping Basalt..." -ForegroundColor Yellow

# Stop PowerShell background jobs
Get-Job -Name "BasaltBackend","BasaltFrontend" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Stopping job: $($_.Name)" -ForegroundColor DarkGray
    $_ | Stop-Job
    $_ | Remove-Job
}

# Kill any stray Java processes on port 8080
$pid8080 = netstat -ano 2>$null | Select-String ":8080 " | ForEach-Object {
    ($_ -split '\s+')[-1]
} | Select-Object -First 1
if ($pid8080) {
    Write-Host "  Killing process on :8080 (PID $pid8080)" -ForegroundColor DarkGray
    Stop-Process -Id $pid8080 -Force -ErrorAction SilentlyContinue
}

# Kill any stray node processes on port 4200
$pid4200 = netstat -ano 2>$null | Select-String ":4200 " | ForEach-Object {
    ($_ -split '\s+')[-1]
} | Select-Object -First 1
if ($pid4200) {
    Write-Host "  Killing process on :4200 (PID $pid4200)" -ForegroundColor DarkGray
    Stop-Process -Id $pid4200 -Force -ErrorAction SilentlyContinue
}

# Ask whether to stop Docker containers too
Write-Host ""
$ans = Read-Host "  Stop Docker containers (Postgres + Ollama)? [y/N]"
if ($ans -match '^[Yy]') {
    Set-Location $ROOT
    docker compose down
    Write-Host "  ✓ Docker containers stopped" -ForegroundColor Green
}

Write-Host ""
Write-Host "  ✓ Basalt stopped." -ForegroundColor Cyan
Write-Host ""

