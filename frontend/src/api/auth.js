const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const TOKEN_KEY = 'volunteer_token'
export const USER_KEY = 'volunteer_user'
export const LOGIN_NOTICE_KEY = 'volunteer_login_notice'

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

function normalizeCode(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function normalizeCodeList(values, preferredFirstCode = null) {
  const normalized = []
  const seen = new Set()

  const append = (value) => {
    const code = normalizeCode(value)
    if (!code || seen.has(code)) {
      return
    }
    seen.add(code)
    normalized.push(code)
  }

  append(preferredFirstCode)
  if (Array.isArray(values)) {
    values.forEach(append)
  }

  return normalized
}

export function normalizeUser(user) {
  if (!user || typeof user !== 'object') {
    return null
  }

  const primaryRole = normalizeCode(user.primaryRole) || normalizeCode(user.role)
  const roles = normalizeCodeList(user.roles, primaryRole)
  const permissions = normalizeCodeList(user.permissions)
  const resolvedPrimaryRole = primaryRole || roles[0] || null

  return {
    ...user,
    role: resolvedPrimaryRole,
    primaryRole: resolvedPrimaryRole,
    roles,
    permissions
  }
}

export function hasRole(roleCode, user = getCachedUser()) {
  const normalizedRole = normalizeCode(roleCode)
  const normalizedUser = normalizeUser(user)
  return !!normalizedRole && !!normalizedUser && normalizedUser.roles.includes(normalizedRole)
}

export function hasPermission(permissionCode, user = getCachedUser()) {
  const normalizedPermission = normalizeCode(permissionCode)
  const normalizedUser = normalizeUser(user)
  return !!normalizedPermission && !!normalizedUser && normalizedUser.permissions.includes(normalizedPermission)
}

export function getWorkspaceRoute(user = getCachedUser()) {
  const normalizedUser = normalizeUser(user)
  if (!normalizedUser) {
    return '/'
  }
  if (hasRole('system_admin', normalizedUser) || normalizedUser.primaryRole === 'system_admin') {
    return '/admin'
  }
  if (
    hasRole('activity_reviewer', normalizedUser) ||
    hasRole('product_reviewer', normalizedUser) ||
    normalizedUser.primaryRole === 'activity_reviewer' ||
    normalizedUser.primaryRole === 'product_reviewer'
  ) {
    return '/reviewer'
  }
  if (hasRole('organization_admin', normalizedUser) || normalizedUser.primaryRole === 'organization_admin') {
    return '/organization-workbench'
  }
  if (hasRole('volunteer', normalizedUser) || normalizedUser.primaryRole === 'volunteer') {
    return '/profile'
  }
  return '/'
}

export async function register(payload) {
  const user = await request('/api/auth/register', {
    method: 'POST',
    body: payload
  })
  return normalizeUser(user)
}

export async function login(payload) {
  const authData = await request('/api/auth/login', {
    method: 'POST',
    body: payload
  })
  return {
    ...authData,
    user: normalizeUser(authData?.user)
  }
}

export async function getCurrentUser(token) {
  const user = await request('/api/auth/me', {
    token
  })
  return normalizeUser(user)
}

export function saveAuth(authResponse) {
  if (authResponse?.token) {
    localStorage.setItem(TOKEN_KEY, authResponse.token)
  }

  const normalizedUser = normalizeUser(authResponse?.user)
  if (normalizedUser) {
    localStorage.setItem(USER_KEY, JSON.stringify(normalizedUser))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function setLoginNotice(message = '请登录后再试！') {
  try {
    sessionStorage.setItem(LOGIN_NOTICE_KEY, message)
  } catch (error) {
    // ignore storage errors
  }
}

export function consumeLoginNotice() {
  try {
    const message = sessionStorage.getItem(LOGIN_NOTICE_KEY)
    if (message) {
      sessionStorage.removeItem(LOGIN_NOTICE_KEY)
    }
    return message || ''
  } catch (error) {
    return ''
  }
}

export function redirectToLogin(role = 'volunteer', message = '请登录后再试！') {
  setLoginNotice(message)
  const query = role === 'organization_admin' ? '?role=organization_admin' : '?role=volunteer'
  if (typeof window !== 'undefined') {
    window.location.href = `/login${query}`
  }
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
    return normalizeUser(JSON.parse(raw))
  } catch (error) {
    clearAuth()
    return null
  }
}
