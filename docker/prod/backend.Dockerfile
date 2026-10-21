# ==============================================================================
# Production Dockerfile for Quarkus Backend (Multi-stage Maven -> Temurin JRE 21)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Quarkus fast-jar
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Pre-cache Maven dependencies
COPY backend/pom.xml ./
COPY backend/.mvn ./.mvn
COPY backend/mvnw ./
RUN mvn dependency:go-offline -B || true

# Copy source code and build production fast-jar package
COPY backend/src ./src
RUN mvn clean package -DskipTests -Dquarkus.package.jar.type=fast-jar

# ------------------------------------------------------------------------------
# Stage 2: Minimal JRE 21 Runtime Container
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

# Create non-root system user for security compliance
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /deployments

# Copy Quarkus fast-jar application layers with correct permissions
COPY --from=builder --chown=appuser:appgroup /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder --chown=appuser:appgroup /build/target/quarkus-app/*.jar /deployments/
COPY --from=builder --chown=appuser:appgroup /build/target/quarkus-app/app/ /deployments/app/
COPY --from=builder --chown=appuser:appgroup /build/target/quarkus-app/quarkus/ /deployments/quarkus/

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]
