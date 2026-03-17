# Freezer Tracking API

Spring Boot REST API for the Freezer Tracking platform.

## Build Plugins

- `org.springframework.boot` `3.5.11` — Spring Boot plugin for packaging and runtime tasks
- `io.spring.dependency-management` `1.1.7` — manages transitive dependency versions aligned with Spring Boot

## Dependencies

Application dependencies:

- `org.springframework.boot:spring-boot-starter-web` — REST controllers, embedded server, and JSON support
- `org.springframework.boot:spring-boot-starter-data-jpa` — JPA, Hibernate, and repository support
- `org.springframework.boot:spring-boot-starter-validation` — Jakarta Bean Validation for request DTOs
- `org.springframework.security:spring-security-crypto` — password hashing and related crypto utilities
- `org.liquibase:liquibase-core` — database schema migrations
- `org.postgresql:postgresql` — PostgreSQL JDBC driver at runtime

Test dependencies:

- `org.springframework.boot:spring-boot-starter-test` — JUnit, Spring test support, MockMvc, and related test utilities
- `com.h2database:h2` — in-memory database for tests

## Commands

- `./gradlew bootRun`
- `./gradlew test`
