import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { useBloqueo } from '../hooks'

const ANIO = new Date().getFullYear()

const BENEFICIOS = [
  ['Cuentas de cobro', 'Genera y envía cada cuenta en pocos clics.'],
  ['Pagos a propietarios', 'Comprobantes de egreso calculados al instante.'],
  ['Histórico completo', 'Todo queda guardado y a un clic de distancia.'],
]

/** Línea de casas y árboles, en el mismo estilo que el logo. */
function Ilustracion() {
  const trazo = { fill: 'none', strokeLinecap: 'round', strokeLinejoin: 'round', pathLength: 1 }
  const draw = { strokeDasharray: 1, strokeDashoffset: 1 }
  return (
    <svg viewBox="0 0 520 220" className="w-full max-w-lg animate-float" aria-hidden="true">
      <g stroke="#c9a98a" strokeWidth="2.5" {...trazo}>
        <path d="M20 190 L130 90 L240 190" style={{ ...draw, animation: 'var(--animate-draw)' }} />
        <path d="M150 74 V52 H172 V90" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '0.3s' }} />
        <rect x="86" y="130" width="64" height="60" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '0.6s' }} />
        <path d="M118 130 V190 M86 160 H150" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '0.9s' }} />
      </g>
      <g stroke="#f4ede4" strokeWidth="2.5" {...trazo}>
        <path d="M230 190 V110 L330 60 L430 110 V190" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '0.5s' }} />
        <path d="M385 82 V58 H408 V98" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '0.8s' }} />
        <path d="M290 190 V140 H340 V190" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '1.1s' }} />
      </g>
      <g stroke="#c9a98a" strokeWidth="2.5" {...trazo}>
        <path d="M452 190 V160 M452 160 C430 150 436 112 452 100 C468 112 474 150 452 160" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '1.2s' }} />
        <path d="M490 190 V150 M490 150 C464 138 474 90 490 76 C506 90 516 138 490 150" style={{ ...draw, animation: 'var(--animate-draw)', animationDelay: '1.4s' }} />
      </g>
      <path d="M0 190 H520" stroke="#c9a98a" strokeOpacity=".5" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

function Campo({ id, etiqueta, derecha, ...props }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-brand-900">
        {etiqueta}
      </label>
      <div className="relative">
        <input
          id={id}
          className="w-full rounded-xl border border-sand-300 bg-white px-4 py-3 text-brand-950 shadow-sm outline-none transition placeholder:text-brand-900/30 focus:border-brand-700 focus:ring-4 focus:ring-brand-700/15"
          {...props}
        />
        {derecha}
      </div>
    </div>
  )
}

export default function Login() {
  const { usuario, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [ver, setVer] = useState(false)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [intento, setIntento] = useState(0)
  const bloquear = useBloqueo()

  if (usuario) return <Navigate to="/" replace />

  function enviar(e) {
    e.preventDefault()
    return bloquear(async () => {
      setError('')
      setEnviando(true)
      try {
        await login(email.trim(), password)
        navigate('/', { replace: true })
      } catch (err) {
        setError(err.status === 401 ? 'Correo o contraseña incorrectos.' : err.message)
        setIntento((n) => n + 1)
      } finally {
        setEnviando(false)
      }
    })
  }

  return (
    <main className="grid min-h-screen bg-sand-50 lg:grid-cols-[1.05fr_1fr]">
      {/* Panel de marca */}
      <section className="relative hidden overflow-hidden bg-brand-950 lg:flex lg:flex-col lg:justify-between lg:p-14">
        <div className="pointer-events-none absolute -left-32 -top-32 h-96 w-96 rounded-full bg-brand-700/40 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-40 right-0 h-[28rem] w-[28rem] rounded-full bg-sand-400/10 blur-3xl" />
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.06]"
          style={{
            backgroundImage:
              'linear-gradient(#f4ede4 1px, transparent 1px), linear-gradient(90deg, #f4ede4 1px, transparent 1px)',
            backgroundSize: '48px 48px',
          }}
        />

        <p className="relative animate-rise font-display text-lg tracking-[0.3em] text-sand-300">
          SOLUCIONES INMOBILIARIAS <span className="text-sand-400">360</span>
        </p>

        <div className="relative">
          <h2
            className="animate-rise font-display text-5xl font-medium leading-[1.1] text-sand-50"
            style={{ animationDelay: '0.1s' }}
          >
            Tu cartera de arriendos,
            <br />
            <span className="text-sand-400">en orden y a un clic.</span>
          </h2>

          <div className="mt-10">
            <Ilustracion />
          </div>

          <ul className="mt-10 grid gap-5 sm:grid-cols-3">
            {BENEFICIOS.map(([titulo, texto], i) => (
              <li key={titulo} className="animate-rise border-t border-sand-400/30 pt-4" style={{ animationDelay: `${0.3 + i * 0.12}s` }}>
                <p className="text-sm font-semibold text-sand-100">{titulo}</p>
                <p className="mt-1 text-sm leading-relaxed text-sand-300/80">{texto}</p>
              </li>
            ))}
          </ul>
        </div>

        <p className="relative text-xs text-sand-300/60">© {ANIO} Soluciones Inmobiliarias 360 · La Ceja, Antioquia</p>
      </section>

      {/* Formulario */}
      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-md animate-rise">
          <div className="rounded-3xl border border-sand-300/60 bg-white p-8 shadow-[0_20px_60px_-25px_rgba(31,61,74,0.35)] backdrop-blur sm:p-10">
            <img src="/logo.jpg" alt="Soluciones Inmobiliarias 360" className="mx-auto -mt-2 mb-4 w-56" />
            <h1 className="font-display text-3xl font-medium text-brand-900">Bienvenido</h1>
            <p className="mt-1.5 text-sm text-brand-700/80">Ingresa con tu cuenta para gestionar tus arriendos.</p>

            <form onSubmit={enviar} className="mt-8 space-y-5">
              <Campo
                id="email"
                etiqueta="Correo electrónico"
                type="email"
                autoComplete="username"
                placeholder="tucorreo@empresa.com"
                required
                autoFocus
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <Campo
                id="password"
                etiqueta="Contraseña"
                type={ver ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                derecha={
                  <button
                    type="button"
                    onClick={() => setVer((v) => !v)}
                    aria-label={ver ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    className="absolute inset-y-0 right-0 px-4 text-xs font-medium text-brand-700 transition hover:text-brand-950"
                  >
                    {ver ? 'Ocultar' : 'Mostrar'}
                  </button>
                }
              />

              {error && (
                <p
                  key={intento}
                  role="alert"
                  className="animate-shake rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
                >
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={enviando}
                className="group relative flex w-full items-center justify-center gap-2 overflow-hidden rounded-xl bg-brand-900 px-4 py-3.5 font-medium text-sand-50 shadow-lg shadow-brand-900/20 transition hover:bg-brand-800 focus:outline-none focus-visible:ring-4 focus-visible:ring-brand-700/30 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-70"
              >
                {enviando && (
                  <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="3" opacity=".25" />
                    <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
                  </svg>
                )}
                {enviando ? 'Ingresando…' : 'Ingresar'}
              </button>
            </form>

            {enviando && (
              <p className="mt-4 text-center text-xs text-brand-700/70">
                Si el servidor estaba inactivo, la primera vez puede tardar hasta un minuto.
              </p>
            )}
          </div>

          <p className="mt-6 text-center text-xs text-brand-700/60">¿Problemas para ingresar? Contacta al administrador.</p>
        </div>
      </section>
    </main>
  )
}
