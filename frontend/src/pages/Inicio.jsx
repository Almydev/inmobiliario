import { useAuth } from '../auth'

export default function Inicio() {
  const { usuario, logout } = useAuth()
  return (
    <div className="min-h-screen bg-sand-100 text-brand-900">
      <header className="flex items-center justify-between bg-brand-900 px-6 py-3 text-sand-100">
        <span className="font-semibold tracking-wide">Soluciones Inmobiliarias 360</span>
        <div className="flex items-center gap-4 text-sm">
          <span>
            {usuario.nombre} · {usuario.rol === 'ADMIN' ? 'Administrador' : 'Operador'}
          </span>
          <button onClick={logout} className="rounded-lg border border-sand-400 px-3 py-1 transition hover:bg-brand-700">
            Salir
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl p-6">
        <h1 className="text-2xl font-semibold">Bienvenido, {usuario.nombre}</h1>
        <p className="mt-2 text-brand-700">Aquí irán los módulos: propietarios, inquilinos, cuentas de cobro y egresos.</p>
      </main>
    </div>
  )
}
