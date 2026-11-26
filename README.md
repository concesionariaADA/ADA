# Concesionaria ADA - TPI Programacion II

## Reglas de negocio

La columna **Texto original** conserva literalmente el texto de cada regla tal como estaba escrito, incluidas las líneas cortadas, duplicadas o con restos de edición. La columna **Observaciones** señala con `[PENDIENTE DEFINICIÓN]` los puntos que el equipo todavía tiene que resolver; no se decidió ni se agregó ninguna regla nueva.

| Regla | Texto original | Observaciones |
| --- | --- | --- |
| RN01 | El número de serie (motor) identifica de forma única a cada unidad física; el número de motor tampoco puede repetirse. | `[PENDIENTE DEFINICIÓN]` Identificador de unidad: no queda claro si "número de serie" y "número de motor" son el mismo dato o dos distintos. RN22 usa el número de cuadro (VIN) para motocicletas; falta definir el identificador de automóviles y camionetas. |
| RN02 | La condición de una unidad es Nueva o Usada; su estado es Disponible, Reservada, Vendida o Fuera de exhibición. | `[PENDIENTE DEFINICIÓN]` Estados/enums: aquí se escribe "Nueva/Usada" y "Fuera de exhibición"; en RN17 y RN18 aparece `NUEVO`/`USADO`, `DISPONIBLE`, `RESERVADA`, `VENDIDA`, `FUERA_DE_EXHIBICION`. Falta acordar los nombres definitivos. |
| RN03 | Cada unidad corresponde a un único modelo. | |
| RN04 | Cada empleado trabaja en función de Venta o Administrativa. | `[PENDIENTE DEFINICIÓN]` Roles: RN19 habla de roles "Administrador o Empleado". Falta definir cómo se relacionan con las funciones Venta/Administrativa. |
| RN05 | Solo un empleado con función de Venta puede gestionar una venta. | |
| RN06 | la venta es gestionada por un único vendedor y registra una única unidad a vender; su estado es En negociación, Confirmado o Cancelado. | `[PENDIENTE DEFINICIÓN]` Relación venta/boleto: esta regla habla de "venta"; RN09, RN13, RN19 y RN20 hablan de "boleto". Falta definir si son la misma entidad o dos distintas. |
| RN07 | La unidad entregada como parte de pago es opcional; si existe, debe registrarse el valor reconocido (y viceversa), y no puede ser la misma unidad que se vende. | |
| RN08 | Una unidad recibida como parte de pago ingresa al stock como Usada y puede volver a venderse, | `[PENDIENTE DEFINICIÓN]` Regla cortada: el texto original termina en coma y la frase queda incompleta. |
| RN09 | Una misma unidad no puede figurar en dos boletos confirmados (ni como vendida ni como recibida en parte de pago).------------------------ | El original termina con una línea de guiones (`------------------------`); se conserva tal cual. Ver relación venta/boleto en RN06. |
| RN10 | (AUTOMATICA, MECANICA) para autos y camioneta. | `[PENDIENTE DEFINICIÓN]` Regla incompleta: no indica a qué atributo corresponden los valores (AUTOMATICA, MECANICA). |
| RN11 | *(no existe en el texto original)* | `[PENDIENTE DEFINICIÓN]` Regla faltante: la numeración salta de RN10 a RN13. |
| RN12 | *(no existe en el texto original)* | `[PENDIENTE DEFINICIÓN]` Regla faltante: la numeración salta de RN10 a RN13. |
| RN13 | Un accesorio puede estar en muchos boletos y un boleto puede incluir varios accesorios; se conserva la cantidad y el precio pactado.---basico | El original termina con `---basico`; se conserva tal cual. Ver relación venta/boleto en RN06. |
| RN14 | Un cliente se identifica por número de cuil . | |
| RN15 | Los importes y precios son valores positivos. | |
| RN16 | (Tipos de Vehículos y Jerarquía): La concesionaria comercializa Automóviles, Camionetas y Motocicletas. Todos comparten marca, modelo, año, color, kilometraje, precio base, condición y estado, pero se diferencian en atributos específicos: los automóviles registran tipo de carrocería (Sedán, Hatchback, SUV, etc.), las camionetas registran tipo de cabina y tracción (4x2 / 4x4), y las motocicletas registran cilindrada y tipo de moto (Naked, Enduro, Scooter, etc.). | `[PENDIENTE DEFINICIÓN]` Tipos de moto: aquí se listan "Naked, Enduro, Scooter, etc."; RN22 lista URBANA, ENDURO, TRAIL (y SCOOTER en la segunda copia). Falta definir la lista definitiva. |
| RN17 | (Kilometraje según Condición): Si la condición del vehículo es NUEVO (0 Km), su kilometraje debe ser exactamente 0. Si es USADO, el kilometraje debe ser mayor a 0. | Ver estados/enums en RN02. |
| RN18 | (Visibilidad en Frontoffice / Catálogo Público): La página pública solo exhibe unidades cuyo estado sea DISPONIBLE o RESERVADA (indicando esta última como "Reservado"). Las unidades VENDIDA o FUERA_DE_EXHIBICION quedan ocultas automáticamente del catálogo público y solo son visibles en /admin. | Ver estados/enums en RN02. |
| RN19 | (Control de Acceso a /admin): Toda operación de alta, baja, modificación (CRUD) de vehículos, gestión de adquisiciones, boletos y cambio de estados requiere autenticación previa mediante credenciales válidas de un usuario con rol habilitado (Administrador o Empleado según permisos). | Ver roles en RN04. |
| RN20 | (Trazabilidad / Auditoría en Admin): Toda modificación de precio, cambio de estado de una unidad o confirmación/cancelación de un boleto desde /admin registra la fecha/hora y el usuario autenticado que ejecutó la acción. | |
| RN21 | (Identificación por Número de Legajo): Cada empleado (incluyendo todo vendedor que asiste a un cliente o gestiona un boleto) es identificado de forma única mediante un número de legajo único y obligatorio (no puede repetirse entre empleados ni quedar vacío). | |
| *(sin número)* | Trazabilidad del Vendedor en el Frontoffice y /admin: Cuando un cliente consulta o inicia una gestión desde el frontoffice asistido por un vendedor —o cuando un vendedor opera en /admin—, su legajo queda asociado a la atención/boleto para identificar quién asesoró y cerró la operación. | Párrafo sin número en el original; aparece a continuación de RN21. |
| RN22 (copia 1) | (Identificación y Caracterización de Motocicletas): Toda motocicleta se identifica de forma única mediante su número de cuadro (equivalente al VIN como clave primaria / identificador único de unidad) y su número de motor (único). Además de los atributos generales de todo vehículo (marca, modelo, año, color, kilometraje, condición y estado), debe registrar obligatoriamente su cilindrada (en cc, valor entero positivo), su categoría de uso (URBANA, ENDURO, TRAIL) y su | `[PENDIENTE DEFINICIÓN]` RN22 duplicada: en el original la regla aparece dos veces seguidas en la misma línea. Esta primera copia está cortada y no incluye SCOOTER. Ver también tipos de moto en RN16 e identificador de unidad en RN01. |
| RN22 (copia 2) | (Identificación y Caracterización de Motocicletas): Toda motocicleta se identifica de forma única mediante su número de cuadro (equivalente al VIN como clave primaria / identificador único de unidad) y su número de motor (único). Además de los atributos generales de todo vehículo (marca, modelo, año, color, kilometraje, condición y estado), debe registrar obligatoriamente su cilindrada (en cc, valor entero positivo), su categoría de uso (URBANA, ENDURO, TRAIL, SCOOTER) y su tipo de transmisión (AUTOMATICA, MECANICA). | `[PENDIENTE DEFINICIÓN]` RN22 duplicada: segunda copia, completa. Falta confirmar cuál de las dos versiones es la vigente. |

## Puntos pendientes de definición

Resumen de los `[PENDIENTE DEFINICIÓN]` de la tabla. Son preguntas para el equipo; no hay una respuesta elegida.

1. **RN08 cortada:** ¿cómo termina la regla?
2. **RN10 incompleta:** ¿a qué atributo corresponden los valores (AUTOMATICA, MECANICA) para autos y camionetas?
3. **RN11 y RN12 faltantes:** ¿existen esas reglas o hay que renumerar?
4. **RN22 duplicada:** ¿cuál de las dos versiones es la vigente?
5. **Tipos de moto:** RN16 (Naked, Enduro, Scooter, etc.) y RN22 (URBANA, ENDURO, TRAIL / URBANA, ENDURO, TRAIL, SCOOTER) no coinciden.
6. **Identificador de unidad:** RN01 (número de serie / número de motor) y RN22 (número de cuadro / VIN para motocicletas). Falta definir el identificador de cada tipo de vehículo.
7. **Estados/enums:** RN02 (Nueva/Usada, Fuera de exhibición), RN06 (En negociación, Confirmado, Cancelado), RN17 (NUEVO/USADO) y RN18 (DISPONIBLE, RESERVADA, VENDIDA, FUERA_DE_EXHIBICION) usan nombres distintos.
8. **Relación venta/boleto:** RN05 y RN06 hablan de "venta"; RN09, RN13, RN19, RN20 y RN21 hablan de "boleto".
9. **Roles:** RN04 (funciones Venta/Administrativa) y RN19 (roles Administrador/Empleado).

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
