# Multi-stage build for smaller final image
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Install necessary packages
RUN apk add --no-cache bash curl

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

# Final stage - use JRE for smaller image
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Install necessary packages for runtime
RUN apk add --no-cache bash curl

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/AMHSDataGen-0.0.1-SNAPSHOT.jar app.jar

# Create data directory
RUN mkdir -p /app/data

# Create logs directory
RUN mkdir -p /app/logs

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-Xmx1024m -Xms512m -Dspring.profiles.active=production"
ENV TZ=Asia/Seoul

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
