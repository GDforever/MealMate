# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MealMate — a Spring Boot 3.5.13 web application (Java 21, Maven). Base package: `com.gd.mealmate`.

## Build & Run Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=MealMateApplicationTests

# Run a single test method
./mvnw test -Dtest=MealMateApplicationTests#contextLoads
```

## Tech Stack

- **Java 21** with Spring Boot 3.5.13
- **Lombok** — annotation processor configured for both compile and test-compile phases
- **Spring Web** (servlet-based)
- **JUnit 5** via spring-boot-starter-test

## Architecture

Standard Spring Boot layered layout under `src/main/java/com/gd/mealmate/`. Follow Spring conventions: controllers in `controller`, services in `service`, repositories in `repository`, entities in `model`/`entity`, DTOs in `dto`.

Configuration lives in `src/main/resources/application.properties`.
