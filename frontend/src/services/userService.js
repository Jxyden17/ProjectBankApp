const USERS_ENDPOINT = '/api/users'

const request = async (url, options = {}) => {
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    const message = data?.message ?? `Request failed with status ${response.status}`
    const error = new Error(message)
    error.status = response.status
    error.data = data
    throw error
  }

  return data
}

export const usersEndpoint = USERS_ENDPOINT

export const getUsers = () => request(USERS_ENDPOINT)

export const createUser = (payload) =>
  request(USERS_ENDPOINT, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
