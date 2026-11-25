# Instala el agente como tarea programada que arranca con Windows (sin ventana).
# Ejecutar en PowerShell COMO ADMINISTRADOR, desde la carpeta del agente:
#   powershell -ExecutionPolicy Bypass -File .\instalar-agente-windows.ps1
# Si Java no esta en el PATH, indica la carpeta del JDK:
#   powershell -ExecutionPolicy Bypass -File .\instalar-agente-windows.ps1 -JavaHome "C:\Program Files\Apache NetBeans\jdk"
param(
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$carpeta = Split-Path -Parent $MyInvocation.MyCommand.Path
$tarea = 'ideacom IDS agente'

# 1) Buscar javaw.exe: primero en -JavaHome / JAVA_HOME, despues en el PATH
$javaw = $null
if ($JavaHome -and (Test-Path (Join-Path $JavaHome 'bin\javaw.exe'))) {
    $javaw = Join-Path $JavaHome 'bin\javaw.exe'
} else {
    $javaw = (Get-Command javaw.exe -ErrorAction SilentlyContinue).Source
}
if (-not $javaw) {
    throw 'No se encontro javaw.exe. Usa -JavaHome "C:\ruta\al\jdk" o define JAVA_HOME.'
}

# 2) Verificar que esten el JAR y la configuracion
foreach ($archivo in 'ids-agente.jar', 'agente.properties') {
    if (-not (Test-Path (Join-Path $carpeta $archivo))) { throw "Falta $archivo en $carpeta" }
}

# 3) Registrar la tarea: al iniciar Windows, como SYSTEM, sin ventana
$accion = New-ScheduledTaskAction -Execute $javaw `
    -Argument "-Xmx64m -jar `"$carpeta\ids-agente.jar`" `"$carpeta\agente.properties`"" `
    -WorkingDirectory $carpeta
$disparador = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
# Sin limite de tiempo (por defecto Windows detiene tareas a las 72 h) y con reinicio automatico si falla
$ajustes = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 999 `
    -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries

Register-ScheduledTask -TaskName $tarea -Action $accion -Trigger $disparador -Principal $principal -Settings $ajustes -Force | Out-Null

# 4) Arrancarla ya, sin esperar al siguiente reinicio
Start-ScheduledTask -TaskName $tarea
Write-Host "Agente instalado e iniciado con $javaw"
Write-Host "Log: $carpeta\datos\agente.0.log"
