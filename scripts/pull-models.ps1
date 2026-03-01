#!/usr/bin/env pwsh
# ─────────────────────────────────────────────────────────────────────────────
# Basalt — Pull Ollama Models
# Run this once after first `docker compose up -d`
# ─────────────────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "  Basalt — Pulling Ollama Models" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────" -ForegroundColor DarkGray

# Ensure Ollama container is running
$ollamaRunning = docker ps --filter "name=basalt-ollama" --filter "status=running" -q
if (-not $ollamaRunning) {
    Write-Host "  ✗ basalt-ollama container is not running." -ForegroundColor Red
    Write-Host "    Run: docker compose up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "  [1/2] Pulling nomic-embed-text (274 MB — embedding model)..." -ForegroundColor Yellow
docker exec basalt-ollama ollama pull nomic-embed-text
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✓ nomic-embed-text ready" -ForegroundColor Green
} else {
    Write-Host "  ✗ Failed to pull nomic-embed-text" -ForegroundColor Red
}

Write-Host ""
Write-Host "  [2/2] Pulling llama3.1 (~4.9 GB — chat model)..." -ForegroundColor Yellow
Write-Host "        This may take several minutes on first run." -ForegroundColor DarkGray
docker exec basalt-ollama ollama pull llama3.1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✓ llama3.1 ready" -ForegroundColor Green
} else {
    Write-Host "  ✗ Failed to pull llama3.1" -ForegroundColor Red
}

Write-Host ""
Write-Host "  Installed models:" -ForegroundColor DarkGray
docker exec basalt-ollama ollama list

Write-Host ""
Write-Host "  ✓ All models ready. You can now run start-basalt.ps1" -ForegroundColor Cyan
Write-Host ""

