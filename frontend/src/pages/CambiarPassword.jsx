import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import Boton from '../components/Boton'
import { useBloqueo } from '../hooks'
import { claseInput } from '../lib'

/** Mismas reglas que el servidor (que es quien decide); aquí solo para guiar al usuario mientras escribe. */
const REGLAS = [
  ['Al menos 10 caracteres', (p) => p.length >= 10],
  ['Una letra minúscula', (p) => /[a-zñ]/.test(p)],
  ['Una letra mayúscula', (p) => /[A-ZÑ]/.test(p)],
  ['Un número', (p) => /\d/.test(p)],
]

function Campo({ id, etiqueta, valor, onCambio, autoComplete, ver }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-ink">{etiqueta}</label>
      <input id={id} type={ver ? 'text' : 'password'} required autoComplete={autoComplete} value={valor} onChange={(e) => onCambio(e.target.value)} maxLength={200} className={`${claseInput} w-full`} />
    </div>
  )
}

export default function CambiarPassword() {
  const { usuario, passwordCambiada, logout } = useAuth()
  const navigate = useNavigate()
  const obligatorio = usuario.debeCambiarPassword
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [repetida, setRepetida] = useState('')
  const [ver, setVer] = useState(false)
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [listo, setListo] = useState(false)
  const bloquear = useBloqueo()

  const cumplidas = REGLAS.map(([, ok]) => ok(nueva))
  const fuerte = cumplidas.every(Boolean)
  const coincide = nueva !== '' && nueva === repetida
  const puntaje = cumplidas.filter(Boolean).length

  function enviar(e) {
    e.preventDefault()
    if (!fuerte) return setError('La nueva contraseña no cumple todas las reglas.')
    if (!coincide) return setError('Las contraseñas nuevas no coinciden.')
    return bloquear(async () => {
      setError('')
      setGuardando(true)
      try {
        await api('/api/auth/password', { method: 'PATCH', body: { actual, nueva } })
        passwordCambiada()
        setListo(true)
        if (obligatorio) navigate('/', { replace: true })
      } catch (err) {
        setError(err.message)
      } finally {
        setGuardando(false)
      }
    })
  }

  return (
    <div className={obligatorio ? 'grid min-h-screen place-items-center bg-page px-4 py-10' : ''}>
      <div className={obligatorio ? 'w-full max-w-md' : 'mx-auto max-w-lg animate-rise'}>
        {obligatorio && (
          <div className="mb-4 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200" role="status">
            Por seguridad debes cambiar tu contraseña antes de continuar.
          </div>
        )}
        <form onSubmit={enviar} className="rounded-2xl border border-line/60 bg-surface p-6 shadow-sm sm:p-8">
          <h1 className="font-display text-2xl text-ink">Cambiar contraseña</h1>
          <p className="mt-1 text-sm text-ink-soft/80">{usuario.email}</p>

          <div className="mt-6 space-y-4">
            <Campo id="actual" etiqueta="Contraseña actual" valor={actual} onCambio={setActual} autoComplete="current-password" ver={ver} />
            <Campo id="nueva" etiqueta="Contraseña nueva" valor={nueva} onCambio={setNueva} autoComplete="new-password" ver={ver} />

            <div aria-hidden="true" className="flex gap-1">
              {[0, 1, 2, 3].map((i) => (
                <span key={i} className={`h-1.5 flex-1 rounded-full transition-colors ${i < puntaje ? (puntaje < 4 ? 'bg-amber-400' : 'bg-emerald-500') : 'bg-subtle'}`} />
              ))}
            </div>
            <ul className="space-y-1 text-sm">
              {REGLAS.map(([texto], i) => (
                <li key={texto} className={cumplidas[i] ? 'text-emerald-700 dark:text-emerald-400' : 'text-ink-soft/70'}>
                  <span aria-hidden="true">{cumplidas[i] ? '✓' : '○'}</span> {texto}
                </li>
              ))}
            </ul>

            <Campo id="repetida" etiqueta="Repite la contraseña nueva" valor={repetida} onCambio={setRepetida} autoComplete="new-password" ver={ver} />
            {repetida !== '' && !coincide && <p className="text-sm text-red-700 dark:text-red-400">No coincide.</p>}

            <label className="flex items-center gap-2 text-sm text-ink">
              <input type="checkbox" checked={ver} onChange={(e) => setVer(e.target.checked)} className="h-4 w-4 accent-primary" />
              Mostrar contraseñas
            </label>
          </div>

          {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">{error}</p>}
          {listo && !obligatorio && <p role="status" className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">Contraseña actualizada.</p>}

          <div className="mt-6 flex items-center justify-between gap-3">
            {obligatorio ? <button type="button" onClick={logout} className="text-sm text-ink-soft hover:text-ink">Salir</button> : <span />}
            <Boton type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar contraseña'}</Boton>
          </div>
        </form>
      </div>
    </div>
  )
}
