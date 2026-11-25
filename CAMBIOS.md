# Cambios — versión 1.1 (17 nov 2026)

Resumen de lo que cambió respecto a la primera versión, para revisarlo antes de pasar a producción.

## Correcciones acumuladas de la sesión
- `ConfigAgenteTest`: la prueba usaba `/tmp`, que no es ruta absoluta en Windows.
- `ids-agente/pom.xml`: filtro del `maven-shade-plugin` (sin el aviso de `MANIFEST.MF`).
- `WEB-INF/glassfish-web.xml` en `ids-api` e `ids-web`: fija las rutas `/ids-api` e `/ids-web`.
- `configurar-glassfish.bat` con saltos de línea de Windows; `.gitattributes` para que Git los conserve.
- `instalar-agente-windows.ps1`: parámetro `-JavaHome`.
- `.gitignore`: excluye el driver JDBC descargado.

## Textos y presentación
- Acentos y ñ en el panel, en los mensajes de la API y en los datos iniciales.
- Favicon (`favicon.svg`) en el panel y en el reporte.

## IPv6
- Columna `dispositivo.ipv6`; el agente la reporta al registrarse y en cada latido.
- Se muestra en la lista, el detalle y el formulario de equipos sin agente.
- **Base existente:** ejecutar `db/migraciones/001-ipv6-y-acentos.sql`.

## Estados
- El agente distingue la causa de un corte: *sin red local*, *sin internet* o *API no disponible*.
- Windows: al reiniciar, el agente lee el registro de eventos. Un apagado normal convierte la desconexión en
  *Apagado* (deja de contar como caída); un apagado inesperado queda anotado en el arranque.
- Un equipo importado o agregado a mano con la misma MAC es "adoptado" por su agente al registrarse.

## Exportar, importar y respaldar (desde el panel)
- *Dispositivos*: exportar CSV/JSON, importar CSV/JSON con vista previa, exportar historial de 30 días.
- *Incidencias*: exportar CSV/JSON con el filtro actual.
- *Administración > Respaldo y restauración*: respaldo completo en JSON (eventos opcionales) y restauración por fusión.

## Reporte de disponibilidad (JSP)
- Botón *Reporte* en la barra. `ReporteServlet` (controlador) + `reporte-disponibilidad.jsp` (vista, JSTL)
  + etiquetas personalizadas `<ids:estado>` y `<ids:porcentaje>` (`WEB-INF/ids.tld`).
- Acceso con ticket de un solo uso (60 s) canjeado por una sesión HTTP de 15 minutos.

## Roadmap
- La sonda de red y otras funciones quedan documentadas en `docs/roadmap.md`.

## Para actualizar tu instalación local
1. `mysql -u root -p < db/migraciones/001-ipv6-y-acentos.sql`
2. *Clean and Build* y volver a desplegar `ids-api` e `ids-web` (*Undeploy* + *Deploy*).
3. Reemplazar `ids-agente.jar` en cada equipo y reiniciar la tarea o el servicio (conserva la carpeta `datos`).
