# ============================================================
# ErpCore quickstart — Render deployment
# Multi-stage build, correct mvnw path, PORT env handling
# ============================================================

# ---- Stage 1: Build ----
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Layer caching: dependency downloads first
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copy security module source
COPY src ./src

# Install erp-security-module to local .m2
RUN ./mvnw -B install -DskipTests

# Quickstart: use ROOT mvnw with -f, NOT a wrapper that doesn't exist
COPY examples/quickstart-app/pom.xml examples/quickstart-app/pom.xml
RUN ./mvnw -B -f examples/quickstart-app/pom.xml dependency:go-offline

COPY examples/quickstart-app/src examples/quickstart-app/src
RUN ./mvnw -B -f examples/quickstart-app/pom.xml package -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built fat jar
COPY --from=build /app/examples/quickstart-app/target/quickstart-app-1.0.0.jar app.jar

# Non-root user (security best practice on Render)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Render injects $PORT at runtime — bind to it (fallback 8080 for local test)
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT:-8080}"]