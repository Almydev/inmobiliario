import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import { api, apiBlob } from '../api'
import Boton from '../components/Boton'
import { useBloqueo } from '../hooks'
import { claseInput, moneda } from '../lib'

const COLOR_RANGO = ['bg-emerald-500', 'bg-amber-400', 'bg-orange-500', 'bg-red-500', 'bg-red-700']

const fecha = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })

function Mora({ dias }) {
  const clase =
    dias === 0
      ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
      : dias <= 30
        ? 'bg-amber-50 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300'
        : 'bg-red-50 text-red-700 dark:bg-red-500/15 dark:text-red-300'
  return <span className={`whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-medium ${clase}`}>{dias === 0 ? 'Al día' : `${dias} día${dias === 1 ? '' : 's'} de mora`}</span>
}

function Dato({ titulo, valor, tono }) {
  const color = tono === 'mal' ? 'text-red-700 dark:text-red-400' : 'text-ink'
  return (
    <div className="rounded-2xl border border-line/60 bg-surface p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wider text-ink-soft/70">{titulo}</p>
      <p className={`mt-1 font-display text-2xl tabular-nums ${color}`}>{valor}</p>
    </div>
  )
}

export default function Cartera() {
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [q, setQ] = useState('')
  const [soloMora, setSoloMora] = useState(false)
  const [abierto, setAbierto] = useState(() => new Set())
  const [aviso, setAviso] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const bloquear = useBloqueo()

  const cargar = useCallback(async () => {
    try {
      setDatos(await api('/api/cartera'))
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  const filas = useMemo(() => {
    if (!datos) return []
    const t = q.trim().toLowerCase()
    return datos.inquilinos.filter(
      (i) => (!soloMora || i.maxDiasMora > 0) && (!t || i.nombre.toLowerCase().includes(t) || i.cuentas.some((c) => c.inmueble.toLowerCase().includes(t))),
    )
  }, [datos, q, soloMora])

  function alternar(id) {
    const n = new Set(abierto)
    if (n.has(id)) n.delete(id)
    else n.add(id)
    setAbierto(n)
  }

  function accion(fn, exito) {
    return bloquear(async () => {
      setOcupado(true)
      setAviso(null)
      try {
        await fn()
        setAviso({ tipo: 'ok', texto: exito })
      } catch (err) {
        setAviso({ tipo: 'error', texto: err.message })
      } finally {
        await cargar()
        setOcupado(false)
      }
    })
  }

  async function verPdf(c) {
    try {
      const blob = await apiBlob(`/api/cuentas-cobro/${c.id}/pdf`)
      window.open(URL.createObjectURL(blob), '_blank', 'noopener')
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    }
  }

  const reenviar = (c, nombre) => accion(() => api(`/api/cuentas-cobro/${c.id}/enviar`, { method: 'POST' }), `Cuenta No. ${c.consecutivo} enviada de nuevo a ${nombre}.`)
  const pagar = (c) => accion(() => api(`/api/cuentas-cobro/${c.id}/pagar`, { method: 'POST' }), `Cuenta No. ${c.consecutivo} registrada como pagada.`)

  const totalRangos = datos ? datos.rangos.reduce((s, r) => s + Number(r.valor), 0) : 0

  return (
    <div className="mx-auto max-w-6xl animate-rise">
      <header>
        <h1 className="font-display text-3xl text-ink">Cartera</h1>
        <p className="mt-1 text-sm text-ink-soft/80">Inquilinos con cuentas de cobro pendientes de pago. Una cuenta vence el día 5 del mes que cobra.</p>
      </header>

      {aviso && (
        <p
          role={aviso.tipo === 'error' ? 'alert' : 'status'}
          className={`mt-4 rounded-xl border px-4 py-3 text-sm ${aviso.tipo === 'error' ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300' : 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300'}`}
        >
          {aviso.texto}
        </p>
      )}

      {datos && (
        <>
          <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Dato titulo="Total pendiente" valor={moneda.format(datos.totalPendiente)} />
            <Dato titulo="Vencido" valor={moneda.format(datos.totalVencido)} tono={Number(datos.totalVencido) > 0 ? 'mal' : undefined} />
            <Dato titulo="Inquilinos en mora" valor={datos.inquilinosEnMora} tono={datos.inquilinosEnMora > 0 ? 'mal' : undefined} />
            <Dato titulo="Cuentas pendientes" valor={datos.cuentasPendientes} />
          </div>

          {totalRangos > 0 && (
            <div className="mt-4 rounded-2xl border border-line/60 bg-surface p-5 shadow-sm">
              <h2 className="font-display text-lg text-ink">Antigüedad de la cartera</h2>
              <div className="mt-3 flex h-4 overflow-hidden rounded-full bg-subtle" role="img" aria-label="Distribución de la cartera por antigüedad">
                {datos.rangos.map((r, i) =>
                  Number(r.valor) > 0 ? <div key={r.etiqueta} className={COLOR_RANGO[i]} style={{ width: `${(Number(r.valor) / totalRangos) * 100}%` }} title={`${r.etiqueta}: ${moneda.format(r.valor)}`} /> : null,
                )}
              </div>
              <ul className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-sm">
                {datos.rangos.map((r, i) => (
                  <li key={r.etiqueta} className="flex items-center gap-2 text-ink-soft">
                    <span className={`h-2.5 w-2.5 rounded-full ${COLOR_RANGO[i]}`} aria-hidden="true" />
                    {r.etiqueta}: <strong className="font-medium text-ink">{moneda.format(r.valor)}</strong>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <input type="search" placeholder="Buscar por inquilino o inmueble…" value={q} onChange={(e) => setQ(e.target.value)} className={`${claseInput} min-w-64 flex-1`} />
        <label className="flex items-center gap-2 text-sm text-ink">
          <input type="checkbox" checked={soloMora} onChange={(e) => setSoloMora(e.target.checked)} className="h-4 w-4 accent-primary" />
          Solo en mora
        </label>
      </div>

      <div className="mt-4 overflow-x-auto rounded-2xl border border-line/60 bg-surface shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-subtle text-xs uppercase tracking-wider text-ink-soft">
            <tr>
              <th className="px-4 py-3 font-semibold">Inquilino</th>
              <th className="px-4 py-3 text-right font-semibold">Pendiente</th>
              <th className="px-4 py-3 text-right font-semibold">Vencido</th>
              <th className="px-4 py-3 text-center font-semibold">Cuentas</th>
              <th className="px-4 py-3 font-semibold">Mora</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {cargando && <tr><td colSpan={6} className="px-4 py-10 text-center text-ink-soft/70">Cargando…</td></tr>}
            {!cargando && filas.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-10 text-center text-ink-soft/70">{datos?.cuentasPendientes ? 'Sin resultados para ese filtro.' : 'Sin cartera pendiente. 🎉'}</td></tr>
            )}
            {filas.map((i) => {
              const abiertoAhora = abierto.has(i.inquilinoId)
              return (
                <Fragment key={i.inquilinoId}>
                  <tr className="cursor-pointer border-t border-line/40 transition hover:bg-page" onClick={() => alternar(i.inquilinoId)}>
                    <td className="px-4 py-3">
                      <button className="text-left" aria-expanded={abiertoAhora} aria-label={`${abiertoAhora ? 'Ocultar' : 'Ver'} cuentas de ${i.nombre}`}>
                        <span className="font-medium text-ink">{abiertoAhora ? '▾' : '▸'} {i.nombre}</span>
                        <span className="block pl-4 text-xs text-ink-soft/70">{[i.email, i.telefono].filter(Boolean).join(' · ') || 'Sin datos de contacto'}</span>
                      </button>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">{moneda.format(i.totalPendiente)}</td>
                    <td className="px-4 py-3 text-right tabular-nums text-red-700 dark:text-red-400">{Number(i.totalVencido) ? moneda.format(i.totalVencido) : '—'}</td>
                    <td className="px-4 py-3 text-center">{i.cuentas.length}</td>
                    <td className="px-4 py-3"><Mora dias={i.maxDiasMora} /></td>
                    <td className="px-4 py-3" />
                  </tr>
                  {abiertoAhora &&
                    i.cuentas.map((c) => (
                      <tr key={c.id} className="bg-page/60 text-sm">
                        <td className="px-4 py-2 pl-10 text-ink-soft">
                          No. {c.consecutivo} · {c.inmueble}
                          <span className="block text-xs text-ink-soft/70">Vence {fecha(c.vencimiento)} · {c.periodo} · {c.estado === 'ENVIADO' ? 'Enviada' : 'Sin enviar'}</span>
                        </td>
                        <td className="px-4 py-2 text-right tabular-nums">{moneda.format(c.total)}</td>
                        <td className="px-4 py-2" />
                        <td className="px-4 py-2" />
                        <td className="px-4 py-2"><Mora dias={c.diasMora} /></td>
                        <td className="whitespace-nowrap px-4 py-2 text-right font-medium">
                          <button onClick={() => verPdf(c)} className="text-ink-soft transition hover:text-ink">PDF</button>
                          <button onClick={() => reenviar(c, i.nombre)} disabled={ocupado || !i.email} title={i.email ? '' : 'El inquilino no tiene correo'} className="ml-4 text-ink-soft transition hover:text-ink disabled:opacity-40">Reenviar</button>
                          <button onClick={() => pagar(c)} disabled={ocupado} className="ml-4 text-emerald-700 transition hover:text-emerald-900 disabled:opacity-50 dark:text-emerald-400 dark:hover:text-emerald-300">Marcar pagada</button>
                        </td>
                      </tr>
                    ))}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
      <div className="mt-4">
        <Boton variante="suave" onClick={cargar} disabled={ocupado} type="button">Actualizar</Boton>
      </div>
    </div>
  )
}
