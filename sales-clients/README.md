# sales-clients

Microservicio de **clientes** de Benito Ventas. Guarda la ficha de cada cliente (sus datos de negocio y su saldo), permite que el cliente consulte y edite la suya, y que el administrador gestione todas.

---

## Arquitectura interna

```
resource/    → Endpoints HTTP
service/     → Lógica de negocio (incluye el chequeo de acceso y el saldo)
repository/  → Acceso a MongoDB (Panache)
entity/      → Documento de Mongo (Client)
dto/         → Objetos de entrada y salida
exception/   → Excepciones de negocio
mapper/
  exceptionMapper/ → Convierten excepciones en respuestas HTTP
helper/      → ErrorResponseBuilder
```

```
Usuario / sales-auth / sales-orders → ClientResource → ClientService → ClientRepository → MongoDB (clientdb)
```

### La ficha del cliente

| Campo | Descripción |
|---|---|
| `id` | Id de la ficha |
| `userId` | Id del usuario dueño de la ficha, en `sales-auth` (referencia externa, `String`) |
| `name`, `lastName`, `phone`, `address` | Datos del cliente |
| `balance` | Saldo disponible para comprar. Empieza en `0` |
| `status`, `createdBy`, `createdAt`, `updatedBy`, `updatedAt` | Borrado lógico y auditoría |

`username` y `email` no se guardan aquí: pertenecen al usuario, en `sales-auth`.

---

## Configuración

| Propiedad | Valor / descripción |
|---|---|
| `quarkus.http.port` | `8081` |
| `quarkus.http.auth.proactive` | `false` |
| `quarkus.mongodb.database` | `clientdb` |
| `mp.jwt.verify.issuer` | `https://benito-ventas.app` |
| `mp.jwt.verify.publickey.location` | En desarrollo: `src/main/resources/publicKey.pem`. En Docker: `/run/secrets/jwt_public_key` |

---

## Endpoints

Ruta base: `/api/v1/client`

| Método | Ruta | Acceso | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/api/v1/client` | `admin` | Lista los clientes activos, paginados, por nombre y apellido | `200` |
| `GET` | `/api/v1/client/{id}` | `admin`, `client` (solo la suya) | Obtiene una ficha | `200` |
| `POST` | `/api/v1/client` | `aService` (solo `sales-auth`) | Crea la ficha de un usuario recién registrado | `201` + `Location` |
| `PUT` | `/api/v1/client/{id}` | `admin`, `client` (solo la suya) | Actualiza los datos editables | `200` |
| `DELETE` | `/api/v1/client/{id}` | `admin` | Desactiva una ficha | `204` |
| `POST` | `/api/v1/client/{id}/balance/debit` | `admin`, `client` (solo la suya) | Descuenta saldo (lo usa `sales-orders` al cobrar) | `200` |
| `POST` | `/api/v1/client/{id}/balance/credit` | `admin` | Recarga saldo | `200` |

### `POST /api/v1/client` (lo llama `sales-auth`)

```json
{
  "userId": "6ae17c914cdbb65e4a90a72a",
  "name": "Juan",
  "lastName": "Pérez",
  "email": "juan.perez@email.com"
}
```

| Campo | Regla |
|---|---|
| `userId` | Obligatorio |
| `name`, `lastName` | Obligatorios, máximo 50 caracteres |
| `email` | Obligatorio, formato válido, máximo 100 caracteres |
| `phone` | Opcional, máximo 20 caracteres |
| `address` | Opcional, máximo 200 caracteres |

### `PUT /api/v1/client/{id}`

```json
{
  "name": "Juan",
  "lastName": "Pérez",
  "phone": "809-555-1234",
  "address": "Calle Principal #10, Santo Domingo"
}
```

Mismas reglas que arriba para estos campos. No incluye `balance`: el saldo solo cambia con `debit` y `credit`.

### `debit` y `credit`

```json
{ "amount": 99.99 }
```

`amount` es obligatorio y mayor que 0.

### Respuesta (`ClientResponseDTO`)

```json
{
  "id": "6ae17c9146fe9c4fdff515bc",
  "userId": "6ae17c914cdbb65e4a90a72a",
  "name": "Juan",
  "lastName": "Pérez",
  "phone": "809-555-1234",
  "address": "Calle Principal #10, Santo Domingo",
  "balance": 100.00
}
```

---

## Errores

| Situación | Status |
|---|---|
| Id con formato inválido / datos inválidos | `400` |
| Sin token / token inválido | `401` |
| Rol no permitido, o un cliente intenta acceder a una ficha ajena | `403` |
| Ficha inexistente o desactivada | `404` |
| Saldo insuficiente al descontar | `422` |
| Error inesperado | `500` |

---

## Decisiones de diseño

- **Solo `sales-auth` crea fichas**, usando un token de servicio con rol `aService`. Así toda ficha tiene un usuario detrás.
- **El chequeo de "solo lo suyo" está en el service**: compara el `userId` guardado en la ficha con el claim `userId` del token. El resource no puede hacerlo porque solo tiene el id de la ruta, que es el de la ficha, no el del usuario.
- **El saldo tiene endpoints propios** (`debit` y `credit`) en lugar de editarse con el `PUT`. Así un cliente no puede asignarse saldo a sí mismo, y solo el admin puede recargarlo.
- **Saldo insuficiente → 422**: la petición es correcta, pero rompe una regla de negocio.
- **Un DTO por operación**: `ClientRequestDTO` para crear y `ClientUpdateRequestDTO` para editar.

## Limitación a tomar en cuenta

- Dos cobros simultáneos al mismo cliente podrían leer el mismo saldo antes de que alguno lo actualice (pendiente de arreglar).

