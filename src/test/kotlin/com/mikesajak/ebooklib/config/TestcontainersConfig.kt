package com.mikesajak.ebooklib.config

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class TestcontainersConfig : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withStartupTimeout(java.time.Duration.ofMinutes(5))

        val minio: MinIOContainer = MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .withUserName("minioadmin")
            .withPassword("minioadmin")
            .withReuse(true)
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        postgres.start()
        minio.start()
        TestPropertyValues.of(
            "spring.datasource.url=" + postgres.jdbcUrl,
            "spring.datasource.username=" + postgres.username,
            "spring.datasource.password=" + postgres.password,
            "minio.endpoint=" + minio.s3URL,
            "minio.access-key=" + minio.userName,
            "minio.secret-key=" + minio.password,
            "minio.bucket-name=test-bucket"
        ).applyTo(applicationContext.environment)
    }
}
