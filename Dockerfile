# ============================================================
# ErpCore quickstart — Render deployment
# ============================================================
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Layer-cache dependency downloads
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -q

# Build security module
COPY src ./src
RUN ./mvnw -B install -DskipTests -q

# Build quickstart using ROOT mvnw with -f
COPY examples/quickstart-app/pom.xml examples/quickstart-app/pom.xml
RUN ./mvnw -B -f examples/quickstart-app/pom.xml dependency:go-offline -q

COPY examples/quickstart-app/src examples/quickstart-app/src
RUN ./mvnw -B -f examples/quickstart-app/pom.xml package -DskipTests -q

# ---- Runtime ----
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
COPY --from=build /app/examples/quickstart-app/target/quickstart-app-1.0.0.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Render injects $PORT — bind to it
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT:-8080}"]