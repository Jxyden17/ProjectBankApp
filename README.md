# ProjectBankApp

ProjectBankApp is a student banking demo with:
- a Spring Boot backend in `backend/api`
- a Vue 3 frontend in `frontend`
- Docker Compose setups for a remote database flow and a local MariaDB flow

The repository currently includes one example backend feature, `User`, and a frontend home page that can call the example API directly.

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

Current example endpoints:
- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`

There are unit tests for controller and service logic, a functional controller test, and a repository test under `backend/api/src/test/java`.

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

The home page is intentionally minimal and acts as a small API playground for:
- `GET /api/users`
- `POST /api/users`

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

## Current status

- The project has a working frontend shell with a navbar and routed pages.
- The backend has a working example `User` feature with controller, service, repository, DTO, and JPA entity layers.
- The backend is still an example foundation, not a finished banking domain yet.

For more detail, see:
- [backend/api/README.md](backend/api/README.md)
- [frontend/README.md](frontend/README.md)
