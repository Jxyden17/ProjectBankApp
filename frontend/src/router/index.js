import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AccountsView from '../views/AccountsView.vue'
import TransfersView from '../views/TransfersView.vue'
import LoginView from '../views/LoginView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/accounts', name: 'accounts', component: AccountsView },
  { path: '/transfers', name: 'transfers', component: TransfersView },
  { path: '/login', name: 'login', component: LoginView },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
