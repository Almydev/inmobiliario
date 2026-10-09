import DocumentosPage from '../components/DocumentosPage'
import ModalNuevoDocumento from '../components/ModalNuevoDocumento'

const conInquilino = (i) => Boolean(i.inquilinoId)

function ModalNueva(props) {
  return (
    <ModalNuevoDocumento
      {...props}
      titulo="Nueva cuenta de cobro"
      base="/api/cuentas-cobro"
      filtro={conInquilino}
      etiquetaInmueble={(i, moneda) => `${i.descripcion} · ${i.inquilinoNombre} · ${moneda.format(i.canon)}`}
      ayudaVacio="Solo aparecen inmuebles activos con inquilino asignado."
      campos={[{ nombre: 'aplicarAdministracion', tipo: 'checkbox', etiqueta: 'Sumar el % de administración del inmueble al cobro', inicial: false }]}
    />
  )
}

const cfg = {
  titulo: 'Cuentas de cobro',
  descripcion: 'Genera las cuentas del mes, revisa el PDF y envíalas al inquilino por correo en pocos clics.',
  base: '/api/cuentas-cobro',
  singular: 'Cuenta de cobro',
  genero: 'f',
  persona: 'inquilino',
  personaEmail: 'inquilinoEmail',
  personaEtiqueta: 'Inquilino',
  textoGenerarMes: 'Generar todas del mes',
  textoNuevo: '+ Nueva cuenta',
  textoVacio: 'No hay cuentas de cobro para este mes. Usa “Generar todas del mes”.',
  mensajeGenerarMes: (r) => `Se crearon ${r.creadas} cuentas${r.omitidas ? ` (${r.omitidas} ya existían)` : ''}.`,
  Modal: ModalNueva,
}

export default function CuentasCobro() {
  return <DocumentosPage cfg={cfg} />
}
