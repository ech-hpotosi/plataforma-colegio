-- Bloqueo temporal de la cuenta despues de varios intentos fallidos de inicio de sesion.
ALTER TABLE usuario ADD COLUMN bloqueado_hasta TIMESTAMP;
