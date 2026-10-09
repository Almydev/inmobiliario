import CrudPage from '../components/CrudPage'

const campos = [
  { nombre: 'descripcion', etiqueta: 'Nombre del inmueble', requerido: true, ancho: 'completo' },
  { nombre: 'direccion', etiqueta: 'Dirección', requerido: true },
  { nombre: 'ciudad', etiqueta: 'Ciudad' },
  { nombre: 'propietarioId', etiqueta: 'Propietario', tipo: 'select', fuente: '/api/propietarios', requerido: true, ancho: 'completo' },
  { nombre: 'inquilinoId', etiqueta: 'Inquilino actual', tipo: 'select', fuente: '/api/inquilinos', vacio: 'Sin inquilino (disponible)', ancho: 'completo' },
  { nombre: 'canon', etiqueta: 'Canon mensual (COP)', tipo: 'number', requerido: true, ancho: 'completo' },
  { nombre: 'pctAdminCobro', etiqueta: '% administración (cuenta de cobro)', tipo: 'number', porDefecto: 20 },
  { nombre: 'pctAdminEgreso', etiqueta: '% administración (egreso)', tipo: 'number', porDefecto: 10 },
]

const columnas = [
  { nombre: 'descripcion', etiqueta: 'Inmueble' },
  { nombre: 'direccion', etiqueta: 'Dirección' },
  { nombre: 'propietarioNombre', etiqueta: 'Propietario' },
  { nombre: 'inquilinoNombre', etiqueta: 'Inquilino' },
  { nombre: 'canon', etiqueta: 'Canon', formato: 'moneda' },
  { nombre: 'pctAdminEgreso', etiqueta: 'Admin. egreso', formato: 'porcentaje' },
]

export default function Inmuebles() {
  return (
    <CrudPage
      titulo="Inmuebles"
      singular="inmueble"
      descripcion="Cada inmueble une un propietario, su inquilino actual y el canon que alimenta las cuentas de cobro y los egresos."
      endpoint="/api/inmuebles"
      campos={campos}
      columnas={columnas}
      busqueda="Buscar por inmueble, dirección, propietario o inquilino…"
    />
  )
}
