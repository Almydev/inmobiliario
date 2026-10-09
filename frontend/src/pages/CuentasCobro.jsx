import { useCallback, useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import { api, apiBlob } from '../api'

const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const mesActual = () => new Date().toISOString().slice(0, 7)

const ESTADOS = {
  BORRADOR: { texto: 'Borrador', clase: 'bg-sand-100 text-brand-700' },
  ENVIADO: { texto: 'Enviada', clase: 'bg-sky-50 text-sky-700' },
  PAGADO: { texto: 'Pagada', clase: 'bg-emerald-50 text-emerald-700' },
}

const input =
  'rounded-xl border border-sand-300 bg-white px-3.5 py-2.5 text-brand-950 outline-none transition focus:border-brand-700 focus:ring-4 focus:ring-brand-700/15'

function Boton({ children, variante = 'primario', ...props }) {
  const base = 'rounded-xl px-4 py-2.5 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60'
  const estilos = {
    primario: 'bg-brand-900 text-sand-50 shadow-lg shadow-brand-900/15 hover:bg-brand-800',
    suave: 'border border-sand-300 bg-white text-brand-900 hover:bg-sand-100',
  }
  return (
    <button className={`${base} ${estilos[variante]}`} {...props}>
      {children}
    </button>
  )
}

function ModalNueva({ onCerrar, onCreada }) {
  const [inmuebles, setInmuebles] = useState([])
  const [inmuebleId, setInmuebleId] = useState('')
  const [periodo, setPeriodo] = useState(mesActual())
  const [admin, setAdmin] = useState(false)
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    api('/api/inmuebles?q=')
      .then((f) => setInmuebles(f.filter((i) => i.activo && i.inquilinoId)))
      .catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    const esc = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [onCerrar])

  async function enviar(e) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await api('/api/cuentas-cobro', {
        method: 'POST',
        body: { inmuebleId: Number(inmuebleId), periodo, aplicarAdministracion: admin },
      })
      onCreada()
    } catch (err) {
      setError(err.message)
      setGuardando(false)
    }
  }

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-brand-950/50 p-4 backdrop-blur-sm sm:items-center"
      onMouseDown={onCerrar}
    >
      <form onSubmit={enviar} onMouseDown={(e) => e.stopPropagation()} className="my-auto w-full max-w-md animate-rise rounded-2xl bg-white p-6 shadow-2xl sm:p-8">
        <h2 className="font-display text-2xl text-brand-900">Nueva cuenta de cobro</h2>
        <div className="mt-6 space-y-4">
          <div>
            <label htmlFor="inmueble" className="mb-1 block text-sm font-medium text-brand-900">Inmueble</label>
            <select id="inmueble" required value={inmuebleId} onChange={(e) => setInmuebleId(e.target.value)} className={`${input} w-full`}>
              <option value="">Seleccionar…</option>
              {inmuebles.map((i) => (
                <option key={i.id} value={i.id}>
                  {i.descripcion} · {i.inquilinoNombre} · {moneda.format(i.canon)}
                </option>
              ))}
            </select>
            {inmuebles.length === 0 && <p className="mt-1 text-xs text-brand-700/70">Solo aparecen inmuebles activos con inquilino asignado.</p>}
          </div>
          <div>
            <label htmlFor="periodo" className="mb-1 block text-sm font-medium text-brand-900">Mes a cobrar</label>
            <input id="periodo" type="month" required value={periodo} onChange={(e) => setPeriodo(e.target.value)} className={`${input} w-full`} />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={admin} onChange={(e) => setAdmin(e.target.checked)} className="h-4 w-4 accent-brand-900" />
            Sumar el % de administración del inmueble al cobro
          </label>
        </div>
        {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
        <div className="mt-6 flex justify-end gap-3">
          <Boton type="button" variante="suave" onClick={onCerrar}>Cancelar</Boton>
          <Boton type="submit" disabled={guardando}>{guardando ? 'Generando…' : 'Generar'}</Boton>
        </div>
      </form>
    </div>,
    document.body,
  )
}

export default function CuentasCobro() {
  const [filas, setFilas] = useState([])
  const [q, setQ] = useState('')
  const [periodo, setPeriodo] = useState(mesActual())
  const [cargando, setCargando] = useState(true)
  const [seleccion, setSeleccion] = useState(() => new Set())
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null) // { tipo, texto }
  const [modal, setModal] = useState(false)

  const cargar = useCallback(async () => {
    try {
      setFilas(await api(`/api/cuentas-cobro?q=${encodeURIComponent(q)}&periodo=${periodo}`))
      setSeleccion(new Set())
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    } finally {
      setCargando(false)
    }
  }, [q, periodo])

  useEffect(() => {
    const t = setTimeout(cargar, 250)
    return () => clearTimeout(t)
  }, [cargar])

  const cerrarModal = useCallback(() => setModal(false), [])

  async function accion(fn, exito) {
    setOcupado(true)
    setAviso(null)
    try {
      const r = await fn()
      setAviso({ tipo: 'ok', texto: typeof exito === 'function' ? exito(r) : exito })
      await cargar()
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    } finally {
      setOcupado(false)
    }
  }

  async function verPdf(f) {
    try {
      const blob = await apiBlob(`/api/cuentas-cobro/${f.id}/pdf`)
      window.open(URL.createObjectURL(blob), '_blank', 'noopener')
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    }
  }

  const generarMes = () =>
    accion(
      () => api('/api/cuentas-cobro/generar-mes', { method: 'POST', body: { periodo } }),
      (r) => `Se crearon ${r.creadas} cuentas${r.omitidas ? ` (${r.omitidas} ya existían)` : ''}.`,
    )

  const enviar = (f) =>
    accion(() => api(`/api/cuentas-cobro/${f.id}/enviar`, { method: 'POST' }), `Cuenta No. ${f.consecutivo} enviada a ${f.inquilinoEmail}.`)

  const pagar = (f) =>
    accion(() => api(`/api/cuentas-cobro/${f.id}/pagar`, { method: 'POST' }), `Cuenta No. ${f.consecutivo} marcada como pagada y registrada en el banco.`)

  const enviarSeleccion = () =>
    accion(
      () => api('/api/cuentas-cobro/enviar-lote', { method: 'POST', body: { ids: [...seleccion] } }),
      (res) => {
        const ok = res.filter((r) => r.ok).length
        const fallos = res.filter((r) => !r.ok)
        return `Enviadas: ${ok}. ${fallos.length ? `Con error: ${fallos.length} (${fallos[0].mensaje}).` : ''}`
      },
    )

  const pendientes = useMemo(() => filas.filter((f) => f.estado === 'BORRADOR'), [filas])
  const todasMarcadas = pendientes.length > 0 && pendientes.every((f) => seleccion.has(f.id))

  function alternar(id) {
    const n = new Set(seleccion)
    if (n.has(id)) n.delete(id)
    else n.add(id)
    setSeleccion(n)
  }

  const total = filas.reduce((s, f) => s + Number(f.total), 0)

  return (
    <div className="mx-auto max-w-6xl animate-rise">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl text-brand-900">Cuentas de cobro</h1>
          <p className="mt-1 text-sm text-brand-700/80">Genera las cuentas del mes, revisa el PDF y envíalas por correo en pocos clics.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Boton variante="suave" onClick={generarMes} disabled={ocupado}>Generar todas del mes</Boton>
          <Boton onClick={() => setModal(true)}>+ Nueva cuenta</Boton>
        </div>
      </header>

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <input type="month" value={periodo} onChange={(e) => setPeriodo(e.target.value)} aria-label="Mes" className={input} />
        <input type="search" placeholder="Buscar por inquilino, inmueble o propietario…" value={q} onChange={(e) => setQ(e.target.value)} className={`${input} min-w-64 flex-1`} />
        {seleccion.size > 0 && (
          <Boton onClick={enviarSeleccion} disabled={ocupado}>
            {ocupado ? 'Enviando…' : `Enviar ${seleccion.size} seleccionada${seleccion.size > 1 ? 's' : ''}`}
          </Boton>
        )}
      </div>

      {aviso && (
        <p role={aviso.tipo === 'error' ? 'alert' : 'status'}
          className={`mt-4 rounded-xl border px-4 py-3 text-sm ${aviso.tipo === 'error' ? 'border-red-200 bg-red-50 text-red-700' : 'border-emerald-200 bg-emerald-50 text-emerald-800'}`}>
          {aviso.texto}
        </p>
      )}

      <div className="mt-4 overflow-x-auto rounded-2xl border border-sand-300/60 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-sand-100 text-xs uppercase tracking-wider text-brand-700">
            <tr>
              <th className="w-10 px-4 py-3">
                <input type="checkbox" aria-label="Seleccionar todas las pendientes" checked={todasMarcadas}
                  onChange={() => setSeleccion(todasMarcadas ? new Set() : new Set(pendientes.map((f) => f.id)))} className="h-4 w-4 accent-brand-900" />
              </th>
              <th className="px-4 py-3 font-semibold">N°</th>
              <th className="px-4 py-3 font-semibold">Inquilino</th>
              <th className="px-4 py-3 font-semibold">Inmueble</th>
              <th className="px-4 py-3 text-right font-semibold">Total</th>
              <th className="px-4 py-3 font-semibold">Estado</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {cargando && <tr><td colSpan={7} className="px-4 py-10 text-center text-brand-700/70">Cargando…</td></tr>}
            {!cargando && filas.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-brand-700/70">No hay cuentas de cobro para este mes. Usa “Generar todas del mes”.</td></tr>
            )}
            {filas.map((f) => {
              const est = ESTADOS[f.estado]
              return (
                <tr key={f.id} className="border-t border-sand-100 transition hover:bg-sand-50">
                  <td className="px-4 py-3">
                    {f.estado === 'BORRADOR' && (
                      <input type="checkbox" aria-label={`Seleccionar cuenta ${f.consecutivo}`} checked={seleccion.has(f.id)} onChange={() => alternar(f.id)} className="h-4 w-4 accent-brand-900" />
                    )}
                  </td>
                  <td className="px-4 py-3 font-medium">{f.consecutivo}</td>
                  <td className="px-4 py-3">
                    {f.inquilino}
                    <span className="block text-xs text-brand-700/70">{f.inquilinoEmail ?? 'Sin correo'}</span>
                  </td>
                  <td className="px-4 py-3">{f.inmueble}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{moneda.format(f.total)}</td>
                  <td className="px-4 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-medium ${est.clase}`}>{est.texto}</span></td>
                  <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-medium">
                    <button onClick={() => verPdf(f)} className="text-brand-700 transition hover:text-brand-950">PDF</button>
                    {f.estado !== 'PAGADO' && (
                      <>
                        <button onClick={() => enviar(f)} disabled={ocupado} className="ml-4 text-brand-700 transition hover:text-brand-950 disabled:opacity-50">
                          {f.estado === 'ENVIADO' ? 'Reenviar' : 'Enviar'}
                        </button>
                        <button onClick={() => pagar(f)} disabled={ocupado} className="ml-4 text-emerald-700 transition hover:text-emerald-900 disabled:opacity-50">Marcar pagada</button>
                      </>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
          {filas.length > 0 && (
            <tfoot>
              <tr className="border-t border-sand-300 bg-sand-50 text-sm font-semibold">
                <td colSpan={4} className="px-4 py-3 text-right">Total del listado</td>
                <td className="px-4 py-3 text-right tabular-nums">{moneda.format(total)}</td>
                <td colSpan={2} />
              </tr>
            </tfoot>
          )}
        </table>
      </div>

      {modal && <ModalNueva onCerrar={cerrarModal} onCreada={() => { setModal(false); setAviso({ tipo: 'ok', texto: 'Cuenta de cobro creada.' }); cargar() }} />}
    </div>
  )
}
