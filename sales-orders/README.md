# sales-orders

Microservicio de **órdenes de compra** de Benito Ventas. Recibe la orden, la encola en **Kafka** y la procesa en segundo plano: arma la orden con los datos reales de los productos, cobra el saldo del cliente y la marca como completada o rechazada.

---

## Arquitectura interna

```
resource/    → Endpoints HTTP
service/
  OrderService         → Crear la orden (PENDING) y procesarla (COMPLETED / REJECTED)
  ServiceTokenProvider → Pide a sales-auth el token de servicio para cobrar
consumer/    → OrderConsumer: escucha Kafka y llama a OrderService.processOrder
repository/  → Acceso a MongoDB (Panache)
entity/      → Documento de Mongo (Order), sus líneas (OrderProductRecord) y el enum OrderState
dto/         → Objetos de entrada y salida
  restClientDTO/ → Objetos para hablar con products, clients y auth
restClient/  → Clientes HTTP hacia sales-products, sales-clients y sales-auth
exception/   → Excepciones de negocio y de los otros servicios
mapper/
  exceptionMapper/ → Convierten excepciones en respuestas HTTP
helper/      → ErrorResponseBuilder
filter/      → MtlsGuardFilter: exige certificado de cliente a admin y servicios
```

```
                 POST /order
Usuario ──▶ OrderResource ──▶ OrderService.createPurchaseOrder
                                 │ 1. valida el cliente (token del usuario)
                                 │ 2. guarda la orden en PENDING
                                 │ 3. publica el id de la orden
                                 ▼
                      Kafka (topic order-requests)
                                 │
                                 ▼
                 OrderConsumer ──▶ OrderService.processOrder
                                 ├─▶ sales-products  (precios)
                                 ├─▶ sales-auth      (token de servicio)
                                 ├─▶ sales-clients   (cobro)
                                 └─▶ MongoDB         (COMPLETED / REJECTED)
```

### La orden

| Campo | Descripción |
|---|---|
| `id` | Id de la orden |
| `clientId` | Ficha del cliente que compra (en `sales-clients`) |
| `userId` | Usuario dueño de esa ficha; permite que el cliente vea sus órdenes sin llamar a clients |
| `products` | Lista de líneas: `productId`, `productName`, `pricePerUnit`, `quantity`, `total` |
| `orderTotal` | Suma de las líneas |
| `orderStatus` | `PENDING`, `COMPLETED` o `REJECTED` |
| `rejectionReason` | Motivo del rechazo (solo si es `REJECTED`) |
| `status` + auditoría | Borrado lógico, `createdBy`, `createdAt`, `updatedBy`, `updatedAt` |

Las líneas son documentos **incrustados** dentro de la orden. Mientras la orden está en `PENDING`, las líneas solo tienen `productId` y `quantity`; nombre, precio y totales se completan al procesarla.

---

## Configuración

| Propiedad | Valor / descripción |
|---|---|
| `quarkus.http.ssl-port` | `8084` en desarrollo, `8443` en Docker. Solo https (TLS/mTLS: ver el [README principal](../README.md#tls-y-mtls)) |
| `quarkus.mongodb.database` | `ordersdb` |
| `quarkus.rest-client.product-api.url` | En desarrollo: `https://localhost:8080`. En Docker: `https://sales-products:8443` |
| `quarkus.rest-client.client-api.url` | En desarrollo: `https://localhost:8081`. En Docker: `https://sales-clients:8443` |
| `quarkus.rest-client.auth-api.url` | En desarrollo: `https://localhost:8083`. En Docker: `https://sales-auth:8443` |
| `quarkus.rest-client.connect-timeout` / `read-timeout` | `2000` / `5000` ms |
| `kafka.bootstrap.servers` | En desarrollo: `localhost:9092`. En Docker: `kafka:29092` |
| `mp.messaging.outgoing.orders-out.*` | Canal de salida al topic `order-requests` |
| `mp.messaging.incoming.orders-in.*` | Canal de entrada del topic `order-requests`, grupo `orders-processor`, `auto.offset.reset=earliest`, `failure-strategy=dead-letter-queue` |
| `orders.service.secret` | Secreto para pedir el token de servicio (en Docker: `ORDERS_SERVICE_SECRET`) |
| `quarkus.kafka.health.enabled` | `true`: el health check también revisa Kafka |
| `mp.jwt.verify.issuer` | `https://benito-ventas.app` |
| `mp.jwt.verify.publickey.location` | En desarrollo: `src/main/resources/publicKey.pem`. En Docker: `/run/secrets/jwt_public_key` |

---

## Endpoints

Ruta base: `/api/v1/order`

| Método | Ruta | Acceso | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/api/v1/order` | `admin` | Lista todas las órdenes activas, de la más nueva a la más vieja | `200` |
| `GET` | `/api/v1/order/me` | `client` | Lista las órdenes del cliente autenticado | `200` |
| `GET` | `/api/v1/order/{id}` | `admin`, `client` (solo las suyas) | Obtiene una orden, con su estado | `200` |
| `POST` | `/api/v1/order` | `admin`, `client` | Recibe la orden y la encola para procesarla | `202` |
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

Respuesta `202 Accepted` (la orden se recibió, pero todavía no se procesó):

```json
{
  "id": "6ae17cc5c94113740c81562a",
  "clientId": "6ae17c9146fe9c4fdff515bc",
  "products": [
    { "productId": "6ae179e904e46ca48233edc5", "quantity": 1 },
    { "productId": "6ae179e904e46ca48233edc6", "quantity": 2 }
  ],
  "orderStatus": "PENDING",
  "createdAt": "2026-10-29T18:40:37.599Z"
}
```

### `GET /api/v1/order/{id}`

El cliente usa el `id` recibido para consultar el resultado:

```json
{
  "id": "6ae17cc5c94113740c81562a",
  "clientId": "6ae17c9146fe9c4fdff515bc",
  "products": [
    { "productId": "6ae179e904e46ca48233edc5", "productName": "Teclado mecánico", "pricePerUnit": 49.99, "quantity": 1, "total": 49.99 },
    { "productId": "6ae179e904e46ca48233edc6", "productName": "Mouse", "pricePerUnit": 25, "quantity": 2, "total": 50 }
  ],
  "orderTotal": 99.99,
  "orderStatus": "COMPLETED",
  "rejectionReason": null,
  "createdAt": "2026-10-29T18:40:37.599Z"
}
```

---

## Cómo se procesa una orden

### Parte 1: en la petición HTTP (síncrona)

1. **Busca el cliente** en `sales-clients`, reenviando el token del usuario. Así valida que la ficha exista y que quien llama tenga acceso a ella. Si no, responde el error en ese momento (`403`, `404`...).
2. **Guarda la orden** en estado `PENDING`.
3. **Publica un mensaje en Kafka** (topic `order-requests`) y responde `202`.

### Parte 2: en el consumidor (asíncrona)

4. `OrderConsumer` recibe el mensaje y llama a `OrderService.processOrder`.
5. Si la orden no existe, está desactivada o ya no está en `PENDING`, **no hace nada** (consumidor idempotente: procesar dos veces el mismo mensaje no cobra dos veces).
6. **Busca cada producto** en `sales-products` y calcula el total de cada línea y el de la orden.
7. **Pide un token de servicio** a `sales-auth` y **cobra el saldo** en `sales-clients`.
8. Guarda la orden como `COMPLETED`.

Si algo falla:

| Tipo de error | Ejemplos | Qué pasa |
|---|---|---|
| De negocio | Saldo insuficiente, producto inexistente | `REJECTED` con el motivo en `rejectionReason` |
| Técnico | Un servicio caído, error inesperado | `REJECTED` con "The order could not be processed, please try again later" y el mensaje va a la **dead letter queue** |

Una orden rechazada por saldo insuficiente conserva el total calculado (sirve para ver cuánto faltaba). Una rechazada por un producto inexistente queda con el total en `null`, porque falló antes de poder calcularlo.

### El mensaje de Kafka

| Parte | Valor | Por qué |
|---|---|---|
| Llave | `clientId` | Las órdenes de un mismo cliente caen en la misma partición y se procesan **en orden**, una detrás de otra. Así dos compras del mismo cliente no cobran el saldo al mismo tiempo |
| Valor | `id` de la orden | El mensaje es pequeño; los datos viven en MongoDB |

---

## Errores

Responden en la petición HTTP (antes de encolar):

| Situación | Status |
|---|---|
| Id inválido / datos inválidos | `400` |
| Sin token / token inválido | `401` |
| Rol no permitido / orden o ficha ajena | `403` |
| Orden o cliente inexistente | `404` |
| Un servicio respondió algo inesperado | `502` |
| Un servicio no respondió (caído o timeout) | `503` |
| Error inesperado | `500` |

Los errores que ocurren al procesar (saldo insuficiente, producto inexistente) no se responden por HTTP: quedan en la orden como `REJECTED` + `rejectionReason`.

---

## Decisiones de diseño

- **Órdenes encoladas en Kafka (pub-sub)**: el usuario recibe respuesta rápida (`202`) aunque products o clients estén lentos, y las órdenes no se pierden si el procesamiento falla: quedan en el topic.
- **`REJECTED` en lugar de borrar**: antes, si el cobro fallaba, la orden se borraba. Ahora queda guardada con su motivo, así el cliente puede ver qué pasó.
- **La validación del cliente es síncrona**: si alguien intenta comprar a nombre de otra ficha, se entera en el momento (`403`) y no se encola nada.
- **Token de servicio para cobrar**: el consumidor no tiene el token del usuario (puede haber vencido). Por eso pide a `sales-auth` un token propio de 1 minuto con el rol `orderService`.
- **Snapshot del momento de la compra**: la orden guarda nombre y precio de cada producto. Si mañana el producto cambia, la orden sigue mostrando lo que se pagó.
- **El total lo calcula el servidor**, nunca se acepta del cliente.
- **El admin compra a nombre de un cliente** indicando su `clientId`; no necesita ficha propia.
- **Se guarda el `userId`** en la orden para filtrar "mis órdenes" sin llamar a clients.

## Fuera del alcance de la prueba

- Más estados de la orden (pagada, enviada, cancelada) y reembolsos.
