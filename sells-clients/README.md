# sells-clients

Microservicio de **clientes** del sistema de ventas. Permite que un usuario no autenticado se registre como cliente, que el cliente consulte y actualice sus datos, y que el administrador gestione a todos los clientes.

Forma parte de un sistema dividido en tres servicios por dominio:

| Servicio | Responsabilidad | Estado |
|---|---|---|
| **sells-products** | Catálogo de productos | En desarrollo |
| **sells-clients** | Datos de los clientes | En desarrollo |
| **sells-orders** | Órdenes de los clientes | Pendiente |

> La división es por **dominio** (qué se maneja), lo que puede hacer cada usuario (visitante, cliente o admin) se controla con roles dentro de cada servicio.

> El **registro público** de clientes está en este servicio (`POST` sin autenticación). Las credenciales y la emisión de tokens quedan para el servicio de autenticación (pendiente).

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

- **Roles:** cada endpoint declara quién puede usarlo:
  - **Público** (`@PermitAll`): registrarse (`POST`).
  - **`admin` y `client`**: consultar un cliente por id y actualizarlo.
  - **Solo `admin`**: listar todos los clientes y desactivar uno.


- **Auditoría:** el usuario que realiza cada cambio se toma del claim `sub` del token (`jwt.getSubject()`) y se guarda en `createdBy` / `updatedBy`. En un registro público no hay token, así que `createdBy` se guarda como `"self-registration"`.


- **Claims del token:** `sub` contiene el `username` y el claim `userId` contiene el id del usuario (`jwt.getClaim("userId")`).


- **Autenticación no proactiva** (`quarkus.http.auth.proactive=false`): el token solo se valida cuando un endpoint necesita saber quién es el usuario (los que tienen `@RolesAllowed`).
  - El registro (`@PermitAll`) sigue accesible sin autenticación, aunque la petición traiga un token vencido o inválido.
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
| `GET` | `/api/v1/client` | `admin` | Lista los clientes activos, paginados y ordenados por nombre y apellido | `200 OK` |
| `GET` | `/api/v1/client/{id}` | `admin`, `client` | Obtiene un cliente por id | `200 OK` |
| `POST` | `/api/v1/client` | Público | Registra un cliente nuevo | `201 Created` + header `Location` |
| `PUT` | `/api/v1/client/{id}` | `admin`, `client` | Reemplaza los datos editables de un cliente (no `username` ni `email`) | `200 OK` |
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

### `POST /api/v1/client`

Body (`ClientRequestDTO`):

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
| `phone` | Opcional, máximo 20 caracteres |
| `address` | Opcional, máximo 200 caracteres |
| `balance` | Opcional, mayor o igual a 0, por defecto `0` |

### `PUT /api/v1/client/{id}`

Body (`ClientUpdateRequestDTO`). Igual que el del `POST`, pero **sin `username` ni `email`**:

```json
{
  "name": "Juan",
  "lastName": "Pérez",
  "phone": "809-555-1234",
  "address": "Calle Principal #10, Santo Domingo",
  "balance": 1500.00
}
```

| Campo | Regla |
|---|---|
| `name` | Obligatorio, máximo 50 caracteres |
| `lastName` | Obligatorio, máximo 50 caracteres |
| `phone` | Opcional, máximo 20 caracteres |
| `address` | Opcional, máximo 200 caracteres |
| `balance` | Opcional, mayor o igual a 0, por defecto `0.00` |

Si el JSON incluye `username` o `email`, se ignoran: Quarkus descarta los campos que no existen en el DTO.

### Respuesta del `POST` y del `PUT`

`ClientResponseDTO`:

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
| Sin token en un endpoint protegido | `UnauthorizedException` (Quarkus) | `401 Unauthorized` |
| Token inválido, vencido o de otro emisor | `AuthenticationFailedException` (Quarkus) | `401 Unauthorized` |
| Token válido pero sin el rol que pide el endpoint | `ForbiddenException` (Quarkus) | `403 Forbidden` |
| El cliente no existe o está desactivado | `ClientNotFoundException` | `404 Not Found` |
| Error inesperado | Cualquier otra | `500 Internal Server Error` |

En los errores `500` el cliente recibe un mensaje genérico; el detalle solo se registra en el log del servidor para no exponer información interna.

Las excepciones de seguridad las lanza Quarkus, pero cada una tiene su propio mapper (`UnauthorizedMapper`, `AuthenticationFailedMapper`, `ForbiddenMapper`) para que también respondan con el formato `ErrorDTO`.

---

## Decisiones de diseño

- **Registro público en este servicio.** Un usuario no autenticado puede crear su ficha de cliente con el `POST`. Si quien lo llama es un admin con token, `createdBy` guarda su usuario; si no hay token, se guarda `"self-registration"`.


- **`username` y `email` no se pueden modificar.** Son la identidad del cliente y pertenecerán al servicio de autenticación (el `username` es el que viaja en el claim `sub` del token). Si este servicio los cambiara, quedarían desincronizados con auth. Por eso el `PUT` usa un DTO propio, `ClientUpdateRequestDTO`, que no los incluye.


- **Un DTO por operación.** Crear y actualizar tienen reglas distintas, así que cada una tiene su DTO (`ClientRequestDTO` y `ClientUpdateRequestDTO`). La copia de los datos al entity está centralizada en `mapRequestToClient` y `mapRequestUpdateToClient` dentro del service.


- **Borrado lógico.** `DELETE` no elimina el documento: pone `status = false`. Así se conserva el historial (útil, por ejemplo, para órdenes antiguas del cliente). Para la API, un cliente desactivado se comporta como inexistente (`404`).


- **Campos de auditoría.** Cada cliente guarda `createdBy`, `createdAt`, `updatedBy` y `updatedAt`. No se exponen en la respuesta.


- **Convención de respuestas:**
  - Si el endpoint siempre responde `200` con datos, devuelve el DTO directamente.
  - Si necesita otro status o headers (`201` + `Location`, `204`), usa `RestResponse<T>`.
  - Los errores se lanzan como excepciones y se traducen en `mapper/`.
  - `Response` crudo solo se usa dentro de los ExceptionMappers, porque la interfaz `ExceptionMapper` obliga a devolverlo. Se arma en un único lugar: `ErrorResponseBuilder`.


- **Mismos patrones que `sells-products`.** Estructura de carpetas, nombres, manejo de errores y configuración de seguridad son iguales en ambos servicios.


- **DTOs separados de la entidad.** La API no expone el documento de Mongo directamente; usa `ClientRequestDTO` / `ClientUpdateRequestDTO` para la entrada y `ClientResponseDTO` para la salida.

---

## Pendientes

- [ ] El cliente solo puede consultar y actualizar **su propia** información (comparar el id de la ruta con el claim `userId`).
- [ ] Evitar `username` y `email` duplicados al registrarse (`409 Conflict`).
- [ ] Rate limiting en el registro público (`POST`), para evitar abuso.
- [ ] Configuración por variables de entorno.
- [ ] Dockerfile / `docker-compose` con MongoDB.
- [ ] Pruebas unitarias de los endpoints.
