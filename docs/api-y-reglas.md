# Contrato de la API y reglas de negocio — ideacom IDS

Base: `/ids-api/api/v1` · JSON UTF-8 · fechas ISO-8601 en UTC (`2026-11-20T15:04:05.123Z`).

## Autenticación

| Quién | Cómo | Encabezado |
|---|---|---|
| Panel (usuarios) | `POST /auth/login` devuelve un token de sesión (8 h) | `Authorization: Bearer <token>` |
| Agente | `POST /agentes/registro` con el código de alta del sitio devuelve un token permanente | `Authorization: Bearer <token>` |

Los tokens se guardan en la base de datos solo como hash SHA-256.

## Endpoints

### Agente
| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| POST | `/agentes/registro` (público) | `RegistroAgenteRequest` | 200 `RegistroAgenteResponse` · 403 código inválido o equipo rechazado |
| POST | `/agentes/latido` | `LatidoRequest` | 200 `LatidoResponse` · 401 token inválido |
| POST | `/agentes/eventos` | lista de `EventoAgenteDTO` (ARRANQUE, APAGADO, SIN_CONEXION_SERVIDOR) | 204 |

### Panel
| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/auth/login` | público | `{email, password}` → `{token, nombre, rol, expira}` |
| POST | `/auth/logout` | cualquiera | Cierra la sesión |
| GET | `/auth/yo` | cualquiera | Datos del usuario actual |
| GET | `/dashboard/resumen` | cualquiera | Conteos por estado, pendientes, incidencias abiertas, disponibilidad 24 h, últimos eventos |
| GET | `/dispositivos?registro=PENDIENTE` | cualquiera* | Lista (el usuario solo ve los suyos) |
| GET | `/dispositivos/{id}` | cualquiera* | Detalle |
| POST | `/dispositivos` | ADMIN | Alta manual de un equipo sin agente (switch, router, cámara) |
| PUT | `/dispositivos/{id}` | ADMIN | Edita campos administrativos |
| POST | `/dispositivos/{id}/aprobar` · `/rechazar` | ADMIN | Aprobación del alta automática |
| DELETE | `/dispositivos/{id}` | ADMIN | Baja |
| GET | `/dispositivos/{id}/eventos?limite=50` | cualquiera* | Historial |
| GET | `/dispositivos/{id}/disponibilidad?horas=24` | cualquiera* | Porcentaje de disponibilidad |
| GET | `/dispositivos/mapa` | cualquiera* | Nodos y conexiones para vis-network |
| GET / POST | `/incidencias` | cualquiera* | Listar / levantar |
| PUT | `/incidencias/{id}` | ADMIN | Cambiar estado, prioridad o asignación |
| GET / POST / DELETE | `/catalogos/departamentos` | GET cualquiera, resto ADMIN | |
| GET / POST | `/catalogos/sitios` | ADMIN | El código de alta se genera solo |
| GET / POST / PUT | `/catalogos/usuarios` | ADMIN | Alta, activar/desactivar, cambio de contraseña |
| GET | `/datos/dispositivos?formato=csv\|json` | cualquiera* | Exporta la lista (formato portable) |
| POST | `/datos/dispositivos/importar?simular=true\|false` | ADMIN | Importa CSV (`text/csv`) o JSON; `simular=true` = vista previa |
| GET | `/datos/dispositivos/{id}/eventos?formato=csv&dias=30` | cualquiera* | Exporta el historial |
| GET | `/datos/incidencias?formato=csv\|json` | cualquiera* | Exporta incidencias |
| GET | `/datos/respaldo?eventos=true\|false` | ADMIN | Respaldo completo en JSON |
| POST | `/datos/respaldo/restaurar` | ADMIN | Fusiona un respaldo (no borra datos) |
| POST | `/reportes/ticket` | cualquiera | Ticket de un solo uso (60 s) para abrir el reporte JSP |

### Reporte JSP (fuera de la API REST)
`GET /ids-api/reportes/disponibilidad?ticket=...&horas=24|168|720&sitio=&tz=America/Monterrey`

Servlet (`ReporteServlet`) + JSP con JSTL (`/WEB-INF/vistas/reporte-disponibilidad.jsp`) + etiquetas
personalizadas `<ids:estado>` e `<ids:porcentaje>` (`/WEB-INF/ids.tld`). El ticket se canjea por una sesión HTTP
de 15 minutos y la URL se limpia, para poder recargar o imprimir.

\* El rol USUARIO solo ve los dispositivos donde es responsable y las incidencias que creó.

## Reglas de negocio

| Regla | Valor | Dónde se configura |
|---|---|---|
| Intervalo del latido | 60 s | `configuracion.latido.intervalo.segundos` |
| Umbral de desconexión | 3 latidos perdidos (≈ 3 min) | `configuracion.latido.umbral` |
| Revisión del detector | cada 30 s | `DetectorDesconexiones` |
| Duración de sesión | 8 h | `configuracion.sesion.horas` |

### Máquina de estados del dispositivo

| Estado actual | Ocurre | Nuevo estado | Evento registrado |
|---|---|---|---|
| DESCONOCIDO, APAGADO, DESCONECTADO | Llega un latido | EN_LINEA o SIN_INTERNET | RECONEXION |
| EN_LINEA | Latido con `internetOk=false` | SIN_INTERNET | SIN_INTERNET |
| SIN_INTERNET | Latido con `internetOk=true` | EN_LINEA | INTERNET_RESTABLECIDO |
| EN_LINEA, SIN_INTERNET | El agente avisa apagado | APAGADO | APAGADO |
| EN_LINEA, SIN_INTERNET | Pasan 3 intervalos sin latido | DESCONECTADO | DESCONEXION_INESPERADA |
| APAGADO | Pasan 3 intervalos sin latido | APAGADO (sin cambio) | — |

Solo los dispositivos **APROBADOS** se vigilan. Los equipos agregados a mano quedan como `SIN_AGENTE`.

### Causa de un periodo sin comunicación (agente)
Cuando el agente no logra enviar el latido, clasifica la causa y la reporta al volver (evento `SIN_CONEXION_SERVIDOR`):

| Causa | Cómo la detecta |
|---|---|
| Sin red local | Ninguna interfaz activa con dirección válida (cable o WiFi desconectado) |
| Sin internet | Hay red, pero no responde ningún destino de `internet.hosts` |
| API no disponible | Hay red e internet, pero el servidor no responde |

El estado **SIN_INTERNET** en el panel solo aparece cuando el servidor sí recibe el latido pero el equipo no
tiene internet (por ejemplo, una VM con un adaptador hacia el servidor y otro NAT desconectado). Si el equipo
pierde toda la red, el servidor no recibe nada y lo marca **DESCONECTADO**.

### Apagado en Windows
Al apagar Windows, el Programador de tareas termina el agente sin dejarlo avisar, y el servidor registra una
desconexión inesperada. Al arrancar, el agente consulta el registro de eventos de Windows:
- **6006 / 1074 (apagado normal)** posterior al último contacto: envía `APAGADO` con la fecha real y el servidor
  reclasifica esa desconexión como apagado (deja de contar como caída).
- **6008 (apagado inesperado)**: lo anota en el evento de arranque; la desconexión se mantiene.

En Linux (systemd) el aviso de apagado se envía en el momento.

### Disponibilidad
`disponibilidad = tiempo (EN_LINEA + SIN_INTERNET) / tiempo (EN_LINEA + SIN_INTERNET + DESCONECTADO)`

Un apagado avisado **no** cuenta como caída: se excluye del cálculo, igual que el tiempo en estado desconocido.
