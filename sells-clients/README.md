# sells-clients

Microservicio de **clientes** del sistema de ventas. Permite que el administrador gestione los datos de los clientes.

Forma parte de un sistema dividido en tres servicios por dominio:

| Servicio | Responsabilidad | Estado |
|---|---|---|
| **sells-products** | Catálogo de productos | En desarrollo |
| **sells-clients** | Datos de los clientes | En desarrollo |
| **sells-orders** | Órdenes de los clientes | Pendiente |

> La división es por **dominio** (qué se maneja), lo que puede hacer cada usuario (visitante, cliente o admin) se controla con roles dentro de cada servicio.

> El **registro público** de clientes no está en este servicio: irá en un servicio de autenticación aparte. Este servicio solo expone el CRUD para el administrador.

---

## Stack

- **Java 21**
- **Quarkus 3.40** (`quarkus-rest`, `quarkus-rest-jackson`)
- **MongoDB** con **Panache** (patrón repository)
- **Hibernate Validator** para validar los datos de entrada
- **SmallRye JWT** para autenticación y autorización por roles

---

## Arquitectura interna

```
resource/    → Endpoints HTTP (entrada y salida de la API)
service/     → Lógica de negocio
repository/  → Acceso a MongoDB (Panache)
entity/      → Documento de Mongo (Client)
dto/         → Objetos de entrada y salida de la API
exception/   → Excepciones de negocio
mapper/
  exceptionMapper/ → ExceptionMappers: convierten excepciones en respuestas HTTP
helper/      → Utilidades compartidas (ErrorResponseBuilder)
```

Flujo de una petición:

```
Usuario → ClientResource → ClientService → ClientRepository → MongoDB
```

- El **resource** solo recibe la petición, valida el formato del id o dto de entrada y arma la respuesta.
- El **service** contiene las reglas de negocio (por ejemplo, que un cliente desactivado no se puede consultar).
- Los errores se lanzan como **excepciones** y se convierten en respuestas HTTP en un solo lugar.

---

## Requisitos previos

- JDK 21
- Maven 3.9+ (o el wrapper `./mvnw` incluido)
- Una instancia de MongoDB
- La **clave pública** con la que se verifican los JWT (`publicKey.pem`)

---

## Configuración

Archivo: `src/main/resources/application.properties`

| Propiedad | Descripción |
|---|---|
| `quarkus.http.port` | Puerto del servicio (`8081`, para no chocar con `sells-products` en el `8080`) |
| `quarkus.http.auth.proactive` | `false`: el token solo se valida en los endpoints que lo necesitan (ver *Seguridad*) |
| `quarkus.mongodb.connection-string` | Cadena de conexión a MongoDB |
| `quarkus.mongodb.database` | Base de datos (`clientdb`) |
| `mp.jwt.verify.publickey.location` | Ubicación de la clave pública para verificar los JWT |
| `mp.jwt.verify.issuer` | Emisor esperado de los tokens (`https://benito-ventas.app`) |

> **Pendiente:** mover credenciales y rutas a variables de entorno para no dejarlas en el repositorio.

---

## Cómo ejecutarlo

Modo desarrollo (recarga en caliente):

```shell
./mvnw quarkus:dev
```

Empaquetar y ejecutar:

```shell
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

> **Pendiente:** instrucciones con Docker / `docker-compose`.

---

## Seguridad

El servicio **no emite tokens**: solo los verifica. Los JWT los genera el servicio de autenticación (pendiente) y este servicio comprueba su firma con la clave pública.

- **Roles:** todos los endpoints requieren el rol `admin`. No hay endpoints públicos.


- **Auditoría:** el usuario que realiza cada cambio se toma del claim `sub` del token (`jwt.getSubject()`) y se guarda en `createdBy` / `updatedBy`.


- **Autenticación no proactiva** (`quarkus.http.auth.proactive=false`): el token solo se valida cuando un endpoint necesita saber quién es el usuario (los que tienen `@RolesAllowed`).
  - Se mantiene igual que en `sells-products` para que todos los servicios se comporten igual.
  - Los errores de autenticación llegan a la capa de endpoints, así que también se responden con el formato `ErrorDTO`.

Los tokens se envían en el header:

```
Authorization: Bearer <token>
```

---

## Endpoints

Ruta base: `/api/v1/client`

| Método | Ruta | Acceso | Descripción | Respuesta exitosa |
|---|---|---|---|---|
| `GET` | `/api/v1/client` | `admin` | Lista los clientes activos, paginados | `200 OK` |
| `GET` | `/api/v1/client/{id}` | `admin` | Obtiene un cliente por id | `200 OK` |
| `POST` | `/api/v1/client` | `admin` | Crea un cliente | `201 Created` + header `Location` |
| `PUT` | `/api/v1/client/{id}` | `admin` | Reemplaza los datos de un cliente | `200 OK` |
| `DELETE` | `/api/v1/client/{id}` | `admin` | Desactiva un cliente (borrado lógico) | `204 No Content` |

### `GET /api/v1/client`

Parámetros de consulta:

| Parámetro | Tipo | Por defecto | Regla |
|---|---|---|---|
| `page` | int | `0` | Mayor o igual que 0 (la primera página es la 0) |
| `pageSize` | int | `10` | Mayor que 0, máximo 30 |

Respuesta:

```json
{
  "clients": [ "..." ],
  "page": 0,
  "pageSize": 10,
  "totalPages": 3,
  "totalClients": 25
}
```

### `POST /api/v1/client` y `PUT /api/v1/client/{id}`

Body:

```json
{
  "username": "jperez",
  "name": "Juan",
  "lastName": "Pérez",
  "email": "juan.perez@email.com",
  "phone": "809-555-1234",
  "address": "Calle Principal #10, Santo Domingo",
  "balance": 1500.00
}
```

| Campo | Regla |
|---|---|
| `username` | Obligatorio, máximo 30 caracteres |
| `name` | Obligatorio, máximo 50 caracteres |
| `lastName` | Obligatorio, máximo 50 caracteres |
| `email` | Obligatorio, formato de email válido, máximo 100 caracteres |
| `phone` | Obligatorio, máximo 20 caracteres |
| `address` | Obligatorio, máximo 200 caracteres |
| `balance` | Obligatorio, mayor o igual a 0 |

Respuesta (`ClientResponseDTO`):

```json
{
  "id": "6716f3a2c9e77a1b2c3d4e5f",
  "username": "jperez",
  "name": "Juan",
  "lastName": "Pérez",
  "email": "juan.perez@email.com",
  "phone": "809-555-1234",
  "address": "Calle Principal #10, Santo Domingo",
  "balance": 1500.00
}
```

En el `POST`, además, el header `Location` indica la URL del cliente creado:

```
Location: http://localhost:8081/api/v1/client/6716f3a2c9e77a1b2c3d4e5f
```

---

## Manejo de errores

Todos los errores responden con el mismo formato (`ErrorDTO`), construido por `ErrorResponseBuilder`:

```json
{
  "error_name": "NOT FOUND",
  "error_message": "Client with id: '6716f3a2c9e77a1b2c3d4e5f' was not found",
  "error_status_code": 404
}
```

| Situación | Excepción | Status |
|---|---|---|
| El id no tiene formato de ObjectId | `InvalidIdException` | `400 Bad Request` |
| Datos que no cumplen las validaciones | `ConstraintViolationException` | `400 Bad Request` |
| Sin token | `UnauthorizedException` (Quarkus) | `401 Unauthorized` |
| Token inválido, vencido o de otro emisor | `AuthenticationFailedException` (Quarkus) | `401 Unauthorized` |
| Token válido pero sin rol `admin` | `ForbiddenException` (Quarkus) | `403 Forbidden` |
| El cliente no existe o está desactivado | `ClientNotFoundException` | `404 Not Found` |
| Error inesperado | Cualquier otra | `500 Internal Server Error` |

En los errores `500` el cliente recibe un mensaje genérico; el detalle solo se registra en el log del servidor para no exponer información interna.

Las excepciones de seguridad las lanza Quarkus, pero cada una tiene su propio mapper (`UnauthorizedMapper`, `AuthenticationFailedMapper`, `ForbiddenMapper`) para que también respondan con el formato `ErrorDTO`.

---

## Decisiones de diseño

- **Solo CRUD de administrador.** El registro de clientes por parte de usuarios no autenticados irá en el servicio de autenticación, que manejará las credenciales. Este servicio guarda únicamente los datos de negocio del cliente.


- **Borrado lógico.** `DELETE` no elimina el documento: pone `status = false`. Así se conserva el historial (útil, por ejemplo, para órdenes antiguas del cliente). Para la API, un cliente desactivado se comporta como inexistente (`404`).


- **Campos de auditoría.** Cada cliente guarda `createdBy`, `createdAt`, `updatedBy` y `updatedAt`. No se exponen en la respuesta.


- **Convención de respuestas:**
  - Si el endpoint siempre responde `200` con datos, devuelve el DTO directamente.
  - Si necesita otro status o headers (`201` + `Location`, `204`), usa `RestResponse<T>`.
  - Los errores se lanzan como excepciones y se traducen en `mapper/`.
  - `Response` crudo solo se usa dentro de los ExceptionMappers, porque la interfaz `ExceptionMapper` obliga a devolverlo. Se arma en un único lugar: `ErrorResponseBuilder`.


- **Mismos patrones que `sells-products`.** Estructura de carpetas, nombres, manejo de errores y configuración de seguridad son iguales en ambos servicios.


- **DTOs separados de la entidad.** La API no expone el documento de Mongo directamente; usa `ClientRequestDTO` para la entrada y `ClientResponseDTO` para la salida.

---

## Pendientes

- [ ] Evitar `username` y `email` duplicados (`409 Conflict`).
- [ ] Configuración por variables de entorno.
- [ ] Dockerfile / `docker-compose` con MongoDB.
- [ ] Pruebas unitarias de los endpoints.
