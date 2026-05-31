import { request } from './apiClient'

export const register = (payload) =>
  request('/auth/register', {
    method: 'POST',
    body: payload,
  })

export const login = (payload) =>
  request('/auth/login', {
    method: 'POST',
    body: payload,
  })

export const refresh = () =>
  request('/auth/refresh', {
    method: 'POST',
  })

export const logout = () =>
  request('/auth/logout', {
    method: 'POST',
  })

export const getCurrentUser = (accessToken) =>
  request('/users/me', {
    accessToken,
  })
