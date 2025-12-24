plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "aladin"
version = "0.0.1-SNAPSHOT"
description = "webhook"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // =====================================================
    // Spring Boot Core
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // =====================================================
    // Kotlin / Jackson
    // =====================================================
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // =====================================================
    // Database: SQLite + JDBC
    // =====================================================
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.0")

    // =====================================================
    // DB Migration
    // =====================================================
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-core")

    // =====================================================
    // Security / HMAC utils
    // =====================================================
    implementation("commons-codec:commons-codec:1.16.0")

    // =====================================================
    // (Optional) API Documentation
    // =====================================================

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0-RC1")
    // =====================================================
    // Test
    // =====================================================
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
sourceSets {
    test {
        java.srcDirs("src/test/java", "src/test/kotlin")
    }
}
