-- ideacom IDS - Esquema de base de datos (MySQL 8.0 / MariaDB 10.6+)
-- Normalizado a 3FN. 
-- Todas las fechas se guardan en UTC.

SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS ideacom_ids
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ideacom_ids;


-- Catalogos
CREATE TABLE rol (
  id_rol      TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(30)      NOT NULL,
  descripcion VARCHAR(150)     NULL,
  CONSTRAINT pk_rol PRIMARY KEY (id_rol),
  CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE departamento (
  id_departamento INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre          VARCHAR(80)  NOT NULL,
  CONSTRAINT pk_departamento PRIMARY KEY (id_departamento),
  CONSTRAINT uq_departamento_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE configuracion (
  clave       VARCHAR(60)  NOT NULL,
  valor       VARCHAR(255) NOT NULL,
  descripcion VARCHAR(200) NULL,
  CONSTRAINT pk_configuracion PRIMARY KEY (clave)
) ENGINE=InnoDB;

-- Usuarios y sesiones
CREATE TABLE usuario (
  id_usuario    INT UNSIGNED     NOT NULL AUTO_INCREMENT,
  nombre        VARCHAR(100)     NOT NULL,
  email         VARCHAR(150)     NOT NULL,
  password_hash VARCHAR(200)     NOT NULL,
  id_rol        TINYINT UNSIGNED NOT NULL,
  activo        BOOLEAN          NOT NULL DEFAULT TRUE,
  fecha_alta    DATETIME(3)      NOT NULL,
  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
  CONSTRAINT uq_usuario_email UNIQUE (email),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
) ENGINE=InnoDB;

CREATE TABLE sesion (
  id_sesion      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario     INT UNSIGNED    NOT NULL,
  token_hash     CHAR(64)        NOT NULL,
  fecha_creacion DATETIME(3)     NOT NULL,
  fecha_expira   DATETIME(3)     NOT NULL,
  CONSTRAINT pk_sesion PRIMARY KEY (id_sesion),
  CONSTRAINT uq_sesion_token UNIQUE (token_hash),
  CONSTRAINT fk_sesion_usuario FOREIGN KEY (id_usuario)
    REFERENCES usuario (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Sitios (redes o ubicaciones de un cliente)
CREATE TABLE sitio (
  id_sitio    INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(100) NOT NULL,
  direccion   VARCHAR(200) NULL,
  codigo_alta VARCHAR(40)  NOT NULL,
  activo      BOOLEAN      NOT NULL DEFAULT TRUE,
  fecha_alta  DATETIME(3)  NOT NULL,
  CONSTRAINT pk_sitio PRIMARY KEY (id_sitio),
  CONSTRAINT uq_sitio_codigo UNIQUE (codigo_alta)
) ENGINE=InnoDB;

-- Dispositivos
CREATE TABLE dispositivo (
  id_dispositivo   INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_sitio         INT UNSIGNED NOT NULL,
  id_departamento  INT UNSIGNED NULL,
  id_responsable   INT UNSIGNED NULL,
  id_conectado_a   INT UNSIGNED NULL,
  -- Datos administrativos (solo se editan desde el panel)
  nombre           VARCHAR(100) NOT NULL,
  tipo             VARCHAR(20)  NOT NULL,
  ubicacion        VARCHAR(120) NULL,
  notas            VARCHAR(500) NULL,
  -- Datos técnicos (los actualiza el agente)
  con_agente       BOOLEAN      NOT NULL,
  hostname         VARCHAR(100) NULL,
  ip               VARCHAR(45)  NULL,
  ipv6             VARCHAR(45)  NULL,
  mac              VARCHAR(17)  NULL,
  so_nombre        VARCHAR(80)  NULL,
  so_version       VARCHAR(80)  NULL,
  arquitectura     VARCHAR(30)  NULL,
  cpu_modelo       VARCHAR(150) NULL,
  ram_mb           BIGINT UNSIGNED NULL,
  disco_gb         BIGINT UNSIGNED NULL,
  version_agente   VARCHAR(20)  NULL,
  token_hash       CHAR(64)     NULL,
  -- Estado
  estado_registro  VARCHAR(15)  NOT NULL,
  estado_actual    VARCHAR(20)  NOT NULL,
  internet_ok      BOOLEAN      NULL,
  ultimo_latido    DATETIME(3)  NULL,
  fecha_alta       DATETIME(3)  NOT NULL,
  fecha_aprobacion DATETIME(3)  NULL,
  CONSTRAINT pk_dispositivo PRIMARY KEY (id_dispositivo),
  CONSTRAINT uq_dispositivo_sitio_mac UNIQUE (id_sitio, mac),
  CONSTRAINT uq_dispositivo_token UNIQUE (token_hash),
  CONSTRAINT fk_dispositivo_sitio FOREIGN KEY (id_sitio)
    REFERENCES sitio (id_sitio),
  CONSTRAINT fk_dispositivo_departamento FOREIGN KEY (id_departamento)
    REFERENCES departamento (id_departamento) ON DELETE SET NULL,
  CONSTRAINT fk_dispositivo_responsable FOREIGN KEY (id_responsable)
    REFERENCES usuario (id_usuario) ON DELETE SET NULL,
  CONSTRAINT fk_dispositivo_conectado_a FOREIGN KEY (id_conectado_a)
    REFERENCES dispositivo (id_dispositivo) ON DELETE SET NULL,
  CONSTRAINT ck_dispositivo_tipo CHECK (tipo IN
    ('PC','SERVIDOR','RED','CAMARA','NVR','IMPRESORA','OTRO')),
  CONSTRAINT ck_dispositivo_registro CHECK (estado_registro IN
    ('PENDIENTE','APROBADO','RECHAZADO')),
  CONSTRAINT ck_dispositivo_estado CHECK (estado_actual IN
    ('EN_LINEA','SIN_INTERNET','APAGADO','DESCONECTADO','SIN_AGENTE','DESCONOCIDO'))
) ENGINE=InnoDB;

CREATE INDEX ix_dispositivo_estado ON dispositivo (estado_registro, estado_actual);

-- Eventos (solo cambios de estado; los latidos NO se guardan como filas)
CREATE TABLE evento (
  id_evento         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_dispositivo    INT UNSIGNED    NOT NULL,
  tipo              VARCHAR(30)     NOT NULL,
  estado_resultante VARCHAR(20)     NULL,
  origen            VARCHAR(10)     NOT NULL,
  fecha_hora        DATETIME(3)     NOT NULL,
  fecha_fin         DATETIME(3)     NULL,
  detalle           VARCHAR(300)    NULL,
  fecha_registro    DATETIME(3)     NOT NULL,
  CONSTRAINT pk_evento PRIMARY KEY (id_evento),
  CONSTRAINT fk_evento_dispositivo FOREIGN KEY (id_dispositivo)
    REFERENCES dispositivo (id_dispositivo) ON DELETE CASCADE,
  CONSTRAINT ck_evento_origen CHECK (origen IN ('AGENTE','SERVIDOR','PANEL'))
) ENGINE=InnoDB;

CREATE INDEX ix_evento_dispositivo_fecha ON evento (id_dispositivo, fecha_hora);

-- Incidencias
CREATE TABLE incidencia (
  id_incidencia       INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_dispositivo      INT UNSIGNED NULL,
  id_creada_por       INT UNSIGNED NOT NULL,
  id_asignada_a       INT UNSIGNED NULL,
  titulo              VARCHAR(150) NOT NULL,
  descripcion         TEXT         NULL,
  prioridad           VARCHAR(10)  NOT NULL,
  estado              VARCHAR(15)  NOT NULL,
  fecha_creacion      DATETIME(3)  NOT NULL,
  fecha_actualizacion DATETIME(3)  NOT NULL,
  fecha_cierre        DATETIME(3)  NULL,
  CONSTRAINT pk_incidencia PRIMARY KEY (id_incidencia),
  CONSTRAINT fk_incidencia_dispositivo FOREIGN KEY (id_dispositivo)
    REFERENCES dispositivo (id_dispositivo) ON DELETE SET NULL,
  CONSTRAINT fk_incidencia_creada_por FOREIGN KEY (id_creada_por)
    REFERENCES usuario (id_usuario),
  CONSTRAINT fk_incidencia_asignada_a FOREIGN KEY (id_asignada_a)
    REFERENCES usuario (id_usuario) ON DELETE SET NULL,
  CONSTRAINT ck_incidencia_prioridad CHECK (prioridad IN ('BAJA','MEDIA','ALTA')),
  CONSTRAINT ck_incidencia_estado CHECK (estado IN ('ABIERTA','EN_PROCESO','CERRADA'))
) ENGINE=InnoDB;

CREATE INDEX ix_incidencia_estado ON incidencia (estado);
