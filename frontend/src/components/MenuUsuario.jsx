import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth'

const iniciales = (nombre) =>
  nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('')

/** Menú de la cuenta (arriba a la derecha): datos del usuario, cambiar contraseña y salir. */
export default function MenuUsuario() {
  const { usuario, logout } = useAuth()
  const [abierto, setAbierto] = useState(false)
  const raiz = useRef(null)

  useEffect(() => {
    if (!abierto) return undefined
    const fuera = (e) => !raiz.current?.contains(e.target) && setAbierto(false)
    const esc = (e) => e.key === 'Escape' && setAbierto(false)
    document.addEventListener('mousedown', fuera)
    document.addEventListener('keydown', esc)
    return () => {
      document.removeEventListener('mousedown', fuera)
      document.removeEventListener('keydown', esc)
    }
  }, [abierto])

  const rol = usuario.rol === 'ADMIN' ? 'Administrador' : 'Operador'

  return (
    <div ref={raiz} className="relative">
      <button
        type="button"
        onClick={() => setAbierto((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-label={`Menú de ${usuario.nombre}`}
        className="flex items-center gap-2.5 rounded-full border border-line/70 bg-surface py-1 pl-1 pr-3 transition hover:bg-subtle focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-primary text-xs font-semibold text-on-primary">{iniciales(usuario.nombre)}</span>
        <span className="hidden min-w-0 text-left leading-tight sm:block">
          <span className="block max-w-36 truncate text-sm font-medium text-ink">{usuario.nombre}</span>
          <span className="block text-xs text-ink-soft/70">{rol}</span>
        </span>
        <svg viewBox="0 0 24 24" className={`hidden h-4 w-4 text-ink-soft transition sm:block ${abierto ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>

      {abierto && (
        <div role="menu" className="absolute right-0 z-40 mt-2 w-64 origin-top-right animate-menu overflow-hidden rounded-2xl border border-line/70 bg-surface shadow-xl">
          <div className="border-b border-line/50 px-4 py-3">
            <p className="truncate text-sm font-semibold text-ink">{usuario.nombre}</p>
            <p className="truncate text-xs text-ink-soft/70">{usuario.email}</p>
            <p className="mt-1 inline-block rounded-full bg-subtle px-2 py-0.5 text-xs text-ink-soft">{rol}</p>
          </div>
          <div className="p-1.5">
            <Link
              to="/cuenta/password"
              role="menuitem"
              onClick={() => setAbierto(false)}
              className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-ink transition hover:bg-subtle"
            >
              <svg viewBox="0 0 24 24" className="h-4 w-4 text-ink-soft" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <rect x="5" y="11" width="14" height="9" rx="2" />
                <path d="M8 11V8a4 4 0 0 1 8 0v3" />
              </svg>
              Cambiar contraseña
            </Link>
            <button
              type="button"
              role="menuitem"
              onClick={logout}
              className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-red-700 transition hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-500/10"
            >
              <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M9 21H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3" />
                <path d="m16 17 5-5-5-5M21 12H9" />
              </svg>
              Cerrar sesión
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
