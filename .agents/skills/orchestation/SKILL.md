---
name: orquestator
description: Orquestador maestro. Planifica la feature, coordina a los devs en paralelo y pasa filtros de QA, seguridad e infraestructura si hace falta.
---
Analiza la tarea del usuario: $ARGUMENTS

## Protocolo de Ejecución:

### 1. Fase de Diseño y Contrato (Tú, Agente Principal)
- No escribas código todavía. Define los contratos de API y los requisitos de infraestructura necesarios antes de delegar.

### 2. Fase de Desarrollo y Contenedores (Workers Paralelos)
Si la tarea requiere tocar código y preparar su entorno o empaquetado:
- Invoca a `@quarkus-dev` y `@react-dev` en paralelo para la lógica y la interfaz.
- Si la feature añade nuevas dependencias de servicios (bases de datos, colas, etc.) o modifica Dockerfiles, invoca en paralelo a `@devops-infra`.
- *Espera a que terminen y te devuelvan sus resúmenes.*

### 3. Fase de Validación y Blindaje (Secuencial)
Una vez implementado y empaquetado:
- Invoca a `@tester-qa` para verificar las pruebas automatizadas.
- Invoca a `@security-audit` para revisar vulnerabilidades de código.

### 4. Cierre
- Presenta al usuario un resumen limpio con todo lo construido, testado y empaquetado.