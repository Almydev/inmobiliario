import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { claseInput, mesActual, moneda } from '../lib'

const COLOR_ALERTA = {
  alta: 'bg-red-500',
  media: 'bg-amber-500',
  baja: 'bg-sky-500',
}

const COLOR_RANGO = ['bg-emerald-500', 'bg-amber-400', 'bg-orange-500', 'bg-red-500', 'bg-red-700']

function Tarjeta({ titulo, valor, detalle, tono = 'neutro', enlace }) {
  const color = {
    neutro: 'text-ink',
    bien: 'text-emerald-700 dark:text-emerald-400',
    mal: 'text-red-700 dark:text-red-400',
  }[tono]
  const contenido = (
    <>
      <p className="text-xs font-medium uppercase tracking-wider text-ink-soft/70">{titulo}</p>
      <p className={`mt-2 font-display text-3xl tabular-nums ${color}`}>{valor}</p>
      {detalle && <p className="mt-1 text-sm text-ink-soft/80">{detalle}</p>}
    </>
  )
  const clase = 'block rounded-2xl border border-line/60 bg-surface p-5 shadow-sm transition'
  return enlace ? (
    <Link to={enlace} className={`${clase} hover:-translate-y-0.5 hover:shadow-md`}>{contenido}</Link>
  ) : (
    <div className={clase}>{contenido}</div>
  )
}

function Bloque({ titulo, accion, children }) {
  return (
    <section className="rounded-2xl border border-line/60 bg-surface p-5 shadow-sm">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="font-display text-lg text-ink">{titulo}</h2>
        {accion}
      </div>
      <div className="mt-4">{children}</div>
    </section>
  )
}

const fechaCorta = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString('es-CO', { day: '2-digit', month: 'short' })

export default function Inicio() {
  const { usuario } = useAuth()
  const [periodo, setPeriodo] = useState(mesActual())
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let vigente = true
    api(`/api/panel?periodo=${periodo}`)
      .then((d) => {
        if (vigente) {
          setDatos(d)
          setError('')
        }
      })
      .catch((e) => vigente && setError(e.message))
    return () => {
      vigente = false
    }
  }, [periodo])

  const mesLargo = new Date(`${periodo}-01T00:00:00`).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' })
  const totalCartera = datos ? datos.cartera.rangos.reduce((s, r) => s + Number(r.valor), 0) : 0

  return (
    <div className="mx-auto max-w-6xl animate-rise">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl text-ink">Hola, {usuario.nombre}</h1>
          <p className="mt-1 text-sm text-ink-soft/80">Resumen de {mesLargo}</p>
        </div>
        <input type="month" value={periodo} onChange={(e) => e.target.value && setPeriodo(e.target.value)} aria-label="Mes" className={claseInput} />
      </header>

      {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">{error}</p>}
      {!datos && !error && <p className="mt-10 text-center text-ink-soft/70">Cargando…</p>}

      {datos && (
        <>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Tarjeta
              titulo="Recaudado del mes"
              valor={moneda.format(datos.cobro.valorRecaudado)}
              detalle={`${datos.cobro.pagadas} de ${datos.cobro.emitidas} cuentas pagadas`}
              tono="bien"
              enlace="/cuentas-cobro"
            />
            <Tarjeta titulo="Por cobrar del mes" valor={moneda.format(datos.cobro.valorPorCobrar)} detalle={`${datos.cobro.porCobrar} cuenta(s) sin pagar`} enlace="/cuentas-cobro" />
            <Tarjeta
              titulo="Cartera vencida"
              valor={moneda.format(datos.cartera.totalVencido)}
              detalle={datos.cartera.inquilinosEnMora ? `${datos.cartera.inquilinosEnMora} inquilino(s) en mora` : 'Sin inquilinos en mora'}
              tono={Number(datos.cartera.totalVencido) > 0 ? 'mal' : 'bien'}
              enlace="/cartera"
            />
            <Tarjeta
              titulo="Saldo en banco"
              valor={moneda.format(datos.banco.saldoFinal)}
              detalle={`Ingresos ${moneda.format(datos.banco.ingresos)} · Gastos ${moneda.format(datos.banco.gastos)}`}
              tono={Number(datos.banco.saldoFinal) < 0 ? 'mal' : 'neutro'}
              enlace="/banco"
            />
          </div>

          {datos.cobro.emitidas > 0 && (
            <div className="mt-4 rounded-2xl border border-line/60 bg-surface p-5 shadow-sm">
              <div className="flex items-baseline justify-between text-sm">
                <span className="font-medium text-ink">Avance del recaudo</span>
                <span className="tabular-nums text-ink-soft">{datos.cobro.porcentajeRecaudo}% de {moneda.format(datos.cobro.valorEmitido)}</span>
              </div>
              <div className="mt-3 h-3 overflow-hidden rounded-full bg-subtle" role="progressbar" aria-valuenow={datos.cobro.porcentajeRecaudo} aria-valuemin={0} aria-valuemax={100} aria-label="Avance del recaudo">
                <div className="h-full rounded-full bg-emerald-500 transition-all duration-700" style={{ width: `${datos.cobro.porcentajeRecaudo}%` }} />
              </div>
            </div>
          )}

          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            <Bloque titulo="Qué hacer ahora">
              {datos.alertas.length === 0 ? (
                <p className="text-sm text-ink-soft/80">Todo al día. No hay pendientes para este mes.</p>
              ) : (
                <ul className="space-y-1">
                  {datos.alertas.map((a) => (
                    <li key={a.texto}>
                      <Link to={a.enlace} className="group flex items-start gap-3 rounded-xl px-3 py-2.5 transition hover:bg-subtle">
                        <span className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${COLOR_ALERTA[a.nivel]}`} aria-hidden="true" />
                        <span className="flex-1 text-sm text-ink">{a.texto}</span>
                        <span className="text-ink-soft opacity-0 transition group-hover:opacity-100" aria-hidden="true">→</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Bloque>

            <Bloque titulo="Cartera por antigüedad" accion={<Link to="/cartera" className="text-sm font-medium text-ink-soft hover:text-ink">Ver cartera</Link>}>
              {datos.cartera.cuentasPendientes === 0 ? (
                <p className="text-sm text-ink-soft/80">No hay cuentas pendientes de pago.</p>
              ) : (
                <ul className="space-y-3">
                  {datos.cartera.rangos.map((r, i) => (
                    <li key={r.etiqueta}>
                      <div className="flex justify-between text-sm">
                        <span className="text-ink">{r.etiqueta} <span className="text-ink-soft/60">· {r.cuentas}</span></span>
                        <span className="tabular-nums text-ink-soft">{moneda.format(r.valor)}</span>
                      </div>
                      <div className="mt-1 h-2 overflow-hidden rounded-full bg-subtle">
                        <div className={`h-full rounded-full ${COLOR_RANGO[i]}`} style={{ width: `${totalCartera ? (Number(r.valor) / totalCartera) * 100 : 0}%` }} />
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </Bloque>
          </div>

          <div className="mt-4 grid gap-4 lg:grid-cols-3">
            <Bloque titulo="Pagos a propietarios">
              <p className="font-display text-3xl tabular-nums text-ink">{moneda.format(datos.egresos.valorTotal)}</p>
              <p className="mt-1 text-sm text-ink-soft/80">{datos.egresos.emitidos} comprobante(s) · {datos.egresos.pagados} pagado(s)</p>
              {datos.propietariosPorPagar > 0 && (
                <Link to="/comprobantes-egreso" className="mt-4 block rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-900 transition hover:bg-amber-100 dark:bg-amber-500/10 dark:text-amber-200 dark:hover:bg-amber-500/20">
                  <strong>{datos.propietariosPorPagar}</strong> propietario(s) listos para pagar: el inquilino ya pagó ({moneda.format(datos.valorPropietariosPorPagar)} de arriendo).
                </Link>
              )}
            </Bloque>

            <div className="lg:col-span-2">
              <Bloque titulo="Últimos movimientos del banco" accion={<Link to="/banco" className="text-sm font-medium text-ink-soft hover:text-ink">Ver cuadre</Link>}>
                {datos.ultimosMovimientos.length === 0 ? (
                  <p className="text-sm text-ink-soft/80">Aún no hay movimientos este mes.</p>
                ) : (
                  <ul className="divide-y divide-line/40">
                    {datos.ultimosMovimientos.map((m) => (
                      <li key={m.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                        <span className="min-w-0 flex-1 truncate text-ink">
                          <span className="mr-2 text-ink-soft/60">{fechaCorta(m.fecha)}</span>
                          {m.concepto}
                        </span>
                        <span className={`tabular-nums ${Number(m.ingreso) ? 'text-emerald-700 dark:text-emerald-400' : 'text-red-700 dark:text-red-400'}`}>
                          {Number(m.ingreso) ? `+ ${moneda.format(m.ingreso)}` : `− ${moneda.format(m.gasto)}`}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </Bloque>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
