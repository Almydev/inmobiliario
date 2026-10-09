"""Prueba de carga simple para la API (solo biblioteca estandar de Python).

Uso:
  python docs/pruebas/carga.py --url http://localhost:8080 --email admin@... --password ... [--hilos 20] [--peticiones 400]

Mide latencia (p50/p95/p99) y errores en: health, listados autenticados y generacion de PDF.
Usa SOLO contra qa/local: crea carga real sobre la base. La contrasena se pasa por argumento o variable
PRUEBA_PASSWORD, nunca se guarda.
"""
import argparse
import json
import os
import statistics
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor


def pedir(url, token=None, cuerpo=None):
    req = urllib.request.Request(url, data=json.dumps(cuerpo).encode() if cuerpo else None)
    if cuerpo:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    inicio = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            r.read()
            codigo = r.status
    except urllib.error.HTTPError as e:
        codigo = e.code
    except Exception:
        codigo = 0
    return codigo, (time.perf_counter() - inicio) * 1000


def escenario(nombre, url, token, total, hilos):
    inicio = time.perf_counter()
    with ThreadPoolExecutor(hilos) as pool:
        resultados = list(pool.map(lambda _: pedir(url, token), range(total)))
    dur = time.perf_counter() - inicio
    lat = sorted(ms for _, ms in resultados)
    errores = sum(1 for c, _ in resultados if c != 200)
    p = lambda q: lat[min(len(lat) - 1, int(len(lat) * q))]
    print(f"{nombre:<28} {total:>5} pet  {total / dur:>7.1f} req/s  p50={p(.5):>7.0f}ms  p95={p(.95):>7.0f}ms  "
          f"p99={p(.99):>7.0f}ms  errores={errores}")
    return errores


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--url", required=True)
    ap.add_argument("--email", required=True)
    ap.add_argument("--password", default=os.environ.get("PRUEBA_PASSWORD"))
    ap.add_argument("--hilos", type=int, default=20)
    ap.add_argument("--peticiones", type=int, default=400)
    a = ap.parse_args()
    if not a.password:
        raise SystemExit("Falta la contrasena (--password o PRUEBA_PASSWORD)")

    try:
        req = urllib.request.Request(f"{a.url}/api/auth/login",
                                     data=json.dumps({"email": a.email, "password": a.password}).encode(),
                                     headers={"Content-Type": "application/json"})
        token = json.loads(urllib.request.urlopen(req, timeout=60).read())["token"]
    except urllib.error.HTTPError as e:
        raise SystemExit(f"No se pudo iniciar sesion (HTTP {e.code})")

    errores = 0
    errores += escenario("health", f"{a.url}/actuator/health", None, a.peticiones, a.hilos)
    errores += escenario("listar inmuebles", f"{a.url}/api/inmuebles", token, a.peticiones, a.hilos)
    errores += escenario("listar cuentas de cobro", f"{a.url}/api/cuentas-cobro", token, a.peticiones, a.hilos)

    cuentas = json.loads(urllib.request.urlopen(urllib.request.Request(
        f"{a.url}/api/cuentas-cobro", headers={"Authorization": f"Bearer {token}"})).read())
    if cuentas:
        errores += escenario("generar PDF", f"{a.url}/api/cuentas-cobro/{cuentas[0]['id']}/pdf", token, a.peticiones // 2, a.hilos)
    print("\nOK" if errores == 0 else f"\nHubo {errores} respuestas distintas de 200")


if __name__ == "__main__":
    main()
