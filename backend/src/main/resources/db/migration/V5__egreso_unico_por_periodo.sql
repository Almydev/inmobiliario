-- Un solo comprobante de egreso por inmueble, periodo y dias (evita duplicados por doble clic)
CREATE UNIQUE INDEX uq_egreso_inmueble_periodo_dias ON comprobantes_egreso(inmueble_id, periodo, dias);
