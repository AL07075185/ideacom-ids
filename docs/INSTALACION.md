# ideacom IDS

**IDS: Infrastructure & Device Status.** Versión lite de una plataforma de monitoreo de disponibilidad. Un agente ligero en cada equipo se registra solo, envía un latido periódico a una API REST y avisa cuando el equipo se apaga. Un panel web muestra el estado, el historial, la disponibilidad, un mapa lógico de la red y las incidencias.

> Guía rápida para compilar y probar. La documentación completa (Fase IV) se agregará después.

## Arquitectura

```
VPS AWS (o tu equipo en local)
├── GlassFish 8 + Java 21
│   ├── ids-api.war   →  /ids-api/api/v1   REST: agentes, dispositivos, incidencias, catálogos
│   └── ids-web.war   →  /ids-web/         Panel: HTML + Bootstrap + JS + vis-network
└── MySQL 8 / MariaDB  →  base ideacom_ids (solo localhost)

Cada equipo monitoreado
└── ids-agente.jar  →  latido cada 60 s por HTTP(S), solo conexiones salientes
```

| Módulo | Contenido |
|---|---|
| `ids-common` | DTO compartidos, máquina de estados (`ReglasEstado`), cálculo de disponibilidad, pruebas JUnit |
| `ids-api` | Entidades JPA, repositorios, servicios, recursos JAX-RS, filtros de autenticación y de log, detector programado |
| `ids-web` | Páginas del panel |
| `ids-agente` | Agente sin dependencias externas (solo Java estándar) |
| `db/` | `schema.sql`, `datos-iniciales.sql`, `crear-usuario-bd.sql` |
| `deploy/` | GlassFish, Nginx, servicio Linux y tarea de Windows para el agente |
| `docs/` | Modelo de datos (normalización, ERD, diccionario) y contrato de la API |

## Requisitos

- **JDK 21**: Eclipse Temurin 21 o Amazon Corretto 21 (gratuitos; evita Oracle JDK 21 en producción, que ya requiere licencia).
- **Maven 3.9+**
- **GlassFish 8.0** (Jakarta EE 11)
- **MySQL 8.0** o **MariaDB 10.6+**
- **MySQL Connector/J** (`mysql-connector-j-*.jar`) para GlassFish

## 1. Compilar

```bash
mvn clean package
```

Genera:
- `ids-api/target/ids-api.war`
- `ids-web/target/ids-web.war`
- `ids-agente/target/ids-agente.jar`

Para importar en el IDE, abre la carpeta raíz como proyecto Maven (NetBeans: *File > Open Project*; IntelliJ: *Open* sobre `pom.xml`).

## 2. Base de datos

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/datos-iniciales.sql
mysql -u root -p < db/crear-usuario-bd.sql      # usuario ids / CambiaEstaClave_2026
```

Si cambias la contraseña del usuario `ids`, cámbiala también en `deploy/glassfish/glassfish-resources.xml`.

## 3. GlassFish

```bash
# Linux / macOS
deploy/glassfish/configurar-glassfish.sh /ruta/glassfish8 /ruta/mysql-connector-j-X.jar
# Windows
deploy\glassfish\configurar-glassfish.bat C:\glassfish8 C:\ruta\mysql-connector-j-X.jar
```

El script copia el driver, crea el pool `idsPool` y el recurso `jdbc/idsDS`, y despliega los dos WAR.
`asadmin` necesita Java en el `PATH` (o `JAVA_HOME`).

El driver se puede bajar con Maven: `mvn dependency:copy -Dartifact=com.mysql:mysql-connector-j:9.1.0 -DoutputDirectory=deploy/glassfish`.

**Alternativa sin script** (por ejemplo, con GlassFish administrado desde NetBeans):
1. Copia `mysql-connector-j-*.jar` a `glassfish/domains/domain1/lib/` y reinicia GlassFish.
2. En la consola `http://localhost:4848`: *Resources > JDBC > JDBC Connection Pools > New*:
   `idsPool`, `javax.sql.DataSource`, clase `com.mysql.cj.jdbc.MysqlDataSource`, y en *Additional Properties*
   `url`, `user` y `password` con los valores de `glassfish-resources.xml`. Presiona *Ping*.
3. *JDBC Resources > New*: `jdbc/idsDS` sobre `idsPool`.
4. Despliega `ids-api.war` e `ids-web.war` (*Applications > Deploy* o *Run* en NetBeans). Las rutas `/ids-api`
   e `/ids-web` las fija `WEB-INF/glassfish-web.xml`.

Abre **http://localhost:8080/ids-web/** y entra con:
- correo `admin@ideacom.local`
- contraseña `Admin123!` (cámbiala en *Administración > Usuarios*)

## 4. Agente

```bash
cp deploy/agente/agente.properties.ejemplo agente.properties    # ajusta api.url y codigo.alta
java -jar ids-agente/target/ids-agente.jar agente.properties
```

1. El equipo aparece en *Dispositivos* como **PENDIENTE**.
2. Apruébalo y asígnale nombre, departamento, responsable y conexión.
3. El agente guarda su token en `datos/agente-estado.properties`: no lo compartas ni lo subas a GitHub.

Instalación permanente (segundo plano):
- **Linux:** `deploy/agente/ids-agente.service` (systemd; las instrucciones están en el archivo).
- **Windows:** desde la carpeta del agente (por ejemplo `C:\ids-agente`), en PowerShell como administrador:
  `powershell -ExecutionPolicy Bypass -File .\instalar-agente-windows.ps1`
  (agrega `-JavaHome "C:\ruta\al\jdk"` si Java no está en el `PATH`).

Para otro equipo de la red: solo necesita Java 21 (JRE), `ids-agente.jar` y `agente.properties` con la IP del
servidor en `api.url`. No copies la carpeta `datos` (contiene el token del equipo original). En el servidor,
abre el puerto 8080 en el firewall.

## 5. Pruebas con máquinas virtuales

Cada VM lleva dos adaptadores de red:
- **Red interna o solo-anfitrión:** simula la LAN. El agente usa `api.url=http://<IP del anfitrión>:8080/ids-api/api/v1`.
- **NAT:** simula la salida a internet.

| Acción en la VM | Resultado esperado en el panel |
|---|---|
| Arrancar la VM con el agente instalado | Alta PENDIENTE; tras aprobar, EN_LINEA |
| Desconectar el cable del adaptador NAT | SIN_INTERNET en el siguiente latido |
| Volver a conectar NAT | EN_LINEA, evento *Internet restablecido* |
| Apagado normal (ACPI) | APAGADO, evento enviado por el agente |
| Apagado forzado o cable de la red interna desconectado | DESCONECTADO después de ~3 min (3 latidos perdidos) |
| Reconectar | RECONEXION y, si hubo corte, evento *Periodo sin comunicación* con inicio y fin |

## Funciones del panel

- **Dashboard, Dispositivos, Mapa, Incidencias, Administración.**
- **Exportar** dispositivos, incidencias e historial a CSV (Excel) o JSON.
- **Importar** dispositivos desde CSV o JSON, con vista previa, sin duplicar: se compara por MAC o por nombre dentro del sitio.
- **Respaldo y restauración** en *Administración*: un JSON portable que se fusiona en otra instalación, por ejemplo de local a AWS.
- **Reporte imprimible** (botón *Reporte*): Servlet + JSP + JSTL + etiquetas personalizadas, listo para guardar como PDF.

## Actualizar una instalación anterior

Si ya tenías la base creada con la versión anterior, ejecuta una sola vez:
```bash
mysql -u root -p < db/migraciones/001-ipv6-y-acentos.sql
```
Después vuelve a desplegar los WAR y reemplaza `ids-agente.jar` en los equipos (conservan su carpeta `datos`).

## 6. Producción en AWS (resumen)

1. Instalar Corretto 21, MariaDB/MySQL y GlassFish 8, y repetir los pasos 2 y 3.
2. Hacer que GlassFish escuche solo en localhost:
   `asadmin set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.address=127.0.0.1`
3. Nginx: copiar `deploy/nginx/ids.conf` e `ids-proxy-headers.inc` a `/etc/nginx/conf.d/` y ejecutar `sudo certbot --nginx -d ids.ideacom.mx`.
4. Security Group: abrir solo 22, 80 y 443. La consola de GlassFish (4848) se usa solo por túnel SSH.
5. En los agentes: `api.url=https://ids.ideacom.mx/ids-api/api/v1` y `perfil=produccion`.

## Pruebas unitarias

`mvn test` ejecuta las pruebas JUnit 5 de:
- `ReglasEstado`: transiciones y vencimiento de latidos.
- `DisponibilidadCalculadora`
- Fechas UTC
- `PasswordHasher`, tokens y el encabezado Bearer.
- JSON del agente, buffer de eventos, configuración y recolector.

## Roadmap

Lo que queda fuera de esta versión está en [`docs/roadmap.md`](docs/roadmap.md).

## Licencia

© 2026 ideacom. Todos los derechos reservados. Consulta el archivo [LICENSE](LICENSE).
