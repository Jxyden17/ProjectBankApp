import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AccountsView from '../views/AccountsView.vue'
import TransfersView from '../views/TransfersView.vue'
import PendingCustomersView from '../views/PendingCustomersView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import { useAuth } from '../composables/useAuth'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/accounts', name: 'accounts', component: AccountsView },
  { path: '/transfers', name: 'transfers', component: TransfersView },
  {
    path: '/customers/pending',
    name: 'pending-customers',
    component: PendingCustomersView,
    meta: { employeeOnly: true },
  },
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/register', name: 'register', component: RegisterView, meta: { public: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  await auth.bootstrapAuth()

  if (to.meta.public && auth.isAuthenticated.value) {
    return resolveAuthenticatedPublicRedirect(to, auth)
  }

  if (!to.meta.public && !auth.isAuthenticated.value) {
    return loginRedirect(to)
  }

  if (shouldRestrictCustomerToHome(to, auth)) {
    return { name: 'home' }
  }

  if (to.meta.employeeOnly && !auth.isEmployee.value) {
    return { name: 'home' }
  }

  return true
})

const resolveAuthenticatedPublicRedirect = (to, auth) => {
  const redirectTarget =
    typeof to.query.redirect === 'string' && to.query.redirect.startsWith('/')
      ? to.query.redirect
      : '/'

  return auth.isRestrictedCustomer.value && redirectTarget !== '/' ? '/' : redirectTarget
}

const loginRedirect = (to) => ({
  name: 'login',
  query: { redirect: to.fullPath },
})

const shouldRestrictCustomerToHome = (to, auth) =>
  auth.isAuthenticated.value &&
  auth.isRestrictedCustomer.value &&
  to.path !== '/'

export default router
