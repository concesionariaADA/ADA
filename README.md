# Concesionaria ADA - TPI Programacion II 
RN01	El número de serie (motor) identifica de forma única a cada unidad física; el número de motor tampoco puede repetirse.
RN02	La condición de una unidad es Nueva o Usada; su estado es Disponible, Reservada, Vendida o Fuera de exhibición.
RN03	Cada unidad corresponde a un único modelo.
RN04	Cada empleado trabaja en función de Venta o Administrativa.
RN05	Solo un empleado con función de Venta puede gestionar una venta.
RN06	la venta es gestionada por un único vendedor y registra una única unidad a vender; su estado es En negociación, Confirmado o Cancelado.
RN07	La unidad entregada como parte de pago es opcional; si existe, debe registrarse el valor reconocido (y viceversa), y no puede ser la misma unidad que se vende.
RN08	Una unidad recibida como parte de pago ingresa al stock como Usada y puede volver a venderse, 
RN09	Una misma unidad no puede figurar en dos boletos confirmados (ni como vendida ni como recibida en parte de pago).------------------------
RN10    (AUTOMATICA, MECANICA) para autos y camioneta.
RN13	Un accesorio puede estar en muchos boletos y un boleto puede incluir varios accesorios; se conserva la cantidad y el precio pactado.---basico
RN14	Un cliente se identifica por número de cuil .
RN15	Los importes y precios son valores positivos.
RN16 (Tipos de Vehículos y Jerarquía): La concesionaria comercializa Automóviles, Camionetas y Motocicletas. Todos comparten marca, modelo, año, color, kilometraje, precio base, condición y estado, pero se diferencian en atributos específicos: los automóviles registran tipo de carrocería (Sedán, Hatchback, SUV, etc.), las camionetas registran tipo de cabina y tracción (4x2 / 4x4), y las motocicletas registran cilindrada y tipo de moto (Naked, Enduro, Scooter, etc.).   
RN17 (Kilometraje según Condición): Si la condición del vehículo es NUEVO (0 Km), su kilometraje debe ser exactamente 0. Si es USADO, el kilometraje debe ser mayor a 0.   
RN18 (Visibilidad en Frontoffice / Catálogo Público): La página pública solo exhibe unidades cuyo estado sea DISPONIBLE o RESERVADA (indicando esta última como "Reservado"). Las unidades VENDIDA o FUERA_DE_EXHIBICION quedan ocultas automáticamente del catálogo público y solo son visibles en /admin.
RN19 (Control de Acceso a /admin): Toda operación de alta, baja, modificación (CRUD) de vehículos, gestión de adquisiciones, boletos y cambio de estados requiere autenticación previa mediante credenciales válidas de un usuario con rol habilitado (Administrador o Empleado según permisos).
RN20 (Trazabilidad / Auditoría en Admin): Toda modificación de precio, cambio de estado de una unidad o confirmación/cancelación de un boleto desde /admin registra la fecha/hora y el usuario autenticado que ejecutó la acción.
RN21 (Identificación por Número de Legajo): Cada empleado (incluyendo todo vendedor que asiste a un cliente o gestiona un boleto) es identificado de forma única mediante un número de legajo único y obligatorio (no puede repetirse entre empleados ni quedar vacío).
Trazabilidad del Vendedor en el Frontoffice y /admin: Cuando un cliente consulta o inicia una gestión desde el frontoffice asistido por un vendedor —o cuando un vendedor opera en /admin—, su legajo queda asociado a la atención/boleto para identificar quién asesoró y cerró la operación.
RN22 (Identificación y Caracterización de Motocicletas): Toda motocicleta se identifica de forma única mediante su número de cuadro (equivalente al VIN como clave primaria / identificador único de unidad) y su número de motor (único). Además de los atributos generales de todo vehículo (marca, modelo, año, color, kilometraje, condición y estado), debe registrar obligatoriamente su cilindrada (en cc, valor entero positivo), su categoría de uso (URBANA, ENDURO, TRAIL) y su RN22 (Identificación y Caracterización de Motocicletas): Toda motocicleta se identifica de forma única mediante su número de cuadro (equivalente al VIN como clave primaria / identificador único de unidad) y su número de motor (único). Además de los atributos generales de todo vehículo (marca, modelo, año, color, kilometraje, condición y estado), debe registrar obligatoriamente su cilindrada (en cc, valor entero positivo), su categoría de uso (URBANA, ENDURO, TRAIL, SCOOTER) y su tipo de transmisión (AUTOMATICA, MECANICA).

## Entorno de desarrollo

Requisitos: JDK 17 o superior, Maven y Docker Desktop.

La base de datos es PostgreSQL 17 y corre en Docker; no hace falta tener PostgreSQL instalado.

1. Copiar la configuración local: `cp .env.example .env`. El `.env` no se commitea; si el puerto `5433` está ocupado, cambiar `POSTGRES_PORT` solo en tu `.env`.
2. Levantar la base y pgAdmin: `docker compose up -d`.
3. Compilar: `mvn compile`. La aplicación se conecta con `com.ada.persistencia.ConexionBaseDatos`, que lee los datos de conexión del `.env`.

Acceso a la base:

- Consola: `docker exec -it ada-postgres psql -U ada -d ada` (si cambiaste `POSTGRES_USER` o `POSTGRES_DB` en tu `.env`, usá esos valores).
- pgAdmin: http://localhost:5050, con el usuario y la contraseña `PGADMIN_*` del `.env`. Para registrar el servidor usar host `postgres`, puerto `5432` y los datos `POSTGRES_*` del `.env`.
- Otro cliente (DBeaver, IntelliJ): host `localhost`, puerto `POSTGRES_PORT` del `.env`.

Comandos útiles: `docker compose stop` (detiene sin borrar datos) y `docker compose down -v` (borra los contenedores **y los datos**).
