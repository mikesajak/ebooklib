# Use a base image with Java
FROM eclipse-temurin:21-jdk-jammy

# Set the working directory
WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradlew .
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
COPY build.gradle.kts settings.gradle.kts ./

# Copy the source code
COPY src src/

# Build the application
RUN ./gradlew bootJar

# Expose the port the application runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "build/libs/EbookLibrary4-0.0.1-SNAPSHOT.jar"]
