import { clearAuth, getToken } from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const token = getToken()
  const {
    method = 'GET',
    body,
    isForm = false
  } = options

  const headers = {}
  if (!isForm) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body ? (isForm ? body : JSON.stringify(body)) : undefined
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
    }
    throw new Error(result.message || '请求失败')
  }

  return result.data
}

export function getOrganizations() {
  return request('/api/organizations')
}

export function createJoinRequest(payload) {
  return request('/api/join-requests', { method: 'POST', body: payload })
}

export function getMyJoinRequests() {
  return request('/api/join-requests/mine')
}

export function getOrganizationJoinRequests(status) {
  const suffix = status ? `?status=${encodeURIComponent(status)}` : ''
  return request(`/api/join-requests/organization${suffix}`)
}

export function reviewJoinRequest(id, payload) {
  return request(`/api/join-requests/${id}/review`, { method: 'POST', body: payload })
}

export function createActivity(payload) {
  return request('/api/activities', { method: 'POST', body: payload })
}

export function updateActivity(id, payload) {
  return request(`/api/activities/${id}`, { method: 'PUT', body: payload })
}

export function deleteActivity(id) {
  return request(`/api/activities/${id}`, { method: 'DELETE' })
}

export function submitActivity(id) {
  return request(`/api/activities/${id}/submit`, { method: 'POST' })
}

export function getVolunteerActivities() {
  return request('/api/activities/volunteer')
}

export function getOrganizationActivities() {
  return request('/api/activities/organization')
}

export function joinActivity(activityId) {
  return request(`/api/participations/${activityId}`, { method: 'POST' })
}

export function cancelParticipation(activityId) {
  return request(`/api/participations/${activityId}`, { method: 'DELETE' })
}

export function getMyParticipations() {
  return request('/api/participations/mine')
}

export function submitCompletion(payload) {
  return request('/api/completions', { method: 'POST', body: payload })
}

export function getMyCompletions() {
  return request('/api/completions/mine')
}

export function getCompletionReviews(status) {
  const suffix = status ? `?status=${encodeURIComponent(status)}` : ''
  return request(`/api/reviews${suffix}`)
}

export function reviewCompletion(completionId, payload) {
  return request(`/api/reviews/${completionId}`, { method: 'POST', body: payload })
}

export function getMyPointsRecords() {
  return request('/api/points')
}

export function getMyPointsBalance() {
  return request('/api/points/balance')
}

export function createProduct(payload) {
  return request('/api/products', { method: 'POST', body: payload })
}

export function updateProduct(id, payload) {
  return request(`/api/products/${id}`, { method: 'PUT', body: payload })
}

export function deleteProduct(id) {
  return request(`/api/products/${id}`, { method: 'DELETE' })
}

export function submitProduct(id) {
  return request(`/api/products/${id}/submit`, { method: 'POST' })
}

export function getVolunteerProducts() {
  return request('/api/products/volunteer')
}

export function getOrganizationProducts() {
  return request('/api/products/organization')
}

export function redeemProduct(payload) {
  return request('/api/redemptions', { method: 'POST', body: payload })
}

export function getMyRedemptions() {
  return request('/api/redemptions/mine')
}

export function getOrganizationRedemptions() {
  return request('/api/redemptions/organization')
}

export function updateRedemptionStatus(id, payload) {
  return request(`/api/redemptions/${id}/status`, { method: 'POST', body: payload })
}

export function getEvidence(bizType, bizId) {
  return request(`/api/evidences/${bizType}/${bizId}`)
}

export function getOrganizationEvidences() {
  return request('/api/evidences/organization')
}

export function retryEvidence(id) {
  return request(`/api/evidences/${id}/retry`, { method: 'POST' })
}

export async function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request('/api/files/upload', {
    method: 'POST',
    body: formData,
    isForm: true
  })
  return data.path
}
