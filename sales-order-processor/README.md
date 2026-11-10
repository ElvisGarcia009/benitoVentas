# sales-order-processor

Microservicio que **cobra las órdenes** de Benito Ventas. Escucha en Kafka las órdenes que publica `sales-orders`, le debita el total al cliente en `sales-clients` y publica el resultado (`COMPLETED` o `REJECTED`) para que `sales-orders` actualice la orden.

No tiene endpoints de negocio: todo su trabajo ocurre en segundo plano, a partir de los mensajes de Kafka.

---

## Por qué es un servicio aparte

Antes, `sales-orders` publicaba la orden y la consumía él mismo. Funcionaba, pero la API y el procesamiento vivían en el mismo servicio:

- No se podían **escalar por separado**: más consumidores obligaban a levantar más instancias de toda la API.
- Si el procesamiento se volvía pesado, **afectaba a la API** que atiende a los usuarios.
- Un cambio en el cobro obligaba a **redesplegar también la API**.

Ahora `sales-orders` solo recibe órdenes y muestra su estado, y este servicio se encarga del cobro.

---

## Arquitectura interna

```
consumer/    → OrderProcessor: escucha el topic de órdenes y llama a OrderProcessorService
service/
  OrderProcessorService → Cobra la orden y publica el resultado
  ServiceTokenProvider  → Pide a sales-auth el token de servicio para cobrar
repository/  → OrdersPaymentsRepository: registro de pagos en MongoDB (Panache)
entity/      → OrdersPayments (pago de una orden) y el enum OrderStatus
dto/         → Mensajes de Kafka y objetos para hablar con clients y auth
restClient/  → Clientes HTTP hacia sales-clients y sales-auth
exception/   → Excepciones de los otros servicios
```

```
sales-orders ──▶ Kafka (topic de órdenes, key = clientId)
                        │
                        ▼
        OrderProcessor ──▶ OrderProcessorService.processPayment
                        │ 1. ¿esta orden ya se procesó? (MongoDB)
                        │ 2. token de servicio      ──▶ sales-auth
                        │ 3. debita con el orderId   ──▶ sales-clients
                        │ 4. guarda el resultado    ──▶ MongoDB
                        ▼
              Kafka (topic de resultados) ──▶ sales-orders actualiza la orden
```

### El registro de pago (`OrdersPayments`)

| Campo | Descripción |
|---|---|
| `orderId` | Orden que se cobró |
| `status` | Resultado del cobro: `COMPLETED` o `REJECTED` |

Sirve para saber si una orden **ya se procesó**, y así no cobrarla dos veces si el mismo mensaje llega otra vez.

---

## Cómo se cobra una orden

1. `OrderProcessor` recibe el mensaje con `clientId`, `orderId` y el total de la orden.
2. **Busca si la orden ya se procesó.** Si ya tiene un resultado guardado, **no vuelve a cobrar**: solo publica otra vez ese resultado, por si el mensaje anterior se perdió.
3. **Pide un token de servicio** a `sales-auth`.
4. **Debita el total** en `sales-clients`, enviando el `orderId`.
5. Guarda el resultado y lo **publica** en el topic de resultados.

| Resultado del débito | Qué pasa |
|---|---|
| Se cobró | Guarda y publica `COMPLETED` |
| Saldo insuficiente (`422`) | Guarda y publica `REJECTED` con el motivo |
| `sales-clients` no respondió (caído o timeout) | **No publica nada.** No se sabe si el cobro se aplicó, así que no se puede decir `REJECTED`. La excepción sube y el mensaje va a la **dead letter queue** |

---

## Idempotencia: una orden nunca se cobra dos veces

Kafka entrega los mensajes **al menos una vez** (*at-least-once*): si el servicio se cae después de cobrar pero antes de confirmar el mensaje, ese mismo mensaje llega otra vez. Hay dos protecciones:

1. **En este servicio:** el registro de `OrdersPayments`. Si la orden ya tiene un resultado, no se vuelve a cobrar.
2. **En `sales-clients`:** el `orderId` viaja con el débito como **llave de idempotencia**. Clients guarda las órdenes que ya cobró a cada cliente, y lo comprueba **en la misma operación atómica** que resta el saldo. Si le llega el mismo `orderId` otra vez, no cobra y responde como un débito exitoso.

Gracias a la segunda, reintentar un débito es seguro, incluso después de un timeout o al reprocesar un mensaje de la dead letter queue.

---

## Configuración

Las propiedades exactas están en `src/main/resources/application.properties`. Las piezas principales:

| Pieza | Descripción |
|---|---|
| HTTPS | Solo https (TLS/mTLS: ver el [README principal](../README.md#tls-y-mtls)). Expone `/q/health` |
| MongoDB | Base propia, con la colección de registros de pago |
| `sales-clients` y `sales-auth` | URLs de los REST clients. En Docker, por el nombre del servicio |
| Kafka entrada | Topic de órdenes, con la estrategia `dead-letter-queue` |
| Kafka salida | Topic de resultados, que consume `sales-orders` |
| Kafka seguridad | `SSL` con el certificado del procesador y `ca.crt`: mTLS hacia Kafka |
| Token de servicio | Secreto y certificado propios para pedir su token a `sales-auth` |

---

## Decisiones de diseño

- **Servicio separado del API de órdenes:** la API y el cobro escalan, fallan y se despliegan por separado.
- **Llave de idempotencia con el `orderId`:** un mensaje repetido o un reintento nunca cobra dos veces.
- **Con un timeout no se rechaza la orden:** el resultado es incierto; decir `REJECTED` podría ser falso si el cobro sí se aplicó. El mensaje queda en la dead letter queue para reprocesarlo con seguridad.
- **El resultado viaja por Kafka, no por HTTP:** `sales-orders` no necesita estar disponible en el momento del cobro; el resultado espera en el topic.
- **La key sigue siendo el `clientId`:** las órdenes de un mismo cliente se cobran en orden, una detrás de otra.