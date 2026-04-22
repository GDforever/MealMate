# MealMate Phase 1 — Backend Core Design

**Date:** 2026-04-22
**Phase:** 1 of 3 (Backend Core)
**Status:** Approved

## Overview

Build the backend foundation for MealMate: data models, basic CRUD APIs, and JWT authentication. This phase delivers a fully testable REST API that Phase 2 (Spring AI integration) and Phase 3 (Vue 3 frontend) will build upon.

## Tech Stack

| Component | Choice |
|-----------|--------|
| Runtime | Java 21, Spring Boot 3.5.13 |
| Database | PostgreSQL (JSONB for tags/preferences) |
| ORM | Spring Data JPA + Hibernate |
| Auth | Spring Security + JWT (jjwt 0.12.x) |
| DTO Mapping | MapStruct |
| Validation | spring-boot-starter-validation |
| API Docs | springdoc-openapi |
| Build | Maven |

## Project Structure

```
src/main/java/com/gd/mealmate/
├── MealMateApplication.java
├── config/
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── MealRecordController.java
│   └── RestaurantController.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── MealRecordService.java
│   └── RestaurantService.java
├── repository/
│   ├── UserRepository.java
│   ├── MealRecordRepository.java
│   └── RestaurantRepository.java
├── model/entity/
│   ├── User.java
│   ├── MealRecord.java
│   └── Restaurant.java
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── MealRecordRequest.java
│   │   └── RestaurantQueryRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── UserDto.java
│       ├── MealRecordDto.java
│       └── RestaurantDto.java
├── security/
│   ├── JwtTokenProvider.java
│   └── JwtAuthFilter.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── BusinessException.java
    └── ErrorCode.java
```

## Data Models

### User

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| username | String | unique, not null | |
| password | String | not null | BCrypt hashed |
| email | String | unique | |
| tastePreferences | String | jsonb | `["辣", "清淡", "素食"]` |
| createdAt | LocalDateTime | | Auto-set on create |
| updatedAt | LocalDateTime | | Auto-set on update |

### MealRecord

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| user | User | FK, not null, LAZY | Many-to-one |
| mealType | MealType (enum) | not null | BREAKFAST, LUNCH, DINNER, SNACK |
| foodName | String | not null | |
| restaurantName | String | | Optional |
| location | String | | Address text |
| latitude | Double | | |
| longitude | Double | | |
| recordedAt | LocalDateTime | not null | When the meal was eaten |
| userRating | Integer | 1-5 | Optional |
| tags | String | jsonb | `["辣", "川菜"]` |
| createdAt | LocalDateTime | | Auto-set |

### Restaurant

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| name | String | not null | |
| address | String | | |
| latitude | Double | | |
| longitude | Double | | |
| cuisineType | String | | 中餐, 西餐, 日料, etc. |
| avgPrice | BigDecimal | | |
| rating | Double | | 0.0 - 5.0 |
| source | String | | MANUAL or AMAP |
| externalId | String | | Gaode POI ID for dedup |
| createdAt | LocalDateTime | | |
| updatedAt | LocalDateTime | | |

## API Endpoints

### Authentication (no JWT required)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register new user, returns JWT |
| POST | `/api/auth/login` | Login, returns JWT |

### User (JWT required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/user/me` | Get current user profile |
| PUT | `/api/user/preferences` | Update taste preference tags |

### Meal Records (JWT required)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/meal-records` | Create meal record |
| GET | `/api/meal-records` | List user's records (paginated, filterable by date range & mealType) |
| GET | `/api/meal-records/{id}` | Get record detail |
| PUT | `/api/meal-records/{id}` | Update record |
| DELETE | `/api/meal-records/{id}` | Delete record |

### Restaurants (JWT required)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/restaurants/nearby` | Search by lat/lng, optional radius & cuisine filter |
| GET | `/api/restaurants/{id}` | Get restaurant detail |

### Query Parameters

- `GET /api/meal-records?page=0&size=20&startDate=2026-04-01&endDate=2026-04-22&mealType=LUNCH`
- `GET /api/restaurants/nearby?lat=39.9042&lng=116.4074&radius=3000&cuisineType=中餐`

### Response Format

Success:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

Error:
```json
{
  "code": 40001,
  "message": "用户名已存在",
  "data": null
}
```

Paginated list wraps data in Spring Data's `Page<T>` structure.

## Authentication Design

- **Password hashing:** BCrypt (Spring Security default)
- **JWT claims:** `userId`, `username`
- **JWT expiration:** 7 days
- **JWT signing key:** configured in `application.yml` (`jwt.secret`)
- **Filter chain:**
  1. `JwtAuthFilter` extracts token from `Authorization: Bearer <token>`, validates signature, sets `SecurityContext`
  2. `/api/auth/**` paths permit all; all other paths require authentication
- **No refresh token** in MVP (7-day expiration is sufficient)

## Error Codes

| Code | Meaning |
|------|---------|
| 40001 | Username already exists |
| 40002 | Email already registered |
| 40101 | Not authenticated / Invalid token |
| 40102 | Token expired |
| 40401 | Resource not found |
| 50001 | General business error |

## Configuration

### Maven Dependencies (additions)

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `postgresql` driver
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.x)
- `mapstruct` + `mapstruct-processor`
- `springdoc-openapi-starter-webmvc-ui`

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mealmate
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

jwt:
  secret: ${JWT_SECRET:default-dev-secret-key-at-least-256-bits-long-for-hs256}
  expiration: 604800000  # 7 days in ms

# Reserved for Phase 2
amap:
  api-key: ${AMAP_API_KEY:your-amap-key}
  base-url: https://restapi.amap.com/v3

spring.ai:
  openai:
    base-url: ${AI_BASE_URL:https://api.deepseek.com}
    api-key: ${AI_API_KEY:your-api-key}
    chat:
      options:
        model: deepseek-chat
```

## Phase 1 Deliverables

1. Running Spring Boot application connected to PostgreSQL
2. User registration/login with JWT
3. Meal record CRUD (5 endpoints)
4. Restaurant basic queries
5. User preference management
6. Swagger UI accessible at `/swagger-ui.html`
7. Global exception handling
8. All endpoints testable via curl or Swagger UI

## Future Phases

- **Phase 2:** Spring AI integration (ChatClient, Function Calling), Gaode Maps API, streaming `/api/chat` endpoint
- **Phase 3:** Vue 3 + Element Plus frontend (chat UI, history calendar, settings page)
