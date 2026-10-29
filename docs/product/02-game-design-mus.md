# 02. Game Design Document (GDD) — Mus Tradicional & Mecánicas de Atención

**Estado:** Especificación Funcional (v0.1)  
**Juego Base:** Mus Clásico Español (4 jugadores / 2 parejas)

## 1. Parámetros y Reglas del Reglamento Oficial

- **Configuración de Baraja:** 40 cartas con **8 Reyes y 8 Ases**.
  - Los **Tres (3)** funcionan y computan exactamente como **Reyes**.
  - Los **Dos (2)** funcionan y computan exactamente como **Ases**.
- **Estructura de la Partida:**
  - Al mejor de **3 Juegos** (1 Vaca).
  - Cada juego se disputa a **40 piedras** (8 amarracos de 5 piedras).
- **Orden de Turno y Mesa:**
  - Sentido antihorario (derecha).
  - Compañeros sentados frente a frente (Asientos 0 y 2 vs. Asientos 1 y 3).
  - El rol de **"Mano"** rota una posición a la derecha en cada nueva mano y resuelve cualquier empate matemático a su favor.

## 2. Flujo de la Mano y Lances

1. **Reparto:** 4 cartas a cada jugador.
2. **Fase de Mus (Descartes):** Habla el mano ("Mus" o "Corto"). Si los 4 dan Mus, se descartan de 1 a 4 cartas y se repone del mazo hasta que alguien corte.
3. **Lance 1 — Grande:** Combinación de cartas más altas (Reyes/3 > Caballos > Sotas > 7 > 6 > 5 > 4 > Ases/2).
4. **Lance 2 — Chica:** Combinación de cartas más bajas (Ases/2 > 4 > 5 > 6 > 7 > Sotas > Caballos > Reyes/3).
5. **Lance 3 — Pares:** Declaración previa ("¿Pares?"). Jerarquía: **Duples** (3 piedras) > **Medias** (2 piedras) > **Pares simples** (1 piedra).
6. **Lance 4 — Juego o Punto:**
   - Figuras y 3s valen 10; Ases y 2s valen 1; resto su valor facial.
   - **Con Juego (>=31):** Jerarquía: **31 (3 piedras) > 32 (2 piedras) > 40 > 37 > 36 > 35 > 34 > 33** (2 piedras todos salvo la 31).
   - **Al Punto (<31):** Si nadie llega a 31, gana quien más se acerque a 30 (1 piedra de recompensa base).
7. **Resolución (Showdown) y Órdago:**
   - Se cobran envites aceptados y bonificaciones de Pares y Juego por orden.
   - El **Órdago** aceptado ("Quiero") detiene la mano al instante, voltea las cartas y otorga el juego de 40 piedras al ganador de ese lance.

## 3. Mecánica Central: "Foco Exclusivo" y "Caza Activa de Señas"

### A. Foco de Mirada Exclusivo (1 Target a la vez)

El jugador solo puede enfocar un elemento en cada momento mediante atajos rápidos (Espacio, W, A, D) o clic:

1. **CARDS (Mis Cartas):** Ves tus naipes con nitidez; el resto de la mesa y los avatares quedan desenfocados. Eres ciego a cualquier seña o mirada en la mesa.
2. **PARTNER (Compañero):** Miras de frente a tu pareja para enviarle o recibir una seña.
3. **RIVAL_LEFT (Rival Izquierdo):** Vigilas el rostro del oponente izquierdo.
4. **RIVAL_RIGHT (Rival Derecho):** Vigilas el rostro del oponente derecho.

*Feedback visual en mesa:* El avatar de cada jugador gira la cabeza y los ojos hacia su foco activo, permitiendo a los demás leer hacia dónde está mirando cada uno.

### B. Catálogo de Señas Oficiales (Duración: 1.2s)

| Jugada / Combinación | Gesto / Seña Facial |
|---|---|
| **Dos Reyes** | Morderse el labio inferior |
| **Dos Ases** | Sacar la punta de la lengua |
| **Medias (Trío)** | Torcer la boca a un lado |
| **Duples** | Levantar ambas cejas |
| **31 (Juego Real)** | Guiñar un ojo |
| **Ciego (Nada)** | Cerrar ambos ojos brevemente |

### C. Resolución y Botón de "¡Cazar Seña!" (Anti-Spam)

- **Recepción del Compañero:** Si emites una seña y tu compañero te tiene enfocado (PARTNER), ve la animación claramente en tu avatar. Si está mirando sus cartas (CARDS), la pierde.
- **Intercepción Activa del Rival:** Si un rival te tiene enfocado (RIVAL_LEFT o RIVAL_RIGHT) mientras ejecutas la seña, **no recibe ningún cartel automático**: ve la animación facial en tu muñeco 3D/2D y debe pulsar activamente el botón **"¡Cazar Seña!"** (tecla F) dentro de la ventana de 1.2 segundos (más 0.3s de margen por latencia).
- **Reglas Anti-Spam:**
  - El botón "¡Cazar Seña!" está deshabilitado si tu foco está en CARDS o PARTNER.
  - **Penalización por Falsa Alarma:** Si pulsas "¡Cazar Seña!" enfocando a un rival que NO está haciendo ninguna seña en ese instante, sufres un bloqueo (*cooldown*) de 5 segundos en el botón y tu avatar hace un gesto de confusión visible para toda la mesa.

## 4. Temporizadores y Ritmo (Anti-AFK)

- **Turno de Acción / Envite:** 15 segundos.
- **Fase de Descarte:** 20 segundos.
- Si el tiempo expira, el servidor ejecuta la acción pasiva por defecto (*Cortar* / *Paso* / *No Quiero*). Dos turnos agotados seguidos activan el reemplazo temporal por bot.
