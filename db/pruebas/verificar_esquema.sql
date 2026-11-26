-- Checks the schema created by db/init/01_esquema_inicial.sql (Issue #29).
--
-- Run it against the Docker database:
--   docker exec -i ada-postgres sh -c 'psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < db/pruebas/verificar_esquema.sql
--
-- Everything runs inside one transaction that is rolled back at the end,
-- so the database is left exactly as it was. Any failed check stops the run
-- with "FAIL: ..." and a non-zero exit code.

\set ON_ERROR_STOP on
SET client_min_messages = notice;
BEGIN;

-- debe_fallar: the statement must be rejected by an integrity constraint
-- (SQLSTATE class 23: check, not null, unique, foreign key).
CREATE FUNCTION pg_temp.debe_fallar(descripcion text, sentencia text) RETURNS void AS $$
BEGIN
    BEGIN
        EXECUTE sentencia;
    EXCEPTION WHEN integrity_constraint_violation THEN
        RAISE NOTICE 'OK  rejected: %', descripcion;
        RETURN;
    END;
    RAISE EXCEPTION 'FAIL: expected a rejection: %', descripcion;
END $$ LANGUAGE plpgsql;

-- comprobar: the condition must be true.
CREATE FUNCTION pg_temp.comprobar(descripcion text, condicion boolean) RETURNS void AS $$
BEGIN
    IF condicion IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'FAIL: %', descripcion;
    END IF;
    RAISE NOTICE 'OK  %', descripcion;
END $$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- Seed data
-- ---------------------------------------------------------------------------
SELECT pg_temp.comprobar('vehicle types are AUTOMOVIL, CAMIONETA, MOTOCICLETA with ids 1, 2, 3',
    (SELECT string_agg(id || '=' || nombre, ',' ORDER BY id) FROM tipo_vehiculo)
        = '1=AUTOMOVIL,2=CAMIONETA,3=MOTOCICLETA');

-- ---------------------------------------------------------------------------
-- Catalog: brands, brand types and models (RN03, RN23, RN29)
-- ---------------------------------------------------------------------------
INSERT INTO marca (make_id_vpic, nombre) VALUES (448, 'TOYOTA'), (474, 'HONDA');
INSERT INTO marca_tipo_vehiculo (marca_id, tipo_vehiculo_id) VALUES
    ((SELECT id FROM marca WHERE nombre = 'TOYOTA'), 1),
    ((SELECT id FROM marca WHERE nombre = 'TOYOTA'), 2),
    ((SELECT id FROM marca WHERE nombre = 'HONDA'), 3);
INSERT INTO modelo (model_id_vpic, nombre, marca_id, tipo_vehiculo_id) VALUES
    (2469, 'Corolla', (SELECT id FROM marca WHERE nombre = 'TOYOTA'), 1),
    (2471, 'Hilux',   (SELECT id FROM marca WHERE nombre = 'TOYOTA'), 2),
    (5000, 'CB190',   (SELECT id FROM marca WHERE nombre = 'HONDA'), 3);

SELECT pg_temp.comprobar('an imported brand starts not enabled and not deleted (RN23, RN24)',
    (SELECT NOT habilitada AND NOT baja FROM marca WHERE nombre = 'TOYOTA'));
SELECT pg_temp.debe_fallar('duplicate vPIC MakeId',
    $q$INSERT INTO marca (make_id_vpic, nombre) VALUES (448, 'TOYOTA BIS')$q$);
SELECT pg_temp.debe_fallar('enabled brand without logo (RN29)',
    $q$UPDATE marca SET habilitada = true WHERE nombre = 'TOYOTA'$q$);
UPDATE marca SET habilitada = true, logo_ruta = 'logos/toyota.png' WHERE nombre = 'TOYOTA';
SELECT pg_temp.comprobar('a brand with a logo can be enabled (RN23, RN29)',
    (SELECT habilitada FROM marca WHERE nombre = 'TOYOTA'));
SELECT pg_temp.debe_fallar('model of a brand not offered in that vehicle type (RN03 c)',
    $q$INSERT INTO modelo (model_id_vpic, nombre, marca_id, tipo_vehiculo_id)
       VALUES (9999, 'Moto Toyota', (SELECT id FROM marca WHERE nombre = 'TOYOTA'), 3)$q$);
SELECT pg_temp.debe_fallar('same vPIC model imported twice for the same brand and type',
    $q$INSERT INTO modelo (model_id_vpic, nombre, marca_id, tipo_vehiculo_id)
       VALUES (2469, 'Corolla', (SELECT id FROM marca WHERE nombre = 'TOYOTA'), 1)$q$);

-- ---------------------------------------------------------------------------
-- Units and subtypes (RN01, RN02, RN03, RN15, RN16, RN17, RN22)
-- ---------------------------------------------------------------------------
INSERT INTO unidad (numero_serie, numero_motor, modelo_id, tipo_vehiculo_id, anio, color,
                    kilometraje, precio, condicion, estado) VALUES
    ('AUTO-0KM-1', 'MOT-1', (SELECT id FROM modelo WHERE nombre = 'Corolla'), 1, 2026, 'Blanco',
     0, 30000000.00, 'NUEVA', 'DISPONIBLE'),
    ('AUTO-USADO-1', 'MOT-2', (SELECT id FROM modelo WHERE nombre = 'Corolla'), 1, 2018, 'Gris',
     85000, 12000000.00, 'USADA', 'DISPONIBLE'),
    ('CAMI-1', 'MOT-3', (SELECT id FROM modelo WHERE nombre = 'Hilux'), 2, 2025, 'Negro',
     0, 45000000.00, 'NUEVA', 'DISPONIBLE'),
    ('CUADRO-1', 'MOT-4', (SELECT id FROM modelo WHERE nombre = 'CB190'), 3, 2024, 'Rojo',
     1200, 4500000.50, 'USADA', 'RESERVADA');
INSERT INTO automovil (unidad_id, transmision, carroceria) VALUES
    ((SELECT id FROM unidad WHERE numero_serie = 'AUTO-0KM-1'), 'AUTOMATICA', 'SEDAN'),
    ((SELECT id FROM unidad WHERE numero_serie = 'AUTO-USADO-1'), 'MECANICA', 'SEDAN');
INSERT INTO camioneta (unidad_id, transmision, cabina, traccion) VALUES
    ((SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), 'MECANICA', 'DOBLE', '4x4');
INSERT INTO motocicleta (unidad_id, cilindrada, categoria_uso, transmision) VALUES
    ((SELECT id FROM unidad WHERE numero_serie = 'CUADRO-1'), 190, 'CALLE', 'MECANICA');
SELECT pg_temp.comprobar('valid units of the three types are stored with two-decimal prices (RN16, RN31)',
    (SELECT count(*) = 4 AND sum(precio) = 91500000.50 FROM unidad));

SELECT pg_temp.debe_fallar('duplicate serial number (RN01)',
    $q$INSERT INTO unidad (numero_serie, numero_motor, modelo_id, tipo_vehiculo_id, anio, color, kilometraje, precio, condicion, estado)
       VALUES ('AUTO-0KM-1', 'MOT-99', (SELECT id FROM modelo WHERE nombre = 'Corolla'), 1, 2026, 'Azul', 0, 1, 'NUEVA', 'DISPONIBLE')$q$);
SELECT pg_temp.debe_fallar('duplicate engine number (RN01)',
    $q$INSERT INTO unidad (numero_serie, numero_motor, modelo_id, tipo_vehiculo_id, anio, color, kilometraje, precio, condicion, estado)
       VALUES ('AUTO-99', 'MOT-1', (SELECT id FROM modelo WHERE nombre = 'Corolla'), 1, 2026, 'Azul', 0, 1, 'NUEVA', 'DISPONIBLE')$q$);
SELECT pg_temp.debe_fallar('unknown condition (RN02)',
    $q$UPDATE unidad SET condicion = 'SEMINUEVA' WHERE numero_serie = 'AUTO-USADO-1'$q$);
SELECT pg_temp.debe_fallar('unknown status (RN02)',
    $q$UPDATE unidad SET estado = 'PERDIDA' WHERE numero_serie = 'AUTO-USADO-1'$q$);
SELECT pg_temp.debe_fallar('new unit with mileage above 0 (RN17)',
    $q$UPDATE unidad SET kilometraje = 10 WHERE numero_serie = 'AUTO-0KM-1'$q$);
SELECT pg_temp.debe_fallar('used unit with mileage 0 (RN17)',
    $q$UPDATE unidad SET kilometraje = 0 WHERE numero_serie = 'AUTO-USADO-1'$q$);
SELECT pg_temp.debe_fallar('price equal to 0 (RN15)',
    $q$UPDATE unidad SET precio = 0 WHERE numero_serie = 'AUTO-USADO-1'$q$);
SELECT pg_temp.debe_fallar('negative price (RN15)',
    $q$UPDATE unidad SET precio = -1 WHERE numero_serie = 'AUTO-USADO-1'$q$);
SELECT pg_temp.debe_fallar('unit whose vehicle type differs from its model type (RN03 f)',
    $q$INSERT INTO unidad (numero_serie, numero_motor, modelo_id, tipo_vehiculo_id, anio, color, kilometraje, precio, condicion, estado)
       VALUES ('AUTO-98', 'MOT-98', (SELECT id FROM modelo WHERE nombre = 'Corolla'), 3, 2026, 'Azul', 0, 1, 'NUEVA', 'DISPONIBLE')$q$);
SELECT pg_temp.debe_fallar('changing a unit to a model of another vehicle type (RN03 f)',
    $q$UPDATE unidad SET modelo_id = (SELECT id FROM modelo WHERE nombre = 'Hilux') WHERE numero_serie = 'AUTO-0KM-1'$q$);
SELECT pg_temp.debe_fallar('car subtype row for a pickup unit (RN03 b)',
    $q$INSERT INTO automovil (unidad_id, transmision, carroceria)
       VALUES ((SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), 'MECANICA', 'SEDAN')$q$);
SELECT pg_temp.debe_fallar('unknown transmission (RN10)',
    $q$UPDATE automovil SET transmision = 'CVT'$q$);
SELECT pg_temp.debe_fallar('unknown traction (RN16)',
    $q$UPDATE camioneta SET traccion = '6x6'$q$);
SELECT pg_temp.debe_fallar('motorcycle displacement 0 (RN22)',
    $q$UPDATE motocicleta SET cilindrada = 0$q$);

-- ---------------------------------------------------------------------------
-- Employees and clients (RN04, RN14, RN19, RN21)
-- ---------------------------------------------------------------------------
INSERT INTO empleado (legajo, nombre, apellido, funcion, nombre_usuario, hash_contrasena) VALUES
    ('L-001', 'Ana', 'Admin', 'ADMINISTRATIVA', 'admin', 'hash-de-prueba'),
    ('L-002', 'Victor', 'Vendedor', 'VENTA', NULL, NULL);
INSERT INTO cliente (cuil, nombre, apellido) VALUES
    ('20-12345678-9', 'Juan', 'Perez'),
    ('27-87654321-3', 'Pedro', 'Gomez');

SELECT pg_temp.debe_fallar('duplicate employee file number (RN21)',
    $q$INSERT INTO empleado (legajo, nombre, apellido, funcion) VALUES ('L-001', 'Otro', 'Empleado', 'VENTA')$q$);
SELECT pg_temp.debe_fallar('unknown employee role (RN04)',
    $q$INSERT INTO empleado (legajo, nombre, apellido, funcion) VALUES ('L-003', 'Otro', 'Empleado', 'GERENCIA')$q$);
SELECT pg_temp.debe_fallar('administrative employee without login credentials (RN19)',
    $q$INSERT INTO empleado (legajo, nombre, apellido, funcion) VALUES ('L-004', 'Otra', 'Admin', 'ADMINISTRATIVA')$q$);
SELECT pg_temp.debe_fallar('user name without password',
    $q$INSERT INTO empleado (legajo, nombre, apellido, funcion, nombre_usuario) VALUES ('L-005', 'Otro', 'Vendedor', 'VENTA', 'vendedor2')$q$);
SELECT pg_temp.debe_fallar('duplicate user name',
    $q$INSERT INTO empleado (legajo, nombre, apellido, funcion, nombre_usuario, hash_contrasena) VALUES ('L-006', 'Otra', 'Admin', 'ADMINISTRATIVA', 'admin', 'x')$q$);
SELECT pg_temp.debe_fallar('duplicate CUIL (RN14)',
    $q$INSERT INTO cliente (cuil, nombre, apellido) VALUES ('20-12345678-9', 'Otro', 'Cliente')$q$);

-- ---------------------------------------------------------------------------
-- Sales, trade-in and certificate (RN06, RN07, RN09, RN15, RN25)
-- ---------------------------------------------------------------------------
-- Juan buys the new Corolla for 30,000,000 and trades in his used car for 8,000,000.
INSERT INTO unidad (numero_serie, numero_motor, modelo_id, tipo_vehiculo_id, anio, color,
                    kilometraje, precio, condicion, estado) VALUES
    ('USADO-DE-JUAN', 'MOT-5', (SELECT id FROM modelo WHERE nombre = 'Corolla'), 1, 2015, 'Verde',
     120000, 9000000.00, 'USADA', 'DISPONIBLE');
INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, unidad_recibida_id,
                   importe_operacion, valor_reconocido, estado) VALUES
    ((SELECT id FROM empleado WHERE legajo = 'L-002'),
     (SELECT id FROM cliente WHERE cuil = '20-12345678-9'),
     (SELECT id FROM unidad WHERE numero_serie = 'AUTO-0KM-1'),
     (SELECT id FROM unidad WHERE numero_serie = 'USADO-DE-JUAN'),
     30000000.00, 8000000.00, 'CONFIRMADA');
SELECT pg_temp.comprobar('final price = operation amount - trade-in value: 30,000,000 - 8,000,000 = 22,000,000 (RN15)',
    (SELECT precio_final = 22000000.00 FROM venta WHERE valor_reconocido = 8000000.00));
SELECT pg_temp.comprobar('a new sale starts with a timestamp',
    (SELECT fecha_hora IS NOT NULL FROM venta WHERE valor_reconocido = 8000000.00));

-- Pedro later buys Juan's traded-in car: it was received in one confirmed sale and sold in another.
INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, importe_operacion, estado) VALUES
    ((SELECT id FROM empleado WHERE legajo = 'L-002'),
     (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
     (SELECT id FROM unidad WHERE numero_serie = 'USADO-DE-JUAN'),
     9000000.00, 'CONFIRMADA');
SELECT pg_temp.comprobar('without a trade-in, the final price equals the operation amount (RN15)',
    (SELECT precio_final = 9000000.00 FROM venta WHERE importe_operacion = 9000000.00));

SELECT pg_temp.debe_fallar('trade-in unit without a recognized value (RN07)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, unidad_recibida_id, importe_operacion, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), (SELECT id FROM unidad WHERE numero_serie = 'CUADRO-1'),
               45000000, 'EN_NEGOCIACION')$q$);
SELECT pg_temp.debe_fallar('recognized value without a trade-in unit (RN07)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, importe_operacion, valor_reconocido, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), 45000000, 1000, 'EN_NEGOCIACION')$q$);
SELECT pg_temp.debe_fallar('trade-in unit equal to the sold unit (RN07)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, unidad_recibida_id, importe_operacion, valor_reconocido, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'),
               45000000, 1000, 'EN_NEGOCIACION')$q$);
SELECT pg_temp.debe_fallar('trade-in value that leaves a final price of 0 or less (RN15)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, unidad_recibida_id, importe_operacion, valor_reconocido, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), (SELECT id FROM unidad WHERE numero_serie = 'CUADRO-1'),
               45000000, 45000000, 'EN_NEGOCIACION')$q$);
SELECT pg_temp.debe_fallar('operation amount equal to 0 (RN15)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, importe_operacion, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), 0, 'EN_NEGOCIACION')$q$);
SELECT pg_temp.debe_fallar('unknown sale status (RN06)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, importe_operacion, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), 1, 'PAGADA')$q$);
SELECT pg_temp.debe_fallar('unit sold in two confirmed sales (RN09)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, importe_operacion, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'AUTO-0KM-1'), 30000000, 'CONFIRMADA')$q$);
SELECT pg_temp.debe_fallar('unit received as trade-in in two confirmed sales (RN09)',
    $q$INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, unidad_recibida_id, importe_operacion, valor_reconocido, estado)
       VALUES ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
               (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), (SELECT id FROM unidad WHERE numero_serie = 'USADO-DE-JUAN'),
               45000000, 1000, 'CONFIRMADA')$q$);

-- The same unit may appear in several sales that are not confirmed.
INSERT INTO venta (empleado_id, cliente_id, unidad_vendida_id, importe_operacion, estado) VALUES
    ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
     (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), 45000000, 'EN_NEGOCIACION'),
    ((SELECT id FROM empleado WHERE legajo = 'L-002'), (SELECT id FROM cliente WHERE cuil = '27-87654321-3'),
     (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1'), 45000000, 'CANCELADA');
SELECT pg_temp.comprobar('a unit can be in sales under negotiation or cancelled more than once (RN09)',
    (SELECT count(*) = 2 FROM venta WHERE unidad_vendida_id = (SELECT id FROM unidad WHERE numero_serie = 'CAMI-1')));

INSERT INTO certificado_venta (venta_id, numero) VALUES
    ((SELECT id FROM venta WHERE valor_reconocido = 8000000.00), 'CV-0001');
SELECT pg_temp.debe_fallar('second certificate for the same sale (RN25)',
    $q$INSERT INTO certificado_venta (venta_id, numero) VALUES ((SELECT id FROM venta WHERE valor_reconocido = 8000000.00), 'CV-0002')$q$);

-- ---------------------------------------------------------------------------
-- Customer service, images and audit (RN20, RN21, RN30)
-- ---------------------------------------------------------------------------
INSERT INTO atencion (cliente_id, empleado_id, detalle) VALUES
    ((SELECT id FROM cliente WHERE cuil = '20-12345678-9'), (SELECT id FROM empleado WHERE legajo = 'L-002'), 'Consulta por Corolla');
INSERT INTO imagen_unidad (unidad_id, ruta, orden) VALUES
    ((SELECT id FROM unidad WHERE numero_serie = 'AUTO-0KM-1'), 'fotos/auto-0km-1-1.jpg', 1);
INSERT INTO auditoria (empleado_id, entidad, entidad_id, accion, valor_anterior, valor_nuevo) VALUES
    ((SELECT id FROM empleado WHERE legajo = 'L-001'), 'unidad',
     (SELECT id FROM unidad WHERE numero_serie = 'AUTO-USADO-1'), 'CAMBIO_PRECIO', '12000000.00', '11500000.00');
SELECT pg_temp.comprobar('customer service, image and audit rows are stored with their timestamps',
    (SELECT count(*) = 1 FROM atencion WHERE fecha_hora IS NOT NULL)
    AND (SELECT count(*) = 1 FROM imagen_unidad)
    AND (SELECT count(*) = 1 FROM auditoria WHERE fecha_hora IS NOT NULL));
SELECT pg_temp.debe_fallar('unknown audit action (RN20)',
    $q$INSERT INTO auditoria (empleado_id, entidad, entidad_id, accion) VALUES ((SELECT id FROM empleado WHERE legajo = 'L-001'), 'unidad', 1, 'BORRADO')$q$);
SELECT pg_temp.debe_fallar('audit row without an employee (RN20)',
    $q$INSERT INTO auditoria (entidad, entidad_id, accion) VALUES ('unidad', 1, 'CAMBIO_PRECIO')$q$);

\echo 'ALL SCHEMA CHECKS PASSED'
ROLLBACK;
