# Multi-stage build for optimized production image
FROM eclipse-temurin:21-jdk-jammy AS builder

# Install required packages
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy gradle wrapper and configuration files
COPY gradlew* ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make gradlew executable and download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the production jar
RUN ./gradlew productionBuild -x test --no-daemon

# Final stage - use JRE for smaller image
FROM eclipse-temurin:21-jre-jammy

# Install required packages and create user
RUN apt-get update && \
    apt-get install -y curl dumb-init && \
    rm -rf /var/lib/apt/lists/* && \
    addgroup --system spring && \
    adduser --system --group spring

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/AMHSDataGen-production.jar app.jar

# Create necessary directories and set permissions
RUN mkdir -p /app/data /app/logs && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set environment variables for production
ENV SPRING_PROFILES_ACTIVE=production
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
