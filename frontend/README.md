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
  - `/accounts/directory`
  - `/accounts/directory/:accountId`
  - `/transfers`
  - `/customers`
  - `/customers/approvals`
  - `/customers/:customerId`
  - `/login`
  - `/register`
- Session bootstrap through `/api/auth/refresh`
- Login and registration forms backed by the backend auth API
- Role-aware home content for customers, pending customers, and employees
- Approval-aware route gating and navigation
- Customer account overview backed by `/api/accounts/me`
- Employee customer overview, customer detail, pending approval, account directory, and account detail screens
- Tailwind-based styling instead of the default Vite starter UI

## Folder structure

Main frontend files:

- `src/main.js` mounts the Vue app and router
- `src/App.vue` contains the app shell
- `src/router/index.js` defines the routes
- `src/composables/useAuth.js` centralizes auth state and access helpers
- `src/services/authService.js` wraps the auth API calls
- `src/services/accountService.js` wraps account API calls
- `src/services/customerService.js` wraps customer API calls
- `src/components/layout/AppHeader.vue` renders the approval-aware navigation
- `src/views/HomeView.vue` shows role-specific customer, pending-customer, and employee home states
- `src/views/LoginView.vue` and `src/views/RegisterView.vue` handle auth forms
- `src/views/AccountsView.vue` shows the authenticated customer's account overview
- `src/views/AccountDirectoryView.vue` and `src/views/AccountDetailView.vue` support employee account lookup
- `src/views/CustomersView.vue`, `src/views/CustomerDetailView.vue`, and `src/views/PendingCustomersView.vue` support employee customer management
- `src/views/TransfersView.vue` is present but is not wired to transaction creation yet
- `public/favicon.ico` is the site favicon

## Route access behavior

- unauthenticated users are redirected to `/login` for protected routes
- `/login` and `/register` are public routes
- authenticated unapproved customers are restricted to `/`
- authenticated approved customers may access `/accounts` and `/transfers`
- authenticated employees may access `/customers`, `/customers/approvals`, `/customers/:customerId`, `/accounts/directory`, and `/accounts/directory/:accountId`
- `/customers/pending` redirects to `/customers/approvals`
- the header hides unavailable links for restricted customers
- the home page shows different content for approved customers, pending customers, and employees

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

- `Transfers` is still a presentational page; connect it to the transaction API before treating it as complete.
- The home page is role-aware and should stay focused on user-facing actions, not implementation details.
- When you connect more backend features, keep using the routed page structure instead of putting everything in `App.vue`.
