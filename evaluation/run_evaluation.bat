@echo off
chcp 65001 > nul
echo =======================================================
echo Ejecutando Banco de Pruebas de Tesis: Calculo de WER
echo =======================================================
python "%~dp0benchmark_runner.py"
pause
