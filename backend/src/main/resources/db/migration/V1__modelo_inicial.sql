CREATE TABLE usuarios (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nombre        VARCHAR(150) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE propietarios (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    documento      VARCHAR(30)  NOT NULL UNIQUE,
    email          VARCHAR(150),
    telefono       VARCHAR(30),
    banco          VARCHAR(80),
    tipo_cuenta    VARCHAR(30),
    numero_cuenta  VARCHAR(40),
    activo         BOOLEAN   NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE inquilinos (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(150) NOT NULL,
    documento  VARCHAR(30)  NOT NULL UNIQUE,
    email      VARCHAR(150),
    telefono   VARCHAR(30),
    ciudad     VARCHAR(80),
    direccion  VARCHAR(200),
    activo     BOOLEAN   NOT NULL DEFAULT TRUE,
    creado_en  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE inmuebles (
    id                  BIGSERIAL PRIMARY KEY,
    descripcion         VARCHAR(200) NOT NULL,
    direccion           VARCHAR(200) NOT NULL,
    ciudad              VARCHAR(80),
    propietario_id      BIGINT NOT NULL REFERENCES propietarios(id),
    inquilino_id        BIGINT REFERENCES inquilinos(id),
    canon               NUMERIC(14,2) NOT NULL,
    pct_admin_cobro     NUMERIC(5,2)  NOT NULL DEFAULT 20,
    pct_admin_egreso    NUMERIC(5,2)  NOT NULL DEFAULT 10,
    activo              BOOLEAN   NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE SEQUENCE cuenta_cobro_consecutivo START 1;
CREATE SEQUENCE comprobante_egreso_consecutivo START 1;

CREATE TABLE cuentas_cobro (
    id                   BIGSERIAL PRIMARY KEY,
    consecutivo          BIGINT NOT NULL UNIQUE,
    fecha                DATE   NOT NULL,
    periodo              VARCHAR(7) NOT NULL,
    inmueble_id          BIGINT NOT NULL REFERENCES inmuebles(id),
    inquilino_id         BIGINT NOT NULL REFERENCES inquilinos(id),
    propietario_id       BIGINT NOT NULL REFERENCES propietarios(id),
    concepto             VARCHAR(300) NOT NULL,
    valor_arriendo       NUMERIC(14,2) NOT NULL,
    valor_administracion NUMERIC(14,2) NOT NULL DEFAULT 0,
    otros                NUMERIC(14,2) NOT NULL DEFAULT 0,
    rete_fuente          NUMERIC(14,2) NOT NULL DEFAULT 0,
    total                NUMERIC(14,2) NOT NULL,
    estado               VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    enviado_en           TIMESTAMP,
    pagado_en            TIMESTAMP,
    creado_por           BIGINT REFERENCES usuarios(id),
    creado_en            TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_cuentas_cobro_inquilino ON cuentas_cobro(inquilino_id, periodo);

CREATE TABLE comprobantes_egreso (
    id                   BIGSERIAL PRIMARY KEY,
    consecutivo          BIGINT NOT NULL UNIQUE,
    fecha                DATE   NOT NULL,
    propietario_id       BIGINT NOT NULL REFERENCES propietarios(id),
    inmueble_id          BIGINT NOT NULL REFERENCES inmuebles(id),
    concepto             VARCHAR(400) NOT NULL,
    dias                 INT NOT NULL DEFAULT 30,
    valor_bruto          NUMERIC(14,2) NOT NULL,
    valor_administracion NUMERIC(14,2) NOT NULL DEFAULT 0,
    otros_descuentos     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_pagado         NUMERIC(14,2) NOT NULL,
    imputacion_contable  VARCHAR(200),
    estado               VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    enviado_en           TIMESTAMP,
    creado_por           BIGINT REFERENCES usuarios(id),
    creado_en            TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_egresos_propietario ON comprobantes_egreso(propietario_id, fecha);

CREATE TABLE movimientos_banco (
    id                    BIGSERIAL PRIMARY KEY,
    fecha                 DATE NOT NULL,
    concepto              VARCHAR(400) NOT NULL,
    administracion        NUMERIC(14,2) NOT NULL DEFAULT 0,
    ingreso               NUMERIC(14,2) NOT NULL DEFAULT 0,
    gasto                 NUMERIC(14,2) NOT NULL DEFAULT 0,
    cuenta_cobro_id       BIGINT REFERENCES cuentas_cobro(id),
    comprobante_egreso_id BIGINT REFERENCES comprobantes_egreso(id),
    creado_en             TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_movimientos_fecha ON movimientos_banco(fecha, id);
