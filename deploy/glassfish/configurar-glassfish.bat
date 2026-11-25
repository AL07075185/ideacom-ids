@echo off
REM Configura GlassFish 8 para ideacom IDS en Windows.
REM Uso: configurar-glassfish.bat C:\glassfish8 C:\descargas\mysql-connector-j-X.jar
setlocal
if "%~2"=="" (
  echo Uso: %~nx0 RUTA_GLASSFISH RUTA_DRIVER_MYSQL
  exit /b 1
)
set GF_HOME=%~1
set DRIVER=%~2
set AQUI=%~dp0
set ASADMIN=%GF_HOME%\bin\asadmin.bat

echo 1) Copiando el driver JDBC al dominio
copy /Y "%DRIVER%" "%GF_HOME%\glassfish\domains\domain1\lib\" || exit /b 1

echo 2) Reiniciando el dominio
call "%ASADMIN%" restart-domain domain1 || call "%ASADMIN%" start-domain domain1

echo 3) Creando pool y recurso JDBC
call "%ASADMIN%" add-resources "%AQUI%glassfish-resources.xml"
call "%ASADMIN%" ping-connection-pool idsPool || exit /b 1

echo 4) Desplegando los WAR
call "%ASADMIN%" deploy --force=true "%AQUI%..\..\ids-api\target\ids-api.war" || exit /b 1
call "%ASADMIN%" deploy --force=true "%AQUI%..\..\ids-web\target\ids-web.war" || exit /b 1

echo Listo. Panel: http://localhost:8080/ids-web/
endlocal
