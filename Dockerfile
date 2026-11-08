FROM eclipse-temurin:25-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the entire project
COPY . .

# Grant execute permission to the Maven wrapper (required on Linux)
RUN chmod +x mvnw

# Build the security module and the quickstart app
RUN ./mvnw clean install -DskipTests

# The quickstart app will run on port 8080
EXPOSE 8080

# Start the quickstart app
WORKDIR /app/examples/quickstart-app
CMD ["java", "-jar", "target/quickstart-app-1.0.0.jar"]