const BASE = import.meta.env.VITE_API_URL ?? ''
const KEY = 'inmo360.token'

export const token = {
  get: () => localStorage.getItem(KEY),
  set: (t) => localStorage.setItem(KEY, t),
  clear: () => localStorage.removeItem(KEY),
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
  if (!res.ok) throw new ApiError(res.status, data?.mensaje ?? 'Ocurrió un error')
  return data
}
