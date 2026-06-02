import { request } from './apiClient'

export const getOwnAccounts = (accessToken) =>
  request('/accounts/me', {
    accessToken,
  })
