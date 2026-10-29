# ==============================================================================
# Development Dockerfile for Quarkus Backend (Live Reload Mode)
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine

WORKDIR /app

# Pre-cache Maven dependencies
COPY apps/backend/pom.xml ./
COPY apps/backend/.mvn ./.mvn
COPY apps/backend/mvnw ./
RUN mvn dependency:go-offline -B || true

# Copy backend source
COPY apps/backend/src ./src

# Expose HTTP port and JVM debug port
EXPOSE 8080 5005

ENV QUARKUS_HTTP_HOST=0.0.0.0
ENV JAVA_ENABLE_DEBUG=true

# Start Quarkus in development mode with live reload
CMD ["mvn", "quarkus:dev", "-Dquarkus.http.host=0.0.0.0"]
