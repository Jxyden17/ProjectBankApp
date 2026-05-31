# Frontend

This module contains the Vue frontend for ProjectBankApp.

## Stack

- Vue 3
- Vue Router
- Tailwind CSS
- Vite
- Nginx in the production Docker image

## What is currently implemented

- A reusable authenticated app shell
- Vue Router pages for:
  - `/`
  - `/accounts`
  - `/transfers`
  - `/login`
  - `/register`
- Session bootstrap through `/api/auth/refresh`
- Login and registration forms backed by the backend auth API
- Approval-aware route gating and navigation
- Tailwind-based styling instead of the default Vite starter UI

## Folder structure

Main frontend files:

- `src/main.js` mounts the Vue app and router
- `src/App.vue` contains the app shell
- `src/router/index.js` defines the routes
- `src/composables/useAuth.js` centralizes auth state and access helpers
- `src/services/authService.js` wraps the auth API calls
- `src/components/layout/AppHeader.vue` renders the approval-aware navigation
- `src/views/HomeView.vue` shows the authenticated and pending-approval home states
- `src/views/LoginView.vue` and `src/views/RegisterView.vue` handle auth forms
- `src/views/AccountsView.vue` and `src/views/TransfersView.vue` are still placeholder protected pages
- `public/favicon.ico` is the site favicon

## Route access behavior

- unauthenticated users are redirected to `/login` for protected routes
- `/login` and `/register` are public routes
- authenticated unapproved customers are restricted to `/`
- authenticated approved customers and employees may access `/accounts` and `/transfers`
- the header hides `Accounts` and `Transfers` for restricted customers
- the home page shows a waiting-for-approval state for unapproved customers

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

- `Accounts` and `Transfers` are still placeholder views behind the current route guard.
- The home page is no longer an API playground; it reflects authenticated user state from the backend.
- When you connect more backend features, keep using the routed page structure instead of putting everything in `App.vue`.
