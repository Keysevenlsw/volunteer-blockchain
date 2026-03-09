const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const TOKEN_KEY = 'volunteer_token'
export const USER_KEY = 'volunteer_user'

async function request(path, options = {}) {
  const { method = 'GET', body, token } = options
  const headers = {
    'Content-Type': 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })

  let result = null
  try {
    result = await response.json()
  } catch (error) {
    throw new Error('服务端返回格式错误')
  }

  if (!response.ok || !result.success) {
    throw new Error(result.message || '请求失败')
  }

  return result.data
}

export function register(payload) {
  return request('/api/auth/register', {
    method: 'POST',
    body: payload
  })
}

export function login(payload) {
  return request('/api/auth/login', {
    method: 'POST',
    body: payload
  })
}

export function getCurrentUser(token) {
  return request('/api/auth/me', {
    token
  })
}

export function saveAuth(authResponse) {
  localStorage.setItem(TOKEN_KEY, authResponse.token)
  localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user || {}))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getCachedUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    clearAuth()
    return null
  }
}
