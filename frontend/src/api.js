const BASE = import.meta.env.VITE_API_URL ?? ''
const KEY = 'inmo360.token'

export const token = {
  get: () => localStorage.getItem(KEY),
  set: (t) => localStorage.setItem(KEY, t),
  clear: () => localStorage.removeItem(KEY),
}

/** Limpia el token y avisa a la app para que lleve al usuario al login. */
function sesionVencida() {
  token.clear()
  window.dispatchEvent(new Event('sesion-vencida'))
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const t = token.get()
  if (t) headers.Authorization = `Bearer ${t}`

  let res
  try {
    res = await fetch(`${BASE}${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor')
  }
  const data = await res.json().catch(() => null)
  if (res.status === 401 && token.get() && !path.startsWith('/api/auth/login')) {
    sesionVencida()
    throw new ApiError(401, 'Tu sesión expiró. Inicia sesión de nuevo.')
  }
  if (!res.ok) throw new ApiError(res.status, data?.mensaje ?? 'Ocurrió un error')
  return data
}

/** Descarga un archivo (p. ej. un PDF) enviando el token, y lo devuelve como Blob. */
export async function apiBlob(path) {
  const t = token.get()
  let res
  try {
    res = await fetch(`${BASE}${path}`, { headers: t ? { Authorization: `Bearer ${t}` } : {} })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor')
  }
  if (!res.ok) {
    if (res.status === 401) {
      sesionVencida()
      throw new ApiError(401, 'Tu sesión expiró. Inicia sesión de nuevo.')
    }
    const data = await res.json().catch(() => null)
    throw new ApiError(res.status, data?.mensaje ?? 'No se pudo descargar el archivo')
  }
  return res.blob()
}
