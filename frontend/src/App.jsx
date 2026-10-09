import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import Inicio from './pages/Inicio'
import Login from './pages/Login'

function Protegida({ children }) {
  const { usuario, cargando } = useAuth()
  if (cargando) return <div className="min-h-screen grid place-items-center text-brand-700">Cargando…</div>
  return usuario ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Protegida><Inicio /></Protegida>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
