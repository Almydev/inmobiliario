import { Link } from 'react-router-dom'
import { useAuth } from '../auth'

const ACCESOS = [
  { a: '/propietarios', titulo: 'Propietarios', texto: 'Registra a los dueños y sus datos de pago.' },
  { a: '/inquilinos', titulo: 'Inquilinos', texto: 'Registra a quienes reciben la cuenta de cobro.' },
]

export default function Inicio() {
  const { usuario } = useAuth()
  return (
    <div className="mx-auto max-w-6xl animate-rise">
      <h1 className="font-display text-3xl text-brand-900">Bienvenido, {usuario.nombre}</h1>
      <p className="mt-1 text-sm text-brand-700/80">Elige un módulo para empezar.</p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        {ACCESOS.map((x) => (
          <Link
            key={x.a}
            to={x.a}
            className="group rounded-2xl border border-sand-300/60 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:border-brand-700/40 hover:shadow-md"
          >
            <h2 className="font-display text-xl text-brand-900">{x.titulo}</h2>
            <p className="mt-1 text-sm text-brand-700/80">{x.texto}</p>
            <span className="mt-4 inline-block text-sm font-medium text-brand-700 transition group-hover:translate-x-1">Abrir →</span>
          </Link>
        ))}
      </div>
    </div>
  )
}
