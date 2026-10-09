import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, apiBlob } from '../api'
import { useBloqueo } from '../hooks'
import { claseInput, mesActual, moneda } from '../lib'
import Boton from './Boton'

/**
 * Pantalla comun de documentos (cuentas de cobro y comprobantes de egreso):
 * listado por mes, generar, ver PDF, enviar (uno o en lote) y marcar pagado.
 *
 * `cfg`: { titulo, descripcion, base, singular, genero ('f'|'m'), persona, personaEmail, personaEtiqueta,
 *          textoGenerarMes, mensajeGenerarMes(r), Modal, textoNuevo }
 */
export default function DocumentosPage({ cfg }) {
  const f = cfg.genero === 'f'
  const ESTADOS = {
    BORRADOR: { texto: 'Borrador', clase: 'bg-subtle text-ink-soft' },
    ENVIADO: { texto: f ? 'Enviada' : 'Enviado', clase: 'bg-sky-50 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300' },
    PAGADO: { texto: f ? 'Pagada' : 'Pagado', clase: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300' },
  }

  const [filas, setFilas] = useState([])
  const [q, setQ] = useState('')
  const [periodo, setPeriodo] = useState(mesActual())
  const [cargando, setCargando] = useState(true)
  const [seleccion, setSeleccion] = useState(() => new Set())
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null) // { tipo: 'ok' | 'error', texto }
  const [modal, setModal] = useState(false)
  const bloquear = useBloqueo()

  const cargar = useCallback(async () => {
    try {
      setFilas(await api(`${cfg.base}?q=${encodeURIComponent(q)}&periodo=${periodo}`))
      setSeleccion(new Set())
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    } finally {
      setCargando(false)
    }
  }, [cfg.base, q, periodo])

  useEffect(() => {
    const t = setTimeout(cargar, 250)
    return () => clearTimeout(t)
  }, [cargar])

  const cerrarModal = useCallback(() => setModal(false), [])

  function accion(fn, exito) {
    return bloquear(async () => {
      setOcupado(true)
      setAviso(null)
      try {
        const r = await fn()
        setAviso({ tipo: 'ok', texto: typeof exito === 'function' ? exito(r) : exito })
      } catch (err) {
        setAviso({ tipo: 'error', texto: err.message })
      } finally {
        await cargar() // aun con error se refresca: otro clic o pestaña pudo cambiar el estado
        setOcupado(false)
      }
    })
  }

  async function verPdf(fila) {
    try {
      const blob = await apiBlob(`${cfg.base}/${fila.id}/pdf`)
      window.open(URL.createObjectURL(blob), '_blank', 'noopener')
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    }
  }

  const generarMes = () =>
    accion(() => api(`${cfg.base}/generar-mes`, { method: 'POST', body: { periodo } }), cfg.mensajeGenerarMes)

  const enviar = (fila) =>
    accion(() => api(`${cfg.base}/${fila.id}/enviar`, { method: 'POST' }), `No. ${fila.consecutivo} enviado a ${fila[cfg.personaEmail]}.`)

  const pagar = (fila) =>
    accion(() => api(`${cfg.base}/${fila.id}/pagar`, { method: 'POST' }), `No. ${fila.consecutivo} registrado como ${f ? 'pagada' : 'pagado'} en el cuadre de banco.`)

  const enviarSeleccion = () =>
    accion(
      () => api(`${cfg.base}/enviar-lote`, { method: 'POST', body: { ids: [...seleccion] } }),
      (res) => {
        const ok = res.filter((r) => r.ok).length
        const fallos = res.filter((r) => !r.ok)
        return `Enviados: ${ok}. ${fallos.length ? `Con error: ${fallos.length} (${fallos[0].mensaje}).` : ''}`
      },
    )

  const pendientes = useMemo(() => filas.filter((x) => x.estado === 'BORRADOR'), [filas])
  const todasMarcadas = pendientes.length > 0 && pendientes.every((x) => seleccion.has(x.id))

  function alternar(id) {
    const n = new Set(seleccion)
    if (n.has(id)) n.delete(id)
    else n.add(id)
    setSeleccion(n)
  }

  const total = filas.reduce((s, x) => s + Number(x.total), 0)
  const Modal = cfg.Modal

  return (
    <div className="mx-auto max-w-6xl animate-rise">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl text-ink">{cfg.titulo}</h1>
          <p className="mt-1 text-sm text-ink-soft/80">{cfg.descripcion}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Boton variante="suave" onClick={generarMes} disabled={ocupado}>{cfg.textoGenerarMes}</Boton>
          <Boton onClick={() => setModal(true)}>{cfg.textoNuevo}</Boton>
        </div>
      </header>

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <input type="month" value={periodo} onChange={(e) => setPeriodo(e.target.value)} aria-label="Mes" className={claseInput} />
        <input type="search" placeholder={`Buscar por ${cfg.personaEtiqueta.toLowerCase()} o inmueble…`} value={q} onChange={(e) => setQ(e.target.value)} className={`${claseInput} min-w-64 flex-1`} />
        {seleccion.size > 0 && (
          <Boton onClick={enviarSeleccion} disabled={ocupado}>
            {ocupado ? 'Enviando…' : `Enviar ${seleccion.size} seleccionad${f ? 'a' : 'o'}${seleccion.size > 1 ? 's' : ''}`}
          </Boton>
        )}
      </div>

      {aviso && (
        <p
          role={aviso.tipo === 'error' ? 'alert' : 'status'}
          className={`mt-4 rounded-xl border px-4 py-3 text-sm ${aviso.tipo === 'error' ? 'border-red-200 bg-red-50 text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300' : 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300'}`}
        >
          {aviso.texto}
        </p>
      )}

      <div className="mt-4 overflow-x-auto rounded-2xl border border-line/60 bg-surface shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-subtle text-xs uppercase tracking-wider text-ink-soft">
            <tr>
              <th className="w-10 px-4 py-3">
                <input
                  type="checkbox"
                  aria-label="Seleccionar todos los pendientes"
                  checked={todasMarcadas}
                  onChange={() => setSeleccion(todasMarcadas ? new Set() : new Set(pendientes.map((x) => x.id)))}
                  className="h-4 w-4 accent-primary"
                />
              </th>
              <th className="px-4 py-3 font-semibold">N°</th>
              <th className="px-4 py-3 font-semibold">{cfg.personaEtiqueta}</th>
              <th className="px-4 py-3 font-semibold">Inmueble</th>
              <th className="px-4 py-3 text-right font-semibold">Total</th>
              <th className="px-4 py-3 font-semibold">Estado</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {cargando && <tr><td colSpan={7} className="px-4 py-10 text-center text-ink-soft/70">Cargando…</td></tr>}
            {!cargando && filas.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-ink-soft/70">{cfg.textoVacio}</td></tr>
            )}
            {filas.map((fila) => {
              const est = ESTADOS[fila.estado]
              return (
                <tr key={fila.id} className="border-t border-line/40 transition hover:bg-page">
                  <td className="px-4 py-3">
                    {fila.estado === 'BORRADOR' && (
                      <input
                        type="checkbox"
                        aria-label={`Seleccionar No. ${fila.consecutivo}`}
                        checked={seleccion.has(fila.id)}
                        onChange={() => alternar(fila.id)}
                        className="h-4 w-4 accent-primary"
                      />
                    )}
                  </td>
                  <td className="px-4 py-3 font-medium">{fila.consecutivo}</td>
                  <td className="px-4 py-3">
                    {fila[cfg.persona]}
                    <span className="block text-xs text-ink-soft/70">{fila[cfg.personaEmail] ?? 'Sin correo'}</span>
                  </td>
                  <td className="px-4 py-3">{fila.inmueble}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{moneda.format(fila.total)}</td>
                  <td className="px-4 py-3"><span className={`rounded-full px-2.5 py-1 text-xs font-medium ${est.clase}`}>{est.texto}</span></td>
                  <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-medium">
                    <button onClick={() => verPdf(fila)} className="text-ink-soft transition hover:text-ink">PDF</button>
                    {fila.estado !== 'PAGADO' && (
                      <>
                        <button onClick={() => enviar(fila)} disabled={ocupado} className="ml-4 text-ink-soft transition hover:text-ink disabled:opacity-50">
                          {fila.estado === 'ENVIADO' ? 'Reenviar' : 'Enviar'}
                        </button>
                        <button onClick={() => pagar(fila)} disabled={ocupado} className="ml-4 text-emerald-700 transition hover:text-emerald-900 dark:text-emerald-400 dark:hover:text-emerald-300 disabled:opacity-50">
                          Marcar {f ? 'pagada' : 'pagado'}
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
          {filas.length > 0 && (
            <tfoot>
              <tr className="border-t border-line bg-page text-sm font-semibold">
                <td colSpan={4} className="px-4 py-3 text-right">Total del listado</td>
                <td className="px-4 py-3 text-right tabular-nums">{moneda.format(total)}</td>
                <td colSpan={2} />
              </tr>
            </tfoot>
          )}
        </table>
      </div>

      {modal && (
        <Modal
          onCerrar={cerrarModal}
          onCreada={() => {
            setModal(false)
            setAviso({ tipo: 'ok', texto: `${cfg.singular} cread${f ? 'a' : 'o'}.` })
            cargar()
          }}
        />
      )}
    </div>
  )
}
