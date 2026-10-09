import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import Boton from '../components/Boton'
import ModalBase from '../components/ModalBase'
import { useBloqueo } from '../hooks'
import { claseInput } from '../lib'

const ROLES = { ADMIN: 'Administrador', OPERADOR: 'Operador' }

/** Genera una clave temporal que cumple la política (el usuario deberá cambiarla al entrar). */
function claveTemporal() {
  const a = 'abcdefghijkmnpqrstuvwxyz'
  const A = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
  const n = '23456789'
  const todo = a + A + n
  const buf = new Uint32Array(14)
  crypto.getRandomValues(buf)
  const pick = (s, i) => s[buf[i] % s.length]
  const chars = [pick(a, 0), pick(A, 1), pick(n, 2), ...Array.from({ length: 11 }, (_, i) => pick(todo, i + 3))]
  return `${chars.join('')}-1`
}

function ModalUsuario({ usuario, onCerrar, onGuardado }) {
  const editando = Boolean(usuario)
  const [email, setEmail] = useState(usuario?.email ?? '')
  const [nombre, setNombre] = useState(usuario?.nombre ?? '')
  const [rol, setRol] = useState(usuario?.rol ?? 'OPERADOR')
  const [password, setPassword] = useState(editando ? '' : claveTemporal())
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [creada, setCreada] = useState(null)
  const bloquear = useBloqueo()

  function enviar(e) {
    e.preventDefault()
    return bloquear(async () => {
      setError('')
      setGuardando(true)
      try {
        if (editando) {
          const body = { nombre, rol }
          if (password) body.password = password
          await api(`/api/usuarios/${usuario.id}`, { method: 'PATCH', body })
          if (password) setCreada({ email: usuario.email, password })
          else onGuardado()
        } else {
          await api('/api/usuarios', { method: 'POST', body: { email, nombre, rol, password } })
          setCreada({ email, password })
        }
      } catch (err) {
        setError(err.message)
      } finally {
        setGuardando(false)
      }
    })
  }

  if (creada) {
    return (
      <ModalBase titulo="Usuario listo" onCerrar={onGuardado} onSubmit={(e) => { e.preventDefault(); onGuardado() }}>
        <p className="mt-3 text-sm text-ink-soft">Entrega estos datos al usuario. <strong className="text-ink">Esta es la única vez que verás la contraseña.</strong> Deberá cambiarla en su primer ingreso.</p>
        <dl className="mt-4 space-y-2 rounded-xl bg-subtle p-4 text-sm">
          <div><dt className="text-ink-soft/70">Correo</dt><dd className="font-medium text-ink">{creada.email}</dd></div>
          <div><dt className="text-ink-soft/70">Contraseña temporal</dt><dd className="select-all font-mono text-ink">{creada.password}</dd></div>
        </dl>
        <div className="mt-6 flex justify-end">
          <Boton type="submit">Listo</Boton>
        </div>
      </ModalBase>
    )
  }

  return (
    <ModalBase titulo={editando ? 'Editar usuario' : 'Nuevo usuario'} onCerrar={onCerrar} onSubmit={enviar}>
      <div className="mt-5 space-y-4">
        <div>
          <label htmlFor="email" className="mb-1 block text-sm font-medium text-ink">Correo</label>
          <input id="email" type="email" required disabled={editando} value={email} onChange={(e) => setEmail(e.target.value)} className={`${claseInput} w-full disabled:opacity-60`} />
        </div>
        <div>
          <label htmlFor="nombre" className="mb-1 block text-sm font-medium text-ink">Nombre</label>
          <input id="nombre" required maxLength={150} value={nombre} onChange={(e) => setNombre(e.target.value)} className={`${claseInput} w-full`} />
        </div>
        <div>
          <label htmlFor="rol" className="mb-1 block text-sm font-medium text-ink">Rol</label>
          <select id="rol" value={rol} onChange={(e) => setRol(e.target.value)} className={`${claseInput} w-full`}>
            {Object.entries(ROLES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="password" className="mb-1 block text-sm font-medium text-ink">{editando ? 'Restablecer contraseña (opcional)' : 'Contraseña temporal'}</label>
          <div className="flex gap-2">
            <input id="password" required={!editando} maxLength={200} value={password} onChange={(e) => setPassword(e.target.value)} className={`${claseInput} w-full font-mono`} autoComplete="off" />
            <Boton type="button" variante="suave" onClick={() => setPassword(claveTemporal())}>Generar</Boton>
          </div>
          <p className="mt-1 text-xs text-ink-soft/70">Mínimo 10 caracteres con mayúscula, minúscula y número. El usuario deberá cambiarla al entrar.</p>
        </div>
      </div>
      {error && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">{error}</p>}
      <div className="mt-6 flex justify-end gap-3">
        <Boton type="button" variante="suave" onClick={onCerrar}>Cancelar</Boton>
        <Boton type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</Boton>
      </div>
    </ModalBase>
  )
}

export default function Usuarios() {
  const { usuario: yo } = useAuth()
  const [filas, setFilas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [aviso, setAviso] = useState(null)
  const [modal, setModal] = useState(null) // null | 'nuevo' | usuario
  const bloquear = useBloqueo()

  const cargar = useCallback(async () => {
    try {
      setFilas(await api('/api/usuarios'))
    } catch (err) {
      setAviso(err.message)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  const cerrar = useCallback(() => setModal(null), [])

  function alternarActivo(u) {
    return bloquear(async () => {
      setAviso(null)
      try {
        await api(`/api/usuarios/${u.id}`, { method: 'PATCH', body: { activo: !u.activo } })
      } catch (err) {
        setAviso(err.message)
      } finally {
        cargar()
      }
    })
  }

  return (
    <div className="mx-auto max-w-4xl animate-rise">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl text-ink">Usuarios</h1>
          <p className="mt-1 text-sm text-ink-soft/80">Quién puede entrar al sistema. Un usuario desactivado pierde el acceso en segundos.</p>
        </div>
        <Boton onClick={() => setModal('nuevo')}>+ Nuevo usuario</Boton>
      </header>

      {aviso && <p role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">{aviso}</p>}

      <div className="mt-6 overflow-x-auto rounded-2xl border border-line/60 bg-surface shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-subtle text-xs uppercase tracking-wider text-ink-soft">
            <tr>
              <th className="px-4 py-3 font-semibold">Usuario</th>
              <th className="px-4 py-3 font-semibold">Rol</th>
              <th className="px-4 py-3 font-semibold">Estado</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {cargando && <tr><td colSpan={4} className="px-4 py-10 text-center text-ink-soft/70">Cargando…</td></tr>}
            {filas.map((u) => {
              const soyYo = u.email === yo.email
              return (
                <tr key={u.id} className="border-t border-line/40 transition hover:bg-page">
                  <td className="px-4 py-3">
                    <span className="font-medium text-ink">{u.nombre}{soyYo && <span className="ml-2 text-xs text-ink-soft/70">(tú)</span>}</span>
                    <span className="block text-xs text-ink-soft/70">{u.email}</span>
                  </td>
                  <td className="px-4 py-3">{ROLES[u.rol]}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${u.activo ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300' : 'bg-subtle text-ink-soft'}`}>{u.activo ? 'Activo' : 'Inactivo'}</span>
                    {u.debeCambiarPassword && <span className="ml-2 rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-800 dark:bg-amber-500/15 dark:text-amber-300">Debe cambiar clave</span>}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-right font-medium">
                    <button onClick={() => setModal(u)} className="text-ink-soft transition hover:text-ink">Editar</button>
                    {!soyYo && (
                      <button onClick={() => alternarActivo(u)} className="ml-4 text-ink-soft transition hover:text-ink">{u.activo ? 'Desactivar' : 'Activar'}</button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {modal && (
        <ModalUsuario
          key={modal === 'nuevo' ? 'nuevo' : modal.id}
          usuario={modal === 'nuevo' ? null : modal}
          onCerrar={cerrar}
          onGuardado={() => {
            setModal(null)
            cargar()
          }}
        />
      )}
    </div>
  )
}
