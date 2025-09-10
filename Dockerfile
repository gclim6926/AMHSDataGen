# Multi-stage build for smaller final image
FROM openjdk:21-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy all gradle-related files at once
COPY gradlew* ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew build -x test

# Final stage - use the same JDK image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/AMHSDataGen-0.0.1-SNAPSHOT.jar app.jar

# Create data directory
RUN mkdir -p /app/data

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
