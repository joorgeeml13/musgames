# 01. Technical Specification — System Architecture

**Módulo:** Arquitectura Global (Monorepo)  
**Enfoque:** SPA Unificada en Frontend + Backend Reactivo/Autoritativo en Memoria.

## 1. Stack Tecnológico

### Frontend (dominio.com — Single Page Application)

- **Core:** React 19 + TypeScript + Vite.
- **Routing:** TanStack Router (tipado estricto de rutas: `/`, `/lobby`, `/profile`, `/room/$roomId`).
- **Gestión de Estado en Tiempo Real:** Zustand (sincronizado directamente con los eventos del WebSocket).
- **UI & Maquetación:** Tailwind CSS + shadcn/ui (para el Lobby, perfiles, rankings y el MVP 2D).
- **Motor Gráfico (Fase v1.0):** Three.js + React Three Fiber (R3F) + Drei (montado dinámicamente en la vista `/room/$roomId`).

### Backend (api.dominio.com — Servidor Autoritativo)

- **Framework:** Quarkus (Java 21), preparado para compilación nativa GraalVM.
- **Tiempo Real:** quarkus-websockets-next para comunicación bidireccional de baja latencia en las salas de juego.
- **API REST:** quarkus-rest-jackson para autenticación, perfiles, inventario de skins, rankings y matchmaking.
- **Persistencia & Seguridad:** Hibernate ORM con Panache, PostgreSQL 16 y autenticación mediante JWT (quarkus-smallrye-jwt).

## 2. Decisiones Arquitectónicas Clave

### A. Unificación en una sola SPA (Sin redirección de subdominios)

El lobby social y la mesa de juego conviven bajo el mismo origen (`dominio.com`). Esto elimina problemas de CORS cruzado, pérdida de contexto de sesión JWT y tiempos de recarga al entrar a una partida. El lienzo 3D de Three.js se carga mediante Lazy Loading únicamente cuando el usuario entra en la ruta `/room/$roomId`.

### B. Estado de Partida en Memoria (RAM) vs. Persistencia en Disco (PostgreSQL)

- **Durante la partida (Hot State):** Todo el estado de las manos de mus, mazos, apuestas, focos de mirada y temporizadores vive exclusivamente en la memoria RAM del nodo Quarkus dentro de un `RoomManager` (`ConcurrentHashMap<String, GameRoom>`). Esto garantiza latencia sub-milisegundo y evita saturar PostgreSQL con escrituras en cada cambio de foco o envite.
- **Al finalizar la partida (Cold Persistence):** Solo cuando una pareja gana la vaca (3 juegos de 40 piedras), el servidor abre una transacción en base de datos para guardar el registro en `matches`, registrar `match_participants` y actualizar el `elo_rating` de los 4 jugadores.

## 3. Flujo de Componentes

```text
[ Cliente React SPA ]
├── (REST HTTPS) ──> [ Quarkus REST Controllers ] ──> [ PostgreSQL ]
│                    (Login, Perfil, Skins, ELO)
│
└── (WSS Socket) ──> [ Quarkus WebSocket Endpoint ]
                     │
                     ▼
             [ RoomManager (In-Memory) ]
             ├── MusEngine (FSM)
             ├── SignalInterceptor (Foco & Ventanas de 1.2s)
             └── TurnTimerScheduler
```

## 4. Seguridad y Prevención de Trampas (Anti-Cheat)

1. **Filtrado de Cartas (Zero-Trust Client):** El servidor nunca envía las cartas de los demás jugadores por el WebSocket. Cada jugador recibe un DTO proyectado exclusivamente para su asiento.
2. **Validación de Foco en Servidor:** El cliente no decide si ha cazado una seña. El cliente envía el comando `CATCH_SIGNAL` y es el backend en Quarkus quien comprueba los timestamps en memoria (`lastSignalTimestamp`, `currentFocus` del cazador y `cooldownUntil`) para determinar si la intercepción es legal.
