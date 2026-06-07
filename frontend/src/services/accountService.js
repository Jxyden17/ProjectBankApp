import { request } from './apiClient'

export const getOwnAccounts = (accessToken) =>
  request('/accounts/me', {
    accessToken,
  })

export const listAccounts = (accessToken) =>
  request('/accounts', {
    accessToken,
  })

export const getAccountById = (accountId, accessToken) =>
  request(`/accounts/${accountId}`, {
    accessToken,
  })
