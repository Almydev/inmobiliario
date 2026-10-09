-- Periodo (AAAA-MM) del comprobante de egreso, para filtrar y evitar duplicados
ALTER TABLE comprobantes_egreso ADD COLUMN periodo VARCHAR(7) NOT NULL DEFAULT '1970-01';
ALTER TABLE comprobantes_egreso ALTER COLUMN periodo DROP DEFAULT;
CREATE INDEX idx_egresos_periodo ON comprobantes_egreso(periodo);
