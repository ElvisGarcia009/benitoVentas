# Benito Ventas

Sistema de ventas construido con **microservicios Quarkus + MongoDB**. Permite consultar un catálogo de productos sin autenticación, registrarse como cliente, comprar con el saldo de la cuenta y que un administrador gestione productos, usuarios, clientes y órdenes.

## Requisitos de negocio

| Quién | Qué puede hacer |
|---|---|
| Usuario no autenticado | Consultar productos y precios. Registrarse como cliente |
| Cliente | Consultar y actualizar sus datos. Crear órdenes de compra y consultar las suyas |
| Administrador | Gestionar productos, usuarios, clientes y órdenes |

---

## Arquitectura

El sistema está dividido en **cinco servicios por dominio** (qué se maneja). Lo que puede hacer cada usuario se controla con **roles en el JWT**, dentro de cada servicio.

> **Rama `mejoras-post-entrega`.** Esta rama contiene mejoras hechas después de la entrega original (que está en `main`). El resumen de los cambios está en [Mejoras post-entrega](#mejoras-post-entrega).

```
                         ┌──────────────────┐
     login / registro    │   sales-auth     │  emite los JWT (clave privada)
   ─────────────────────▶│   usuarios       │──┐
                         └──────────────────┘  │ crea la ficha del cliente
                            ▲            ▲     ▼ (token de servicio)
               token de     │            │ token de servicio
               servicio     │            │
┌──────────────────┐     ┌──────────────────┐ │   ┌──────────────────┐
│  sales-products  │◀────│   sales-orders   │─┼──▶│  sales-clients   │
│  catálogo        │ IDs │   órdenes        │ │   │  fichas y saldo  │
└──────────────────┘     └──────────────────┘ │   └──────────────────┘
                     publica │       ▲ consume │            ▲
                    la orden ▼       │ el      │            │ cobra con el
                         ┌───────────┴────┐    │            │ id de la orden
                         │     Kafka      │    │            │
                         │ order-requests │    │            │
                         │ order-results  │    │            │
                         └───────────┬────┘    │            │
                         consume ▼   ▲ publica │            │
                         ┌───────────┴──────────┴───────────┴─┐
                         │       sales-order-processor        │
                         │       cobra las órdenes            │
                         └────────────────────────────────────┘

           Todos verifican los JWT con la clave pública
                          ┌────────────┐
                          │  MongoDB   │  una base de datos por servicio
                          └────────────┘
```

| Servicio | Responsabilidad | Base de datos | Puerto (local) | Puerto (Docker) |
|---|---|---|---|---|
| [sales-products](sales-products/README.md) | Catálogo de productos | `productdb` | 8080 | 9080 |
| [sales-clients](sales-clients/README.md) | Fichas de clientes y saldo | `clientdb` | 8081 | 9081 |
| [sales-orders](sales-orders/README.md) | Órdenes de compra | `ordersdb` | 8084 | 9082 |
| [sales-auth](sales-auth/README.md) | Usuarios, registro, login y emisión de tokens | `userdb` | 8083 | 9083 |
| [sales-order-processor](sales-order-processor/README.md) | Cobro de las órdenes | `paymentsdb` | 8085 | No se publica: solo trabaja con Kafka |

| Infraestructura | Puerto |
|---|---|
| MongoDB 7 (con usuario y contraseña) | 27017 |
| Kafka 3.9 (modo KRaft, sin Zookeeper) | 9092 desde fuera de Docker, `kafka:29092` dentro de Docker |

### Usuario vs. cliente

- **Usuario** (`sales-auth`): identidad y acceso. `username`, `email`, hash de la contraseña y roles.
- **Cliente** (`sales-clients`): datos de negocio. Nombre, teléfono, dirección y saldo.

Todo cliente es un usuario, pero no todo usuario es un cliente (un administrador no necesita ficha). La ficha guarda el `userId` del usuario al que pertenece, como **identificador externo** (`String`): los dos viven en servicios y bases de datos distintas.

---

## Stack

- **Java 21**, **Quarkus 3.40** (`quarkus-rest`, `quarkus-rest-jackson`, `quarkus-rest-client-jackson`)
- **MongoDB 7** con **Panache** (patrón repository)
- **SmallRye JWT** para firmar y verificar tokens (RS256)
- **Hibernate Validator** para validar la entrada
- **Bucket4j** (`quarkus-bucket4j`) para rate limiting por IP
- **BCrypt** (`quarkus-elytron-security-common`) para el hash de contraseñas
- **Apache Kafka** + **SmallRye Reactive Messaging** (`quarkus-messaging-kafka`) para encolar las órdenes
- **SmallRye Health** (`quarkus-smallrye-health`) para los health checks
- **SmallRye OpenAPI** + **Swagger UI** (`quarkus-smallrye-openapi`) para documentar la API
- **Docker Compose** para levantar todo el sistema

---

## Cómo ejecutarlo

### Con Docker (recomendado)

1. Generar el par de claves para los JWT en la carpeta `secrets/` (no se versiona):

   ```bash
   mkdir -p secrets && cd secrets
   openssl genrsa -out rsaPrivateKey.pem 2048
   openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem
   openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
   cd ..
   ```

2. Generar la CA propia y los certificados TLS en `secrets/tls/` (tampoco se versiona):

   ```bash
   mkdir -p secrets/tls
   docker run --rm -v "${PWD}/secrets/tls:/tls" -w /tls alpine sh -c '
   apk add -q openssl
   openssl genrsa -out ca.key 2048
   openssl req -x509 -new -key ca.key -days 365 -subj "/CN=Benito-CA" -out ca.crt
   for s in products clients orders auth order-processor admin; do
     openssl genrsa -out $s.key 2048
     openssl req -new -key $s.key -subj "/CN=sales-$s" -out $s.csr
     echo "subjectAltName=DNS:sales-$s,DNS:localhost" > $s.ext
     openssl x509 -req -in $s.csr -CA ca.crt -CAkey ca.key -CAcreateserial -days 365 -extfile $s.ext -out $s.crt
   done
   openssl genrsa -out kafka.key 2048
   openssl req -new -key kafka.key -subj "/CN=kafka" -out kafka.csr
   echo "subjectAltName=DNS:kafka,DNS:localhost" > kafka.ext
   openssl x509 -req -in kafka.csr -CA ca.crt -CAkey ca.key -CAcreateserial -days 365 -extfile kafka.ext -out kafka.crt
   rm *.csr *.ext
   cat kafka.key kafka.crt > kafka.pem
   cat orders.key orders.crt > orders.pem
   cat order-processor.key order-processor.crt > order-processor.pem
   chmod 640 products.key clients.key orders.key auth.key order-processor.key orders.pem order-processor.pem
   chown 1000 kafka.pem && chmod 600 kafka.pem'
   ```

   - Los permisos hacen falta porque Quarkus corre en el contenedor con el usuario `185` (grupo `root`) y Kafka con el usuario `1000`, y cada uno tiene que poder leer su llave.
   - Kafka usa la llave y el certificado en un solo archivo (`.pem`); orders y el procesador usan `orders.pem` y `order-processor.pem` para conectarse a Kafka.
   - El certificado del procesador queda con `CN=sales-order-processor`: auth lo comprueba antes de darle su token de servicio.

3. Compilar cada servicio (el Dockerfile copia `target/quarkus-app`):

   ```bash
   for s in sales-products sales-clients sales-orders sales-order-processor sales-auth; do (cd $s && ./mvnw package -DskipTests); done
   ```

4. Levantar todo:

   ```bash
   docker compose up --build -d
   ```

Las claves y certificados llegan a los contenedores como **Docker secrets** (`/run/secrets/...`). Dentro de Docker, los servicios se llaman entre sí por su nombre y por el puerto interno 8443 (`https://sales-clients:8443`); los puertos 908x son solo para acceder desde fuera, también por https.

Kafka tiene dos "puertas": `kafka:29092` para los contenedores y `localhost:9092` para lo que corre fuera de Docker (por ejemplo, un servicio en modo desarrollo).

> **Nota:** los secretos de los servicios (`ORDERS_SERVICE_SECRET`, `PROCESSOR_SERVICE_SECRET`), la contraseña del admin inicial (`USER_SEEDER_ADMINPASSWORD`) y el usuario de MongoDB están escritos en `docker-compose.yml` como valores por defecto **solo porque es una prueba técnica**, para que el sistema arranque sin pasos extra. En producción irían en un archivo `.env` fuera del repositorio o en un gestor de secretos.

### En modo desarrollo

Con MongoDB en `localhost:27017` y Kafka en `localhost:9092` (por ejemplo, `docker compose up -d mongodb kafka`), en cada servicio:

```bash
./mvnw quarkus:dev
```

Las propiedades con prefijo `%dev.` del `application.properties` solo aplican en este modo.

### Credenciales de prueba

Al arrancar, `sales-auth` crea un administrador si no existe:

| Usuario | Contraseña |
|---|---|
| `admin` | La de `USER_SEEDER_ADMINPASSWORD` en `docker-compose.yml` (en desarrollo, `user.seeder.adminPassword`) |

> En producción, esta contraseña vendría de un gestor de secretos, no del repositorio.

Para probar desde Bruno o Postman: configurar `secrets/tls/ca.crt` como CA de confianza y, para los endpoints de admin, `admin.crt` / `admin.key` como certificado de cliente.

### Health checks

Cada servicio expone:

| Ruta | Qué responde |
|---|---|
| `/q/health` | Estado general |
| `/q/health/live` | ¿El servicio está vivo? |
| `/q/health/ready` | ¿Está listo para recibir peticiones? (MongoDB y, en orders, Kafka) |

El procesador no publica ningún puerto en Docker: su estado se ve con `docker compose ps` y sus logs.

Ejemplo: `https://localhost:9082/q/health`.

### Documentación de la API (Swagger)

| Servicio | URL |
|---|---|
| sales-products | `https://localhost:9080/q/swagger-ui` |
| sales-clients | `https://localhost:9081/q/swagger-ui` |
| sales-orders | `https://localhost:9082/q/swagger-ui` |
| sales-auth | `https://localhost:9083/q/swagger-ui` |

- El navegador muestra una advertencia de certificado porque no conoce la CA propia: se puede aceptar, o importar `secrets/tls/ca.crt` como CA de confianza.
- El botón **Authorize** acepta el JWT del login.
- Los endpoints públicos y de cliente se pueden probar desde Swagger; los de admin no, porque el navegador no presenta el certificado del admin (para eso, Bruno o curl con `admin.crt`).

---

## Flujo principal

1. **Registro** (`POST sales-auth /api/v1/auth/register`): auth crea el usuario con rol `client` y llama a clients para crear su ficha con saldo 0.
2. **Login** (`POST sales-auth /api/v1/auth/login`): devuelve un JWT de 15 minutos con `sub` (username), `userId` y `groups` (roles).
3. **Recarga de saldo** (`POST sales-clients /api/v1/client/{id}/balance/credit`): solo admin.
4. **Compra** (`POST sales-orders /api/v1/order`): orders valida el cliente, consulta **todos los productos en una sola llamada**, calcula los totales, guarda la orden en `PENDING`, la publica en Kafka (topic `order-requests`) y responde **202**. Si un producto no existe o el ID es inválido, responde el error en ese momento.
5. **Cobro** (en segundo plano): `sales-order-processor` consume la orden y le debita el total al cliente en `sales-clients`, enviando el `id` de la orden como **llave de idempotencia**. Publica el resultado (`COMPLETED` o `REJECTED` con su motivo) en el topic `order-results`.
6. **Resultado:** orders consume el resultado y actualiza el estado de la orden.
7. **Consulta** (`GET sales-orders /api/v1/order/{id}` o `/order/me`): el cliente ve el estado de sus órdenes.

---

## Seguridad

- **JWT RS256.** Solo `sales-auth` tiene la clave privada y firma los tokens; todos los servicios los verifican con la clave pública.
- **Roles:** `admin`, `client`, y tres roles de servicio: `aService`, `orderService` y `paymentService`.
- **Tokens de servicio** (duran **1 minuto**):
  - Para crear la ficha de un cliente, auth se firma un token propio con el rol `aService`. Así nadie más puede crear fichas directamente en clients.
  - Para leer productos, orders le pide a auth un token con el rol `orderService` (`POST /api/v1/auth/service-token`).
  - Para cobrar, el procesador le pide un token con el rol `paymentService`. Lo necesita porque trabaja en segundo plano, sin el token de ningún usuario.
  - Auth solo entrega un token de servicio si quien lo pide **presenta el certificado de ese servicio** (`CN=sales-orders` o `CN=sales-order-processor`) y además envía su secreto, así el secreto no se puede adivinar a fuerza de intentos.
- **Propagación del token:** al recibir la orden, orders valida el cliente reenviando el token del usuario. Clients aplica sus propias reglas con los permisos de ese usuario, ni más ni menos.
- **Cada cliente solo accede a lo suyo:** clients y orders comparan el `userId` del token con el dueño del recurso.
- **Autenticación no proactiva** (`quarkus.http.auth.proactive=false`).
- **Rate limiting por IP** en los endpoints públicos: catálogo de productos, login y registro. Orders lee los productos por un endpoint interno por lotes (`GET /api/v1/product/internal?ids=...`, solo rol `orderService` + certificado), así el límite de los usuarios no frena la creación de órdenes.
- **Solo el admin y el procesador pueden debitar saldo.** El cliente ya no puede debitarse directamente: el saldo solo baja por una orden o por una acción del admin (principio de menor privilegio).
- **Login sin filtraciones:** usuario inexistente y contraseña incorrecta responden el mismo 401, para no revelar qué usuarios existen.
- **Contraseñas** guardadas con BCrypt; el hash nunca se devuelve en ninguna respuesta.

### TLS y mTLS

| Comunicación | Protección |
|---|---|
| Usuarios externos → servicios | **TLS**: solo necesitan confiar en `ca.crt` |
| Admin → servicios | **mTLS**: presenta `admin.crt` |
| Servicio → servicio | **mTLS**: cada servicio presenta su propio certificado |
| sales-orders y sales-order-processor ↔ Kafka | **mTLS obligatorio**: Kafka rechaza cualquier conexión sin un certificado firmado por la CA |

- Una **CA propia** firma un certificado por servicio y uno para el admin. El SAN de cada certificado incluye el nombre del servicio en Docker y `localhost`.
- Todo es **https** (`quarkus.http.insecure-requests=disabled`).
- Cada servicio **pide** certificado de cliente (`quarkus.http.ssl.client-auth=request`) y lo valida contra la CA: uno que no firmó la CA se rechaza en el handshake.
- Los REST clients usan la configuración TLS `internal`, así que siempre presentan el certificado del servicio que llama.
- **`MtlsGuardFilter`** (en los 4 servicios con API): si el token tiene un rol privilegiado (`admin`, `aService`, `orderService`, `paymentService`) y no se presentó certificado, responde **403**.
- `quarkus.smallrye-jwt.priority=2000`: el JWT se evalúa antes que el certificado, para que los roles salgan siempre del token.

**Por qué `request` + filtro y no `required`:** el mismo puerto atiende a usuarios externos (solo TLS) y a admins y servicios (mTLS). Con `required`, ningún usuario sin certificado podría ver el catálogo ni registrarse. En producción se separaría con un API gateway o un service mesh, y los servicios exigirían el certificado siempre.

**Kafka:** el listener interno (`kafka:29092`) usa SSL con `ssl.client.auth=required`. Aquí sí se exige siempre, porque solo los servicios se conectan a Kafka. El listener externo (`localhost:9092`) queda sin TLS, solo para el modo desarrollo.

**Fuera de alcance:** TLS hacia MongoDB. MongoDB exige usuario y contraseña, pero la conexión no va cifrada.

---

## Consistencia entre servicios (compensación)

Cada servicio tiene su propia base de datos, así que no hay transacciones entre ellos. Las operaciones que tocan dos servicios usan **compensación**: si el segundo paso falla, se deshace el primero.

| Operación | Paso 1 | Paso 2 | Si el paso 2 falla |
|---|---|---|---|
| Registro | Auth guarda el usuario | Clients crea la ficha | Auth borra el usuario y responde 503 |
| Compra | Orders guarda la orden (`PENDING`) | El procesador cobra en clients | El procesador publica `REJECTED` con el motivo (por ejemplo, saldo insuficiente) y orders actualiza la orden |

Si el cobro **no se sabe si se aplicó** (clients no respondió), no se rechaza la orden: el mensaje queda en la dead letter queue para reintentarlo, y la llave de idempotencia evita cobrar dos veces.

---

## Formato de errores

Todos los servicios responden los errores con el mismo formato:

```json
{
  "error_name": "NOT FOUND",
  "error_message": "Product with id: '6716f3a2c9e77a1b2c3d4e5f' was not found",
  "error_status_code": 404
}
```

Cuando un servicio llama a otro, traduce sus errores: por ejemplo, un 404 de products se convierte en "Product ... was not found", y si el otro servicio no responde, se devuelve **503**.

---

## Convenciones de la API

- Respuesta siempre `200` con datos → se devuelve el DTO directamente.
- Otro status o headers (`201` + `Location`, `204`) → `RestResponse<T>`.
- Errores → excepciones de negocio + `ExceptionMapper` central (`Response` crudo solo dentro de los mappers, vía `ErrorResponseBuilder`).
- Borrado lógico en todas las entidades (`status = false`); un recurso desactivado se comporta como inexistente (404).
- Campos de auditoría en todas las entidades: `createdBy`, `createdAt`, `updatedBy`, `updatedAt`.

---

## Órdenes con Kafka (pub-sub)

Las órdenes viajan por dos topics:

| Topic | Quién publica | Quién consume | Llave | Contenido |
|---|---|---|---|---|
| `order-requests` | `sales-orders` | `sales-order-processor` (grupo `orders-processor`, 3 consumidores) | `clientId` | `clientId`, `orderId` y el total |
| `order-results` | `sales-order-processor` | `sales-orders` (grupo `orders-results`) | `orderId` | `orderId`, estado y motivo del rechazo |

- **Llave `clientId`:** las órdenes de un mismo cliente van a la misma partición, y cada partición la lee un solo consumidor del grupo. Así se cobran en orden, y dos compras simultáneas no compiten por el mismo saldo.
- **3 consumidores** (`concurrency=3`): uno por partición.
- **Cobro idempotente:** el `orderId` viaja con el débito como llave de idempotencia. Clients guarda las órdenes que ya cobró y lo comprueba en la misma operación atómica que resta el saldo. Además, el procesador guarda un registro por orden (índice único en `orderId`). Un mensaje repetido nunca cobra dos veces.
- **Reintentos seguros:** gracias a la idempotencia, el débito ahora **sí se reintenta** (2 veces) si clients no responde.
- **Resultado idempotente en orders:** orders solo actualiza la orden si sigue en `PENDING`, en una sola operación. Un resultado repetido no cambia nada.
- **Dead letter queue:** recibe los mensajes cuyo cobro quedó incierto (clients no respondió tras los reintentos) y los resultados que orders no pudo guardar.

Detalle completo en los READMEs de [sales-orders](sales-orders/README.md) y [sales-order-processor](sales-order-processor/README.md).

---

## Fuera de alcance

Decisiones tomadas conscientemente para el tiempo de la prueba:

- **Inventario (stock):** los productos no manejan existencias.
- **Más estados de la orden** (pagada, enviada, cancelada)
- **reembolsos:** desactivar una orden no devuelve el saldo.

## Pruebas

- **Unitarias** (JUnit + Mockito, sin Quarkus ni infraestructura), enfocadas en la lógica crítica: creación de órdenes, cobro e idempotencia del procesador, débito (saldo insuficiente, balance real devuelto por la base y conflicto por cambios concurrentes), borrado lógico de productos y el token de servicio. Se corren con `./mvnw test` en cada servicio.
- **De punta a punta** sobre Docker (`qa-mejoras.ps1`): TLS/mTLS (con y sin certificado, certificado falso), token de servicio, rate limit, consulta de productos por lotes, débito con **20 operaciones simultáneas** sobre el mismo cliente, idempotencia del débito, flujo completo de órdenes y Kafka con mTLS.

## Limitaciones conocidas

- **Dead letter queue sin consumidor:** los mensajes que llegan ahí se reprocesan a mano. La llave de idempotencia hace que reprocesarlos sea seguro.
- **El registro de órdenes cobradas crece:** cada ficha guarda los `orderId` que ya se cobraron; en producción se limpiaría o se movería a una colección aparte.
- **Registro de usuario:** si falla la compensación (borrar el usuario cuando no se pudo crear la ficha), puede quedar un usuario sin ficha. La mejora sería no borrar, dejar el usuario "pendiente de ficha", hacer idempotente la creación por `userId` y completar los pendientes con un proceso de fondo.
- **Kafka acepta cualquier certificado de la CA:** cualquier servicio con un certificado válido podría publicar en los topics. La mejora sería activar ACLs para que solo orders y el procesador usen cada topic.
- **Rate limit por IP detrás de un proxy:** si todas las peticiones llegan desde la misma IP (un balanceador, la red de Docker), el límite se vuelve global. En producción iría en el gateway o leería `X-Forwarded-For`.
- **Usuario y ficha independientes:** desactivar un usuario no desactiva su ficha de cliente, y al revés.
- **Los cambios de rol o la desactivación** aplican cuando vence el token (15 minutos).

---

## Mejoras post-entrega

Cambios de esta rama respecto a la entrega original (`main`):

| Cambio | Qué resuelve |
|---|---|
| **Débito y crédito atómicos** (`findOneAndUpdate` con `$inc` y la condición de saldo en la misma operación) | La **actualización perdida**: un crédito del admin y un débito simultáneos ya no se pisan, y el balance devuelto es el real |
| Editar o desactivar una ficha solo cambia sus propios campos (`$set`) | Editar la ficha ya no puede pisar el balance |
| Conflicto por cambios concurrentes → **409** | Tras 3 intentos fallidos se responde un error claro, con la certeza de que no se cobró |
| **Consulta de productos por lotes** | El problema N+1: de N llamadas y N consultas a **una** de cada una |
| Validación de productos al **crear** la orden | El cliente se entera al instante si un producto no existe, y el mensaje nombra el ID |
| No se permiten productos repetidos en una orden | Datos de entrada consistentes |
| Solo el admin y el procesador debitan saldo | Menor privilegio: el saldo solo baja por una orden |
| **`sales-order-processor`** | El cobro sale de orders: la API y el cobro escalan, fallan y se despliegan por separado |
| **Llave de idempotencia** en el débito (`orderId`) | Un mensaje repetido, un reintento o un reprocesamiento desde la DLQ nunca cobran dos veces. Resuelve el **cobro ambiguo** por timeout de la entrega |
| 3 consumidores para las 3 particiones | Más rendimiento sin perder el orden por cliente |
