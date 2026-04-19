# Backend API

This module contains the Spring Boot backend for ProjectBankApp.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Jakarta Bean Validation
- MariaDB driver
- H2 for tests
- JUnit with Spring Boot test support

## Package structure

The backend uses a package-by-layer structure under `src/main/java/nl/donniebankoebarkie/api`:

- `config` for application-wide Spring MVC configuration
- `controller` for HTTP endpoints
- `dto` for request and response models
- `exception` for shared exception handling
- `model` for JPA entities
- `repository` for data access wrappers
- `repository/interfaces` for repository contracts
- `service` for service implementations
- `service/interfaces` for service contracts

The matching test structure lives under `src/test/java/nl/donniebankoebarkie/api`.

## API conventions

- All REST controllers are exposed under the global `/api` prefix
- Controllers return DTOs instead of exposing entities directly
- `Location` headers for created resources are built from the current request path

Currently implemented endpoints:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/users/me`

Example request body for `POST /api/auth/register`:

```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com",
  "password": "Welkom123",
  "phoneNumber": "+31612345678",
  "bsnNumber": "123456789"
}
```

## Auth flow

The current backend slice is centered on authentication and the authenticated user profile:

- `controller/AuthController.java` handles registration, login, refresh, and logout
- `controller/UserController.java` exposes `/users/me`
- `service/AuthService.java` contains the auth business logic
- `repository/AuthRepository.java` and `RefreshTokenRepository.java` handle persistence
- `model/User.java` maps authenticated users
- `model/RefreshToken.java` stores hashed refresh tokens
- `security/JwtService.java` and `security/JwtAuthenticationFilter.java` handle JWT creation and request authentication

Registration behavior:
- registration always creates a `CUSTOMER`
- new customers are created with `approved = false`
- duplicate email and BSN are rejected
- no bank accounts are created during registration
- passwords are validated at the request boundary

Login behavior:
- successful login returns a short-lived bearer token in JSON
- refresh token rotation is handled through an HttpOnly cookie
- in the `prod` profile the refresh cookie is marked `Secure`

## Configuration

Main configuration is in `src/main/resources/application.properties`.

The backend reads these environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Default local fallback values exist in the properties file, but in practice the project is meant to be run through Docker Compose or with explicit environment configuration.

For local and CI tests:
- `src/test/resources/application.properties` switches the datasource to H2
- `src/main/resources/schema.sql` creates the `users` and `refresh_tokens` tables
- `src/main/resources/data.sql` is intentionally minimal
- functional tests create their own users instead of relying on shared application seed data

## Running locally

### Run the application

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

### Run tests

Linux/macOS:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

## Test structure

The project currently has:
- `AuthControllerTest` for controller unit tests
- `AuthControllerFunctionalTest` for auth flow functional coverage
- `AuthControllerProductionCookieFunctionalTest` for prod-only secure cookie behavior
- `AuthServiceTest` for service unit tests
- `UserControllerFunctionalTest` for authenticated `/users/me` access rules
- `ApiApplicationTests` for application context loading

This matches the current auth-first backend slice with both unit and higher-level functional coverage.

## CI

The backend is part of the GitHub Actions pipeline in:

- `.github/workflows/ci.yml`

The CI backend job:
- checks out the repository
- installs Java 21
- runs `chmod +x ./mvnw`
- runs `./mvnw test`

The `chmod` step is needed because the CI runner is Linux-based. Without it, the Maven wrapper can fail with `Permission denied`.

If CI fails with H2 schema or auth test errors, check these files first:
- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`
- `src/test/resources/application.properties`

## Notes for teammates

- The backend currently implements authentication and current-user retrieval, not the full banking domain yet.
- Several resources described in `OpenAPI.yml` are planned but not implemented yet.
- If you add a new feature, follow the same layer split and keep public endpoints under `/api`.
