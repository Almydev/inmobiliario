const BASE = 'rounded-xl px-4 py-2.5 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60'
const ESTILOS = {
  primario: 'bg-brand-900 text-sand-50 shadow-lg shadow-brand-900/15 hover:bg-brand-800',
  suave: 'border border-sand-300 bg-white text-brand-900 hover:bg-sand-100',
}

export default function Boton({ children, variante = 'primario', ...props }) {
  return (
    <button className={`${BASE} ${ESTILOS[variante]}`} {...props}>
      {children}
    </button>
  )
}
