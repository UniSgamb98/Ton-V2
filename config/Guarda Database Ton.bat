@echo off
setlocal

set "PS1=%TEMP%\ij_embedded.ps1"

(
  echo $ErrorActionPreference = 'Stop'
  echo $p = Join-Path $env:TEMP 'ij-auto.properties'
  echo Set-Content -Path $p -Value "ij.database=jdbc:derby:C:\\TonDatabase;user=APP;password=pw" -Encoding ASCII
  echo Set-Location "C:\Apache\db-derby-10.17.1.0-bin\lib"
  echo java -jar .\derbyrun.jar ij -p $p
) > "%PS1%"

start "Derby IJ (embedded)" powershell -NoExit -ExecutionPolicy Bypass -File "%PS1%"

endlocal
