const STATUS_META = {
  joinRequest: {
    pending: { label: '待审核', tone: 'warning' },
    approved: { label: '已通过', tone: 'success' },
    rejected: { label: '已拒绝', tone: 'danger' }
  },
  activity: {
    pending: { label: '待开始', tone: 'warning' },
    ongoing: { label: '进行中', tone: 'primary' },
    completed: { label: '已完成', tone: 'success' }
  },
  completion: {
    pending: { label: '待审核', tone: 'warning' },
    approved: { label: '已通过', tone: 'success' },
    rejected: { label: '已驳回', tone: 'danger' }
  },
  redemption: {
    pending: { label: '待处理', tone: 'warning' },
    fulfilled: { label: '已发放', tone: 'success' },
    cancelled: { label: '已取消', tone: 'neutral' }
  },
  evidence: {
    pending: { label: '待上链', tone: 'warning' },
    success: { label: '已上链', tone: 'success' },
    failed: { label: '上链失败', tone: 'danger' }
  },
  points: {
    earned: { label: '收入', tone: 'success' },
    spent: { label: '支出', tone: 'danger' }
  },
  review: {
    draft: { label: '草稿', tone: 'neutral' },
    pending_review: { label: '待平台审核', tone: 'warning' },
    approved: { label: '已通过', tone: 'success' },
    rejected: { label: '已驳回', tone: 'danger' },
    off_shelf: { label: '已下架', tone: 'neutral' }
  }
}

const ROLE_LABELS = {
  volunteer: '志愿者',
  organization_admin: '公益组织管理员',
  activity_reviewer: '活动审核员',
  product_reviewer: '商品审核员',
  system_admin: '系统管理员'
}

export function getStatusMeta(type, status) {
  const normalizedType = STATUS_META[type] || {}
  return normalizedType[status] || {
    label: status || '未知状态',
    tone: 'neutral'
  }
}

export function getRoleLabel(roleCode) {
  return ROLE_LABELS[roleCode] || roleCode || '未设置'
}

export function formatDate(value) {
  if (!value) {
    return '-'
  }
  return String(value).slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

export function toDateTimeInputValue(value) {
  if (!value) {
    return ''
  }
  return String(value).replace(' ', 'T').slice(0, 19)
}

export function shortHash(value, head = 10, tail = 8) {
  if (!value || value.length <= head + tail + 3) {
    return value || '-'
  }
  return `${value.slice(0, head)}...${value.slice(-tail)}`
}

export function fallbackText(value, fallback = '暂无') {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  return value
}
