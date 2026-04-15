import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../components/pages/HomePage.vue'
import AccountsPage from '../components/pages/AccountsPage.vue'
import TransfersPage from '../components/pages/TransfersPage.vue'
import LoginPage from '../components/pages/LoginPage.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
  },
  {
    path: '/accounts',
    name: 'accounts',
    component: AccountsPage,
  },
  {
    path: '/transfers',
    name: 'transfers',
    component: TransfersPage,
  },
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
