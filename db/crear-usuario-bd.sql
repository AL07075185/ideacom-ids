-- Ejecutar como root una sola vez. Cambia la contrasena antes de usarlo en AWS.
CREATE USER IF NOT EXISTS 'ids'@'localhost' IDENTIFIED BY 'CambiaEstaClave_2026';
GRANT SELECT, INSERT, UPDATE, DELETE ON ideacom_ids.* TO 'ids'@'localhost';
FLUSH PRIVILEGES;
