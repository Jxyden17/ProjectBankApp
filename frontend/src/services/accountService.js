import { request } from './apiClient'
import { buildQuery } from './queryString'

export const getOwnAccounts = (accessToken) =>
  request('/accounts/me', {
    accessToken,
  })

export const listAccounts = (accessToken, params = {}) =>
  request(`/accounts${buildQuery(params)}`, {
    accessToken,
  })

export const getAccountById = (accountId, accessToken) =>
  request(`/accounts/${accountId}`, {
    accessToken,
  })

export const updateAccount = (accountId, payload, accessToken) =>
  request(`/accounts/${accountId}`, {
    method: 'PATCH',
    body: payload,
    accessToken,
  })
