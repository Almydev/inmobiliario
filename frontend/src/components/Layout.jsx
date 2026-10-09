import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth'
import MenuUsuario from './MenuUsuario'
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

function Marca() {
  return (
    <div>
      <p className="font-display text-xl leading-tight">
        Inmobiliarias <span className="text-sand-400">360</span>
      </p>
      <p className="mt-1 text-xs tracking-widest text-sand-300/60">PANEL DE GESTIÓN</p>
    </div>
  )
}

function Navegacion({ enlaces, onNavegar }) {
  return (
    <nav aria-label="Principal" className="flex flex-col gap-1 px-3">
      {enlaces.map((l) => (
        <NavLink
          key={l.a}
          to={l.a}
          end={l.fin}
          onClick={onNavegar}
          className={({ isActive }) =>
            `rounded-lg px-3 py-2.5 text-sm font-medium transition ${isActive ? 'bg-brand-800 text-white' : 'text-sand-300 hover:bg-brand-900 hover:text-white'}`
          }
        >
          {l.texto}
        </NavLink>
      ))}
    </nav>
  )
}

export default function Layout() {
  const { usuario } = useAuth()
  const { pathname } = useLocation()
  const [menu, setMenu] = useState(false)
  const enlaces = usuario.rol === 'ADMIN' ? [...ENLACES, { a: '/usuarios', texto: 'Usuarios' }] : ENLACES

  // El menú móvil se cierra al navegar, con Esc, y bloquea el scroll del fondo mientras está abierto
  useEffect(() => setMenu(false), [pathname])
  useEffect(() => {
    if (!menu) return undefined
    const previo = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const esc = (e) => e.key === 'Escape' && setMenu(false)
    window.addEventListener('keydown', esc)
    return () => {
      document.body.style.overflow = previo
      window.removeEventListener('keydown', esc)
    }
  }, [menu])

  return (
    <div className="min-h-screen bg-page text-ink lg:grid lg:grid-cols-[16rem_1fr]">
      {/* Barra lateral (escritorio): solo marca y navegación */}
      <aside className="hidden bg-brand-950 text-sand-100 lg:block">
        <div className="sticky top-0 flex max-h-screen flex-col gap-6 overflow-y-auto py-6">
          <div className="px-6"><Marca /></div>
          <Navegacion enlaces={enlaces} />
        </div>
      </aside>

      {/* Menú desplegable (móvil y tablet) */}
      {menu && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Menú de navegación">
          <button type="button" aria-label="Cerrar menú" className="absolute inset-0 bg-brand-950/60 backdrop-blur-sm" onClick={() => setMenu(false)} />
          <div className="relative flex h-full w-72 max-w-[85vw] animate-drawer flex-col gap-6 overflow-y-auto bg-brand-950 py-6 text-sand-100 shadow-2xl">
            <div className="flex items-start justify-between px-6">
              <Marca />
              <button type="button" onClick={() => setMenu(false)} aria-label="Cerrar menú" className="grid h-9 w-9 place-items-center rounded-lg text-sand-300 hover:bg-brand-800 hover:text-white">
                <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18" /></svg>
              </button>
            </div>
            <Navegacion enlaces={enlaces} onNavegar={() => setMenu(false)} />
          </div>
        </div>
      )}

      <div className="flex min-w-0 flex-col">
        {/* Barra superior: menú (móvil), tema y cuenta */}
        <header className="sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-line/60 bg-page/85 px-4 py-2.5 backdrop-blur sm:px-8">
          <div className="flex min-w-0 items-center gap-3">
            <button
              type="button"
              onClick={() => setMenu(true)}
              aria-label="Abrir menú"
              className="grid h-10 w-10 place-items-center rounded-xl border border-line/70 bg-surface text-ink transition hover:bg-subtle lg:hidden"
            >
              <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16" /></svg>
            </button>
            <p className="truncate font-display text-lg text-ink lg:hidden">
              Inmobiliarias <span className="text-sand-400">360</span>
            </p>
          </div>
          <div className="flex items-center gap-2">
            <TemaBoton className="border border-line/70 bg-surface text-ink-soft hover:bg-subtle hover:text-ink" />
            <MenuUsuario />
          </div>
        </header>

        <main className="min-w-0 flex-1 p-4 sm:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
