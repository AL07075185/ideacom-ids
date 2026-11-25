# Detiene y elimina la tarea del agente. Ejecutar como administrador.
$tarea = 'ideacom IDS agente'
Stop-ScheduledTask -TaskName $tarea -ErrorAction SilentlyContinue
Unregister-ScheduledTask -TaskName $tarea -Confirm:$false -ErrorAction SilentlyContinue
Write-Host 'Agente desinstalado.'
