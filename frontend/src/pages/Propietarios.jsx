import CrudPage from '../components/CrudPage'

const campos = [
  { nombre: 'nombre', etiqueta: 'Nombre completo', requerido: true, ancho: 'completo' },
  { nombre: 'documento', etiqueta: 'Cédula o NIT', requerido: true },
  { nombre: 'telefono', etiqueta: 'Teléfono' },
  { nombre: 'email', etiqueta: 'Correo electrónico', tipo: 'email', ancho: 'completo' },
  { nombre: 'banco', etiqueta: 'Banco' },
  { nombre: 'tipoCuenta', etiqueta: 'Tipo de cuenta' },
  { nombre: 'numeroCuenta', etiqueta: 'Número de cuenta', ancho: 'completo' },
]

const columnas = [
  { nombre: 'nombre', etiqueta: 'Nombre' },
  { nombre: 'documento', etiqueta: 'Documento' },
  { nombre: 'telefono', etiqueta: 'Teléfono' },
  { nombre: 'email', etiqueta: 'Correo' },
  { nombre: 'banco', etiqueta: 'Banco' },
]

export default function Propietarios() {
  return (
    <CrudPage
      titulo="Propietarios"
      singular="propietario"
      descripcion="Dueños de los inmuebles a quienes se les paga el arriendo."
      endpoint="/api/propietarios"
      campos={campos}
      columnas={columnas}
    />
  )
}
