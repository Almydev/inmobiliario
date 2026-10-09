"""API simulada con estado, solo para grabar el video de demostracion.

Sirve datos ficticios coherentes (los mismos de la carga demo de QA) y reacciona a las acciones del video:
generar cuentas, enviar, marcar pagada, generar egresos, movimientos de banco. No usa base de datos.
Uso: python servidor_demo.py   (escucha en 127.0.0.1:8080)
"""
import json
import re
from datetime import date
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

HOY = date(2026, 10, 9)
PERIODO = "2026-10"
VENCE = date(2026, 10, 5)

PROPIETARIOS = [
    (1, "Marta Elena Ruiz Londoño", "900000001", "marta.ruiz", "Bancolombia", "Ahorros", "00011100001"),
    (2, "Carlos Andrés Mejía Torres", "900000002", "carlos.mejia", "Davivienda", "Ahorros", "00011100002"),
    (3, "Inversiones Altos del Retiro S.A.S.", "900000003", "altos.retiro", "Bancolombia", "Corriente", "00011100003"),
    (4, "Gloria Patricia Henao Giraldo", "900000004", "gloria.henao", "Nequi", "Ahorros", "3000000004"),
    (5, "Jorge Iván Ospina Cardona", "900000005", "jorge.ospina", "Banco de Bogotá", "Ahorros", "00011100005"),
]
INQUILINOS = [
    (1, "Esteban Rincón Osorio", "910000001", "esteban.rincon", "Calle 17 # 21-45"),
    (2, "Camilo Paternina Vélez", "910000002", "camilo.paternina", "Carrera 9 # 12-30"),
    (3, "Angie Tatiana Flórez Calle", "910000003", "angie.florez", "Calle 20 # 18-11"),
    (4, "Gabriel Vargas Zuluaga", "910000004", "gabriel.vargas", "Carrera 15 # 8-62"),
    (5, "Fabiola Sarmiento Gómez", "910000005", "fabiola.sarmiento", "Calle 12 # 25-07"),
]
# id, descripcion, direccion, propietario, inquilino, canon
INMUEBLES = [
    (1, "Apartamento 402 Portobello", "Carrera 10 # 5-20", 1, 1, 1750000),
    (2, "Casa Villalaura", "Calle 15 # 22-40", 2, 5, 1500000),
    (3, "Casa Pinolinda", "Carrera 21 # 16-29", 3, 3, 2500000),
    (4, "Apartamento Ara 301", "Calle 27 # 9-112", 4, 2, 1650000),
    (5, "Local Comercial Centro", "Carrera 20 # 17-05", 5, 4, 3500000),
]

ESTADO = {}


def reiniciar():
    global ESTADO
    ESTADO = {
        "cuentas": [
            cuenta(1, 1, "BORRADOR", 0),
            cuenta(2, 2, "ENVIADO", 300000),
            cuenta(3, 3, "PAGADO", 0),
            cuenta(4, 4, "PAGADO", 0),
        ],
        "egresos": [egreso(1, 3, "PAGADO")],
        "banco": [
            mov(1, "2026-10-02", "pago de gravamen 4x1000", 0, 7560),
            mov(2, "2026-10-05", "pago de arriendo Casa Pinolinda (Angie Tatiana Flórez Calle)", 2500000, 0, origen="Cuenta de cobro 3"),
            mov(3, "2026-10-06", "pago de arriendo Apartamento Ara 301 (Camilo Paternina Vélez)", 1650000, 0, origen="Cuenta de cobro 4"),
            mov(4, "2026-10-07", "pago de arriendo Casa Pinolinda (Inversiones Altos del Retiro S.A.S.)", 0, 2250000, origen="Egreso 1"),
        ],
        "usuarios": [
            {"id": 1, "email": "admin@inmobiliaria360.co", "nombre": "Yaned Gómez", "rol": "ADMIN", "activo": True, "debeCambiarPassword": False},
            {"id": 2, "email": "operador@inmobiliaria360.co", "nombre": "Laura Restrepo", "rol": "OPERADOR", "activo": True, "debeCambiarPassword": False},
        ],
        "sig": {"cuenta": 5, "egreso": 2, "mov": 5},
    }


def inm(i):
    return next(x for x in INMUEBLES if x[0] == i)


def cuenta(consec, inmueble_id, estado, admin):
    _, desc, _, prop, inq, canon = inm(inmueble_id)
    return {
        "id": consec, "consecutivo": consec, "fecha": "2026-10-01", "periodo": PERIODO,
        "inmuebleId": inmueble_id, "inmueble": desc, "inquilinoId": inq,
        "inquilino": next(x[1] for x in INQUILINOS if x[0] == inq),
        "inquilinoEmail": next(x[3] for x in INQUILINOS if x[0] == inq) + "@ejemplo.test",
        "propietario": next(x[1] for x in PROPIETARIOS if x[0] == prop),
        "concepto": "PAGO DE ARRENDAMIENTO DEL MES DE OCTUBRE DE 2026",
        "valorArriendo": canon, "valorAdministracion": admin, "otros": 0, "total": canon + admin,
        "estado": estado, "enviadoEn": None, "pagadoEn": None,
    }


def egreso(consec, inmueble_id, estado, dias=30):
    _, desc, _, prop, _, canon = inm(inmueble_id)
    bruto = round(canon * dias / 30)
    admin = round(bruto * 0.10)
    return {
        "id": consec, "consecutivo": consec, "fecha": "2026-10-07", "periodo": PERIODO, "inmuebleId": inmueble_id,
        "inmueble": desc, "propietarioId": prop, "propietario": next(x[1] for x in PROPIETARIOS if x[0] == prop),
        "propietarioEmail": next(x[3] for x in PROPIETARIOS if x[0] == prop) + "@ejemplo.test",
        "concepto": "PAGO DE ALQUILER DE ARRENDAMIENTO DEL MES DE OCTUBRE DE 2026", "dias": dias,
        "valorBruto": bruto, "valorAdministracion": admin, "otrosDescuentos": 0, "total": bruto - admin,
        "imputacionContable": None, "estado": estado, "enviadoEn": None,
    }


def mov(i, fecha, concepto, ingreso, gasto, origen="Manual"):
    return {"id": i, "fecha": fecha, "concepto": concepto, "administracion": 0, "ingreso": ingreso, "gasto": gasto,
            "origen": origen, "manual": origen == "Manual"}


# ---------------- calculos ----------------

def banco_cuadre():
    saldo = 0
    lineas, ing, gas = [], 0, 0
    for m in sorted(ESTADO["banco"], key=lambda x: (x["fecha"], x["id"])):
        saldo += m["ingreso"] - m["gasto"]
        ing += m["ingreso"]
        gas += m["gasto"]
        lineas.append({**m, "saldo": saldo})
    return {"periodo": PERIODO, "saldoInicial": 0, "totalIngresos": ing, "totalGastos": gas,
            "totalAdministracion": 0, "saldoFinal": saldo, "movimientos": lineas}


RANGOS = ["Al día", "1 a 30 días", "31 a 60 días", "61 a 90 días", "Más de 90 días"]


def cartera():
    pendientes = [c for c in ESTADO["cuentas"] if c["estado"] != "PAGADO"]
    mora = max(0, (HOY - VENCE).days)
    rangos = [{"etiqueta": e, "cuentas": 0, "valor": 0} for e in RANGOS]
    por_inq = {}
    for c in pendientes:
        r = 0 if mora == 0 else 1
        rangos[r]["cuentas"] += 1
        rangos[r]["valor"] += c["total"]
        por_inq.setdefault(c["inquilinoId"], []).append(c)
    inquilinos = []
    for iid, cs in por_inq.items():
        i = next(x for x in INQUILINOS if x[0] == iid)
        total = sum(c["total"] for c in cs)
        inquilinos.append({
            "inquilinoId": iid, "nombre": i[1], "email": i[3] + "@ejemplo.test", "telefono": "310" + i[2][-7:],
            "totalPendiente": total, "totalVencido": total if mora else 0, "maxDiasMora": mora,
            "cuentas": [{"id": c["id"], "consecutivo": c["consecutivo"], "periodo": c["periodo"], "inmueble": c["inmueble"],
                         "total": c["total"], "estado": c["estado"], "vencimiento": VENCE.isoformat(), "diasMora": mora} for c in cs],
        })
    inquilinos.sort(key=lambda x: -x["totalPendiente"])
    total = sum(c["total"] for c in pendientes)
    return {"totalPendiente": total, "totalVencido": total if mora else 0, "cuentasPendientes": len(pendientes),
            "inquilinosEnMora": len(inquilinos) if mora else 0, "rangos": rangos, "inquilinos": inquilinos}


def panel():
    cs, es = ESTADO["cuentas"], ESTADO["egresos"]
    emitido = sum(c["total"] for c in cs)
    recaudado = sum(c["total"] for c in cs if c["estado"] == "PAGADO")
    pagadas = sum(1 for c in cs if c["estado"] == "PAGADO")
    borradores = sum(1 for c in cs if c["estado"] == "BORRADOR")
    egr_total = sum(e["total"] for e in es)
    egr_pag = sum(e["total"] for e in es if e["estado"] == "PAGADO")
    ids_con_egreso = {e["inmuebleId"] for e in es}
    sin_egreso = [c for c in cs if c["estado"] == "PAGADO" and c["inmuebleId"] not in ids_con_egreso]
    sin_cuenta = len(INMUEBLES) - len({c["inmuebleId"] for c in cs})
    b = banco_cuadre()
    ca = cartera()
    alertas = []
    if ca["inquilinosEnMora"]:
        alertas.append({"nivel": "alta", "texto": f"{ca['inquilinosEnMora']} inquilino(s) en mora por un total vencido de $ {ca['totalVencido']:,}".replace(",", "."), "enlace": "/cartera"})
    if sin_cuenta:
        alertas.append({"nivel": "media", "texto": f"{sin_cuenta} inmueble(s) con inquilino aún sin cuenta de cobro este mes", "enlace": "/cuentas-cobro"})
    if borradores:
        alertas.append({"nivel": "media", "texto": f"{borradores} cuenta(s) de cobro en borrador sin enviar", "enlace": "/cuentas-cobro"})
    if sin_egreso:
        alertas.append({"nivel": "media", "texto": f"{len(sin_egreso)} propietario(s) por pagar: el inquilino ya pagó y falta el comprobante de egreso", "enlace": "/comprobantes-egreso"})
    por_pagar = sum(1 for e in es if e["estado"] != "PAGADO")
    if por_pagar:
        alertas.append({"nivel": "baja", "texto": f"{por_pagar} comprobante(s) de egreso pendiente(s) de pago", "enlace": "/comprobantes-egreso"})
    return {
        "periodo": PERIODO,
        "cobro": {"emitidas": len(cs), "valorEmitido": emitido, "pagadas": pagadas, "valorRecaudado": recaudado,
                  "porCobrar": len(cs) - pagadas, "valorPorCobrar": emitido - recaudado, "sinEnviar": borradores,
                  "porcentajeRecaudo": round(recaudado * 100 / emitido) if emitido else 0},
        "egresos": {"emitidos": len(es), "valorTotal": egr_total, "pagados": sum(1 for e in es if e["estado"] == "PAGADO"),
                    "valorPagado": egr_pag, "porPagar": por_pagar, "valorPorPagar": egr_total - egr_pag},
        "banco": {"saldoFinal": b["saldoFinal"], "ingresos": b["totalIngresos"], "gastos": b["totalGastos"]},
        "cartera": ca, "inmueblesSinCuenta": sin_cuenta,
        "propietariosPorPagar": len(sin_egreso), "valorPropietariosPorPagar": sum(c["valorArriendo"] for c in sin_egreso),
        "alertas": alertas,
        "ultimosMovimientos": list(reversed(b["movimientos"][-5:])),
    }


def filtrar(lista, q, campos):
    q = (q or "").lower()
    return [x for x in lista if not q or any(q in str(x[c]).lower() for c in campos)]


class H(BaseHTTPRequestHandler):
    def _json(self, datos, codigo=200):
        cuerpo = json.dumps(datos).encode()
        self.send_response(codigo)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(cuerpo)))
        self.end_headers()
        self.wfile.write(cuerpo)

    def _cuerpo(self):
        n = int(self.headers.get("Content-Length") or 0)
        return json.loads(self.rfile.read(n) or b"{}") if n else {}

    def do_GET(self):
        u = urlparse(self.path)
        q = parse_qs(u.query).get("q", [""])[0]
        r = u.path
        if r == "/api/auth/me":
            return self._json(ESTADO["usuarios"][0])
        if r == "/api/panel":
            return self._json(panel())
        if r == "/api/cartera":
            return self._json(cartera())
        if r == "/api/banco":
            return self._json(banco_cuadre())
        if r == "/api/usuarios":
            return self._json(ESTADO["usuarios"])
        if r == "/api/propietarios":
            lista = [{"id": p[0], "nombre": p[1], "documento": p[2], "email": p[3] + "@ejemplo.test", "telefono": "300" + p[2][-7:],
                      "banco": p[4], "tipoCuenta": p[5], "numeroCuenta": p[6], "activo": True} for p in PROPIETARIOS]
            return self._json(filtrar(lista, q, ["nombre", "documento"]))
        if r == "/api/inquilinos":
            lista = [{"id": i[0], "nombre": i[1], "documento": i[2], "email": i[3] + "@ejemplo.test", "telefono": "310" + i[2][-7:],
                      "ciudad": "La Ceja - Antioquia", "direccion": i[4], "activo": True} for i in INQUILINOS]
            return self._json(filtrar(lista, q, ["nombre", "documento"]))
        if r == "/api/inmuebles":
            lista = [{"id": m[0], "descripcion": m[1], "direccion": m[2], "ciudad": "La Ceja - Antioquia", "propietarioId": m[3],
                      "propietarioNombre": next(p[1] for p in PROPIETARIOS if p[0] == m[3]), "inquilinoId": m[4],
                      "inquilinoNombre": next(i[1] for i in INQUILINOS if i[0] == m[4]), "canon": m[5],
                      "pctAdminCobro": 20, "pctAdminEgreso": 10, "activo": True} for m in INMUEBLES]
            return self._json(filtrar(lista, q, ["descripcion", "direccion", "propietarioNombre", "inquilinoNombre"]))
        if r == "/api/cuentas-cobro":
            return self._json(sorted(filtrar(ESTADO["cuentas"], q, ["inquilino", "inmueble", "propietario"]), key=lambda c: -c["consecutivo"]))
        if r == "/api/comprobantes-egreso":
            return self._json(sorted(filtrar(ESTADO["egresos"], q, ["propietario", "inmueble"]), key=lambda c: -c["consecutivo"]))
        self._json([])

    def do_POST(self):
        r = urlparse(self.path).path
        body = self._cuerpo()
        if r == "/api/auth/login":
            return self._json({"token": "demo", "expiraEnSegundos": 28800, "usuario": ESTADO["usuarios"][0]})
        if r == "/api/cuentas-cobro/generar-mes":
            existentes = {c["inmuebleId"] for c in ESTADO["cuentas"]}
            creadas = 0
            for m in INMUEBLES:
                if m[0] not in existentes:
                    c = cuenta(ESTADO["sig"]["cuenta"], m[0], "BORRADOR", 0)
                    ESTADO["sig"]["cuenta"] += 1
                    ESTADO["cuentas"].append(c)
                    creadas += 1
            return self._json({"creadas": creadas, "omitidas": len(existentes)})
        if r == "/api/cuentas-cobro/enviar-lote":
            res = []
            for i in body.get("ids", []):
                c = next((x for x in ESTADO["cuentas"] if x["id"] == i), None)
                if c:
                    c["estado"] = "ENVIADO" if c["estado"] == "BORRADOR" else c["estado"]
                res.append({"id": i, "ok": bool(c), "mensaje": "Enviada"})
            return self._json(res)
        m = re.fullmatch(r"/api/cuentas-cobro/(\d+)/(enviar|pagar)", r)
        if m:
            c = next(x for x in ESTADO["cuentas"] if x["id"] == int(m.group(1)))
            if m.group(2) == "enviar":
                c["estado"] = "ENVIADO" if c["estado"] == "BORRADOR" else c["estado"]
            else:
                c["estado"] = "PAGADO"
                ESTADO["banco"].append(mov(ESTADO["sig"]["mov"], "2026-10-09", f"pago de arriendo {c['inmueble']} ({c['inquilino']})", c["total"], 0, f"Cuenta de cobro {c['consecutivo']}"))
                ESTADO["sig"]["mov"] += 1
            return self._json(c)
        if r == "/api/comprobantes-egreso/generar-mes":
            con = {e["inmuebleId"] for e in ESTADO["egresos"]}
            creados = 0
            for c in ESTADO["cuentas"]:
                if c["estado"] == "PAGADO" and c["inmuebleId"] not in con:
                    ESTADO["egresos"].append(egreso(ESTADO["sig"]["egreso"], c["inmuebleId"], "BORRADOR"))
                    ESTADO["sig"]["egreso"] += 1
                    creados += 1
            return self._json({"creados": creados, "omitidos": len(con)})
        m = re.fullmatch(r"/api/comprobantes-egreso/(\d+)/(enviar|pagar)", r)
        if m:
            e = next(x for x in ESTADO["egresos"] if x["id"] == int(m.group(1)))
            if m.group(2) == "enviar":
                e["estado"] = "ENVIADO" if e["estado"] == "BORRADOR" else e["estado"]
            else:
                e["estado"] = "PAGADO"
                ESTADO["banco"].append(mov(ESTADO["sig"]["mov"], "2026-10-09", f"pago de arriendo {e['inmueble']} ({e['propietario']})", 0, e["total"], f"Egreso {e['consecutivo']}"))
                ESTADO["sig"]["mov"] += 1
            return self._json(e)
        if r == "/api/banco/movimientos":
            ESTADO["banco"].append(mov(ESTADO["sig"]["mov"], body["fecha"], body["concepto"], body.get("ingreso") or 0, body.get("gasto") or 0))
            ESTADO["sig"]["mov"] += 1
            return self._json({}, 201)
        if r == "/api/reiniciar":
            reiniciar()
            return self._json({"ok": True})
        self._json({})

    def log_message(self, *a):
        pass


if __name__ == "__main__":
    reiniciar()
    ThreadingHTTPServer(("127.0.0.1", 8080), H).serve_forever()
