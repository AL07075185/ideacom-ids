#!/usr/bin/env bash
# Configura GlassFish 8 para ideacom IDS (Linux / macOS / AWS).
# Uso: ./configurar-glassfish.sh /ruta/a/glassfish8 /ruta/a/mysql-connector-j-X.jar
set -euo pipefail
GF_HOME="${1:?Indica la carpeta de GlassFish (la que contiene bin/asadmin)}"
DRIVER="${2:?Indica la ruta del driver mysql-connector-j-*.jar}"
AQUI="$(cd "$(dirname "$0")" && pwd)"
ASADMIN="$GF_HOME/bin/asadmin"

echo "1) Copiando el driver JDBC al dominio"
cp "$DRIVER" "$GF_HOME/glassfish/domains/domain1/lib/"

echo "2) Reiniciando el dominio para cargar el driver"
"$ASADMIN" restart-domain domain1 || "$ASADMIN" start-domain domain1

echo "3) Creando pool y recurso JDBC (jdbc/idsDS)"
"$ASADMIN" add-resources "$AQUI/glassfish-resources.xml" || echo "   (ya existian, se continua)"
"$ASADMIN" ping-connection-pool idsPool

echo "4) Desplegando los WAR"
"$ASADMIN" deploy --force=true "$AQUI/../../ids-api/target/ids-api.war"
"$ASADMIN" deploy --force=true "$AQUI/../../ids-web/target/ids-web.war"

echo "Listo. Panel: http://localhost:8080/ids-web/   API: http://localhost:8080/ids-api/api/v1"
