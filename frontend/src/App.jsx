import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import Layout from './components/Layout'
import CambiarPassword from './pages/CambiarPassword'
import Cartera from './pages/Cartera'
import ComprobantesEgreso from './pages/ComprobantesEgreso'
import CuadreBanco from './pages/CuadreBanco'
import CuentasCobro from './pages/CuentasCobro'
import Inicio from './pages/Inicio'
import Inmuebles from './pages/Inmuebles'
import Inquilinos from './pages/Inquilinos'
import Login from './pages/Login'
import Propietarios from './pages/Propietarios'
import Usuarios from './pages/Usuarios'

function Protegida({ children }) {
  const { usuario, cargando } = useAuth()
  if (cargando) return <div className="min-h-screen grid place-items-center text-ink-soft">Cargando…</div>
  if (!usuario) return <Navigate to="/login" replace />
  // Mientras deba cambiar su contraseña no ve nada más (el servidor tampoco le deja usar la API)
  if (usuario.debeCambiarPassword) return <CambiarPassword />
  return children
}

function SoloAdmin({ children }) {
  const { usuario } = useAuth()
  return usuario.rol === 'ADMIN' ? children : <Navigate to="/" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<Protegida><Layout /></Protegida>}>
            <Route path="/" element={<Inicio />} />
            <Route path="/propietarios" element={<Propietarios />} />
            <Route path="/inquilinos" element={<Inquilinos />} />
            <Route path="/inmuebles" element={<Inmuebles />} />
            <Route path="/cuentas-cobro" element={<CuentasCobro />} />
            <Route path="/comprobantes-egreso" element={<ComprobantesEgreso />} />
            <Route path="/cartera" element={<Cartera />} />
            <Route path="/banco" element={<CuadreBanco />} />
            <Route path="/cuenta/password" element={<CambiarPassword />} />
            <Route path="/usuarios" element={<SoloAdmin><Usuarios /></SoloAdmin>} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
