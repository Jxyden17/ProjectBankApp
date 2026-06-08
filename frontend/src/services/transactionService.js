import { request } from './apiClient'
import { buildQuery } from './queryString'

export const getTransactions = (params, accessToken) =>
  request(`/transactions${buildQuery(params)}`, {
    accessToken,
  })

export const getTransaction = (transactionId, accessToken) =>
  request(`/transactions/${transactionId}`, {
    accessToken,
  })

export const createTransferTransaction = (payload, accessToken) =>
  request('/transactions/transfers', {
    method: 'POST',
    body: payload,
    accessToken,
  })

export const createDepositTransaction = (payload, accessToken) =>
  request('/transactions/deposits', {
    method: 'POST',
    body: payload,
    accessToken,
  })

export const createWithdrawalTransaction = (payload, accessToken) =>
  request('/transactions/withdrawals', {
    method: 'POST',
    body: payload,
    accessToken,
  })
