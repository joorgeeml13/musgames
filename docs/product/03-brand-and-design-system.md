# 03. Brand & Design System — Órdago (ordago.gg)

**Identidad de Marca:** "Cyber-Tavern" (La fusión entre el eSport competitivo de élite y la tensión clandestina de una taberna oscura).  
**Dominio:** ordago.gg

## 1. El Concepto y el Alma de la Marca

Órdago no es una web escolar de cartas; es un club clandestino moderno. Queremos que el usuario sienta la misma adrenalina que al soltar un envite definitivo en la mesa de un bar, pero con la finura visual de una plataforma competitiva de primer nivel (estilo Chess.com cruzado con la atmósfera inmersiva de Liar's Bar).

- **Palabras Clave de Marca:** Tensión, Pique, Elegancia Oscura, Clandestinidad, Estatus.
- **Tono de Comunicación:** Canalla pero pulido. Directo, competitivo, sin rodeos corporativos.

## 2. Paleta de Colores (Cyber-Tavern Palette)

La interfaz es 100% Dark Mode por defecto para evitar fatiga visual en sesiones nocturnas y resaltar la tensión del juego:

- **Backgrounds & Superficies:**
  - Obsidian Black (`#0D0F12`): Color de fondo principal (cuerpo y pantallas generales).
  - Deep Charcoal (`#13161C`): Superficies de tarjetas, paneles de lobby y contenedores de UI.
  - Tavern Wood (`#2A1E14`): Acentos sutiles de madera envejecida para bordes de mesas o marcos especiales.
- **Acentos & Acciones (Gamification & Bets):**
  - **Órdago Crimson (`#E53935` / `#C62828`):** Color de alerta máxima, usado para botones de Órdago, peligro de derrota y alertas de envites altos.
  - **Victory Gold (`#FFB300` / `#F57F17`):** Color de ELO, rachas ganadoras, rarezas Legendarias en skins y resaltado del jugador "Mano".
- **Textos & Tipografía UI:**
  - Parchment White (`#F5F5F7`): Texto principal de alta legibilidad.
  - Slate Muted (`#9BA1A6`): Textos secundarios, contadores de tiempo y etiquetas inactivos.

## 3. Tipografía

- **Logotipo y Títulos Principales:**
  - Fuente Serif con carácter afilado y clásico modernizado (ej. *Cinzel*, *Syne* o *Playfair Display* en pesos Bold/Black). Transmite solidez de baraja tradicional y peso competitivo.
- **Interfaz de Usuario (UI) y Textos del Sistema:**
  - Tipografía Sans-Serif geométrica y ultra-limpia (ej. *Plus Jakarta Sans* o *Inter*). Excelente legibilidad en números de ELO, temporizadores, botones de acción y chats rápidos.

## 4. Conceptos de Logotipo (ordago.gg)

1. **Opción A (El As de Bastos Cyber):** Una reinterpretación minimalista del As de Bastos de la baraja española, cruzado con un trazo geométrico afilado que evoca una espada o un haz de luz láser tenue.
2. **Opción B (La Corona del Órdago):** Un emblema circular limpio que combina la silueta de un amarraco (ficha de piedra de mus) con una corona geométrica minimalista.
3. **Opción C (Tipografía Logotype):** La palabra **ÓRDAGO** en mayúsculas pesadas, con la tilde de la 'Ó' sutilmente estilizada como una carta levantada o un brillo dorado.

## 5. Directrices de Interfaz (Tailwind & shadcn/ui)

- **Efectos de Vidrio (Glassmorphism):** Las barras de navegación flotantes, los modales y las cajitas de chat usan fondos semitransparentes con desenfoque (`backdrop-blur-md bg-black/60 border border-white/10`).
- **Micro-interacciones y Estados de Foco:**
  - El indicador de foco activo (cuando miras a tu compañero o a un rival) brilla con un sutil halo ámbar o rojo según el estado.
  - Los botones de acción crítica (Quiero, Órdago, Cazar Seña) tienen efectos de pulso lumínico para guiar la atención del jugador bajo presión de tiempo.
- **Legibilidad de Naipes:** Independientemente de la rareza o skin equipada, los valores numéricos y los palos de las cartas españolas deben ocupar siempre las esquinas superiores con contraste absoluto para evitar errores en partidas rápidas.
