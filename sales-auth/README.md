# sales-auth

Microservicio de **identidad** de Benito Ventas. Registra usuarios, hace el login, emite los JWT que usan todos los servicios y permite que el administrador gestione los usuarios.

---

## Arquitectura interna

```
resource/
  AuthResource → Registro y login (públicos)
  UserResource → Gestión de usuarios (admin)
service/
  AuthService    → Registro y login
  UserService    → CRUD de usuarios
  TokenGenerator → Firma los JWT de usuario y de servicio
seed/        → AdminSeeder: crea el primer administrador al arrancar Quarkus
repository/  → Acceso a MongoDB (Panache)
entity/      → Documento de Mongo (User)
dto/         → Objetos de entrada y salida
  restClientDTO/ → Objetos para hablar con sales-clients
restClient/  → Cliente HTTP hacia sales-clients
exception/, mapper/exceptionMapper/, helper/ → Errores
```

### El usuario

| Campo | Descripción |
|---|---|
| `id` | Id del usuario; viaja en el claim `userId` del token |
| `username` | Único; viaja en el claim `sub`. No se puede modificar |
| `email` | Único. No se puede modificar |
| `passwordHash` | Contraseña con BCrypt. Nunca se devuelve |
| `roles` | `admin` y/o `client` |
| `status` + auditoría | Borrado lógico, `createdBy`, `createdAt`, `updatedBy`, `updatedAt` |

---

## Configuración

| Propiedad | Valor / descripción |
|---|---|
| `quarkus.http.port` | `8083` |
| `quarkus.http.auth.proactive` | `false` |
| `quarkus.mongodb.database` | `userdb` |
| `smallrye.jwt.sign.key.location` | Clave **privada**. En desarrollo: `../secrets/privateKey.pem`. En Docker: `/run/secrets/jwt_private_key` |
| `smallrye.jwt.new-token.issuer` / `mp.jwt.verify.issuer` | `https://benito-ventas.app` |
| `mp.jwt.verify.publickey.location` | Clave pública, para verificar los tokens que recibe |
| `quarkus.rest-client.client-api.url` | En desarrollo: `http://localhost:8081`. En Docker: `http://sales-clients:8080` |
| `user.seeder.adminPassword` | Contraseña del administrador inicial (en Docker: `USER_SEEDER_ADMINPASSWORD`) |
| `quarkus.rate-limiter.buckets.authentication.*` | 10 peticiones por segundo y 100 cada 5 minutos, por IP |

La clave privada **solo existe en este servicio** y no se versiona (`secrets/` está en `.gitignore`).

---

## Tokens

| Tipo | `sub` | Claims extra | Roles (`groups`) | Duración |
|---|---|---|---|---|
| Usuario | `username` | `userId` | Los del usuario | 15 minutos |
| Servicio | `AuthService` | — | `aService` | 1 minuto |

El token de servicio se usa solo para crear la ficha del cliente en `sales-clients` durante el registro.

---

## Endpoints

### Autenticación: `/api/v1/auth` (públicos, con rate limit)

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Registra un cliente: crea el usuario y su ficha | `204` |
| `POST` | `/api/v1/auth/login` | Devuelve un token de acceso | `200` |

**Registro:**

```json
{
  "username": "jperez",
  "name": "Juan",
  "lastName": "Pérez",
  "email": "juan.perez@email.com",
  "password": "MiClave123!"
}
```

| Campo | Regla |
|---|---|
| `username` | Obligatorio, máximo 30 caracteres, único |
| `name`, `lastName` | Obligatorios, máximo 50 caracteres |
| `email` | Obligatorio, formato válido, máximo 100 caracteres, único |
| `password` | 8 a 72 caracteres, con minúscula, mayúscula, número y símbolo |

**Login:**

```json
{ "username": "jperez", "password": "MiClave123!" }
```

```json
{ "access_token": "eyJ...", "token_type": "Bearer", "expires_in": 900 }
```

### Usuarios: `/api/v1/user`

| Método | Ruta | Acceso | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/api/v1/user` | `admin` | Lista los usuarios activos, paginados | `200` |
| `GET` | `/api/v1/user/{id}` | `admin`, `client` (solo el suyo) | Obtiene un usuario | `200` |
| `POST` | `/api/v1/user` | `admin` | Crea un usuario con roles (por ejemplo, otro admin). No crea ficha de cliente | `201` + `Location` |
| `PUT` | `/api/v1/user/{id}/roles` | `admin` | Cambia los roles de un usuario | `200` |
| `DELETE` | `/api/v1/user/{id}` | `admin` | Desactiva un usuario | `204` |

`roles` solo acepta `admin` y `client`. Un admin **no puede cambiar sus propios roles ni desactivarse a sí mismo**, para que el sistema nunca se quede sin administradores.

Respuesta (`UserResponseDTO`, sin el hash):

```json
{ "id": "...", "username": "jperez", "email": "juan.perez@email.com", "roles": ["client"] }
```

---

## Cómo funciona el registro

1. Revisa que `username` y `email` no estén en uso (409 si lo están).
2. Guarda el usuario con el rol `client` y la contraseña en BCrypt.
3. Se firma un token de servicio y llama a `POST sales-clients /api/v1/client` con los datos de la ficha.
4. Si clients falla o no responde, **borra el usuario** (compensación) y responde **503**.

---

## Errores

| Situación | Status |
|---|---|
| Datos inválidos / id inválido | `400` |
| Usuario o contraseña incorrectos (mismo mensaje en los dos casos) | `401` |
| Sin token / token inválido | `401` |
| Rol no permitido, o un admin intenta modificarse a sí mismo | `403` |
| Usuario inexistente | `404` |
| `username` o `email` en uso | `409` |
| Demasiadas peticiones | `429` |
| No se pudo crear la ficha en clients | `503` |

---

## Decisiones de diseño

- **Gestión de usuarios dentro de auth**: el login necesita el hash y los roles, así que el servicio dueño de esos datos también es dueño de su CRUD.
- **`username` y `email` inmutables**: son la identidad del usuario y viajan en el token.
- **El login no revela qué usuarios existen**: mismo 401 y mismo mensaje para usuario inexistente y contraseña incorrecta.
- **El primer admin se crea solo al arrancar** (`AdminSeeder`), para que el sistema sea usable sin pasos manuales.
- **Los cambios de rol aplican en el siguiente login**: el token vigente conserva los roles anteriores hasta vencer.
