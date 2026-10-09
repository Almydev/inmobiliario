-- Forzar el cambio de contrasena: el usuario inicial se comparte al entregar el sistema
ALTER TABLE usuarios ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE usuarios SET debe_cambiar_password = TRUE WHERE email = 'admin@inmobiliaria360.co';
