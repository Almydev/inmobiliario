import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'

export default function Login() {
  const { usuario, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (usuario) return <Navigate to="/" replace />

  async function enviar(e) {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await login(email.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.status === 401 ? 'Correo o contraseña incorrectos' : err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="min-h-screen grid lg:grid-cols-2 bg-sand-100">
      <section className="hidden lg:flex flex-col justify-between bg-brand-900 p-12 text-sand-100">
        <img src="/logo.jpg" alt="Soluciones Inmobiliarias 360" className="w-64 rounded-xl bg-white p-2" />
        <div>
          <h2 className="text-4xl font-semibold leading-tight">
            Cuentas de cobro, egresos y cuadre de banco en un solo lugar.
          </h2>
          <p className="mt-4 max-w-md text-sand-400">
            Genera y envía cada documento en pocos clics, con todo el histórico guardado.
          </p>
        </div>
        <p className="text-sm text-sand-400">© {new Date().getFullYear()} Soluciones Inmobiliarias 360</p>
      </section>

      <section className="flex items-center justify-center px-6 py-12">
        <form onSubmit={enviar} className="w-full max-w-sm">
          <img src="/logo.jpg" alt="Soluciones Inmobiliarias 360" className="mb-8 w-44 lg:hidden" />
          <h1 className="text-2xl font-semibold text-brand-900">Iniciar sesión</h1>
          <p className="mt-1 text-sm text-brand-700">Ingresa con tu cuenta para continuar.</p>

          <label className="mt-8 block text-sm font-medium text-brand-900" htmlFor="email">
            Correo
          </label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full rounded-lg border border-sand-400 bg-white px-3 py-2.5 outline-none transition focus:border-brand-700 focus:ring-2 focus:ring-brand-700/20"
          />

          <label className="mt-5 block text-sm font-medium text-brand-900" htmlFor="password">
            Contraseña
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded-lg border border-sand-400 bg-white px-3 py-2.5 outline-none transition focus:border-brand-700 focus:ring-2 focus:ring-brand-700/20"
          />

          {error && (
            <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={enviando}
            className="mt-6 w-full rounded-lg bg-brand-900 px-4 py-2.5 font-medium text-white transition hover:bg-brand-700 disabled:opacity-60"
          >
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>
      </section>
    </main>
  )
}
