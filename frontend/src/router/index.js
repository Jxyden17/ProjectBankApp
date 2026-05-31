import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AccountsView from '../views/AccountsView.vue'
import TransfersView from '../views/TransfersView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import { useAuth } from '../composables/useAuth'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/accounts', name: 'accounts', component: AccountsView },
  { path: '/transfers', name: 'transfers', component: TransfersView },
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
  const isRestrictedCustomer = auth.isRestrictedCustomer.value

  if (to.meta.public && auth.isAuthenticated.value) {
    const redirectTarget =
      typeof to.query.redirect === 'string' && to.query.redirect.startsWith('/')
        ? to.query.redirect
        : '/'

    return isRestrictedCustomer && redirectTarget !== '/' ? '/' : redirectTarget
  }

  if (!to.meta.public && !auth.isAuthenticated.value) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (
    auth.isAuthenticated.value &&
    isRestrictedCustomer &&
    to.path !== '/'
  ) {
    return { name: 'home' }
  }

  return true
})

export default router
