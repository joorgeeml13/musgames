# 03. Technical Specification — WebSocket Protocol & Network Contract

**Módulo:** Comunicación en tiempo real (Backend WS <-> Frontend Services)  
**Endpoint:** `/ws/rooms/{roomId}` *(Sin token en query param)*

---

## 0. Protocolo de Handshake y Autenticación Segura

Por motivos de seguridad (evitar fuga de tokens JWT en logs de acceso de servidores, proxies inversos y URLs de navegador), la conexión WebSocket **no acepta tokens por Query Params** (`?token=...`).

### Flujo de Conexión y Autenticación:
1. **Conexión Abierta (`@OnOpen`):** Al establecer la conexión a `/ws/rooms/{roomId}`, el backend marca la sesión en `userData` como no autenticada (`ws.authenticated = false`) y arranca un temporizador estricto de **3 segundos** (`AUTH_TIMEOUT_MS = 3000L`).
2. **Envío de Frame `AUTH`:** El cliente debe transmitir inmediatamente dentro del plazo de 3 segundos un frame de acción con su token JWT:
   ```json
   {
     "action": "AUTH",
     "token": "<JWT_ACCESS_TOKEN>"
   }
   ```
3. **Validación Exitosa:**
   - Si el JWT es válido y emitido por el emisor configurado (`https://mus.org/issuer`):
     - El temporizador se cancela.
     - La conexión se marca como autenticada (`ws.authenticated = true`).
     - Se guardan los claims del usuario en `userData` (`accountId`, `playerTag`, `email`, `roomId`).
     - El backend responde con el evento de confirmación `AUTH_SUCCESS`:
       ```json
       {
         "event": "AUTH_SUCCESS",
         "payload": {
           "accountId": "a3bb189e-8bf9-3888-9912-ace4e6543002",
           "playerTag": "Carlos#1001",
           "roomId": "sala-norte-1"
         }
       }
       ```
4. **Rechazo y Cierre no Autorizado (Código 4401):**
   - Si transcurren los **3 segundos** sin recibir un frame de autenticación válido.
   - Si se envía cualquier frame con acción distinta a `AUTH` antes de estar autenticado.
   - Si el token es inválido, malformado, vacío o ha expirado.
   - Si el JSON recibido es inválido.
   En cualquiera de estos casos, el backend cancela el temporizador y cierra inmediatamente la conexión con **código WebSocket `4401`** (`WS_CLOSE_UNAUTHORIZED`) y razón `"Unauthorized"`.

---

## 1. Eventos Cliente -> Servidor (Comandos de Entrada)

### A. Cambio de Foco Exclusivo (SET_FOCUS)

El cliente notifica inmediatamente cada vez que cambia su objetivo visual en la mesa:

```json
{
  "action": "SET_FOCUS",
  "payload": {
    "target": "RIVAL_LEFT" // "CARDS" | "PARTNER" | "RIVAL_LEFT" | "RIVAL_RIGHT"
  }
}
```

### B. Emisión de Seña (`SEND_SIGNAL`)

Lanza una animación gestual de 1.2 segundos en el avatar del emisor.

```json
{
  "action": "SEND_SIGNAL",
  "payload": {
    "signalType": "DOS_REYES" // "DOS_REYES" | "DOS_ASES" | "MEDIAS" | "DUPLES" | "TREINTA_Y_UNO" | "CIEGO"
  }
}
```

### C. Caza Activa de Seña (CATCH_SIGNAL)

Acción ejecutada cuando el jugador ve un gesto en el avatar del rival al que está enfocando y pulsa el botón de intercepción:

```json
{
  "action": "CATCH_SIGNAL",
  "payload": {}
}
```

> **Nota:** El servidor evalúa automáticamente contra el rival al que el jugador está enfocando en su estado `currentFocus`.

### D. Comandos de Juego (DECLARE_MUS, DISCARD, BET_ACTION)

```json
{ "action": "DECLARE_MUS", "payload": { "mus": false } }
```

```json
{ "action": "DISCARD", "payload": { "cardIndices": [0, 3] } }
```

```json
{ "action": "BET_ACTION", "payload": { "type": "ENVIDO", "points": 5 } }
```

## 2. Eventos Servidor -> Cliente (Salidas y Filtrado)

### A. Estado Proyectado del Jugador (STATE_UPDATE)

Las cartas de los demás asientos jamás viajan por la red hasta la fase `SHOWDOWN_SCORING`.

```json
{
  "event": "STATE_UPDATE",
  "payload": {
    "phase": "BETTING_GRANDE",
    "currentTurnSeat": 1,
    "manoSeat": 0,
    "mySeat": 2,
    "myCards": [
      { "id": "c1", "suit": "OROS", "rawValue": 1, "skinId": "skin_as_oros_founder" },
      { "id": "c2", "suit": "COPAS", "rawValue": 3, "skinId": null },
      { "id": "c3", "suit": "ESPADAS", "rawValue": 12, "skinId": null },
      { "id": "c4", "suit": "BASTOS", "rawValue": 10, "skinId": null }
    ],
    "players": [
      { "seat": 0, "username": "Carlos", "cardCount": 4, "currentFocus": "PARTNER", "catchCooldownUntil": null },
      { "seat": 1, "username": "Dani", "cardCount": 4, "currentFocus": "CARDS", "catchCooldownUntil": null },
      { "seat": 2, "username": "Jorge", "cardCount": 4, "currentFocus": "RIVAL_LEFT", "catchCooldownUntil": null },
      { "seat": 3, "username": "Alex", "cardCount": 4, "currentFocus": "PARTNER", "catchCooldownUntil": null }
    ],
    "scores": { "teamA": 15, "teamB": 22 }
  }
}
```

### B. Renderizado Visual de Gesto (AVATAR_GESTURE)

Se envía únicamente a los jugadores que en ese instante tienen su `currentFocus` apuntando al asiento `fromSeat`. Dispara la animación facial en el avatar sin revelar con texto qué significa, obligando a los jugadores a estar atentos.

```json
{
  "event": "AVATAR_GESTURE",
  "payload": {
    "fromSeat": 0,
    "gestureAnimation": "BITE_LIP",
    "durationMs": 1200
  }
}
```

### C. Resolución de Caza de Seña (SIGNAL_CAUGHT_RESULT)

Se emite a toda la mesa cuando un rival pulsa el botón de cazar seña:

```json
{
  "event": "SIGNAL_CAUGHT_RESULT",
  "payload": {
    "hunterSeat": 2,
    "targetSeat": 1,
    "success": true,
    "revealedSignal": "DOS_REYES", // null si success == false
    "penaltyCooldownMs": 0         // 5000 si success == false
  }
}
```

## 3. Lógica de Validación de Intercepción en Quarkus

```java
public void handleCatchSignal(int hunterSeat) {
    PlayerState hunter = players.get(hunterSeat);
    long now = System.currentTimeMillis();
    // 1. Validar que no esté en cooldown por spam previo
    if (now < hunter.getCatchCooldownUntil()) return;
    // 2. Solo puede cazar si su foco está en RIVAL_LEFT o RIVAL_RIGHT
    Integer targetSeat = resolveFocusedRivalSeat(hunterSeat, hunter.getCurrentFocus());
    if (targetSeat == null) return;
    PlayerState targetRival = players.get(targetSeat);
    // 3. Comprobar ventana de 1200ms + 300ms de gracia de red
    if (targetRival.getLastSignalTime() != null && (now - targetRival.getLastSignalTime()) <= 1500) {
        broadcastSignalCaught(hunterSeat, targetSeat, true, targetRival.getLastSignalType(), 0);
    } else {
        // Fallo: penalización anti-spam de 5 segundos
        hunter.setCatchCooldownUntil(now + 5000);
        broadcastSignalCaught(hunterSeat, targetSeat, false, null, 5000);
    }
}
```
