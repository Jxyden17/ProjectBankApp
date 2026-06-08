import { request } from './apiClient'
import { buildQuery } from './queryString'

// Loads the employee customer overview with filters and pagination.
export const getCustomers = (params, accessToken) =>
  request(`/customers${buildQuery(params)}`, {
    accessToken,
  })

// Loads one employee-visible customer profile.
export const getCustomer = (customerId, accessToken) =>
  request(`/customers/${customerId}`, {
    accessToken,
  })

// Loads customers waiting for employee approval.
export const getPendingCustomers = (accessToken, params = {}) =>
  request(`/customers/pending${buildQuery(params)}`, {
    accessToken,
  })

// Sends an approval request that also creates the customer's accounts.
export const approveCustomer = (customerId, payload, accessToken) =>
  request(`/customers/${customerId}/approval`, {
    method: 'PATCH',
    body: payload,
    accessToken,
  })

export const lookupCustomerIban = (params, accessToken) =>
  request(`/customers/lookup${buildQuery(params)}`, {
    accessToken,
  })
