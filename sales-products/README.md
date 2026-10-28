# sales-products

Microservicio de **productos** de Benito Ventas. Expone el catálogo de forma pública y permite que el administrador lo gestione.

---

## Arquitectura interna

```
resource/    → Endpoints HTTP
service/     → Lógica de negocio
repository/  → Acceso a MongoDB (Panache)
entity/      → Documento de Mongo (Product)
dto/         → Objetos de entrada y salida de la API
exception/   → Excepciones de negocio
mapper/
  exceptionMapper/ → Convierten excepciones en respuestas HTTP
helper/      → Utilidades compartidas (ErrorResponseBuilder)
```

```
Usuario → ProductResource → ProductService → ProductRepository → MongoDB (productdb)
```

---

## Configuración

| Propiedad | Valor / descripción |
|---|---|
| `quarkus.http.port` | `8080` |
| `quarkus.http.auth.proactive` | `false`: el token solo se valida donde hace falta |
| `quarkus.mongodb.database` | `productdb` |
| `mp.jwt.verify.issuer` | `https://benito-ventas.app` |
| `mp.jwt.verify.publickey.location` | En desarrollo: `src/main/resources/publicKey.pem`. En Docker: `/run/secrets/jwt_public_key` |
| `quarkus.rate-limiter.buckets.unauthenticatedResource.*` | 10 peticiones por segundo y 100 cada 5 minutos, por IP |

En Docker, la conexión a Mongo y la clave pública llegan por variables de entorno (`docker-compose.yml`).

---

## Endpoints

Ruta base: `/api/v1/product`

| Método | Ruta | Acceso | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/api/v1/product` | Público (con rate limit) | Lista los productos activos, paginados, por nombre | `200` |
| `GET` | `/api/v1/product/{id}` | Público (con rate limit) | Obtiene un producto | `200` |
| `POST` | `/api/v1/product` | `admin` | Crea un producto | `201` + `Location` |
| `PUT` | `/api/v1/product/{id}` | `admin` | Reemplaza los datos de un producto | `200` |
| `DELETE` | `/api/v1/product/{id}` | `admin` | Desactiva un producto (borrado lógico) | `204` |

### `GET /api/v1/product`

| Parámetro | Por defecto | Regla |
|---|---|---|
| `page` | `0` | Mayor o igual que 0 |
| `pageSize` | `10` | Entre 1 y 30 |

```json
{
  "products": [ { "id": "...", "name": "...", "description": "...", "price": 49.99 } ],
  "page": 0,
  "pageSize": 10,
  "totalPages": 1,
  "totalProducts": 2
}
```

### `POST` y `PUT`

```json
{
  "name": "Teclado mecánico",
  "description": "Teclado con switches rojos",
  "price": 49.99
}
```

| Campo | Regla |
|---|---|
| `name` | Obligatorio, máximo 100 caracteres |
| `description` | Obligatorio, máximo 500 caracteres |
| `price` | Obligatorio, mayor o igual a 0 |

Respuesta (`ProductResponseDTO`):

```json
{
  "id": "6716f3a2c9e77a1b2c3d4e5f",
  "name": "Teclado mecánico",
  "description": "Teclado con switches rojos",
  "price": 49.99
}
```

---

## Errores

| Situación | Status |
|---|---|
| Id con formato inválido | `400` |
| Datos que no cumplen las validaciones | `400` |
| Sin token / token inválido | `401` |
| Token sin rol `admin` | `403` |
| Producto inexistente o desactivado | `404` |
| Más peticiones de las permitidas | `429` |
| Error inesperado | `500` (mensaje genérico; el detalle va al log) |

---

## Decisiones de diseño

- **Catálogo público con rate limit por IP**: cualquiera puede consultar, pero una sola IP no puede saturar el servicio.
- **Sin inventario**: los productos no tienen stock (fuera del alcance de la prueba).
- **El precio vive aquí, pero la orden guarda una copia**: `sales-orders` guarda nombre y precio en el momento de la compra, así un cambio de precio no altera las órdenes viejas.
- **Borrado lógico** y **campos de auditoría** (`createdBy`, `createdAt`, `updatedBy`, `updatedAt`), que no se exponen en la respuesta.


