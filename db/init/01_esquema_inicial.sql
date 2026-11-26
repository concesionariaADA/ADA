-- Initial schema of the ADA dealership database (Issue #29), built from the DER in Issue #28.
--
-- PostgreSQL runs the scripts in this folder (docker-entrypoint-initdb.d) only when the
-- data volume is empty. Later schema changes go in new numbered scripts (02_..., 03_...).
--
-- Pending decisions that may add or change columns later through new scripts:
-- D05 (seller login), D06 (commission), D09 (certificate number), D11 (body/cab/use
-- categories), D18 (CUIL format), D25a/D26 (vPIC import), D27 (images), D29 (description),
-- D30.
--
-- Intentional deviation from the DER: unidad also stores tipo_vehiculo_id, tied to its
-- model through a composite foreign key, so the unit type always matches the model type
-- (RN03 f) and each subtype table can only hold units of its own type (RN03 b, RN16).

-- ---------------------------------------------------------------------------
-- Catalog: vehicle types, brands and models
-- ---------------------------------------------------------------------------
CREATE TABLE tipo_vehiculo (
    id        smallint PRIMARY KEY,
    nombre    varchar  NOT NULL,
    tipo_vpic varchar,  -- vPIC vehicle type; CAMIONETA source is pending (D01)
    CONSTRAINT uk_tipo_vehiculo_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipo_vehiculo_nombre CHECK (nombre IN ('AUTOMOVIL', 'CAMIONETA', 'MOTOCICLETA'))
);

INSERT INTO tipo_vehiculo (id, nombre, tipo_vpic) VALUES
    (1, 'AUTOMOVIL',   'car'),
    (2, 'CAMIONETA',   NULL),
    (3, 'MOTOCICLETA', 'motorcycle');

CREATE TABLE marca (
    id           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    make_id_vpic integer,  -- vPIC MakeId; nullable until D26 is decided
    nombre       varchar NOT NULL,
    habilitada   boolean NOT NULL DEFAULT false,  -- RN23: imported brands start disabled
    logo_ruta    varchar,
    baja         boolean NOT NULL DEFAULT false,  -- RN24: logical delete
    CONSTRAINT uk_marca_make_id_vpic UNIQUE (make_id_vpic),
    -- RN29: a brand can only be enabled once it has a logo.
    CONSTRAINT ck_marca_habilitada_logo CHECK (NOT habilitada OR logo_ruta IS NOT NULL)
);

-- Vehicle types in which each brand is offered.
CREATE TABLE marca_tipo_vehiculo (
    marca_id         integer  NOT NULL,
    tipo_vehiculo_id smallint NOT NULL,
    CONSTRAINT pk_marca_tipo_vehiculo PRIMARY KEY (marca_id, tipo_vehiculo_id),
    CONSTRAINT fk_marca_tipo_vehiculo_marca FOREIGN KEY (marca_id) REFERENCES marca (id),
    CONSTRAINT fk_marca_tipo_vehiculo_tipo FOREIGN KEY (tipo_vehiculo_id) REFERENCES tipo_vehiculo (id)
);

CREATE TABLE modelo (
    id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    model_id_vpic    integer,
    nombre           varchar  NOT NULL,
    marca_id         integer  NOT NULL,
    tipo_vehiculo_id smallint NOT NULL,
    baja             boolean  NOT NULL DEFAULT false,
    -- RN03 c: a model belongs to a brand in a vehicle type that brand offers.
    CONSTRAINT fk_modelo_marca_tipo FOREIGN KEY (marca_id, tipo_vehiculo_id)
        REFERENCES marca_tipo_vehiculo (marca_id, tipo_vehiculo_id),
    CONSTRAINT uk_modelo_vpic UNIQUE (marca_id, tipo_vehiculo_id, model_id_vpic),
    -- Target of the composite foreign key from unidad.
    CONSTRAINT uk_modelo_id_tipo UNIQUE (id, tipo_vehiculo_id)
);

-- ---------------------------------------------------------------------------
-- Stock: units, subtypes and images
-- ---------------------------------------------------------------------------
CREATE TABLE unidad (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_serie     varchar       NOT NULL,  -- RN22: frame number for motorcycles
    numero_motor     varchar,
    modelo_id        integer       NOT NULL,
    tipo_vehiculo_id smallint      NOT NULL,
    anio             smallint      NOT NULL,
    color            varchar       NOT NULL,
    kilometraje      integer       NOT NULL,
    precio           numeric(12,2) NOT NULL,
    condicion        varchar       NOT NULL,
    estado           varchar       NOT NULL,
    descripcion      text,  -- RN30 (D29)
    baja             boolean       NOT NULL DEFAULT false,
    CONSTRAINT uk_unidad_numero_serie UNIQUE (numero_serie),  -- RN01
    CONSTRAINT uk_unidad_numero_motor UNIQUE (numero_motor),  -- RN01
    -- RN03 f: the unit type is always the type of its model.
    CONSTRAINT fk_unidad_modelo_tipo FOREIGN KEY (modelo_id, tipo_vehiculo_id)
        REFERENCES modelo (id, tipo_vehiculo_id),
    -- Target of the composite foreign keys from the subtype tables.
    CONSTRAINT uk_unidad_id_tipo UNIQUE (id, tipo_vehiculo_id),
    CONSTRAINT ck_unidad_precio CHECK (precio > 0),  -- RN15, RN31
    CONSTRAINT ck_unidad_condicion CHECK (condicion IN ('NUEVA', 'USADA')),  -- RN02
    CONSTRAINT ck_unidad_estado
        CHECK (estado IN ('DISPONIBLE', 'RESERVADA', 'VENDIDA', 'FUERA_DE_EXHIBICION')),  -- RN02
    -- RN17: new units have 0 km, used units more than 0 km.
    CONSTRAINT ck_unidad_kilometraje CHECK (
        (condicion = 'NUEVA' AND kilometraje = 0) OR (condicion = 'USADA' AND kilometraje > 0))
);

-- Subtypes (RN16). Each row is pinned to its vehicle type, so a unit can only have
-- the subtype row that matches its type (RN03 b).
CREATE TABLE automovil (
    unidad_id        bigint   PRIMARY KEY,
    tipo_vehiculo_id smallint NOT NULL DEFAULT 1,
    transmision      varchar  NOT NULL,
    carroceria       varchar,  -- D11
    CONSTRAINT ck_automovil_tipo CHECK (tipo_vehiculo_id = 1),
    CONSTRAINT fk_automovil_unidad FOREIGN KEY (unidad_id, tipo_vehiculo_id)
        REFERENCES unidad (id, tipo_vehiculo_id),
    CONSTRAINT ck_automovil_transmision CHECK (transmision IN ('AUTOMATICA', 'MECANICA'))  -- RN10
);

CREATE TABLE camioneta (
    unidad_id        bigint   PRIMARY KEY,
    tipo_vehiculo_id smallint NOT NULL DEFAULT 2,
    transmision      varchar  NOT NULL,
    cabina           varchar,  -- D11
    traccion         varchar  NOT NULL,
    CONSTRAINT ck_camioneta_tipo CHECK (tipo_vehiculo_id = 2),
    CONSTRAINT fk_camioneta_unidad FOREIGN KEY (unidad_id, tipo_vehiculo_id)
        REFERENCES unidad (id, tipo_vehiculo_id),
    CONSTRAINT ck_camioneta_transmision CHECK (transmision IN ('AUTOMATICA', 'MECANICA')),  -- RN10
    CONSTRAINT ck_camioneta_traccion CHECK (traccion IN ('4x2', '4x4'))  -- RN16
);

CREATE TABLE motocicleta (
    unidad_id        bigint   PRIMARY KEY,
    tipo_vehiculo_id smallint NOT NULL DEFAULT 3,
    cilindrada       integer  NOT NULL,  -- cc
    categoria_uso    varchar,  -- D11
    transmision      varchar  NOT NULL,
    CONSTRAINT ck_motocicleta_tipo CHECK (tipo_vehiculo_id = 3),
    CONSTRAINT fk_motocicleta_unidad FOREIGN KEY (unidad_id, tipo_vehiculo_id)
        REFERENCES unidad (id, tipo_vehiculo_id),
    CONSTRAINT ck_motocicleta_cilindrada CHECK (cilindrada > 0),  -- RN22
    CONSTRAINT ck_motocicleta_transmision CHECK (transmision IN ('AUTOMATICA', 'MECANICA'))  -- RN10
);

CREATE TABLE imagen_unidad (
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    unidad_id bigint   NOT NULL,
    ruta      varchar  NOT NULL,
    orden     smallint NOT NULL DEFAULT 1,  -- D27
    CONSTRAINT fk_imagen_unidad_unidad FOREIGN KEY (unidad_id) REFERENCES unidad (id)
);

-- ---------------------------------------------------------------------------
-- People: employees and clients
-- ---------------------------------------------------------------------------
CREATE TABLE empleado (
    id              integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    legajo          varchar NOT NULL,  -- RN21
    nombre          varchar NOT NULL,
    apellido        varchar NOT NULL,
    funcion         varchar NOT NULL,
    nombre_usuario  varchar,  -- /admin login (RN19)
    hash_contrasena varchar,  -- seller logins pending (D05)
    baja            boolean NOT NULL DEFAULT false,
    CONSTRAINT uk_empleado_legajo UNIQUE (legajo),
    CONSTRAINT uk_empleado_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT ck_empleado_funcion CHECK (funcion IN ('VENTA', 'ADMINISTRATIVA')),  -- RN04
    -- A user name and a password hash always come together.
    CONSTRAINT ck_empleado_credenciales CHECK ((nombre_usuario IS NULL) = (hash_contrasena IS NULL)),
    -- RN19: administrative employees log in to /admin, so they need credentials.
    CONSTRAINT ck_empleado_admin_credenciales
        CHECK (funcion <> 'ADMINISTRATIVA' OR nombre_usuario IS NOT NULL)
);

CREATE TABLE cliente (
    id       integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cuil     varchar NOT NULL,  -- RN14; format check pending (D18)
    nombre   varchar NOT NULL,
    apellido varchar NOT NULL,
    telefono varchar,
    email    varchar,
    baja     boolean NOT NULL DEFAULT false,
    CONSTRAINT uk_cliente_cuil UNIQUE (cuil)
);

-- Customer service records (RN21).
CREATE TABLE atencion (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id  integer   NOT NULL,
    empleado_id integer   NOT NULL,
    fecha_hora  timestamp NOT NULL DEFAULT now(),
    detalle     text,
    CONSTRAINT fk_atencion_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_atencion_empleado FOREIGN KEY (empleado_id) REFERENCES empleado (id)
);

-- ---------------------------------------------------------------------------
-- Sales, certificates and audit
-- ---------------------------------------------------------------------------
CREATE TABLE venta (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empleado_id        integer       NOT NULL,  -- seller (RN05)
    cliente_id         integer       NOT NULL,
    unidad_vendida_id  bigint        NOT NULL,  -- RN06
    unidad_recibida_id bigint,  -- client's used unit taken as part of the payment (RN07, RN08)
    importe_operacion  numeric(12,2) NOT NULL,
    valor_reconocido   numeric(12,2),
    -- RN15, D07: there is no discount; final price = operation amount - trade-in value.
    precio_final       numeric(12,2) GENERATED ALWAYS AS
                           (importe_operacion - COALESCE(valor_reconocido, 0)) STORED,
    comision           numeric(12,2),  -- D06
    estado             varchar       NOT NULL DEFAULT 'EN_NEGOCIACION',
    fecha_hora         timestamp     NOT NULL DEFAULT now(),
    CONSTRAINT fk_venta_empleado FOREIGN KEY (empleado_id) REFERENCES empleado (id),
    CONSTRAINT fk_venta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_venta_unidad_vendida FOREIGN KEY (unidad_vendida_id) REFERENCES unidad (id),
    CONSTRAINT fk_venta_unidad_recibida FOREIGN KEY (unidad_recibida_id) REFERENCES unidad (id),
    CONSTRAINT ck_venta_importe_operacion CHECK (importe_operacion > 0),  -- RN15
    CONSTRAINT ck_venta_valor_reconocido CHECK (valor_reconocido > 0),
    CONSTRAINT ck_venta_precio_final CHECK (precio_final > 0),  -- RN15
    CONSTRAINT ck_venta_comision CHECK (comision > 0),
    CONSTRAINT ck_venta_estado CHECK (estado IN ('EN_NEGOCIACION', 'CONFIRMADA', 'CANCELADA')),  -- RN06
    -- RN07: a trade-in unit always comes with its recognized value, and vice versa.
    CONSTRAINT ck_venta_parte_de_pago CHECK ((unidad_recibida_id IS NULL) = (valor_reconocido IS NULL)),
    -- RN07: the trade-in unit cannot be the unit being sold.
    CONSTRAINT ck_venta_unidades_distintas CHECK (unidad_recibida_id <> unidad_vendida_id)
);

-- RN09: a unit can be sold, or received as a trade-in, in only one confirmed sale.
CREATE UNIQUE INDEX ux_venta_unidad_vendida_confirmada
    ON venta (unidad_vendida_id) WHERE estado = 'CONFIRMADA';
CREATE UNIQUE INDEX ux_venta_unidad_recibida_confirmada
    ON venta (unidad_recibida_id) WHERE estado = 'CONFIRMADA';

CREATE TABLE certificado_venta (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venta_id      bigint    NOT NULL,
    numero        varchar   NOT NULL,  -- D09
    fecha_emision timestamp NOT NULL DEFAULT now(),
    CONSTRAINT fk_certificado_venta_venta FOREIGN KEY (venta_id) REFERENCES venta (id),
    CONSTRAINT uk_certificado_venta_venta UNIQUE (venta_id),  -- RN25: one certificate per sale
    CONSTRAINT uk_certificado_venta_numero UNIQUE (numero)
);

-- RN20: who changed what and when.
CREATE TABLE auditoria (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_hora     timestamp NOT NULL DEFAULT now(),
    empleado_id    integer   NOT NULL,
    entidad        varchar   NOT NULL,
    entidad_id     bigint    NOT NULL,
    accion         varchar   NOT NULL,
    valor_anterior text,
    valor_nuevo    text,
    CONSTRAINT fk_auditoria_empleado FOREIGN KEY (empleado_id) REFERENCES empleado (id),
    CONSTRAINT ck_auditoria_accion
        CHECK (accion IN ('CAMBIO_PRECIO', 'CAMBIO_ESTADO', 'CONFIRMA', 'CANCELA'))
);
