# Stage 1: build the boot jar inside the image, so no local Gradle build is required.
# JDK 21 matches the toolchain declared in build.gradle.
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

# Resolve dependencies in their own layer so editing sources does not re-download them.
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# Stage 2: runtime image, JRE only.
FROM eclipse-temurin:21-jre-jammy

# Install wget for the healthcheck (the JRE image has no curl)
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Add a non-root user to run the application
RUN groupadd -r spring && useradd -r -g spring spring

# Set working directory
WORKDIR /opt/app

# Copy the JAR file built in stage 1
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar app.jar

# Set ownership of the files
RUN chown -R spring:spring /opt/app

# Use the spring user
USER spring

# Expose the port the app runs on
EXPOSE 8080

# Configure health check - using wget instead of curl
HEALTHCHECK --interval=30s --timeout=3s \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
