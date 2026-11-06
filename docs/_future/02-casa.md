Aquí tienes el análisis exhaustivo centrado única y exclusivamente en el Backend (apps/backend/): qué áreas de mejora existen, qué vulnerabilidades o inconsistencias convendría pulir y qué funcionalidades
  faltan para que el módulo de autenticación quede 10/10.
  ──────
  ## 1. Seguridad y Hardening Criptográfico 

  ### A. Hashing de Tokens de Reseteo y Verificación en Base de Datos	✅
  • Situación actual: En Account.java, los tokens de recuperación de contraseña (password_reset_token) y de verificación de email (email_verification_token) se generan como un UUID aleatorio y se guardan en
  texto claro en la tabla accounts.
  • Riesgo (OWASP): A diferencia de los refresh_token (que sí se guardan con hash SHA-256 en user_sessions), si la base de datos sufriera un volcado o fuga no autorizada, cualquier atacante podría usar esos
  tokens para secuestrar cuentas cambiando sus contraseñas de inmediato.
  • Solución:
      1. Generar el token en claro (e.g. 32 bytes en Base64Url o UUID criptográfico).
      2. Guardar en PostgreSQL únicamente su hash SHA-256 indexado (tokenProvider.hashToken(token)).
      3. Enviar el valor en claro únicamente en el email/evento al usuario.
      4. En /verify-email y /reset-password, aplicar SHA-256 al token recibido y buscar en DB por el hash.

  ──────
  ### B. Revocación Inmediata de JWTs en Vuelo (Token Blacklist L1/L2)	
  • Situación actual: El JWT de acceso dura 15 minutos (900 s) y es puramente stateless. Si un usuario hace /logout, cambia su contraseña o revoca un dispositivo desde /sessions/{id}, la sesión se marca
  como revocada en base de datos (isRevoked = true) y se borran las cookies locales. Sin embargo, si un atacante ya había interceptado el token JWT, éste sigue siendo válido durante los 15 minutos restantes
  porque Quarkus/SmallRye solo verifica la firma criptográfica RS256.
  • Solución (aprovechando la arquitectura ya existente):
      • El proyecto ya cuenta con el puerto CachePort coordinado entre L1 (Caffeine en memoria) y L2 (Redis distribuido).
      • Al revocar una sesión en AuthService, registrar una clave efímera:
        cachePort.put("revoked_session:" + sessionId, Boolean.TRUE, Duration.ofSeconds(900), Duration.ofSeconds(900));

      • Añadir un filtro HTTP o SecurityIdentityAugmentor ligero en Quarkus que consulte cachePort.exists("revoked_session:" + sessionId). Al resolverse en L1 (Caffeine RAM), la comprobación toma menos de
      10 microsegundos y no sobrecarga PostgreSQL, logrando invalidación instantánea en tiempo real.
  ──────
  ### C. Inconsistencia en la Longitud Máxima de Contraseñas

  • Situación actual:
      • En RegisterRequest.java: @Size(min = 8, max = 128).
      • En ClaimGuestRequest.java: @Size(min = 8, max = 128).
      • En ResetPasswordRequest.java: @Size(min = 8, max = 72).
      • En ChangePasswordRequest.java: @Size(min = 8, max = 72).
  • Problema: El límite de 72 caracteres proviene históricamente de las limitaciones de bcrypt. Pero el backend utiliza Argon2id, que no tiene esa restricción. Si un usuario utiliza un gestor de contraseñas
  (1Password, Bitwarden) y genera una frase de 80 caracteres en el registro, quedará bloqueado sin poder cambiar ni resetear su contraseña.
  • Solución: Unificar el límite a max = 128 en todos los DTOs de contraseñas.
  ──────
  ## 2. Compatibilidad del Transporte de Cookies con WebSockets

  ### Autenticación en GameRoomWebSocketEndpoint

  • Situación actual: En GameRoomWebSocketEndpoint.java:100-130, el endpoint de WebSocket arranca en estado desautenticado y espera recibir un mensaje JSON en texto:
    { "action": "AUTH", "token": "eyJhbGciOi..." }
  • Conflicto con la especificación de Cookies:
      • Según la especificación técnica implementada (06-auth-cookie-transport.md), en clientes web el access_token se transporta en una cookie marcada como HttpOnly.
      • Como JavaScript no tiene acceso a document.cookie, el cliente web no puede extraer el token para enviarlo dentro del JSON.
  • Solución recomendada:
      1. Handshake directo por Cookie en Quarkus WebSockets Next: Durante el evento @OnOpen, inspeccionar las cookies HTTP del upgrade request (el navegador envía automáticamente las cookies del dominio al
      abrir la conexión WebSocket). Si la cookie access_token está presente y es válida, marcar la conexión como autenticada desde el primer instante.
      2. Fallback por Ticket Efímero (WS-Ticket): Para clientes nativos o escenarios donde no se usen cookies, habilitar un endpoint POST /api/v1/auth/ws-ticket (autenticado) que devuelva un ticket de un
      solo uso con 30 segundos de vida para canjear en la conexión.

    ┌───────────────────────────┐┌──────────────────────┐┌─────────┐
    │ GameRoomWebSocketEndpoint ││ JWTParser / SmallRye ││ Cliente │
    └───────────────────────────┘└──────────────────────┘└─────────┘
                  │                          │                │
                  ◄GET /ws/rooms/{roomId} [Cookie: access_token=JWT]
                  │                          │                │
                  │parse(cookie.getValue())──►                │
   ┤ alt Token válido ├
                  │                          │                │
                  │┈┈┈┈┈┈┈HTTP 101 Switching Protocols┈┈┈┈┈┈┈┈►
                  │                          │                │
                  │┈┈┈Frame: {"event":"AUTH_SUCCESS", ...}┈┈┈┈►
                  │                          │                │
                  │Espera frame AUTH (fallback móvil) o descon►cta (44
                  │                          │                │
  ──────
  ## 3. Funcionalidades Faltantes para Completar el Ciclo de Vida

  ### A. Gestión de Identidades Sociales (OAuth Link, Unlink y List)

  Actualmente solo disponemos de login (oauthLogin) y reclamo desde invitado (claimGuestWithSocial). Falta permitir que un usuario registrado gestione sus métodos de acceso:

  1. Vincular proveedor (POST /api/v1/auth/identities/{provider}):
      • Permite a un usuario autenticado asociar su cuenta de Google o Discord para poder iniciar sesión indistintamente con email/password o con su cuenta social.
  2. Desvincular proveedor (DELETE /api/v1/auth/identities/{provider}):
      • Regla de negocio crítica: Antes de eliminar la identidad, comprobar que el usuario tenga una contraseña configurada (passwordHash != null) o al menos otro proveedor vinculado. Si es su único método
      de acceso, rechazar con error para evitar que la cuenta quede huérfana e inaccesible.
  3. Listar identidades (GET /api/v1/auth/identities):
      • Devuelve la lista de proveedores conectados a la cuenta (e.g. [{"provider": "GOOGLE", "email": "..."}, {"provider": "DISCORD", "email": "..."}]).
  ──────
  ### B. Derecho al Olvido y Baja de Cuenta (GDPR / Privacidad)

  • Situación actual: En Account.java ya existe el método de dominio softDelete(), pero no hay ningún caso de uso ni endpoint expuesto.
  • Qué añadir:
      • Endpoint DELETE /api/v1/auth/me (requiriendo confirmación de contraseña en el body como medida de seguridad).
      • El caso de uso debe:
          1. Marcar deleted_at = Instant.now().
          2. Revocar inmediatamente todas sus sesiones activas en user_sessions.
          3. Registrar la sesión en la lista negra de la caché.
          4. Emitir cookies con Max-Age=0 para desloguear al usuario.
          5. Opcional: anonimizar el email (deleted-<uuid>@ordago.internal) para permitir que el usuario vuelva a registrarse en el futuro si lo desea.

  ──────
  ### C. Doble Factor de Autenticación (2FA / TOTP)

  • Situación actual: La tabla accounts y la clase AccountEntity ya tienen reservadas las columnas two_factor_enabled y two_factor_secret, pero no hay ninguna lógica implementada.
  • Para completarlo al 100%:
      1. Algoritmo TOTP (RFC 6238): Usar HMAC-SHA1 estándar (e.g. biblioteca ligera o nativa) para generar secretos Base32 y validar códigos de 6 dígitos con ventana de deriva (±30s).
      2. Endpoints necesarios:
          • POST /api/v1/auth/2fa/setup: Genera un secreto temporal y una URL otpauth://totp/Ordago:<email>?secret=....
          • POST /api/v1/auth/2fa/enable: Recibe el código de 6 dígitos; si coincide, persiste two_factor_enabled = true y genera 8-10 códigos de recuperación (scratch codes) de un solo uso en formato hash.
          • POST /api/v1/auth/2fa/disable: Desactiva 2FA previa validación de contraseña y código actual.
      3. Ajuste en AuthService.login:
          • Si la cuenta tiene 2FA activado, la verificación de contraseña devuelve un estado intermedio 2FA_REQUIRED con un token efímero de sesión (TTL 3 min), sin emitir los JWT definitivos ni las
          cookies finales hasta llamar a POST /api/v1/auth/login/2fa.


  ──────
  ## 4. Resiliencia de Infraestructura y Red

  ### A. Detección Perimetral de IP (ClientIpResolver)

  • En ClientIpResolver.java:20-37, se inspeccionan X-Forwarded-For y X-Real-IP.
  • Mejora: Añadir soporte prioritario explícito para las cabeceras estándar de CDN/Proxy:
      1. CF-Connecting-IP (Cloudflare).
      2. True-Client-IP.
  • Esto actúa como defensa en profundidad si el tráfico ingresa directo sin pasar por la reescritura del Caddyfile local, evitando que todos los clientes compartan el mismo cupo de Rate Limiting.
  ──────
  ### B. Fiabilidad en el Envío de Emails (Dead Letter Exchange en RabbitMQ)

  • RabbitMqEmailEventPublisherAdapter publica eventos a RabbitMQ para verificación de correo y reseteo de contraseña.
  • Mejora: Configurar en RabbitMQ un Dead Letter Exchange (ordago.events.dlx) con reintentos para que, si el microservicio de correos está temporalmente caído o saturado, los correos críticos de
  verificación y cambio de clave no se descarten silenciosamente.
  ──────
  ## 5. Checklist de Implementación Backend (Orden de Prioridad)

                   #                  │ Tarea Backend                                                                                │             Criticidad             │         Esfuerzo Estimado
  ────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────┼───────────────────────────────────
                   1                  │ Unificar longitud de contraseñas (max = 128 en ResetPasswordRequest y ChangePasswordRequest) │            🔴 Inmediato            │              15 min							✅
                   2                  │ Hashing SHA-256 de tokens de verificación de email y reseteo de contraseña en DB             │              🔴 Alta               │             1-2 horas						
                   3                  │ Handshake WebSocket con Cookies / Ticket (GameRoomWebSocketEndpoint)                         │              🔴 Alta               │              2 horas						
                   4                  │ Token Blacklist L1/L2 con CachePort para invalidación inmediata de JWTs revocados            │           🟡 Media-Alta            │             2-3 horas						
                   5                  │ Gestión Social (Link / Unlink / List) de cuentas OAuth2 para usuarios registrados            │              🟡 Media              │             2-3 horas						
                   6                  │ Baja de cuenta y derecho al olvido (DELETE /me)                                              │              🟡 Media              │             1-2 horas						
                   7                  │ Soporte CF-Connecting-IP en ClientIpResolver                                                 │              🟢 Baja               │              20 min							
                   8                  │ Módulo 2FA / TOTP (RFC 6238) con códigos de respaldo                                         │          🟢 Mejora Mayor           │             4-6 horas						
