import { useCallback, useEffect, useState } from 'react'
import { api, apiBlob } from '../api'
import Boton from '../components/Boton'
import ModalBase from '../components/ModalBase'
import { useBloqueo } from '../hooks'
import { claseInput, mesActual, moneda } from '../lib'

const fecha = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString('es-CO', { day: '2-digit', month: 'short' })

function Resumen({ titulo, valor, tono }) {
  const color = { neutro: 'text-ink', bien: 'text-emerald-700', mal: 'text-red-700' }[tono ?? 'neutro']
  return (
    <div className="rounded-2xl border border-line/60 bg-surface p-4 shadow-sm">
      <p className="text-xs font-medium uppercase tracking-wider text-ink-soft/70">{titulo}</p>
      <p className={`mt-1 font-display text-2xl tabular-nums ${color}`}>{moneda.format(valor)}</p>
    </div>
  )
}

function ModalMovimiento({ periodo, onCerrar, onCreado }) {
  const [tipo, setTipo] = useState('gasto')
  const [fechaMov, setFechaMov] = useState(() => {
    const hoy = new Date().toISOString().slice(0, 10)
    return hoy.startsWith(periodo) ? hoy : `${periodo}-01`
  })
  const [concepto, setConcepto] = useState('')
  const [valor, setValor] = useState('')
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)
  const bloquear = useBloqueo()

  function enviar(e) {
    e.preventDefault()
    return bloquear(async () => {
      setError('')
      setGuardando(true)
      try {
        await api('/api/banco/movimientos', {
          method: 'POST',
          body: { fecha: fechaMov, concepto, [tipo]: Number(valor) },
        })
        onCreado()
      } catch (err) {
        setError(err.message)
        setGuardando(false)
      }
    })
  }

  return (
    <ModalBase titulo="Movimiento manual" onCerrar={onCerrar} onSubmit={enviar}>
      <p className="mt-1 text-sm text-ink-soft/80">Para lo que no viene de un documento: gravámenes, comisiones, ajustes.</p>
      <div className="mt-5 space-y-4">
        <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Tipo de movimiento">
          {[['gasto', 'Gasto'], ['ingreso', 'Ingreso']].map(([v, t]) => (
            <button
              type="button"
              key={v}
              role="radio"
              aria-checked={tipo === v}
              onClick={() => setTipo(v)}
              className={`rounded-xl border px-4 py-2.5 text-sm font-medium transition ${tipo === v ? 'border-primary bg-primary text-on-primary' : 'border-line bg-surface text-ink hover:bg-subtle'}`}
            >
              {t}
            </button>
          ))}
        </div>
        <div>
          <label htmlFor="fecha" className="mb-1 block text-sm font-medium text-ink">Fecha</label>
          <input id="fecha" type="date" required value={fechaMov} onChange={(e) => setFechaMov(e.target.value)} className={`${claseInput} w-full`} />
        </div>
        <div>
          <label htmlFor="concepto" className="mb-1 block text-sm font-medium text-ink">Concepto</label>
          <input id="concepto" required maxLength={400} value={concepto} onChange={(e) => setConcepto(e.target.value)} className={`${claseInput} w-full`} />
        </div>
        <div>
          <label htmlFor="valor" className="mb-1 block text-sm font-medium text-ink">Valor (COP)</label>
          <input id="valor" type="number" required min="1" step="any" value={valor} onChange={(e) => setValor(e.target.value)} className={`${claseInput} w-full`} />
        </div>
      </div>
      {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">{error}</p>}
      <div className="mt-6 flex justify-end gap-3">
        <Boton type="button" variante="suave" onClick={onCerrar}>Cancelar</Boton>
        <Boton type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</Boton>
      </div>
    </ModalBase>
  )
}

export default function CuadreBanco() {
  const [periodo, setPeriodo] = useState(mesActual())
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [aviso, setAviso] = useState(null)
  const [modal, setModal] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      setDatos(await api(`/api/banco?periodo=${periodo}`))
      setAviso(null)
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    } finally {
      setCargando(false)
    }
  }, [periodo])

  useEffect(() => {
    cargar()
  }, [cargar])

  const cerrar = useCallback(() => setModal(false), [])

  const bloquearBorrado = useBloqueo()

  function eliminar(m) {
    if (!window.confirm(`¿Eliminar el movimiento "${m.concepto}"?`)) return undefined
    return bloquearBorrado(async () => {
      try {
        await api(`/api/banco/movimientos/${m.id}`, { method: 'DELETE' })
      } catch (err) {
        setAviso({ tipo: 'error', texto: err.message })
      } finally {
        cargar()
      }
    })
  }

  async function descargar() {
    try {
      const blob = await apiBlob(`/api/banco/csv?periodo=${periodo}`)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `cuadre-banco-${periodo}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setAviso({ tipo: 'error', texto: err.message })
    }
  }

  return (
    <div className="mx-auto max-w-6xl animate-rise">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl text-ink">Cuadre de banco</h1>
          <p className="mt-1 text-sm text-ink-soft/80">Ingresos, gastos y saldo del mes. Los pagos de cuentas de cobro y egresos aparecen solos.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Boton variante="suave" onClick={descargar} disabled={!datos}>Descargar CSV</Boton>
          <Boton onClick={() => setModal(true)}>+ Movimiento manual</Boton>
        </div>
      </header>

      <div className="mt-6">
        <input type="month" value={periodo} onChange={(e) => e.target.value && setPeriodo(e.target.value)} aria-label="Mes" className={claseInput} />
      </div>

      {aviso && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">{aviso.texto}</p>}

      {datos && (
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <Resumen titulo="Saldo inicial" valor={datos.saldoInicial} />
          <Resumen titulo="Ingresos" valor={datos.totalIngresos} tono="bien" />
          <Resumen titulo="Gastos" valor={datos.totalGastos} tono="mal" />
          <Resumen titulo="Administración" valor={datos.totalAdministracion} />
          <Resumen titulo="Saldo final" valor={datos.saldoFinal} tono={Number(datos.saldoFinal) < 0 ? 'mal' : 'neutro'} />
        </div>
      )}

      <div className="mt-4 overflow-x-auto rounded-2xl border border-line/60 bg-surface shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-subtle text-xs uppercase tracking-wider text-ink-soft">
            <tr>
              <th className="px-4 py-3 font-semibold">Fecha</th>
              <th className="px-4 py-3 font-semibold">Concepto</th>
              <th className="px-4 py-3 text-right font-semibold">Admin.</th>
              <th className="px-4 py-3 text-right font-semibold">Ingreso</th>
              <th className="px-4 py-3 text-right font-semibold">Gasto</th>
              <th className="px-4 py-3 text-right font-semibold">Saldo</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {cargando && <tr><td colSpan={7} className="px-4 py-10 text-center text-ink-soft/70">Cargando…</td></tr>}
            {!cargando && datos && datos.movimientos.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-ink-soft/70">No hay movimientos este mes.</td></tr>
            )}
            {!cargando && datos && datos.movimientos.length > 0 && (
              <tr className="border-t border-line/40 bg-page/60 text-ink-soft">
                <td className="px-4 py-2" />
                <td className="px-4 py-2 italic" colSpan={4}>Saldo inicial</td>
                <td className="px-4 py-2 text-right tabular-nums">{moneda.format(datos.saldoInicial)}</td>
                <td />
              </tr>
            )}
            {datos?.movimientos.map((m) => (
              <tr key={m.id} className="border-t border-line/40 transition hover:bg-page">
                <td className="whitespace-nowrap px-4 py-3">{fecha(m.fecha)}</td>
                <td className="px-4 py-3">
                  {m.concepto}
                  <span className="block text-xs text-ink-soft/60">{m.origen}</span>
                </td>
                <td className="px-4 py-3 text-right tabular-nums text-ink-soft/80">{Number(m.administracion) ? moneda.format(m.administracion) : '—'}</td>
                <td className="px-4 py-3 text-right tabular-nums text-emerald-700 dark:text-emerald-400">{Number(m.ingreso) ? moneda.format(m.ingreso) : '—'}</td>
                <td className="px-4 py-3 text-right tabular-nums text-red-700 dark:text-red-400">{Number(m.gasto) ? moneda.format(m.gasto) : '—'}</td>
                <td className={`px-4 py-3 text-right font-medium tabular-nums ${Number(m.saldo) < 0 ? 'text-red-700' : ''}`}>{moneda.format(m.saldo)}</td>
                <td className="px-4 py-3 text-right">
                  {m.manual && (
                    <button onClick={() => eliminar(m)} aria-label={`Eliminar ${m.concepto}`} className="text-sm text-red-700/80 transition hover:text-red-800 dark:text-red-400 dark:hover:text-red-300">
                      Eliminar
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modal && (
        <ModalMovimiento
          periodo={periodo}
          onCerrar={cerrar}
          onCreado={() => {
            setModal(false)
            cargar()
          }}
        />
      )}
    </div>
  )
}
