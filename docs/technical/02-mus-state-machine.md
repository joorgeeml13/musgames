# 02. Technical Specification — Mus State Machine & Rules Engine

**Módulo:** Backend Core (`/backend/src/main/java/.../engine`)  
**Patrón:** Finite State Machine (FSM) Determinista con eventos asíncronos.  
**Objetivo:** Garantizar que el servidor sea la única fuente de verdad, previniendo estados inconsistentes o trampas de cliente.

## 1. Ciclo de Vida de la Mano (Estados Principales)

La máquina de estados transiciona strictly por los siguientes nodos:

1. **WAITING_PLAYERS**: Esperando a que los 4 asientos estén ocupados y conectados.
2. **DEALING**: Barajado y reparto de 4 cartas por jugador. Asignación del asiento Mano.
3. **MUS_PHASE**:
   - `ASKING_MUS`: Turno secuencial desde el Mano. Si los 4 dicen "Mus", pasa a `DISCARDING` (seleccionan de 1 a 4 cartas, se reparten nuevas y vuelve a `ASKING_MUS`).
   - Si cualquier jugador dice "Corto", transiciona inmediatamente a `LANCE_GRANDE`.
4. **LANCE_GRANDE**: Ronda de apuestas por la combinación más alta.
5. **LANCE_CHICA**: Ronda de apuestas por la combinación más baja.
6. **CHECK_PARES**:
   - Comprobación automática o declaración de quién lleva pares.
   - Si ambos bandos tienen pares, abre `BETTING_PARES`. Si solo un bando tiene, se salta la apuesta y ese bando cobrará sus piedras de gracia al final.
7. **CHECK_JUEGO**:
   - Si al menos un jugador suma >= 31 puntos: si ambos bandos tienen juego, abre `BETTING_JUEGO`.
   - Si **nadie** en la mesa llega a 31 puntos, abre `BETTING_PUNTO`.
8. **SHOWDOWN_SCORING**:
   - Se revelan las 16 cartas en mesa.
   - Se cobran los envites aceptados (`pendingPiedras`) en orden: Grande -> Chica -> Pares -> Juego/PUNTO.
   - Se suman las piedras adicionales de valor de Pares y Juego/Punto.
   - Si durante este conteo una pareja alcanza las 40 piedras, el juego termina inmediatamente.

## 2. Sub-Máquina de Envites (Betting Sub-FSM)

Dentro de cada lance con apuesta activa:

- **NO_BET**: Estado inicial del lance. Los jugadores pueden decir `PASO`, `ENVIDO (X)` u `ORDAGO`.
- **WAITING_RIVAL_RESPONSE**: Tras una apuesta o subida, solo hablan los integrantes del bando rival. Pueden responder:
  - `QUIERO`: Se cierra el lance y el valor total queda guardado en `pendingPiedras` para el recuento final.
  - `NO_QUIERO`: Se cierra el lance y el bando que apostó cobra en el acto la apuesta anterior (1 piedra si fue el primer envite).
  - `ENVIDO_MAS (Y)`: Sube la apuesta y devuelve el turno de respuesta al otro bando.
  - `ORDAGO`: Lanza apuesta total por el juego entero.
- **ORDAGO_ACCEPTED**: Si se responde `QUIERO` a un órdago, se aborta toda la secuencia de lances restantes, se muestran las cartas y el ganador de ese lance específico se adjudica el juego de 40 piedras.

## 3. Modelo de Dominio en Memoria (Java 21)

```java
public record Card(Suit suit, int rawValue) {
    // En Mus de 8 reyes y 8 ases: 3 computa como Rey (12), 2 computa como As (1)
    public int musPoints() {
        return switch (rawValue) {
            case 1, 2 -> 1;
            case 3, 10, 11, 12 -> 10;
            default -> rawValue;
        };
    }

    public int rankForGrande() {
        return switch (rawValue) {
            case 12, 3 -> 8; // Reyes y Treses
            case 11 -> 7;    // Caballos
            case 10 -> 6;    // Sotas
            case 7 -> 5;
            case 6 -> 4;
            case 5 -> 3;
            case 4 -> 2;
            case 2, 1 -> 1;  // Ases y Doses
            default -> 0;
        };
    }
}

public enum Suit {
    OROS, COPAS, ESPADAS, BASTOS
}

public enum ParesType {
    NONE(0), SIMPLE(1), MEDIAS(2), DUPLES(3);

    public final int bonusPiedras;

    ParesType(int bonus) {
        this.bonusPiedras = bonus;
    }
}
```

## 4. Algoritmo de Desempate y Regla del "Mano"

En el mus no existen los empates al repartir piedras. Si dos rivales tienen una mano idéntica en Grande, Chica, Pares, Juego o Punto, gana siempre el jugador que esté más cerca del **Mano** siguiendo el orden de reparto (antihorario):

- Posición relativa del jugador $i$: $Pos(i) = (Asiento_i - Asiento_{Mano} + 4) \pmod 4$.
- Entre dos rivales empatados $A$ y $B$, gana siempre quien tenga el menor valor de $Pos$.
