# Roadmap — ideacom IDS

Funciones planeadas para versiones posteriores (fuera del alcance de la versión GA del proyecto integrador).

| Función | Descripción |
|---|---|
| **Agente sonda** (`ids-sonda`) | Módulo independiente que barre la subred, descubre equipos y vigila los que no tienen agente (cámaras, NVR, switches, impresoras) por TCP o ICMP |
| Detección de topología | Construir el mapa automáticamente con LLDP/SNMP |
| Notificaciones | Avisos por correo o Telegram cuando un equipo se desconecta |
| Rol de técnico | Usuario que solo atiende las incidencias que tiene asignadas |
| Instalador del agente | Paquete `jpackage`/`jlink` con su propio Java y registro como servicio de Windows |
| Respaldos automáticos | Copia programada de la base de datos en el servidor (snapshot de AWS o `mysqldump`) |
| Monitoreo en contenedores | Imagen Docker del agente para servidores en la nube |
