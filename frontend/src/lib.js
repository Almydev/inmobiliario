export const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

export const mesActual = () => new Date().toISOString().slice(0, 7)

export const claseInput =
  'rounded-xl border border-sand-300 bg-white px-3.5 py-2.5 text-brand-950 outline-none transition focus:border-brand-700 focus:ring-4 focus:ring-brand-700/15'
