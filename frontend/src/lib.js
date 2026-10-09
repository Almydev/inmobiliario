export const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

export const mesActual = () => new Date().toISOString().slice(0, 7)

export const claseInput =
  'rounded-xl border border-line bg-surface px-3.5 py-2.5 text-ink outline-none transition placeholder:text-ink-soft/50 focus:border-ink-soft focus:ring-4 focus:ring-ink-soft/15'
