# sells-products

Microservicio de **productos** del sistema de ventas. Expone el catálogo de productos y permite que el administrador lo gestione.

Forma parte de un sistema dividido en tres servicios por dominio:

| Servicio | Responsabilidad | Estado |
|---|---|---|
| **sells-products** | Catálogo de productos | En desarrollo |
| **sells-clients** | Datos de los clientes | Pendiente |
| **sells-orders** | Órdenes de los clientes | Pendiente |

> La división es por **dominio** (qué se maneja), lo que puede hacer cada usuario (visitante, cliente o admin) se controla con roles dentro de cada servicio.

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
entity/      → Documento de Mongo (Product)
dto/         → Objetos de entrada y salida de la API
exception/   → Excepciones de negocio
mapper/
  exceptionMapper/ → ExceptionMappers: convierten excepciones en respuestas HTTP
helper/      → Utilidades compartidas (ErrorResponseBuilder)
```

Flujo de una petición:

```
Usuario → ProductResource → ProductService → ProductRepository → MongoDB
```

- El **resource** solo recibe la petición, valida el formato del id o dto de entrada y arma la respuesta.
- El **service** contiene las reglas de negocio (por ejemplo, que un producto desactivado no se puede consultar).
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
| `quarkus.http.port` | Puerto del servicio (por defecto `8080`) |
| `quarkus.http.auth.proactive` | `false`: el token solo se valida en los endpoints que lo necesitan (ver *Seguridad*) |
| `quarkus.mongodb.connection-string` | Cadena de conexión a MongoDB |
| `quarkus.mongodb.database` | Base de datos (`productdb`) |
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

- **Roles:** `admin` para las operaciones de escritura. Las consultas son públicas.


- **Auditoría:** el usuario que realiza cada cambio se toma del claim `sub` del token (`jwt.getSubject()`) y se guarda en `createdBy` / `updatedBy`.



- **Autenticación no proactiva** (`quarkus.http.auth.proactive=false`): el token solo se valida cuando un endpoint necesita saber quién es el usuario (los que tienen `@RolesAllowed`).
  - Los endpoints públicos (`@PermitAll`) siguen accesibles sin autenticación.
  - Los errores de autenticación llegan a la capa de endpoints, así que también se responden con el formato `ErrorDTO`.

Los tokens se envían en el header:

```
Authorization: Bearer <token>
```

---

## Endpoints

Ruta base: `/api/v1/product`

| Método | Ruta | Acceso | Descripción | Respuesta exitosa |
|---|---|---|---|---|
| `GET` | `/api/v1/product` | Público | Lista los productos activos, paginados | `200 OK` |
| `GET` | `/api/v1/product/{id}` | Público | Obtiene un producto por id | `200 OK` |
| `POST` | `/api/v1/product` | `admin` | Crea un producto | `201 Created` + header `Location` |
| `PUT` | `/api/v1/product/{id}` | `admin` | Reemplaza los datos de un producto | `200 OK` |
| `DELETE` | `/api/v1/product/{id}` | `admin` | Desactiva un producto (borrado lógico) | `204 No Content` |

### `GET /api/v1/product`

Parámetros de consulta:

| Parámetro | Tipo | Por defecto | Regla |
|---|---|---|---|
| `page` | int | `0` | Mayor o igual que 0 (la primera página es la 0) |
| `pageSize` | int | `10` | Mayor que 0, máximo 30 |

Respuesta:

```json
{
  "products": [ "..." ],
  "page": 0,
  "pageSize": 10,
  "totalPages": 3,
  "totalProducts": 25
}
```

### `POST /api/v1/product` y `PUT /api/v1/product/{id}`

Body:

```json
{
  "name": "Teclado mecánico",
  "description": "Teclado con switches rojos",
  "quantity": 15,
  "price": 49.99
}
```

| Campo | Regla |
|---|---|
| `name` | Obligatorio, máximo 100 caracteres |
| `description` | Obligatorio, máximo 500 caracteres |
| `quantity` | Obligatorio, mayor o igual a 0 |
| `price` | Obligatorio, mayor o igual a 0 |

Respuesta (`ProductResponseDTO`):

```json
{
  "id": "6716f3a2c9e77a1b2c3d4e5f",
  "name": "Teclado mecánico",
  "description": "Teclado con switches rojos",
  "quantity": 15,
  "price": 49.99
}
```

En el `POST`, además, el header `Location` indica la URL del producto creado:

```
Location: http://localhost:8080/api/v1/product/6716f3a2c9e77a1b2c3d4e5f
```

---

## Manejo de errores

Todos los errores responden con el mismo formato (`ErrorDTO`), construido por `ErrorResponseBuilder`:

```json
{
  "error_name": "NOT FOUND",
  "error_message": "Product with id: '6716f3a2c9e77a1b2c3d4e5f' was not found",
  "error_status_code": 404
}
```

| Situación | Excepción | Status |
|---|---|---|
| El id no tiene formato de ObjectId | `InvalidIdException` | `400 Bad Request` |
| Datos que no cumplen las validaciones | `ConstraintViolationException` | `400 Bad Request` |
| Sin token en un endpoint protegido | `UnauthorizedException` (Quarkus) | `401 Unauthorized` |
| Token inválido, vencido o de otro emisor | `AuthenticationFailedException` (Quarkus) | `401 Unauthorized` |
| Token válido pero sin rol `admin` | `ForbiddenException` (Quarkus) | `403 Forbidden` |
| El producto no existe o está desactivado | `ProductNotFoundException` | `404 Not Found` |
| Error inesperado | Cualquier otra | `500 Internal Server Error` |

En los errores `500` el cliente recibe un mensaje genérico; el detalle solo se registra en el log del servidor para no exponer información interna.

Las excepciones de seguridad las lanza Quarkus, pero cada una tiene su propio mapper (`UnauthorizedMapper`, `AuthenticationFailedMapper`, `ForbiddenMapper`) para que también respondan con el formato `ErrorDTO`.

---

## Decisiones de diseño

- **Borrado lógico.** `DELETE` no elimina el documento: pone `status = false`. Así se conserva el historial (útil, por ejemplo, para órdenes antiguas que referencian el producto). Para la API, un producto desactivado se comporta como inexistente (`404`).


- **Campos de auditoría.** Cada producto guarda `createdBy`, `createdAt`, `updatedBy` y `updatedAt`. No se exponen en la respuesta.


- **Convención de respuestas:**
  - Si el endpoint siempre responde `200` con datos, devuelve el DTO directamente.
  - Si necesita otro status o headers (`201` + `Location`, `204`), usa `RestResponse<T>`.
  - Los errores se lanzan como excepciones y se traducen en `mapper/`.
  - `Response` crudo solo se usa dentro de los ExceptionMappers, porque la interfaz `ExceptionMapper` obliga a devolverlo. Se arma en un único lugar: `ErrorResponseBuilder`.


- **Autenticación no proactiva.** Se desactivó para que el catálogo público no se bloquee por un token vencido y para que todos los errores, incluidos los de autenticación, tengan el mismo formato.


- **DTOs separados de la entidad.** La API no expone el documento de Mongo directamente; usa `ProductRequestDTO` para la entrada y `ProductResponseDTO` para la salida.

---

## Pendientes

- [ ] Rate limiting en los endpoints públicos (`GET`), para evitar abuso.
- [ ] Configuración por variables de entorno.
- [ ] Dockerfile / `docker-compose` con MongoDB.
- [ ] Pruebas unitarias de los endpoints.
