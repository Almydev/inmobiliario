-- Usuario administrador inicial. Cambiar la contrasena apenas se ingrese.
INSERT INTO usuarios (email, password_hash, nombre, rol, activo)
VALUES ('admin@inmobiliaria360.co', '$2a$10$NNhnGEvWAD/BviruNj/8DOwGJjp0jJRpaBnRMa7cL.kldsvkWglT2', 'Administrador', 'ADMIN', TRUE)
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash, activo = TRUE, rol = 'ADMIN';
