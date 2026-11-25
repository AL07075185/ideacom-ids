-- Migración 001 (para bases creadas con la versión anterior)
-- Agrega la columna IPv6 y corrige acentos de los datos iniciales.
SET NAMES utf8mb4;
USE ideacom_ids;

ALTER TABLE dispositivo ADD COLUMN ipv6 VARCHAR(45) NULL AFTER ip;

UPDATE departamento SET nombre = 'Administración' WHERE nombre = 'Administracion';
UPDATE departamento SET nombre = 'Recepción'      WHERE nombre = 'Recepcion';
UPDATE configuracion SET descripcion = 'Cada cuántos segundos reporta el agente' WHERE clave = 'latido.intervalo.segundos';
UPDATE configuracion SET descripcion = 'Latidos perdidos antes de marcar desconexión' WHERE clave = 'latido.umbral';
UPDATE configuracion SET descripcion = 'Duración de la sesión del panel' WHERE clave = 'sesion.horas';
