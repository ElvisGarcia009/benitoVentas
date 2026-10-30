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

El sistema está dividido en **cuatro servicios por dominio** (qué se maneja). Lo que puede hacer cada usuario se controla con **roles en el JWT**, dentro de cada servicio.

```
                         ┌──────────────────┐
     login / registro    │   sales-auth     │  emite los JWT (clave privada)
   ─────────────────────▶│   usuarios       │──┐
                         └──────────────────┘  │ crea la ficha del cliente
                                  ▲            ▼ (token de servicio)
                   token de       │
                   servicio       │
┌──────────────────┐     ┌──────────────────┐   ┌──────────────────┐
│  sales-products  │◀────│   sales-orders   │──▶│  sales-clients   │
│  catálogo        │     │   órdenes        │   │  fichas y saldo  │
└──────────────────┘     └──────────────────┘   └──────────────────┘
                            │          ▲
                    publica │          │ consume
                            ▼          │
                         ┌────────────────┐
                         │     Kafka      │  topic order-requests
                         └────────────────┘

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

| Infraestructura | Puerto |
|---|---|
| MongoDB 7 | 27017 |
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
   for s in products clients orders auth admin; do
     openssl genrsa -out $s.key 2048
     openssl req -new -key $s.key -subj "/CN=sales-$s" -out $s.csr
     echo "subjectAltName=DNS:sales-$s,DNS:localhost" > $s.ext
     openssl x509 -req -in $s.csr -CA ca.crt -CAkey ca.key -CAcreateserial -days 365 -extfile $s.ext -out $s.crt
   done
   rm *.csr *.ext
   chmod 640 products.key clients.key orders.key auth.key'
   ```

   El `chmod 640` hace falta porque Quarkus corre en el contenedor con el usuario `185` (grupo `root`) y tiene que poder leer su llave.

3. Compilar cada servicio (el Dockerfile copia `target/quarkus-app`):

   ```bash
   for s in sales-products sales-clients sales-orders sales-auth; do (cd $s && ./mvnw package -DskipTests); done
   ```

4. Levantar todo:

   ```bash
   docker compose up --build -d
   ```

Las claves y certificados llegan a los contenedores como **Docker secrets** (`/run/secrets/...`). Dentro de Docker, los servicios se llaman entre sí por su nombre y por el puerto interno 8443 (`https://sales-clients:8443`); los puertos 908x son solo para acceder desde fuera, también por https.

Kafka tiene dos "puertas": `kafka:29092` para los contenedores y `localhost:9092` para lo que corre fuera de Docker (por ejemplo, un servicio en modo desarrollo).

> **Nota:** el secreto del servicio de órdenes (`ORDERS_SERVICE_SECRET`) y la contraseña del admin inicial (`USER_SEEDER_ADMINPASSWORD`) están escritos en `docker-compose.yml` como valores por defecto **solo porque es una prueba técnica**, para que el sistema arranque sin pasos extra. En producción irían en un archivo `.env` fuera del repositorio o en un gestor de secretos.

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

Ejemplo: `https://localhost:9082/q/health`.

---

## Flujo principal

1. **Registro** (`POST sales-auth /api/v1/auth/register`): auth crea el usuario con rol `client` y llama a clients para crear su ficha con saldo 0.
2. **Login** (`POST sales-auth /api/v1/auth/login`): devuelve un JWT de 15 minutos con `sub` (username), `userId` y `groups` (roles).
3. **Recarga de saldo** (`POST sales-clients /api/v1/client/{id}/balance/credit`): solo admin.
4. **Compra** (`POST sales-orders /api/v1/order`): orders valida el cliente, guarda la orden en `PENDING`, la publica en Kafka y responde **202**.
5. **Procesamiento** (en segundo plano): el consumidor de orders calcula los totales y cobra el saldo. La orden pasa a `COMPLETED` o a `REJECTED` con su motivo.
6. **Consulta** (`GET sales-orders /api/v1/order/{id}` o `/order/me`): el cliente ve el estado de sus órdenes.

---

## Seguridad

- **JWT RS256.** Solo `sales-auth` tiene la clave privada y firma los tokens; todos los servicios los verifican con la clave pública.
- **Roles:** `admin`, `client`, y dos roles de servicio: `aService` y `orderService`.
- **Tokens de servicio** (duran **1 minuto**):
  - Para crear la ficha de un cliente, auth se firma un token propio con el rol `aService`. Así nadie más puede crear fichas directamente en clients.
  - Para cobrar una orden, orders le pide a auth un token con el rol `orderService` (`POST /api/v1/auth/service-token`, con su nombre y su secreto). Hace falta porque el cobro ocurre en segundo plano, cuando el token del usuario puede haber vencido.
- **Propagación del token:** al recibir la orden, orders valida el cliente reenviando el token del usuario. Clients aplica sus propias reglas con los permisos de ese usuario, ni más ni menos.
- **Cada cliente solo accede a lo suyo:** clients y orders comparan el `userId` del token con el dueño del recurso.
- **Autenticación no proactiva** (`quarkus.http.auth.proactive=false`).
- **Rate limiting por IP** en los endpoints públicos: catálogo de productos, login y registro.
- **Login sin filtraciones:** usuario inexistente y contraseña incorrecta responden el mismo 401, para no revelar qué usuarios existen.
- **Contraseñas** guardadas con BCrypt; el hash nunca se devuelve en ninguna respuesta.

### TLS y mTLS

| Comunicación | Protección |
|---|---|
| Usuarios externos → servicios | **TLS**: solo necesitan confiar en `ca.crt` |
| Admin → servicios | **mTLS**: presenta `admin.crt` |
| Servicio → servicio | **mTLS**: cada servicio presenta su propio certificado |

- Una **CA propia** firma un certificado por servicio y uno para el admin. El SAN de cada certificado incluye el nombre del servicio en Docker y `localhost`.
- Todo es **https** (`quarkus.http.insecure-requests=disabled`).
- Cada servicio **pide** certificado de cliente (`quarkus.http.ssl.client-auth=request`) y lo valida contra la CA: uno que no firmó la CA se rechaza en el handshake.
- Los REST clients usan la configuración TLS `internal`, así que siempre presentan el certificado del servicio que llama.
- **`MtlsGuardFilter`** (en los 4 servicios): si el token tiene un rol privilegiado (`admin`, `aService`, `orderService`) y no se presentó certificado, responde **403**.
- `quarkus.smallrye-jwt.priority=2000`: el JWT se evalúa antes que el certificado, para que los roles salgan siempre del token.

**Por qué `request` + filtro y no `required`:** el mismo puerto atiende a usuarios externos (solo TLS) y a admins y servicios (mTLS). Con `required`, ningún usuario sin certificado podría ver el catálogo ni registrarse. En producción se separaría con un API gateway o un service mesh, y los servicios exigirían el certificado siempre.

**Fuera de alcance:** TLS hacia Kafka y MongoDB.

---

## Consistencia entre servicios (compensación)

Cada servicio tiene su propia base de datos, así que no hay transacciones entre ellos. Las operaciones que tocan dos servicios usan **compensación**: si el segundo paso falla, se deshace el primero.

| Operación | Paso 1 | Paso 2 | Si el paso 2 falla |
|---|---|---|---|
| Registro | Auth guarda el usuario | Clients crea la ficha | Auth borra el usuario y responde 503 |
| Compra | Orders guarda la orden (`PENDING`) | Clients cobra el saldo | La orden queda `REJECTED` con el motivo (por ejemplo, saldo insuficiente) |

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

Las órdenes de compra se encolan como mensajes en Kafka:

- **Productor:** `sales-orders` publica en el topic `order-requests` al recibir la orden.
- **Consumidor:** el mismo `sales-orders` (grupo `orders-processor`) lee el topic y procesa cada orden.
- **Mensaje:** llave = `clientId`, valor = `id` de la orden. Con la llave, las órdenes de un mismo cliente van a la misma partición y se procesan en orden, así dos compras simultáneas no leen el mismo saldo.
- **Consumidor idempotente:** solo procesa órdenes en `PENDING`; si un mensaje llega dos veces, no cobra dos veces.
- **Errores técnicos** (un servicio caído): la orden queda `REJECTED` y el mensaje va a una **dead letter queue**, en lugar de detener el consumidor.

Detalle completo en el [README de sales-orders](sales-orders/README.md).

---

## Fuera de alcance

Decisiones tomadas conscientemente para el tiempo de la prueba:

- **Inventario (stock):** los productos no manejan existencias.
- **Más estados de la orden** (pagada, enviada, cancelada) y **reembolsos:** desactivar una orden no devuelve el saldo.

## Pendientes

- [ ] `ConstraintViolationMapper` con status 400 en products, clients y auth (hoy 500).
- [ ] Mensajes en los 401/403 y mappers `UnauthorizedMapper` / `ForbiddenMapper` en auth.
- [ ] Pruebas Unitarias.
