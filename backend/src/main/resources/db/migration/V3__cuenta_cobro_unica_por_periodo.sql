-- Una sola cuenta de cobro por inmueble y periodo (evita duplicados por doble clic o carreras)
CREATE UNIQUE INDEX uq_cuenta_cobro_inmueble_periodo ON cuentas_cobro(inmueble_id, periodo);
