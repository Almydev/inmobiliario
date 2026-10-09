import json
from http.server import BaseHTTPRequestHandler, HTTPServer

RANGOS = [("Al día", 2, 5250000), ("1 a 30 días", 3, 4930000), ("31 a 60 días", 1, 1650000), ("61 a 90 días", 1, 1500000), ("Más de 90 días", 1, 1800000)]

PANEL = {
    "periodo": "2026-10",
    "cobro": {"emitidas": 5, "valorEmitido": 11230000, "pagadas": 2, "valorRecaudado": 4180000, "porCobrar": 3,
              "valorPorCobrar": 7050000, "sinEnviar": 2, "porcentajeRecaudo": 37},
    "egresos": {"emitidos": 5, "valorTotal": 7760000, "pagados": 1, "valorPagado": 2250000, "porPagar": 4, "valorPorPagar": 5510000},
    "banco": {"saldoFinal": 2408390, "ingresos": 4698250, "gastos": 2289860},
    "cartera": {"totalPendiente": 15130000, "totalVencido": 9880000, "cuentasPendientes": 8, "inquilinosEnMora": 5,
                "rangos": [{"etiqueta": e, "cuentas": c, "valor": v} for e, c, v in RANGOS], "inquilinos": []},
    "inmueblesSinCuenta": 1,
    "propietariosPorPagar": 1,
    "valorPropietariosPorPagar": 1650000,
    "alertas": [
        {"nivel": "alta", "texto": "5 inquilino(s) en mora por un total vencido de $9880000", "enlace": "/cartera"},
        {"nivel": "media", "texto": "1 inmueble(s) con inquilino aún sin cuenta de cobro este mes", "enlace": "/cuentas-cobro"},
        {"nivel": "media", "texto": "2 cuenta(s) de cobro en borrador sin enviar", "enlace": "/cuentas-cobro"},
        {"nivel": "media", "texto": "1 propietario(s) por pagar: el inquilino ya pagó y falta el comprobante de egreso", "enlace": "/comprobantes-egreso"},
        {"nivel": "baja", "texto": "4 comprobante(s) de egreso pendiente(s) de pago", "enlace": "/comprobantes-egreso"},
    ],
    "ultimosMovimientos": [
        {"id": 8, "fecha": "2026-10-09", "concepto": "pago de arriendo Casa Pinolinda (Inversiones Altos del Retiro)", "ingreso": 0, "gasto": 2250000},
        {"id": 7, "fecha": "2026-10-09", "concepto": "pago de arriendo Apartamento Ara 301 (Camilo Paternina)", "ingreso": 1680000, "gasto": 0},
        {"id": 6, "fecha": "2026-10-09", "concepto": "pago de arriendo Casa Pinolinda (Angie Flórez)", "ingreso": 2500000, "gasto": 0},
        {"id": 5, "fecha": "2026-10-06", "concepto": "[DEMO] cuota de manejo tarjeta débito", "ingreso": 0, "gasto": 19900},
    ],
}


def cuenta(i, n, inm, per, total, est, venc, mora):
    return {"id": i, "consecutivo": n, "periodo": per, "inmueble": inm, "total": total, "estado": est, "vencimiento": venc, "diasMora": mora}


CARTERA = {
    "totalPendiente": 15130000, "totalVencido": 9880000, "cuentasPendientes": 8, "inquilinosEnMora": 5,
    "rangos": PANEL["cartera"]["rangos"],
    "inquilinos": [
        {"inquilinoId": 1, "nombre": "Fabiola Sarmiento Gómez", "email": "fabiola.sarmiento@ejemplo.test", "telefono": "3100000005",
         "totalPendiente": 3300000, "totalVencido": 3300000, "maxDiasMora": 98,
         "cuentas": [cuenta(1, 41, "Casa Villalaura", "2026-07", 1500000, "ENVIADO", "2026-07-05", 96),
                     cuenta(2, 52, "Casa Villalaura", "2026-10", 1800000, "BORRADOR", "2026-10-05", 4)]},
        {"inquilinoId": 2, "nombre": "Esteban Rincón Osorio", "email": "esteban.rincon@ejemplo.test", "telefono": "3100000001",
         "totalPendiente": 1750000, "totalVencido": 1750000, "maxDiasMora": 35,
         "cuentas": [cuenta(3, 48, "Apartamento 402 Portobello", "2026-09", 1750000, "ENVIADO", "2026-09-05", 35)]},
        {"inquilinoId": 3, "nombre": "Gabriel Vargas Zuluaga", "email": None, "telefono": None,
         "totalPendiente": 3500000, "totalVencido": 3500000, "maxDiasMora": 4,
         "cuentas": [cuenta(4, 5, "Local Comercial Centro", "2026-10", 3500000, "ENVIADO", "2026-10-05", 4)]},
        {"inquilinoId": 4, "nombre": "Ana Gómez", "email": "ana@ejemplo.test", "telefono": "3000000009",
         "totalPendiente": 1200000, "totalVencido": 0, "maxDiasMora": 0,
         "cuentas": [cuenta(5, 60, "Apto 101", "2026-11", 1200000, "BORRADOR", "2026-11-05", 0)]},
    ],
}


class H(BaseHTTPRequestHandler):
    def do_GET(self):
        ruta = self.path.split("?")[0]
        datos = {"/api/auth/me": {"id": 1, "email": "admin@x.co", "nombre": "Administrador", "rol": "ADMIN", "activo": True, "debeCambiarPassword": False},
                 "/api/panel": PANEL, "/api/cartera": CARTERA}.get(ruta)
        if datos is None and ruta == "/api/banco":
            datos = {"periodo": "2026-10", "saldoInicial": 0, "totalIngresos": 0, "totalGastos": 0, "totalAdministracion": 0, "saldoFinal": 0, "movimientos": []}
        if datos is None:
            datos = []
        cuerpo = json.dumps(datos).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)

    def log_message(self, *a):
        pass


HTTPServer(("127.0.0.1", 8080), H).serve_forever()
