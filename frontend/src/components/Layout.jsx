import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'
import TemaBoton from './TemaBoton'

const ENLACES = [
  { a: '/', texto: 'Inicio', fin: true },
  { a: '/propietarios', texto: 'Propietarios' },
  { a: '/inquilinos', texto: 'Inquilinos' },
  { a: '/inmuebles', texto: 'Inmuebles' },
  { a: '/cuentas-cobro', texto: 'Cuentas de cobro' },
  { a: '/cartera', texto: 'Cartera' },
  { a: '/comprobantes-egreso', texto: 'Comprobantes de egreso' },
  { a: '/banco', texto: 'Cuadre de banco' },
]

export default function Layout() {
  const { usuario, logout } = useAuth()
  const enlaces = usuario.rol === 'ADMIN' ? [...ENLACES, { a: '/usuarios', texto: 'Usuarios' }] : ENLACES
  return (
    <div className="min-h-screen bg-page text-ink lg:grid lg:grid-cols-[16rem_1fr]">
      <aside className="flex flex-col bg-brand-950 text-sand-100 lg:min-h-screen">
        <div className="px-6 py-6">
          <p className="font-display text-xl leading-tight">
            Inmobiliarias <span className="text-sand-400">360</span>
          </p>
          <p className="mt-1 text-xs tracking-widest text-sand-300/60">PANEL DE GESTIÓN</p>
        </div>

        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 lg:flex-1 lg:flex-col lg:overflow-visible lg:pb-0">
          {enlaces.map((l) => (
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
          <NavLink to="/cuenta/password" title="Cambiar mi contraseña" aria-label="Cambiar mi contraseña" className="ml-auto grid h-9 w-9 place-items-center rounded-lg text-sand-300 transition hover:bg-brand-800 hover:text-white">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <rect x="5" y="11" width="14" height="9" rx="2" />
              <path d="M8 11V8a4 4 0 0 1 8 0v3" />
            </svg>
          </NavLink>
          <TemaBoton className="text-sand-300 hover:bg-brand-800 hover:text-white" />
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
