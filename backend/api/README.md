# Backend API

This module contains the Spring Boot backend for ProjectBankApp.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
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

Current example controller:
- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`

Example request body for `POST /api/users`:

```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com"
}
```

## Example User flow

The sample feature is centered on the `User` entity:

- `controller/UserController.java` handles the HTTP routes
- `service/interfaces/IUserService.java` defines the service contract
- `service/UserService.java` contains the business logic
- `repository/interfaces/IUserRepository.java` defines the app-level repository contract
- `repository/UserRepository.java` wraps the JPA repository
- `repository/interfaces/IUserJpaRepository.java` extends `JpaRepository`
- `model/User.java` maps to the `example_users` table
- `dto/UserRequest.java` and `dto/UserResponse.java` separate input/output from the entity

## Configuration

Main configuration is in `src/main/resources/application.properties`.

The backend reads these environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Default local fallback values exist in the properties file, but in practice the project is meant to be run through Docker Compose or with explicit environment configuration.

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
- `UserControllerTest` for controller unit tests
- `UserServiceTest` for service unit tests
- `UserControllerFunctionalTest` for functional API testing
- `UserRepositoryTest` for repository persistence coverage
- `ApiApplicationTests` for application context loading

This matches the project goal of having both unit tests and higher-level functional coverage.

## Notes for teammates

- The backend is currently a scaffolded example, not a full banking domain yet.
- `User` is the reference feature for how future modules such as accounts or transactions should be structured.
- If you add a new feature, follow the same layer split and keep public endpoints under `/api`.
