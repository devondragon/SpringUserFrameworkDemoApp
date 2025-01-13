FROM eclipse-temurin:17-jre-jammy

# Add a non-root user to run the application
RUN groupadd -r spring && useradd -r -g spring spring

# Set working directory
WORKDIR /opt/app

# Copy the JAR file
COPY build/libs/*SNAPSHOT.jar app.jar

# Set ownership of the files
RUN chown -R spring:spring /opt/app

# Use the spring user
USER spring

# Expose the port the app runs on
EXPOSE 8080

# Configure health check
HEALTHCHECK --interval=30s --timeout=3s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
