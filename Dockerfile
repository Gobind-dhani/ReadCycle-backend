# === STAGE 1: Build the application using Maven ===
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (leverages Docker cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source files
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# === STAGE 2: Run the application ===
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy only the JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]
