# sales-orders

Microservicio de **órdenes de compra** de Benito Ventas. Arma la orden con los datos reales de los productos, cobra el saldo del cliente y permite consultar y desactivar órdenes.

---

## Arquitectura interna

```
resource/    → Endpoints HTTP
service/     → Lógica de negocio: armar la orden, cobrar, compensar
repository/  → Acceso a MongoDB (Panache)
entity/      → Documento de Mongo (Order) y sus líneas (OrderProductRecord)
dto/         → Objetos de entrada y salida
  restClientDTO/ → Objetos para hablar con products y clients
restClient/  → Clientes HTTP hacia sales-products y sales-clients
exception/   → Excepciones de negocio y de los otros servicios
mapper/
  exceptionMapper/ → Convierten excepciones en respuestas HTTP
helper/      → ErrorResponseBuilder
```

```
Usuario → OrderResource → OrderService ─┬─▶ ProductsApiClient → sales-products
                                        ├─▶ ClientsApiClient  → sales-clients
                                        └─▶ OrderRepository   → MongoDB (ordersdb)
```

### La orden

| Campo | Descripción |
|---|---|
| `id` | Id de la orden |
| `clientId` | Ficha del cliente que compra (en `sales-clients`) |
| `userId` | Usuario dueño de esa ficha; permite que el cliente vea sus órdenes sin llamar a clients |
| `products` | Lista de líneas: `productId`, `productName`, `pricePerUnit`, `quantity`, `total` |
| `orderTotal` | Suma de las líneas |
| `status` + auditoría | Borrado lógico, `createdBy`, `createdAt`, `updatedBy`, `updatedAt` |

Las líneas son documentos **incrustados** dentro de la orden.

---

## Configuración

| Propiedad | Valor / descripción |
|---|---|
| `quarkus.http.port` | `8084` |
| `quarkus.mongodb.database` | `ordersdb` |
| `quarkus.rest-client.product-api.url` | En desarrollo: `http://localhost:8080`. En Docker: `http://sales-products:8080` |
| `quarkus.rest-client.client-api.url` | En desarrollo: `http://localhost:8081`. En Docker: `http://sales-clients:8080` |
| `quarkus.rest-client.connect-timeout` / `read-timeout` | `2000` / `5000` ms |
| `mp.jwt.verify.issuer` | `https://benito-ventas.app` |
| `mp.jwt.verify.publickey.location` | En desarrollo: `src/main/resources/publicKey.pem`. En Docker: `/run/secrets/jwt_public_key` |

---

## Endpoints

Ruta base: `/api/v1/order`

| Método | Ruta | Acceso | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/api/v1/order` | `admin` | Lista todas las órdenes activas, de la más nueva a la más vieja | `200` |
| `GET` | `/api/v1/order/me` | `client` | Lista las órdenes del cliente autenticado | `200` |
| `GET` | `/api/v1/order/{id}` | `admin`, `client` (solo las suyas) | Obtiene una orden | `200` |
| `POST` | `/api/v1/order` | `admin`, `client` | Crea una orden y cobra el saldo | `201` + `Location` |
| `DELETE` | `/api/v1/order/{id}` | `admin` | Desactiva una orden (no reembolsa) | `204` |

Los listados aceptan `page` (desde 0) y `pageSize` (1 a 30).

### `POST /api/v1/order`

```json
{
  "clientId": "6ae17c9146fe9c4fdff515bc",
  "products": [
    { "productId": "6ae179e904e46ca48233edc5", "quantity": 1 },
    { "productId": "6ae179e904e46ca48233edc6", "quantity": 2 }
  ]
}
```

| Campo | Regla |
|---|---|
| `clientId` | Obligatorio, id válido |
| `products` | Al menos un producto |
| `products[].productId` | Obligatorio |
| `products[].quantity` | Mayor que 0 |

El cliente solo manda **qué** quiere y **cuánto**. Nombre, precio y totales los obtiene y calcula el servicio, nunca se aceptan del body.

Respuesta:

```json
{
  "id": "6ae17cc5c94113740c81562a",
  "clientId": "6ae17c9146fe9c4fdff515bc",
  "products": [
    { "productId": "6ae179e904e46ca48233edc5", "productName": "Teclado mecánico", "pricePerUnit": 49.99, "quantity": 1, "total": 49.99 },
    { "productId": "6ae179e904e46ca48233edc6", "productName": "Mouse", "pricePerUnit": 25, "quantity": 2, "total": 50 }
  ],
  "orderTotal": 99.99,
  "createdAt": "2026-10-28T04:40:37.599Z"
}
```

---

## Cómo se crea una orden

1. **Busca el cliente** en `sales-clients`, reenviando el token del usuario. Así valida que la ficha exista y que quien llama tenga acceso a ella.
2. **Busca cada producto** en `sales-products` (endpoint público) y calcula el total de cada línea y el de la orden.
3. **Guarda la orden.**
4. **Cobra el saldo** con `POST /client/{id}/balance/debit`.
   - Si el cobro falla por cualquier motivo (saldo insuficiente, cliente caído...), **borra la orden** (compensación) y responde el error.

---

## Errores

| Situación | Status |
|---|---|
| Id inválido / datos inválidos | `400` |
| Sin token / token inválido | `401` |
| Rol no permitido / orden o ficha ajena | `403` |
| Orden, cliente o producto inexistente | `404` |
| Saldo insuficiente | `422` |
| Un servicio respondió algo inesperado | `502` |
| Un servicio no respondió (caído o timeout) | `503` |
| Error inesperado | `500` |

Los errores de products y clients se traducen a excepciones propias, con mensajes claros, por ejemplo "Product with id: '...' was not found".

---

## Decisiones de diseño

- **Snapshot del momento de la compra**: la orden guarda nombre y precio de cada producto. Si mañana el producto cambia, la orden sigue mostrando lo que se pagó.
- **Primero guardar, después cobrar**: así la compensación es local (borrar la propia orden) y no hace falta un endpoint de reembolso.
- **El total lo calcula el servidor**, nunca se acepta del cliente.
- **Propagación del token**: orders no tiene credenciales propias; llama a clients con los permisos del usuario que originó la compra.
- **El admin compra a nombre de un cliente** indicando su `clientId`; no necesita ficha propia.
- **Se guarda el `userId`** en la orden para filtrar "mis órdenes" sin llamar a clients.

## Fuera del alcance de la prueba

- Estados de la orden (pagada, enviada, cancelada) y reembolsos.
