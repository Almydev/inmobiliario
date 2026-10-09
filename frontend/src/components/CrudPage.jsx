import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'

const input =
  'w-full rounded-xl border border-sand-300 bg-white px-3.5 py-2.5 text-brand-950 outline-none transition focus:border-brand-700 focus:ring-4 focus:ring-brand-700/15'

function Formulario({ titulo, campos, inicial, onGuardar, onCerrar }) {
  const [valores, setValores] = useState(inicial)
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await onGuardar(valores)
    } catch (err) {
      setError(err.message)
      setGuardando(false)
    }
  }

  return (
    <div className="fixed inset-0 z-20 grid place-items-center bg-brand-950/50 p-4 backdrop-blur-sm" onMouseDown={onCerrar}>
      <form
        onSubmit={enviar}
        onMouseDown={(e) => e.stopPropagation()}
        className="max-h-[92vh] w-full max-w-xl animate-rise overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl sm:p-8"
      >
        <h2 className="font-display text-2xl text-brand-900">{titulo}</h2>
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {campos.map((c) => (
            <div key={c.nombre} className={c.ancho === 'completo' ? 'sm:col-span-2' : ''}>
              <label htmlFor={c.nombre} className="mb-1 block text-sm font-medium text-brand-900">
                {c.etiqueta}
                {c.requerido && <span className="text-red-600"> *</span>}
              </label>
              <input
                id={c.nombre}
                type={c.tipo ?? 'text'}
                required={c.requerido}
                value={valores[c.nombre] ?? ''}
                onChange={(e) => setValores({ ...valores, [c.nombre]: e.target.value })}
                className={input}
              />
            </div>
          ))}
          {'activo' in inicial && (
            <label className="flex items-center gap-2 text-sm sm:col-span-2">
              <input
                type="checkbox"
                checked={Boolean(valores.activo)}
                onChange={(e) => setValores({ ...valores, activo: e.target.checked })}
                className="h-4 w-4 accent-brand-900"
              />
              Activo
            </label>
          )}
        </div>

        {error && (
          <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </p>
        )}

        <div className="mt-6 flex justify-end gap-3">
          <button type="button" onClick={onCerrar} className="rounded-xl border border-sand-300 px-4 py-2.5 text-sm transition hover:bg-sand-100">
            Cancelar
          </button>
          <button
            type="submit"
            disabled={guardando}
            className="rounded-xl bg-brand-900 px-5 py-2.5 text-sm font-medium text-sand-50 transition hover:bg-brand-800 disabled:opacity-60"
          >
            {guardando ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  )
}

/**
 * Pagina CRUD generica: listado con busqueda, crear y editar en un modal.
 * `campos`: [{ nombre, etiqueta, tipo?, requerido?, ancho? }]  `columnas`: [{ nombre, etiqueta }]
 */
export default function CrudPage({ titulo, singular, descripcion, endpoint, campos, columnas }) {
  const [filas, setFilas] = useState([])
  const [q, setQ] = useState('')
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [editando, setEditando] = useState(null) // null cerrado | {} nuevo | fila existente

  const cargar = useCallback(async () => {
    setError('')
    try {
      setFilas(await api(`${endpoint}?q=${encodeURIComponent(q)}`))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }, [endpoint, q])

  useEffect(() => {
    const t = setTimeout(cargar, 250)
    return () => clearTimeout(t)
  }, [cargar])

  async function guardar(valores) {
    const esNuevo = !editando.id
    await api(esNuevo ? endpoint : `${endpoint}/${editando.id}`, { method: esNuevo ? 'POST' : 'PUT', body: valores })
    setEditando(null)
    cargar()
  }

  const inicial = editando?.id
    ? { ...editando }
    : Object.fromEntries([...campos.map((c) => [c.nombre, '']), ['activo', true]])

  return (
    <div className="mx-auto max-w-6xl animate-rise">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl text-brand-900">{titulo}</h1>
          <p className="mt-1 text-sm text-brand-700/80">{descripcion}</p>
        </div>
        <button
          onClick={() => setEditando({})}
          className="rounded-xl bg-brand-900 px-5 py-2.5 text-sm font-medium text-sand-50 shadow-lg shadow-brand-900/15 transition hover:bg-brand-800"
        >
          + Nuevo {singular}
        </button>
      </header>

      <input
        type="search"
        placeholder="Buscar por nombre o documento…"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        className={`${input} mt-6 max-w-md`}
      />

      {error && <p role="alert" className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

      <div className="mt-4 overflow-x-auto rounded-2xl border border-sand-300/60 bg-white shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-sand-100 text-xs uppercase tracking-wider text-brand-700">
            <tr>
              {columnas.map((c) => (
                <th key={c.nombre} className="px-4 py-3 font-semibold">{c.etiqueta}</th>
              ))}
              <th className="px-4 py-3 font-semibold">Estado</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {cargando && (
              <tr><td colSpan={columnas.length + 2} className="px-4 py-10 text-center text-brand-700/70">Cargando…</td></tr>
            )}
            {!cargando && filas.length === 0 && (
              <tr>
                <td colSpan={columnas.length + 2} className="px-4 py-10 text-center text-brand-700/70">
                  {q ? 'Sin resultados para esa búsqueda.' : `Aún no hay registros. Crea el primer ${singular}.`}
                </td>
              </tr>
            )}
            {filas.map((f) => (
              <tr key={f.id} className="border-t border-sand-100 transition hover:bg-sand-50">
                {columnas.map((c) => (
                  <td key={c.nombre} className="px-4 py-3">{f[c.nombre] || <span className="text-brand-700/40">—</span>}</td>
                ))}
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${f.activo ? 'bg-emerald-50 text-emerald-700' : 'bg-sand-100 text-brand-700'}`}>
                    {f.activo ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  <button onClick={() => setEditando(f)} className="text-sm font-medium text-brand-700 transition hover:text-brand-950">
                    Editar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editando && (
        <Formulario
          key={editando.id ?? 'nuevo'}
          titulo={editando.id ? `Editar ${singular}` : `Nuevo ${singular}`}
          campos={campos}
          inicial={inicial}
          onGuardar={guardar}
          onCerrar={() => setEditando(null)}
        />
      )}
    </div>
  )
}
