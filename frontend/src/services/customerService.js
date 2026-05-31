import { request } from './apiClient'

export const getPendingCustomers = (accessToken) =>
  request('/customers/pending', {
    accessToken,
  })

export const approveCustomer = (customerId, payload, accessToken) =>
  request(`/customers/${customerId}/approval`, {
    method: 'PATCH',
    body: payload,
    accessToken,
  })
