const API_BASE = '/api'

const buildHeaders = ({ body, accessToken, headers = {} }) => ({
  Accept: 'application/json',
  ...(body ? { 'Content-Type': 'application/json' } : {}),
  ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  ...headers,
})

const parseResponseBody = async (response) => {
  if (response.status === 204) {
    return null
  }

  return response.json().catch(() => null)
}

export const request = async (path, { method = 'GET', body, accessToken, headers } = {}) => {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    credentials: 'include',
    headers: buildHeaders({ body, accessToken, headers }),
    ...(body ? { body: JSON.stringify(body) } : {}),
  })

  const data = await parseResponseBody(response)

  if (!response.ok) {
    const message = data?.message ?? data?.error ?? `Request failed with status ${response.status}`
    const error = new Error(message)
    error.status = response.status
    error.data = data
    throw error
  }

  return data
}
