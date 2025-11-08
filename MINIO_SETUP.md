# MinIO Setup and Usage

This document outlines how to set up and use MinIO for binary file storage (book covers and ebook formats) in the Ebook Library project.

## 1. Development Setup

For local development, MinIO is integrated via `podman-compose` (or `docker-compose`).

### 1.1 Running MinIO and the Spring Boot Application

1.  **Ensure Podman (or Docker) is running.**
2.  **Build the Spring Boot application Docker image:**
    ```bash
    ./gradlew bootJar
    podman build -t ebooklibrary .
    ```
    *(Note: The `podman-compose.yaml` will build the image automatically if it doesn't exist, but pre-building can sometimes be useful.)*
3.  **Start MinIO and the Spring Boot application:**
    Navigate to the project root directory and run:
    ```bash
    podman compose -f podman-compose.yaml up -d
    ```
    This command will:
    *   Start a MinIO container, exposing ports `9000` (API) and `9001` (Console).
    *   Start the Spring Boot application container, exposing port `8080`.
    *   The Spring Boot application will automatically connect to the MinIO service within the `podman-compose` network.

### 1.2 Accessing MinIO

*   **MinIO Console:** Open your web browser and navigate to `http://localhost:9001`.
    *   **Access Key:** `minioadmin`
    *   **Secret Key:** `minioadmin`
*   **MinIO API Endpoint:** `http://localhost:9000`

### 1.3 Spring Boot Configuration

The Spring Boot application's `src/main/resources/application-local.yaml` contains the necessary properties to connect to the local MinIO instance:

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: ebook-library-files
```

### 1.4 Running Tests with Testcontainers

Integration tests for `S3FileStorageAdapter` use Testcontainers to spin up a dedicated MinIO instance.

*   **Prerequisite:** For Testcontainers to work, a Docker-compatible daemon (like Docker or Podman) must be running and accessible.
*   **Podman Configuration:** If you are using Podman, you might need to configure your environment to allow Testcontainers to connect. This typically involves setting the `DOCKER_HOST` environment variable. For example, in your shell before running tests:
    ```bash
    export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
    # The exact path to the Podman socket might vary depending on your system setup.
    # You can usually find it by running `podman info --format "{{.Host.RemoteSocket.Path}}"`
    ```
*   **Running Tests:**
    ```bash
    ./gradlew test
    ```

## 2. Release Setup Considerations

For a production or release environment, consider the following:

*   **Production-Grade Database:** Replace the embedded H2 database (used in development) with a robust production database (e.g., PostgreSQL, MySQL).
*   **MinIO Credentials:**
    *   **Externalize:** Do not hardcode `minioadmin` credentials. Use strong, unique credentials.
    *   **Secure Storage:** Store credentials securely using environment variables, a secrets management service (e.g., HashiCorp Vault, AWS Secrets Manager), or a dedicated configuration server.
*   **MinIO Deployment:**
    *   Deploy MinIO as a highly available, scalable service, potentially using Kubernetes or a dedicated object storage service (e.g., AWS S3, Google Cloud Storage) that offers an S3-compatible API.
    *   Configure appropriate storage persistence and backup strategies for MinIO data.
*   **Spring Boot Configuration:**
    *   Use `application.yaml` or environment variables to configure the production MinIO endpoint, access key, secret key, and bucket name.
    *   Ensure the `MINIO_SERVER_URL` in `podman-compose.yaml` (if used in production) points to the correct internal network address of the MinIO service.
*   **Security:** Implement proper network security, access control, and encryption for both MinIO and the Spring Boot application.
