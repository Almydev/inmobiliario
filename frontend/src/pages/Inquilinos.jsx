import CrudPage from '../components/CrudPage'

const campos = [
  { nombre: 'nombre', etiqueta: 'Nombre completo', requerido: true, ancho: 'completo' },
  { nombre: 'documento', etiqueta: 'Cédula o NIT', requerido: true },
  { nombre: 'telefono', etiqueta: 'Teléfono' },
  { nombre: 'email', etiqueta: 'Correo electrónico', tipo: 'email', ancho: 'completo' },
  { nombre: 'ciudad', etiqueta: 'Ciudad' },
  { nombre: 'direccion', etiqueta: 'Dirección' },
]

const columnas = [
  { nombre: 'nombre', etiqueta: 'Nombre' },
  { nombre: 'documento', etiqueta: 'Documento' },
  { nombre: 'telefono', etiqueta: 'Teléfono' },
  { nombre: 'email', etiqueta: 'Correo' },
  { nombre: 'ciudad', etiqueta: 'Ciudad' },
]

export default function Inquilinos() {
  return (
    <CrudPage
      titulo="Inquilinos"
      singular="inquilino"
      descripcion="Personas a quienes se les genera la cuenta de cobro del arriendo."
      endpoint="/api/inquilinos"
      campos={campos}
      columnas={columnas}
    />
  )
}
