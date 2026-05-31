import { computed, reactive, readonly } from 'vue'
import * as authService from '../services/authService'
import { USER_ROLE } from '../constants/userRole'

const state = reactive({
  accessToken: '',
  currentUser: null,
  isAuthResolved: false,
})

let bootstrapPromise = null

const clearSession = () => {
  state.accessToken = ''
  state.currentUser = null
}

const hydrateCurrentUser = async (accessToken, fallbackUser = null) => {
  state.accessToken = accessToken ?? ''
  state.currentUser = fallbackUser

  if (state.accessToken && !state.currentUser) {
    state.currentUser = await authService.getCurrentUser(state.accessToken)
  }

  return state.currentUser
}

const bootstrapAuth = async () => {
  if (state.isAuthResolved) {
    return state.currentUser
  }

  if (bootstrapPromise) {
    return bootstrapPromise
  }

  bootstrapPromise = (async () => {
    try {
      const response = await authService.refresh()
      await hydrateCurrentUser(response?.accessToken)
    } catch {
      clearSession()
    } finally {
      state.isAuthResolved = true
      bootstrapPromise = null
    }

    return state.currentUser
  })()

  return bootstrapPromise
}

const login = async (credentials) => {
  const response = await authService.login(credentials)
  await hydrateCurrentUser(response.accessToken, response.user ?? null)
  state.isAuthResolved = true
  return state.currentUser
}

const register = async (payload) => authService.register(payload)

const logout = async () => {
  try {
    await authService.logout()
  } finally {
    clearSession()
    state.isAuthResolved = true
  }
}

const auth = {
  state: readonly(state),
  accessToken: computed(() => state.accessToken),
  currentUser: computed(() => state.currentUser),
  currentUserName: computed(() =>
    [state.currentUser?.firstName, state.currentUser?.lastName].filter(Boolean).join(' '),
  ),
  isAuthenticated: computed(() => Boolean(state.accessToken && state.currentUser)),
  isEmployee: computed(() => state.currentUser?.role === USER_ROLE.EMPLOYEE),
  isApprovedCustomer: computed(
    () => state.currentUser?.role === USER_ROLE.CUSTOMER && state.currentUser?.approved === true,
  ),
  isRestrictedCustomer: computed(
    () => state.currentUser?.role === USER_ROLE.CUSTOMER && state.currentUser?.approved === false,
  ),
  bootstrapAuth,
  login,
  register,
  logout,
}

export const useAuth = () => auth
