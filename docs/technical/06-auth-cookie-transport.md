# 06. Technical Specification — Authentication Cookie Transport & Dual-Mode Fallback

**Módulo:** Seguridad y Autenticación (`apps/backend/` + `apps/frontend/`)  
**Enfoque:** Arquitectura Dual de Transporte de Tokens: Cookies Seguras (`HttpOnly`, `SameSite=Strict`, `Secure`) para Clientes Web / SPA + Fallback en Body / Header para Dispositivos Móviles y Clientes Externos.

---

## 1. Visión General y Objetivos

Para proteger la plataforma frente a vulnerabilidades críticas de cliente como **Cross-Site Scripting (XSS)** y **Cross-Site Request Forgery (CSRF)**, el sistema evoluciona de un modelo donde los tokens residían exclusivamente en el payload JSON a un **modelo dual de transporte**:

1. **Clientes Web / SPA (Navegadores):**
   - Los tokens no son accesibles mediante JavaScript (`document.cookie`), mitigando la exfiltración masiva de credenciales ante inyecciones XSS.
   - Las cookies de sesión se marcan con `SameSite=Strict`, garantizando que el navegador nunca las adjunte en peticiones originadas desde sitios externos (protección nativa contra CSRF).
   - En producción, el flag `Secure` asegura que las cookies solo se transmitan sobre canales cifrados TLS (`https://`).
2. **Dispositivos Móviles y Clientes API (No Navegadores):**
   - Las respuestas de autenticación (`/login`, `/register`, `/refresh`) continúan incluyendo los tokens en el cuerpo de la respuesta (`AuthResponse`, `TokenResponse`).
   - Los clientes nativos o APIs externas pueden seguir utilizando la cabecera estándar `Authorization: Bearer <token>` y enviar el `refreshToken` en el cuerpo JSON de forma transparente.

---

## 2. Especificación de Cookies

| Cookie | Ámbito (`Path`) | Tiempo de Vida (`Max-Age`) | Flags | Propósito |
| :--- | :--- | :--- | :--- | :--- |
| `access_token` | `/` | 900 segundos (15 min) | `HttpOnly; SameSite=Strict; Secure` | Autorización de peticiones REST y WebSockets en toda la plataforma. |
| `refresh_token` | `/api/v1/auth` | 2.592.000 segundos (30 días) | `HttpOnly; SameSite=Strict; Secure` | Rotación de credenciales. Su path restringido evita que se envíe en llamadas a `/api/v1/profile`, `/ws/*`, etc. |

> [!NOTE]
> En entornos de desarrollo y pruebas (`dev`, `test`), `ordago.auth.cookie.secure` se establece en `false` para permitir pruebas sobre `http://localhost`. En el perfil de producción (`%prod`), se fuerza `true`.

---

## 3. Diagramas de Flujo y Secuencia

### A. Autenticación (`/login` o `/register`)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Navegador Web / Móvil
    participant AuthAPI as AuthResource
    participant Service as AuthService
    participant CookieSvc as AuthCookieService

    Cliente->>AuthAPI: POST /api/v1/auth/login (email, password)
    AuthAPI->>Service: login(command)
    Service-->>AuthAPI: AuthResult(tokens, user, session)
    
    AuthAPI->>CookieSvc: createAccessTokenCookie(accessToken, expiresIn)
    AuthAPI->>CookieSvc: createRefreshTokenCookie(refreshToken, duration)
    
    AuthAPI-->>Cliente: HTTP 200 OK<br/>Set-Cookie: access_token=...; HttpOnly; SameSite=Strict<br/>Set-Cookie: refresh_token=...; Path=/api/v1/auth; HttpOnly; SameSite=Strict<br/>Body: { accessToken, refreshToken, user, ... }
    Note over Cliente: Navegador: almacena cookies HttpOnly.<br/>Móvil: extrae tokens del cuerpo.
```

---

### B. Petición Autenticada con Fallback (`/me`, endpoints protegidos)

SmallRye JWT está configurado para inspeccionar en primer lugar la cookie `access_token`. Si la cookie no está presente, se activa el fallback a la cabecera `Authorization: Bearer <token>`:

```mermaid
sequenceDiagram
    autonumber
    actor Web as Navegador Web
    actor Mobile as Dispositivo Móvil
    participant SmallRye as SmallRye JWT Filter
    participant Resource as AuthResource / Protected API

    rect rgb(240, 248, 255)
    Note over Web, SmallRye: Flujo Web (Cookie Primaria)
    Web->>SmallRye: GET /api/v1/auth/me [Cookie: access_token=JWT]
    SmallRye->>SmallRye: Extrae JWT desde Cookie
    SmallRye->>Resource: Invoca con SecurityIdentity
    Resource-->>Web: HTTP 200 OK (UserInfoResponse)
    end

    rect rgb(255, 250, 240)
    Note over Mobile, SmallRye: Flujo Móvil (Fallback a Authorization Header)
    Mobile->>SmallRye: GET /api/v1/auth/me [Authorization: Bearer JWT]
    SmallRye->>SmallRye: Cookie ausente -> Fallback a Header Bearer
    SmallRye->>Resource: Invoca con SecurityIdentity
    Resource-->>Mobile: HTTP 200 OK (UserInfoResponse)
    end
```

---

### C. Rotación de Token (`/refresh`) y Cierre de Sesión (`/logout`)

- **`/refresh`:**
  - Extrae el token de refresco evaluando prioritariamente la cookie `@CookieParam("refresh_token")`.
  - Si la cookie no existe, utiliza `request.refreshToken()` del cuerpo JSON.
  - Al rotar el token, emite nuevas cookies `Set-Cookie` actualizadas y devuelve `TokenResponse` en el body.
- **`/logout` y `/change-password`:**
  - Requieren autenticación obligatoria (`@Authenticated` y `@SecurityRequirement(name = "jwtAuth")`), ya sea por cookie `access_token` o header `Authorization: Bearer <token>`.
  - En `/logout`, se extrae el claim `sessionId` del JWT y se invalida la sesión activa en base de datos (`USER_LOGOUT`). Opcionalmente admite `refreshToken` en cookie o body para invalidación redundante.
  - Emite cookies con `Max-Age=0` y valor vacío para ordenar al navegador su destrucción inmediata (`access_token` en `/` y `refresh_token` en `/api/v1/auth`).

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente Web / Móvil
    participant AuthAPI as AuthResource
    participant Service as AuthService
    participant CookieSvc as AuthCookieService

    Cliente->>AuthAPI: POST /api/v1/auth/logout [Bearer JWT o access_token Cookie]
    Note over AuthAPI: Extrae sessionId del JWT (@Authenticated)
    AuthAPI->>Service: logout(sessionId, refreshToken)
    AuthAPI->>CookieSvc: clearAccessTokenCookie()
    AuthAPI->>CookieSvc: clearRefreshTokenCookie()
    AuthAPI-->>Cliente: HTTP 204 No Content<br/>Set-Cookie: access_token=; Max-Age=0; Path=/<br/>Set-Cookie: refresh_token=; Max-Age=0; Path=/api/v1/auth
```

---

## 4. Configuración en Quarkus (`application.properties`)

```properties
# SmallRye JWT: Extracción primaria por Cookie con Fallback a Header
mp.jwt.token.header=Cookie
mp.jwt.token.cookie=access_token
smallrye.jwt.always-check-authorization=true

# Configuración de Seguridad de Cookies de Órdago
ordago.auth.cookie.secure=false
%prod.ordago.auth.cookie.secure=true
ordago.auth.cookie.same-site=STRICT

# Habilitación de Cookies en Cross-Origin (CORS)
quarkus.http.cors=true
quarkus.http.cors.origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
quarkus.http.cors.headers=accept, authorization, content-type, x-requested-with, accept-language
quarkus.http.cors.methods=GET, POST, PUT, DELETE, OPTIONS
quarkus.http.cors.access-control-allow-credentials=true
```

---

## 5. Integración en el Cliente Frontend (`apps/frontend/`)

El cliente HTTP (`httpClient.ts`) invoca la API nativa `fetch` con `credentials: 'include'` por defecto:

```typescript
// apps/frontend/src/core/infrastructure/api/httpClient.ts
response = await fetch(url, {
  credentials: restOptions.credentials ?? 'include',
  ...restOptions,
  body: requestBody,
  headers,
})
```

Esto garantiza que:
1. El navegador envíe automáticamente las cookies en peticiones tanto de mismo origen como cruzadas entre el servidor de desarrollo (`http://localhost:5173`) y el backend (`http://localhost:8080`).
2. Las cabeceras `Set-Cookie` sean procesadas y almacenadas de forma segura por el motor del navegador.

---

## 6. Estructura y Enriquecimiento de Claims JWT (Access Token)

Los tokens de acceso JWT emitidos por `SmallRyeJwtTokenProvider` incorporan claims estándar (RFC 7519 / MicroProfile JWT) y claims enriquecidos para contextualizar la sesión sin necesidad de consultas recurrentes a la base de datos o servicios de perfil en cada petición.

### A. Especificación de Claims

| Claim | Tipo | Origen | Propósito / Descripción |
| :--- | :--- | :--- | :--- |
| `iss` | `String` | Configuración (`mp.jwt.verify.issuer`) | Emisor del token (por defecto `https://ordago.dev`). |
| `sub` | `String` (UUID) | `Account.getId()` | Identificador universal único de la cuenta. |
| `upn` | `String` | `Account.getEmail()` | User Principal Name para compatibilidad MicroProfile JWT y logging. |
| `groups` | `Array<String>` | `Account.getRoles()` | Conjunto de roles (`["USER"]`, `["ADMIN", "USER"]`) para autorización RBAC (`@RolesAllowed`). |
| `iat` | `Long` (segundos) | `Instant.now()` | Timestamp epoch de emisión del token. |
| `exp` | `Long` (segundos) | `Instant.now() + 900s` | Timestamp epoch de expiración (15 minutos). |
| `jti` | `String` (UUID) | `UUID.randomUUID()` | Identificador único criptográfico de token. Previene ataques de replay y facilita trazabilidad en auditoría. |
| `email` | `String` | `Account.getEmail()` | Correo electrónico del usuario autenticado. |
| `sessionId` | `String` (UUID) | `UserSession.getId()` | Identificador de la sesión en base de datos (`user_sessions`), permitiendo invalidación individual. |
| `deviceId` | `String` | `UserSession.getDeviceId()` | Identificador del dispositivo del cliente (web, iOS, Android). |
| `isAdmin` | `Boolean` | `Account.hasRole("ADMIN")` | Flag de conveniencia para consumo rápido en frontend y endpoints. |
| `emailVerified` | `Boolean` | `Account.isEmailVerified()` | Indica si el usuario ha confirmado su dirección de email. Permite gating condicional de funcionalidades (partidas clasificatorias, chat). |
| `playerTag` | `String` | `UserProfile.getPlayerTag().formatted()` | Etiqueta pública de jugador formateada (`GameName#TAG`, ej. `ITMusMaster#ESP`). Si el perfil no existe, el claim se omite limpiamente. |

### B. Ejemplo de Payload Decodificado

```json
{
  "iss": "https://ordago.dev",
  "sub": "b2c0cb35-b217-48f6-be11-37beeb76e5d2",
  "upn": "jugador@ordago.dev",
  "groups": [
    "USER"
  ],
  "iat": 1793719200,
  "exp": 1793720100,
  "jti": "d4e5f6a7-8901-4bc3-a4d5-e6f7a8b9c0d1",
  "email": "jugador@ordago.dev",
  "sessionId": "f8a7e6d5-c4b3-4a21-9e87-654321fedcba",
  "deviceId": "web-browser-chrome-linux",
  "isAdmin": false,
  "emailVerified": true,
  "playerTag": "ITMusMaster#ESP"
}
```

### C. Presupuesto de Tamaño y Límites de Cabecera

- **Payload JSON sin comprimir:** ~380 a 460 bytes.
- **Token firmado completo (RS256 Base64Url):** ~900 a 1.250 bytes.
- **Compatibilidad con estándares:**
  - Límite de cookies de navegadores (RFC 6265): **4.096 bytes** (el token consume ~25% del límite).
  - Límite de cabecera HTTP de Vert.x / Quarkus (`quarkus.http.limits.max-header-size`): **8.192 bytes** por defecto.
- **Rendimiento:** Al contener `playerTag` y `emailVerified` directamente en el token verificado criptográficamente por la clave pública RS256, los endpoints y el WebSocket evitan lecturas de perfil o cuenta en caché/base de datos durante la negociación inicial y peticiones REST concurrentes.
