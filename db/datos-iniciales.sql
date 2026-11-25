SET NAMES utf8mb4;
USE ideacom_ids;

INSERT INTO rol (id_rol, nombre, descripcion) VALUES
  (1, 'ADMIN',   'Administra dispositivos, usuarios, sitios e incidencias'),
  (2, 'USUARIO', 'Consulta sus equipos asignados y levanta incidencias');

INSERT INTO configuracion (clave, valor, descripcion) VALUES
  ('latido.intervalo.segundos', '60', 'Cada cuántos segundos reporta el agente'),
  ('latido.umbral',             '3',  'Latidos perdidos antes de marcar desconexión'),
  ('sesion.horas',              '8',  'Duración de la sesión del panel');

INSERT INTO usuario (nombre, email, password_hash, id_rol, activo, fecha_alta) VALUES
  ('Administrador', 'admin@domain.local',
   'pbkdf2_sha256$210000$Wzw074IG5qLGOzmb1/8Ggw==$728V9drXGqXQfcelhtm7rKxVndi6ANxiz/+MQ2w2gxE=',
   1, TRUE, UTC_TIMESTAMP(3));

INSERT INTO departamento (nombre) VALUES ('Sistemas'), ('Administración'), ('Recepción'), ('Seguridad');

-- Sitio de pruebas. El agente usa este código de alta para registrarse.
INSERT INTO sitio (nombre, direccion, codigo_alta, activo, fecha_alta) VALUES
  ('Oficina ideacom (pruebas)', 'Red local de pruebas', 'IDS-DEMO-2026', TRUE, UTC_TIMESTAMP(3));
