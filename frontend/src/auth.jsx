import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, token } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [cargando, setCargando] = useState(Boolean(token.get()))

  useEffect(() => {
    if (!token.get()) return
    api('/api/auth/me')
      .then(setUsuario)
      .catch(() => token.clear())
      .finally(() => setCargando(false))
  }, [])

  const login = useCallback(async (email, password) => {
    const data = await api('/api/auth/login', { method: 'POST', body: { email, password } })
    token.set(data.token)
    setUsuario(data.usuario)
  }, [])

  const logout = useCallback(() => {
    token.clear()
    setUsuario(null)
  }, [])

  const value = useMemo(() => ({ usuario, cargando, login, logout }), [usuario, cargando, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
