# Benito Ventas (Aún en fase de desarrollo)

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
                                               ▼ (token de servicio)
┌──────────────────┐     ┌──────────────────┐
│  sales-products  │◀────│   sales-orders   │──▶┌──────────────────┐
│  catálogo        │     │   órdenes        │   │  sales-clients   │
└──────────────────┘     └──────────────────┘   │  fichas y saldo  │
                                                └──────────────────┘
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

2. Compilar cada servicio (el Dockerfile copia `target/quarkus-app`):

   ```bash
   for s in sales-products sales-clients sales-orders sales-auth; do (cd $s && ./mvnw package -DskipTests); done
   ```

3. Levantar todo:

   ```bash
   docker compose up --build -d
   ```

Las claves llegan a los contenedores como **Docker secrets** (`/run/secrets/...`). Dentro de Docker, los servicios se llaman entre sí por su nombre y por el puerto interno 8080 (`http://sales-clients:8080`); los puertos 908x son solo para acceder desde fuera.

### En modo desarrollo

Con MongoDB corriendo en `localhost:27017`, en cada servicio:

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

---

## Flujo principal

1. **Registro** (`POST sales-auth /api/v1/auth/register`): auth crea el usuario con rol `client` y llama a clients para crear su ficha con saldo 0.
2. **Login** (`POST sales-auth /api/v1/auth/login`): devuelve un JWT de 15 minutos con `sub` (username), `userId` y `groups` (roles).
3. **Recarga de saldo** (`POST sales-clients /api/v1/client/{id}/balance/credit`): solo admin.
4. **Compra** (`POST sales-orders /api/v1/order`): orders consulta el cliente y los productos, calcula los totales, guarda la orden y cobra el saldo.
5. **Consulta** (`GET sales-orders /api/v1/order/me`): el cliente ve sus órdenes.

---

## Seguridad

- **JWT RS256.** Solo `sales-auth` tiene la clave privada y firma los tokens; todos los servicios los verifican con la clave pública.
- **Roles:** `admin`, `client` y `aService` (token de servicio).
- **Token de servicio:** para crear la ficha de un cliente, auth se firma un token propio de **1 minuto** con el rol `aService`. Así nadie más puede crear fichas directamente en clients.
- **Propagación del token:** cuando orders llama a clients, reenvía el token del usuario. Clients aplica sus propias reglas con los permisos de ese usuario, ni más ni menos.
- **Cada cliente solo accede a lo suyo:** clients y orders comparan el `userId` del token con el dueño del recurso.
- **Autenticación no proactiva** (`quarkus.http.auth.proactive=false`): el token solo se valida en los endpoints que lo requieren.
- **Rate limiting por IP** en los endpoints públicos: catálogo de productos, login y registro.
- **Login sin filtraciones:** usuario inexistente y contraseña incorrecta responden el mismo 401, para no revelar qué usuarios existen.
- **Contraseñas** guardadas con BCrypt; el hash nunca se devuelve en ninguna respuesta.

---

## Consistencia entre servicios (compensación)

Cada servicio tiene su propia base de datos, así que no hay transacciones entre ellos. Las operaciones que tocan dos servicios usan **compensación**: si el segundo paso falla, se deshace el primero.

| Operación | Paso 1 | Paso 2 | Si el paso 2 falla |
|---|---|---|---|
| Registro | Auth guarda el usuario | Clients crea la ficha | Auth borra el usuario y responde 503 |
| Compra | Orders guarda la orden | Clients cobra el saldo | Orders borra la orden y responde el error (por ejemplo 422 por saldo insuficiente) |

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

## Fuera de alcance

Decisiones tomadas conscientemente para el tiempo de la prueba:

- **Inventario (stock):** los productos no manejan existencias.
- **Estados de la orden** (pagada, enviada, cancelada) y **reembolsos:** desactivar una orden no devuelve el saldo.

## Pendientes

- [ ] TLS hacia el exterior y mTLS entre servicios.
- [ ] `ConstraintViolationMapper` con status 400 en products, clients y auth (hoy 500).
- [ ] Mensajes en los 401/403 y mappers `UnauthorizedMapper` / `ForbiddenMapper` en auth.
- [ ] Implementación de Kafka luego de una orden de compra.
- [ ] Pruebas Unitarias.
