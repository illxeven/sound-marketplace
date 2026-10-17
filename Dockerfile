# ==============================================================================
# Sound Marketplace - Production Dockerfile
# Multi-stage build: JDK 21 build -> JRE 21 lightweight non-root runtime
# ==============================================================================

# --- Stage 1: Build Application ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and POM for dependency caching
COPY sound-marketplace/mvnw sound-marketplace/mvnw.cmd ./
COPY sound-marketplace/.mvn ./.mvn
COPY sound-marketplace/pom.xml ./

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and package
COPY sound-marketplace/src ./src
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: Lightweight Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install runtime utilities: ffmpeg (audio transcoding) and curl (health checks)
RUN apk add --no-cache ffmpeg curl

# Create non-root system user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Prepare persistent storage and log directories
RUN mkdir -p /app/storage /app/logs && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup /app/app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
