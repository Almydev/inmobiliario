import DocumentosPage from '../components/DocumentosPage'
import ModalNuevoDocumento from '../components/ModalNuevoDocumento'

const activos = () => true

function ModalNuevo(props) {
  return (
    <ModalNuevoDocumento
      {...props}
      titulo="Nuevo comprobante de egreso"
      base="/api/comprobantes-egreso"
      filtro={activos}
      etiquetaInmueble={(i, moneda) => `${i.descripcion} · ${i.propietarioNombre} · ${moneda.format(i.canon)}`}
      ayudaVacio="Solo aparecen inmuebles activos."
      campos={[
        { nombre: 'dias', tipo: 'number', etiqueta: 'Días a pagar', inicial: 30, min: 1, max: 30, ayuda: 'Con menos de 30 días el valor se prorratea (canon ÷ 30 × días).' },
        { nombre: 'otrosDescuentos', tipo: 'number', etiqueta: 'Otros descuentos (COP)', inicial: 0, min: 0 },
        { nombre: 'imputacionContable', tipo: 'text', etiqueta: 'Imputación contable (opcional)', inicial: '' },
      ]}
    />
  )
}

const cfg = {
  titulo: 'Comprobantes de egreso',
  descripcion: 'Pagos a propietarios: canon menos administración y descuentos, con PDF y envío por correo.',
  base: '/api/comprobantes-egreso',
  singular: 'Comprobante de egreso',
  genero: 'm',
  persona: 'propietario',
  personaEmail: 'propietarioEmail',
  personaEtiqueta: 'Propietario',
  textoGenerarMes: 'Generar de cuentas pagadas',
  textoNuevo: '+ Nuevo comprobante',
  textoVacio: 'No hay comprobantes para este mes. Cuando se pague una cuenta de cobro, usa “Generar de cuentas pagadas”.',
  mensajeGenerarMes: (r) => `Se crearon ${r.creados} comprobantes${r.omitidos ? ` (${r.omitidos} ya existían)` : ''}.`,
  Modal: ModalNuevo,
}

export default function ComprobantesEgreso() {
  return <DocumentosPage cfg={cfg} />
}
