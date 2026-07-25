plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.spring") version "2.4.10"
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.4.10"
}

group = "com.mikesajak"
version = "0.0.1-SNAPSHOT"
description = "Ebook library"

kotlin {
    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.liquibase:liquibase-core")
    implementation("net.lbruun.springboot:preliquibase-spring-boot-starter:1.6.1")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.5")

    implementation("org.apache.tika:tika-core:3.1.0")
    implementation("org.apache.tika:tika-parsers-standard-package:3.1.0")

    implementation(platform("software.amazon.awssdk:bom:2.39.0"))
    implementation("software.amazon.awssdk:s3")

    implementation("cz.jirutka.rsql:rsql-parser:2.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.assertj:assertj-core:3.27.3")

    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:minio")
    testImplementation("org.testcontainers:postgresql")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
    annotation("com.mikesajak.ebooklib.opds.annotation.OpdsModel")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("build") {
    dependsOn(":frontend:copyReactBuild")
}

tasks.test {
    testLogging {
        events("PASSED", "FAILED", "SKIPPED", "STARTED")
        showStandardStreams = true
    }
}

// --- Container/Docker/Podman Image Tasks ---
val buildImage = tasks.register<Exec>("buildImage") {
    group = "docker"
    description = "Builds the Podman container image for the application."
    workingDir(file("."))

    val command = listOf("podman", "build", "-t", "ebooklib-backend:latest", "-f", "Dockerfile", ".")
    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine(listOf("cmd", "/c") + command)
    } else {
        commandLine(command)
    }
}

val buildImageArchive = tasks.register<Exec>("buildImageArchive") {
    group = "docker"
    description = "Builds and archives the Podman container image to a tarball."
    dependsOn(buildImage)
    workingDir(file("."))

    doFirst {
        val archive = file("build/ebooklib-backend.tar")
        if (archive.exists()) {
            archive.delete()
        }
        file("build").mkdirs()
    }

    val archivePath = "build/ebooklib-backend.tar"
    val command = listOf("podman", "save", "-o", archivePath, "ebooklib-backend:latest")
    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine(listOf("cmd", "/c") + command)
    } else {
        commandLine(command)
    }

    doLast {
        println("Production container image archived successfully to: ${file(archivePath).absolutePath}")
    }
}

val tagImage = tasks.register<Exec>("tagImage") {
    group = "docker"
    description = "Tags the local image for the private registry."
    dependsOn(buildImage)
    workingDir(file("."))

    val registryUrl = project.findProperty("registryUrl") as? String ?: "server.local:5000"
    val imageName = "ebooklib-backend"
    val imageTag = project.findProperty("imageTag") as? String ?: "latest"
    val fullImageTarget = "$registryUrl/$imageName:$imageTag"

    val command = listOf("podman", "tag", "$imageName:latest", fullImageTarget)
    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine(listOf("cmd", "/c") + command)
    } else {
        commandLine(command)
    }
}

val pushImage = tasks.register<Exec>("pushImage") {
    group = "docker"
    description = "Pushes the container image to the private registry."
    dependsOn(tagImage)
    workingDir(file("."))

    val registryUrl = project.findProperty("registryUrl") as? String ?: "server.local:5000"
    val imageName = "ebooklib-backend"
    val imageTag = project.findProperty("imageTag") as? String ?: "latest"
    val fullImageTarget = "$registryUrl/$imageName:$imageTag"

    // Uses --tls-verify=false as local registries often run on HTTP without SSL
    val command = listOf("podman", "push", "--tls-verify=false", fullImageTarget)
    if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
        commandLine(listOf("cmd", "/c") + command)
    } else {
        commandLine(command)
    }
}

