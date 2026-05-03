import { clearAuth, getToken, redirectToLogin } from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const token = getToken()
  const { method = 'GET', body } = options
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
    if (response.status === 401 || (result.message || '').includes('登录')) {
      clearAuth()
      redirectToLogin('volunteer')
    }
    throw new Error(result.message || '请求失败')
  }

  return result.data
}

export function getAdminUsers() {
  return request('/api/admin/users')
}

export function getAdminUserRoles(userId) {
  return request(`/api/admin/users/${userId}/roles`)
}

export function assignAdminUserRoles(userId, payload) {
  return request(`/api/admin/users/${userId}/roles`, {
    method: 'PUT',
    body: payload
  })
}

export function getAdminRoles() {
  return request('/api/admin/roles')
}

export function getAdminRolePermissions(roleCode) {
  return request(`/api/admin/roles/${roleCode}/permissions`)
}

export function getAdminPermissions() {
  return request('/api/admin/permissions')
}

export function getActivityReviews(status) {
  const suffix = status ? `?status=${encodeURIComponent(status)}` : ''
  return request(`/api/admin/activity-reviews${suffix}`)
}

export function reviewActivity(activityId, payload) {
  return request(`/api/admin/activity-reviews/${activityId}`, {
    method: 'POST',
    body: payload
  })
}

export function getProductReviews(status) {
  const suffix = status ? `?status=${encodeURIComponent(status)}` : ''
  return request(`/api/admin/product-reviews${suffix}`)
}

export function reviewProduct(productId, payload) {
  return request(`/api/admin/product-reviews/${productId}`, {
    method: 'POST',
    body: payload
  })
}

export function getPointsRules() {
  return request('/api/admin/points-rules')
}

export function createPointsRule(payload) {
  return request('/api/admin/points-rules', {
    method: 'POST',
    body: payload
  })
}

export function updatePointsRule(id, payload) {
  return request(`/api/admin/points-rules/${id}`, {
    method: 'PUT',
    body: payload
  })
}

export function syncPointBalances() {
  return request('/api/admin/points/sync-balances', {
    method: 'POST'
  })
}

export function migratePointsToChain() {
  return request('/api/admin/points/migrate', {
    method: 'POST'
  })
}
