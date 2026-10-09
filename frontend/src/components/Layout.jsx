import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'

const ENLACES = [
  { a: '/', texto: 'Inicio', fin: true },
  { a: '/propietarios', texto: 'Propietarios' },
  { a: '/inquilinos', texto: 'Inquilinos' },
  { a: '/inmuebles', texto: 'Inmuebles' },
  { a: '/cuentas-cobro', texto: 'Cuentas de cobro' },
  { a: '/comprobantes-egreso', texto: 'Comprobantes de egreso' },
]

export default function Layout() {
  const { usuario, logout } = useAuth()
  return (
    <div className="min-h-screen bg-sand-50 text-brand-900 lg:grid lg:grid-cols-[16rem_1fr]">
      <aside className="flex flex-col bg-brand-950 text-sand-100 lg:min-h-screen">
        <div className="px-6 py-6">
          <p className="font-display text-xl leading-tight">
            Inmobiliarias <span className="text-sand-400">360</span>
          </p>
          <p className="mt-1 text-xs tracking-widest text-sand-300/60">PANEL DE GESTIÓN</p>
        </div>

        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 lg:flex-1 lg:flex-col lg:overflow-visible lg:pb-0">
          {ENLACES.map((l) => (
            <NavLink
              key={l.a}
              to={l.a}
              end={l.fin}
              className={({ isActive }) =>
                `whitespace-nowrap rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                  isActive ? 'bg-brand-800 text-white' : 'text-sand-300 hover:bg-brand-900 hover:text-white'
                }`
              }
            >
              {l.texto}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center justify-between gap-3 border-t border-brand-800 px-6 py-4 text-sm">
          <div className="min-w-0">
            <p className="truncate font-medium">{usuario.nombre}</p>
            <p className="text-xs text-sand-300/70">{usuario.rol === 'ADMIN' ? 'Administrador' : 'Operador'}</p>
          </div>
          <button onClick={logout} className="rounded-lg border border-sand-400/40 px-3 py-1.5 text-xs transition hover:bg-brand-800">
            Salir
          </button>
        </div>
      </aside>

      <main className="p-5 sm:p-8">
        <Outlet />
      </main>
    </div>
  )
}
