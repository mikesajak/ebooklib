# Stage 1: Build the Spring Boot application and frontend
FROM docker.io/library/gradle:8.12-jdk21 AS build

# Install Node.js & NPM
USER root
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

USER gradle
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the application
FROM docker.io/library/eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

