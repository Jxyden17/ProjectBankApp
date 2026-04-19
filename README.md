# ProjectBankApp

ProjectBankApp is a student banking demo with:
- a Spring Boot backend in `backend/api`
- a Vue 3 frontend in `frontend`
- Docker Compose setups for a remote database flow and a local MariaDB flow

The current implementation focuses on authentication and gated customer onboarding:
- customer registration
- JWT-based login and `/users/me`
- refresh token rotation via HttpOnly cookie
- frontend route gating for unapproved customers

## Repository layout

- `backend/api` contains the Java API
- `frontend` contains the Vue/Tailwind UI
- `docker-compose.yml` starts the default remote database setup
- `docker-compose.local.yml` adds local MariaDB and phpMyAdmin
- `.env` is the default remote environment
- `.env.local` is the local Docker environment

## Docker modes

### Default remote mode

This mode starts:
- `backend`
- `frontend`

The backend uses the online MariaDB connection from `.env`.

Start it with:

```bash
docker compose up --build
```

The remote database requires TLS, so the JDBC URL in `.env` includes `sslMode=trust`.

Demo accounts on the online database:
- customer: `user@test.nl` / `Welkom123`
- employee: `employee@test.nl` / `Welkom123`

These credentials apply to the remote environment backed by `.env`. They are not created automatically in the local Docker database or in the backend test suite.

### Local mode

This mode starts:
- `backend`
- `frontend`
- `db`
- `phpmyadmin`

Start it with:

```bash
docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml up --build
```

`phpmyadmin` is only available in local mode and uses the values from `.env.local`.

## How the app is structured

### Backend

The backend uses a package-by-layer structure:
- `controller`
- `service`
- `repository`
- `model`
- `dto`
- `config`
- `exception`

All public API routes are prefixed globally with `/api`.

Currently implemented endpoints:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/users/me`

The backend test suite uses H2 and creates its own test users inside the tests instead of depending on shared app-level seed data.

### Frontend

The frontend uses:
- Vue 3
- Vue Router
- Tailwind CSS
- Vite

Current routes:
- `/`
- `/accounts`
- `/transfers`
- `/login`
- `/register`

The frontend is now authentication-aware:
- `/login` and `/register` are public
- `/accounts` and `/transfers` require authentication
- unapproved customers are restricted to `/`
- approved customers and employees keep access to the protected routes

## Running parts separately

### Backend only

```bash
cd backend/api
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd backend/api
.\mvnw.cmd spring-boot:run
```

### Frontend only

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

### Frontend dev with Docker backend

If you want hot reload from Vite but still want to use the backend API, run the backend through Docker and run the frontend locally.

Remote database mode:

```bash
docker compose up backend
```

Local database mode:

```bash
docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml up backend db phpmyadmin
```

Then start the frontend locally:

```bash
cd frontend
npm run dev
```

Open the Vite app in the browser and the frontend will send `/api` requests through the Vite proxy to `http://localhost:8080`.

## CI pipeline

The repository uses GitHub Actions with the workflow file:

- `.github/workflows/ci.yml`

Current CI jobs:
- `backend`: runs `./mvnw test` in `backend/api`
- `frontend`: runs `npm ci` and `npm run build` in `frontend`

Important backend CI notes:
- The workflow includes `chmod +x ./mvnw` because GitHub Actions runs on Linux and the Maven wrapper must be executable there.
- The backend integration tests use H2 and depend on the SQL init files in `backend/api/src/main/resources/schema.sql` and `backend/api/src/main/resources/data.sql`.
- If those SQL files do not match the JPA entity model, CI will fail with table or seed-data errors.

## Current status

- The frontend has a working auth shell with login, registration, session restore, and approval-aware navigation.
- The backend has a working auth slice with registration validation, login, refresh rotation, logout, and current-user lookup.
- Approval management, account APIs, and transfer APIs are still not implemented in the running backend.

For more detail, see:
- [backend/api/README.md](backend/api/README.md)
- [frontend/README.md](frontend/README.md)
