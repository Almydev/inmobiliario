# Soluciones Inmobiliarias 360

Sistema web para generar cuentas de cobro (inquilinos), comprobantes de egreso (propietarios) y cuadre de banco, con envío por correo y históricos.

## Stack
- Backend: Java 21, Spring Boot, Spring Security (JWT), JPA, Flyway
- Frontend: React + Vite + Tailwind CSS
- BD: Neon (Postgres), una por ambiente
- Despliegue: Render/Railway, un servicio por rama

## Ramas
- `main`: producción. Solo recibe PR desde `qa`.
- `qa`: pruebas. Todo el trabajo entra aquí vía ramas `feat/*`.

## Alcance fase 1
Login y roles, propietarios, inquilinos, inmuebles (canon y % administración), cuentas de cobro, comprobantes de egreso, cuadre de banco, históricos y envío por correo con PDF. Contratos: fuera de alcance por ahora.

Referencia de formatos: `docs/MODELOS.xlsx`. Variables de entorno: `.env.example`.
