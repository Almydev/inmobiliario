import { useEffect, useState } from 'react'
import { api } from '../api'
import { claseInput, mesActual, moneda } from '../lib'
import { useBloqueo } from '../hooks'
import Boton from './Boton'
import ModalBase from './ModalBase'

/**
 * Formulario "nuevo documento" compartido: elige inmueble y mes, y agrega los campos propios de cada documento.
 * `campos`: [{ nombre, etiqueta, tipo: 'checkbox' | 'number' | 'text', inicial, min, max, ayuda }]
 * `etiquetaInmueble(inmueble)` arma el texto de cada opcion; `filtro(inmueble)` decide cuales se ofrecen.
 */
export default function ModalNuevoDocumento({ titulo, base, filtro, etiquetaInmueble, campos, ayudaVacio, onCerrar, onCreada }) {
  const [inmuebles, setInmuebles] = useState([])
  const [inmuebleId, setInmuebleId] = useState('')
  const [periodo, setPeriodo] = useState(mesActual())
  const [valores, setValores] = useState(() => Object.fromEntries(campos.map((c) => [c.nombre, c.inicial ?? ''])))
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)
  const bloquear = useBloqueo()

  useEffect(() => {
    api('/api/inmuebles?q=')
      .then((f) => setInmuebles(f.filter((i) => i.activo && filtro(i))))
      .catch((e) => setError(e.message))
  }, [filtro])

  function enviar(e) {
    e.preventDefault()
    return bloquear(guardar)
  }

  async function guardar() {
    setError('')
    setGuardando(true)
    try {
      const body = { inmuebleId: Number(inmuebleId), periodo }
      for (const c of campos) {
        const v = valores[c.nombre]
        body[c.nombre] = c.tipo === 'number' ? (v === '' ? null : Number(v)) : v
      }
      await api(base, { method: 'POST', body })
      onCreada()
    } catch (err) {
      setError(err.message)
      setGuardando(false)
    }
  }

  return (
    <ModalBase titulo={titulo} onCerrar={onCerrar} onSubmit={enviar}>
      <div className="mt-6 space-y-4">
        <div>
          <label htmlFor="inmueble" className="mb-1 block text-sm font-medium text-brand-900">Inmueble</label>
          <select id="inmueble" required value={inmuebleId} onChange={(e) => setInmuebleId(e.target.value)} className={`${claseInput} w-full`}>
            <option value="">Seleccionar…</option>
            {inmuebles.map((i) => (
              <option key={i.id} value={i.id}>{etiquetaInmueble(i, moneda)}</option>
            ))}
          </select>
          {inmuebles.length === 0 && <p className="mt-1 text-xs text-brand-700/70">{ayudaVacio}</p>}
        </div>
        <div>
          <label htmlFor="periodo" className="mb-1 block text-sm font-medium text-brand-900">Mes</label>
          <input id="periodo" type="month" required value={periodo} onChange={(e) => setPeriodo(e.target.value)} className={`${claseInput} w-full`} />
        </div>
        {campos.map((c) =>
          c.tipo === 'checkbox' ? (
            <label key={c.nombre} className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={Boolean(valores[c.nombre])} onChange={(e) => setValores({ ...valores, [c.nombre]: e.target.checked })} className="h-4 w-4 accent-brand-900" />
              {c.etiqueta}
            </label>
          ) : (
            <div key={c.nombre}>
              <label htmlFor={c.nombre} className="mb-1 block text-sm font-medium text-brand-900">{c.etiqueta}</label>
              <input
                id={c.nombre}
                type={c.tipo}
                min={c.min}
                max={c.max}
                step={c.tipo === 'number' ? 'any' : undefined}
                value={valores[c.nombre]}
                onChange={(e) => setValores({ ...valores, [c.nombre]: e.target.value })}
                className={`${claseInput} w-full`}
              />
              {c.ayuda && <p className="mt-1 text-xs text-brand-700/70">{c.ayuda}</p>}
            </div>
          ),
        )}
      </div>
      {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      <div className="mt-6 flex justify-end gap-3">
        <Boton type="button" variante="suave" onClick={onCerrar}>Cancelar</Boton>
        <Boton type="submit" disabled={guardando}>{guardando ? 'Generando…' : 'Generar'}</Boton>
      </div>
    </ModalBase>
  )
}
