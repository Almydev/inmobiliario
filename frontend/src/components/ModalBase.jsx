import { useEffect } from 'react'
import { createPortal } from 'react-dom'

/** Ventana emergente centrada en la pantalla (se monta en <body> para no heredar transformaciones). */
export default function ModalBase({ titulo, onCerrar, onSubmit, children }) {
  useEffect(() => {
    const previo = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const esc = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', esc)
    return () => {
      document.body.style.overflow = previo
      window.removeEventListener('keydown', esc)
    }
  }, [onCerrar])

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-label={titulo}
      className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-brand-950/50 p-4 backdrop-blur-sm sm:items-center"
      onMouseDown={onCerrar}
    >
      <form onSubmit={onSubmit} onMouseDown={(e) => e.stopPropagation()} className="my-auto w-full max-w-md animate-rise rounded-2xl bg-surface p-6 shadow-2xl sm:p-8">
        <h2 className="font-display text-2xl text-ink">{titulo}</h2>
        {children}
      </form>
    </div>,
    document.body,
  )
}
