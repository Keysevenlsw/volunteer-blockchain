const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path) {
  const response = await fetch(`${API_BASE_URL}${path}`)
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

export function getPublicInfo() {
  return request('/api/public/info')
}

export function getCompletedProjects(limit = 6) {
  return request(`/api/public/completed-projects?limit=${limit}`)
}

export function getPublicActivities(params = {}) {
  const searchParams = new URLSearchParams()
  const { keyword = '', limit = 60, organizationId = '' } = params
  searchParams.set('limit', String(limit))
  if (keyword) {
    searchParams.set('keyword', keyword)
  }
  if (organizationId) {
    searchParams.set('organizationId', String(organizationId))
  }
  return request(`/api/public/activities?${searchParams.toString()}`)
}

export function getPublicActivity(id) {
  return request(`/api/public/activities/${id}`)
}

export function getPublicActivityRegistrations(id) {
  return request(`/api/public/activities/${id}/registrations`)
}

export function getPublicOrganizations(params = {}) {
  const searchParams = new URLSearchParams()
  const { keyword = '', organizationId = '' } = params
  if (keyword) {
    searchParams.set('keyword', keyword)
  }
  if (organizationId) {
    searchParams.set('organizationId', String(organizationId))
  }
  const suffix = searchParams.toString()
  return request(`/api/public/organizations${suffix ? `?${suffix}` : ''}`)
}

export function getPublicOrganization(id) {
  return request(`/api/public/organizations/${id}`)
}

export function getPublicEvidences(params = {}) {
  const searchParams = new URLSearchParams()
  const { limit = 20, status, organizationId, keyword } = params
  searchParams.set('limit', String(limit))
  if (status) {
    searchParams.set('status', status)
  }
  if (organizationId) {
    searchParams.set('organizationId', String(organizationId))
  }
  if (keyword) {
    searchParams.set('keyword', keyword)
  }
  return request(`/api/public/evidences?${searchParams.toString()}`)
}

export function getPublicEvidence(bizType, bizId) {
  return request(`/api/public/evidences/${bizType}/${bizId}`)
}
