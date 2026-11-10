# sales-orders

Microservicio de **órdenes de compra** de Benito Ventas. Recibe la orden, la valida, calcula su total con los datos reales de los productos y la publica en **Kafka** para que `sales-order-processor` la cobre. Después consume el resultado del cobro y marca la orden como completada o rechazada.

---

## Arquitectura interna

```
resource/    → Endpoints HTTP
service/
  OrderService         → Crear la orden (PENDING) y aplicar el resultado del cobro (COMPLETED / REJECTED)
  ServiceTokenProvider → Pide a sales-auth el token de servicio para leer los productos
consumer/    → OrderResultConsumer: escucha el topic order-results y llama a OrderService.applyOrderResult
repository/  → Acceso a MongoDB (Panache)
entity/      → Documento de Mongo (Order), sus líneas (OrderProductRecord) y el enum OrderState
dto/         → Objetos de entrada y salida
  KafkaDTO/      → Mensajes de Kafka (la orden publicada y el resultado del cobro)
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
                                 │ 1. valida el cliente (token del usuario)  ──▶ sales-clients
                                 │ 2. token de servicio                      ──▶ sales-auth
                                 │ 3. todos los productos en una llamada     ──▶ sales-products
                                 │ 4. calcula los totales y guarda en PENDING ──▶ MongoDB
                                 │ 5. publica clientId, orderId y total
                                 ▼
                      Kafka (topic order-requests) ──▶ sales-order-processor cobra
                                                              │
                      Kafka (topic order-results)  ◀──────────┘
                                 │
                                 ▼
          OrderResultConsumer ──▶ OrderService.applyOrderResult ──▶ MongoDB (COMPLETED / REJECTED)
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

Las líneas son documentos **incrustados** dentro de la orden. Nombre, precio y totales se calculan **al crear la orden**, así que ya están completos mientras está en `PENDING`.

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
| `mp.messaging.outgoing.orders-out.*` | Canal de salida al topic `order-requests` (lo consume `sales-order-processor`) |
| `mp.messaging.incoming.order-results-in.*` | Canal de entrada del topic `order-results`, grupo `orders-results`, `auto.offset.reset=earliest`, `failure-strategy=dead-letter-queue` |
| `kafka.security.protocol` / `kafka.ssl.*` | `SSL` con `orders.pem` (certificado + llave) y `ca.crt`: mTLS hacia Kafka. En desarrollo, `PLAINTEXT` |
| `orders.service.secret` | Secreto para pedir el token de servicio con el que lee los productos (en Docker: `ORDERS_SERVICE_SECRET`) |
| `quarkus.kafka.health.enabled` | `true`: el health check también revisa Kafka |
| `mp.jwt.verify.issuer` | `https://benito-ventas.app` |
| `mp.jwt.verify.publickey.location` | En desarrollo: `src/main/resources/publicKey.pem`. En Docker: `/run/secrets/jwt_public_key` |
| `quarkus.swagger-ui.always-include` | Swagger UI disponible también en Docker, en `/q/swagger-ui` |

---

## Endpoints

Ruta base: `/api/v1/order`

| Método | Ruta | Acceso | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/api/v1/order` | `admin` | Lista todas las órdenes activas, de la más nueva a la más vieja | `200` |
| `GET` | `/api/v1/order/me` | `client` | Lista las órdenes del cliente autenticado | `200` |
| `GET` | `/api/v1/order/{id}` | `admin`, `client` (solo las suyas) | Obtiene una orden, con su estado | `200` |
| `POST` | `/api/v1/order` | `admin`, `client` | Valida la orden, calcula su total y la publica para cobrarla | `202` |
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
| `products` | Entre 1 y 30 productos, **sin repetir** el mismo `productId` |
| `products[].productId` | Obligatorio, id válido |
| `products[].quantity` | Mayor que 0 |

El cliente solo manda **qué** quiere y **cuánto**. Nombre, precio y totales los obtiene y calcula el servicio, nunca se aceptan del body.

Respuesta `202 Accepted` (la orden se recibió y se validó, pero todavía no se cobró):

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

1. **Busca el cliente** en `sales-clients`, reenviando el token del usuario. Así valida que la ficha exista y que quien llama tenga acceso a ella.
2. **Pide un token de servicio** a `sales-auth` (rol `orderService`).
3. **Busca todos los productos en una sola llamada** al endpoint interno de `sales-products` (`/api/v1/product/internal?ids=...`, sin rate limit).
4. **Calcula el total** de cada línea y el de la orden, y la **guarda** en estado `PENDING`.
5. **Publica un mensaje en Kafka** (topic `order-requests`) y responde `202`.

Si el cliente no existe, no tiene acceso, un producto no existe o un ID es inválido, responde el error **en ese momento** (`403`, `404`, `400`) y no se guarda ni se publica nada. El mensaje de error nombra el producto que falló.

### Parte 2: el cobro (en `sales-order-processor`)

El procesador cobra el total al cliente y publica el resultado en el topic `order-results`. El detalle está en su [README](../sales-order-processor/README.md).

### Parte 3: el resultado (asíncrona)

6. `OrderResultConsumer` recibe el resultado (`orderId`, estado y motivo) y llama a `OrderService.applyOrderResult`.
7. Si el estado no es `COMPLETED` ni `REJECTED`, o el ID no es válido, lo ignora.
8. Actualiza la orden **en una sola operación**, solo si sigue en `PENDING`. Si ya no lo está (el mismo resultado llegó dos veces), no cambia nada (**consumidor idempotente**).

La **dead letter queue** recibe los resultados que no se pudieron guardar (por ejemplo, si falla MongoDB), así se pueden reprocesar.

### Los mensajes de Kafka

| Topic | Llave | Valor | Por qué |
|---|---|---|---|
| `order-requests` (publica) | `clientId` | `clientId`, `orderId` y el total | Las órdenes de un mismo cliente caen en la misma partición y se cobran **en orden**, una detrás de otra. El procesador no necesita leer la base de orders |
| `order-results` (consume) | `orderId` | `orderId`, estado y `rejectionReason` | Lo justo para actualizar la orden |

---

## Errores

Responden en la petición HTTP (antes de publicar):

| Situación | Status |
|---|---|
| Id inválido / datos inválidos | `400` |
| Sin token / token inválido | `401` |
| Rol no permitido / orden o ficha ajena | `403` |
| Orden, cliente o producto inexistente | `404` |
| Un servicio respondió algo inesperado | `502` |
| Un servicio no respondió (caído o timeout) | `503` |
| Error inesperado | `500` |

Los errores del cobro (saldo insuficiente, ficha desactivada) no se responden por HTTP: quedan en la orden como `REJECTED` + `rejectionReason`.

---

## Decisiones de diseño

- **Órdenes encoladas en Kafka (pub-sub)**: el usuario recibe respuesta rápida (`202`) aunque el cobro tarde, y las órdenes no se pierden si el cobro falla: quedan en el topic.


- **El cobro vive en otro servicio** (`sales-order-processor`): la API y el cobro escalan, fallan y se despliegan por separado. Orders ya no llama a clients para cobrar.


- **Validar y calcular al crear**: el cliente se entera al instante si un producto no existe, y el mensaje de Kafka ya lleva el total, así el procesador no depende de products ni de la base de orders.


- **`REJECTED` en lugar de borrar**: antes, si el cobro fallaba, la orden se borraba. Ahora queda guardada con su motivo, así el cliente puede ver qué pasó.


- **La validación del cliente es síncrona**: si alguien intenta comprar a nombre de otra ficha, se entera en el momento (`403`) y no se publica nada.


- **Token de servicio para leer productos**: el endpoint interno de products solo acepta el rol `orderService`, así que orders pide a `sales-auth` un token propio de 1 minuto.


- **Endpoint interno de productos, por lotes**: el endpoint público tiene rate limit por IP, y todas las llamadas de orders vienen de la misma IP. Con el endpoint interno, crear órdenes no compite con el límite de los usuarios; y al pedir todos los IDs juntos, una orden con N productos hace una sola llamada (sin el problema N+1).


- **Reintentos**: leer el cliente, los productos y pedir el token se reintentan si el servicio no responde, porque repetirlos no tiene efectos.


- **Resultado idempotente**: la actualización solo aplica si la orden sigue en `PENDING`, en una sola operación. Un resultado repetido no cambia nada.


- **Snapshot del momento de la compra**: la orden guarda nombre y precio de cada producto. Si mañana el producto cambia, la orden sigue mostrando lo que se pagó.


- **El total lo calcula el servidor**, nunca se acepta del cliente.


- **El admin compra a nombre de un cliente** indicando su `clientId`; no necesita ficha propia.


- **Se guarda el `userId`** en la orden para filtrar "mis órdenes" sin llamar a clients.

## Fuera del alcance de la prueba

- Más estados de la orden (pagada, enviada, cancelada) y reembolsos.