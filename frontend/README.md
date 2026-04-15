# Frontend

This module contains the Vue frontend for ProjectBankApp.

## Stack

- Vue 3
- Vue Router
- Tailwind CSS
- Vite
- Nginx in the production Docker image

## What is currently implemented

- A reusable navbar component
- Vue Router pages for:
  - `/`
  - `/accounts`
  - `/transfers`
  - `/login`
- A minimal home page that acts as a live API test page for the backend example user endpoints
- Tailwind-based styling instead of the default Vite starter UI

## Folder structure

Main frontend files:

- `src/main.js` mounts the Vue app and router
- `src/App.vue` contains the app shell
- `src/router/index.js` defines the routes
- `src/components/organisms/AppNavbar.vue` contains the navbar
- `src/components/pages/HomePage.vue` contains the live API test page
- `src/components/pages/AccountsPage.vue` is a placeholder page
- `src/components/pages/TransfersPage.vue` is a placeholder page
- `src/components/pages/LoginPage.vue` is a placeholder page
- `src/config.js` contains the example API endpoint path
- `public/favicon.ico` is the site favicon

## Home page API test

The home page is intentionally simple. It is meant to help development, not act like a finished landing page.

It currently supports:
- `GET /api/users`
- `POST /api/users`

The page prints the JSON response directly so teammates can quickly check whether the backend is reachable and returning the expected data.

## Running locally

Install dependencies:

```bash
npm install
```

Start the Vite dev server:

```bash
npm run dev
```

Build for production:

```bash
npm run build
```

## CI

The frontend is part of the GitHub Actions pipeline in:

- `.github/workflows/ci.yml`

The frontend CI job currently does:
- `npm ci`
- `npm run build`

There is no dedicated frontend test script yet, so the production build is the current CI check for frontend changes.

## API routing

In local Vite development, `/api` is proxied to:

```text
http://localhost:8080
```

In Docker, Nginx also proxies `/api` to the backend container. This means frontend code can keep using relative `/api/...` paths in both environments.

## Using `npm run dev` with the Docker backend

You can run the frontend locally with Vite and still use the backend API if the backend container is running and publishing port `8080`.

Example workflow from the project root:

Remote database mode:

```bash
docker compose up backend
```

Local database mode:

```bash
docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml up backend db phpmyadmin
```

Then in the `frontend` folder:

```bash
npm run dev
```

This gives you:
- Vite hot reload for frontend work
- backend API access through `/api`
- the same relative API paths in dev and Docker

## Notes for teammates

- `Accounts`, `Transfers`, and `Login` are placeholder routes for now.
- The current home page is a developer-facing test page, not final product UI.
- When you connect more backend features, keep using the routed page structure instead of putting everything in `App.vue`.
