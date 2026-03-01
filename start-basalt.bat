@echo off
:: ─────────────────────────────────────────────────────────────────────────────
:: Basalt — Windows batch launcher
:: Double-click this file to start the full Basalt stack in a new PS window
:: ─────────────────────────────────────────────────────────────────────────────
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-basalt.ps1"
pause

