# Use an official lightweight JDK 17 base image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory inside the container
WORKDIR /app

# ✅ Correct Maven path here
COPY target/server-0.0.1.jar app.jar

# Let Render dynamically assign the port
ENV PORT=8080

# Expose the port (Render uses dynamic ports, but this is good practice)
EXPOSE 8080

# Run the Spring Boot app using the dynamic port
CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]
