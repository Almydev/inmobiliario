const BASE = 'rounded-xl px-4 py-2.5 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60'
const ESTILOS = {
  primario: 'bg-primary text-on-primary shadow-lg shadow-primary/15 hover:bg-primary-hover',
  suave: 'border border-line bg-surface text-ink hover:bg-subtle',
}

export default function Boton({ children, variante = 'primario', ...props }) {
  return (
    <button className={`${BASE} ${ESTILOS[variante]}`} {...props}>
      {children}
    </button>
  )
}
