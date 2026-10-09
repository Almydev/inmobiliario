# Pruebas

## Automáticas (backend)

```bash
cd backend
./mvnw test
```

| Clase | Qué cubre |
|---|---|
| `NumeroALetrasTest` | Valor en letras de los PDF (casos borde, redondeo, negativos). |
| `PdfServiceTest` | El PDF es válido, contiene los datos correctos, soporta caracteres especiales y **rendimiento**: 300 PDF en paralelo. |
| `MailServiceTest` | Envío SMTP real contra un servidor en memoria (GreenMail): destinatario, asunto y PDF adjunto. Nunca sale un correo real. |
| `LoginThrottleTest` | Bloqueo por fuerza bruta: 5 fallos, expiración y reinicio. |
| `ComprobanteEgresoServiceTest` | Cálculo del egreso con el ejemplo real del Excel (3.500.000 x 15 días, 10 % = 1.575.000), redondeo, descuentos, duplicados, pago al banco y PDF. |
| `BancoServiceTest` | Saldo acumulado con los movimientos reales del Excel (saldo final 932.440), saldo inicial arrastrado, movimientos manuales, borrado solo de manuales y CSV a prueba de inyección de fórmulas. |
| `ConcurrenciaTest` | **Multiples clics**: 20 peticiones simultáneas sobre el mismo documento (H2 en memoria, nunca toca Neon). Un solo pago, un solo correo, un solo documento. Antes del arreglo fallaban las 5. |
| `SeguridadApiTest` | **Seguridad**: sin token, token manipulado/expirado/`alg:none`/firmado con otra clave, roles, validación de entradas (inyección en periodo, tope de lote), CORS, usuario inactivo, cabeceras, mensajes que no revelan si el usuario existe. |
| `JwtServiceTest` | Emisión y validación del token. |

## Correo en desarrollo: Mailpit

Mailpit captura los correos en una bandeja local; nadie los recibe.

1. Descarga `mailpit-windows-amd64.zip` desde https://github.com/axllent/mailpit/releases y ejecútalo.
2. Bandeja: http://localhost:8025. El backend ya apunta a `localhost:1025` por defecto.
3. Crea un inquilino con **cualquier** correo (por ejemplo `prueba@ejemplo.com`), genera la cuenta de cobro y pulsa **Enviar**. El correo con el PDF aparece en Mailpit.

Para `qa`/producción define `MAIL_HOST`, `MAIL_PORT`, `MAIL_AUTH`, `MAIL_TLS`, `MAIL_USER`, `MAIL_PASSWORD` y `MAIL_FROM`.

## Rendimiento contra un servidor en marcha

```bash
python docs/pruebas/carga.py --url http://localhost:8080 --email USUARIO --password CLAVE --hilos 20 --peticiones 400
```

Reporta peticiones por segundo, latencia p50/p95/p99 y errores para: health, listados y generación de PDF.
Úsalo solo contra local o `qa`. El plan gratuito de Render duerme el servicio: la primera petición puede tardar ~1 minuto.

## Pendiente (siguiente iteración)

- Pruebas de integración contra Postgres real (Testcontainers) para consecutivos concurrentes y la restricción de unicidad por periodo.
- Análisis de dependencias vulnerables (`./mvnw org.owasp:dependency-check-maven:check`) y revisión OWASP ZAP sobre `qa`.
- Pruebas de interfaz extremo a extremo (Playwright).

## Datos de ejemplo (QA)

`DatosDemo` carga 5 registros por proceso usando los mismos servicios del sistema (consecutivos, cálculos y movimientos de banco reales). Está apagado por defecto.

```bash
cd backend
SEED_DEMO=true SERVER_PORT=8099 ./mvnw spring-boot:run          # cargar (idempotente: si ya existen, solo imprime el resumen)
SEED_DEMO_LIMPIAR=true SERVER_PORT=8099 ./mvnw spring-boot:run  # borrar todo lo demo
```

Detén el proceso al ver `RESUMEN` / `Datos demo cargados`. Lo demo se identifica por el correo `@ejemplo.test` y el prefijo `[DEMO]` en los movimientos manuales. **No activar en producción.**
