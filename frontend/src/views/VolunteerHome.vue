<template>
  <PortalLayout active-key="home" breadcrumb="个人中心">
    <section class="profile-page">
      <div class="portal-shell profile-layout" v-loading="pageLoading">
        <aside class="profile-sidebar">
          <div v-for="group in profileMenus" :key="group.title" class="profile-menu-group">
            <div class="profile-menu-group-title">{{ group.title }}</div>
            <button
              v-for="item in group.children"
              :key="item.key"
              type="button"
              class="profile-menu-item"
              :class="{ active: activeMenu === item.key }"
              @click="activeMenu = item.key"
            >
              {{ item.title }}
            </button>
          </div>
        </aside>

        <main class="profile-main">
          <section v-if="activeMenu === 'home'" class="profile-card profile-hero">
            <div class="avatar-box">
              <div class="avatar-circle">
                <img v-if="avatarUrl" :src="avatarUrl" :alt="user?.username || '志愿者头像'" />
                <span v-else>{{ user?.username?.slice(0, 1) || '志' }}</span>
              </div>
              <el-upload
                class="avatar-upload"
                accept="image/*"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <el-button type="primary" plain :loading="avatarUploading">编辑头像</el-button>
              </el-upload>
            </div>
            <div class="profile-info">
              <h1>{{ user?.username || '志愿者' }}</h1>
              <p><span>账号</span> 志愿者号：{{ user?.userId || user?.email || '-' }}</p>
              <p>{{ user?.email || '-' }} · 所属组织：{{ organizationText }}</p>
              <div class="quick-actions">
                <el-button type="primary" @click="router.push('/activities')">去参与活动</el-button>
                <el-button @click="router.push('/organizations')">申请加入组织</el-button>
              </div>
            </div>
            <div class="profile-stats">
              <div><span>{{ participationCount }}</span><p>参与活动</p></div>
              <div><span>{{ serviceHoursDisplay }}</span><p>总服务时长（小时）</p></div>
              <div><span>{{ volunteerDays }}</span><p>成为志愿者（天）</p></div>
              <div><span>{{ currentPoints }}</span><p>公益积分</p></div>
            </div>
          </section>

          <section v-else-if="activeMenu === 'profile'" class="profile-card">
            <h2>志愿者档案</h2>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户名">{{ user?.username || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ user?.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="归属组织">{{ user?.organizationName || '暂未加入组织' }}</el-descriptions-item>
              <el-descriptions-item label="当前积分">{{ user?.totalPoints ?? 0 }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section v-else-if="activeMenu === 'password'" class="profile-card profile-edit-card">
            <div class="profile-section-head">
              <div>
                <h2>修改个人信息</h2>
              </div>
            </div>
            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="92px"
              class="profile-form"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model.trim="profileForm.username" maxlength="100" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model.trim="profileForm.email" maxlength="100" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存修改</el-button>
                <el-button @click="resetProfileForm">重置</el-button>
              </el-form-item>
            </el-form>
          </section>

          <section v-else-if="activeMenu === 'change-password'" class="profile-card profile-edit-card">
            <div class="profile-section-head">
              <div>
                <h2>修改密码</h2>
              </div>
            </div>
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="92px"
              class="profile-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model.trim="passwordForm.oldPassword" type="password" maxlength="50" show-password placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model.trim="passwordForm.newPassword" type="password" maxlength="50" show-password placeholder="请输入新密码" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model.trim="passwordForm.confirmPassword" type="password" maxlength="50" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">修改密码</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </section>

          <section v-else-if="activeMenu === 'join'" class="profile-card">
            <template v-if="myOrganization">
              <div class="profile-section-head">
                <div>
                  <h2>我的组织</h2>
                </div>
                <el-button type="danger" plain :loading="leavingOrganization" @click="handleLeaveOrganization">退出组织</el-button>
              </div>
              <article class="organization-card">
                <div class="organization-avatar">
                  <img v-if="myOrganizationAvatarUrl" :src="myOrganizationAvatarUrl" :alt="myOrganization.organizationName" />
                  <span v-else>{{ myOrganization.organizationName?.slice(0, 1) || '组' }}</span>
                </div>
                <div class="organization-main">
                  <h3>{{ myOrganization.organizationName }}</h3>
                  <p>{{ myOrganization.organizationDescription || '该组织暂未填写组织信息。' }}</p>
                  <div class="organization-stats">
                    <div><strong>{{ myOrganization.volunteerCount ?? 0 }}</strong><span>组织人数</span></div>
                    <div><strong>{{ myOrganization.publicActivityCount ?? 0 }}</strong><span>公开活动</span></div>
                  </div>
                </div>
              </article>
            </template>
            <template v-else>
              <h2>我的组织</h2>
              <el-table :data="myJoinRequests" empty-text="暂无组织申请">
                <el-table-column prop="organizationName" label="组织" min-width="160" />
                <el-table-column prop="applyReason" label="申请说明" min-width="220" show-overflow-tooltip />
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <StatusBadge :label="getStatusMeta('joinRequest', row.status).label" :tone="getStatusMeta('joinRequest', row.status).tone" />
                  </template>
                </el-table-column>
                <el-table-column label="申请时间" min-width="160">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column prop="reviewNote" label="审核备注" min-width="180" show-overflow-tooltip />
              </el-table>
            </template>
          </section>

          <section v-else-if="activeMenu === 'registrations'" class="profile-card">
            <h2>我的报名</h2>
            <el-table :data="myParticipations" empty-text="暂无报名记录">
              <el-table-column prop="activityName" label="活动" min-width="180" />
              <el-table-column prop="organizationName" label="组织" min-width="160" />
              <el-table-column label="报名时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.participationDate) }}</template>
              </el-table-column>
              <el-table-column label="报告状态" width="130">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('completion', row.completionStatus || 'pending').label" :tone="getStatusMeta('completion', row.completionStatus || 'pending').tone" />
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else-if="activeMenu === 'services'" class="profile-card">
            <h2>服务明细与链上存证</h2>
            <div class="service-list">
              <article v-for="item in myCompletions" :key="item.completionId" class="service-card">
                <div>
                  <h3>{{ item.activityName }}</h3>
                  <p>{{ item.serviceLocation || '-' }} · {{ formatDateTime(item.submissionDate) }}</p>
                  <p>{{ item.reportText || '暂无报告总结' }}</p>
                </div>
                <div class="service-side">
                  <StatusBadge :label="getStatusMeta('completion', item.status).label" :tone="getStatusMeta('completion', item.status).tone" />
                  <StatusBadge :label="getStatusMeta('evidence', item.evidenceStatus || 'pending').label" :tone="getStatusMeta('evidence', item.evidenceStatus || 'pending').tone" />
                  <span>{{ shortHash(item.txHash || item.digest, 10, 8) }}</span>
                </div>
              </article>
            </div>
            <EmptyState v-if="!myCompletions.length" mark="服" title="暂无服务明细" description="完成报告审核后会在这里形成服务记录。" />
          </section>

          <section v-else-if="activeMenu === 'points'" class="profile-card points-card">
            <div class="profile-section-head">
              <div>
                <h2>积分流水</h2>
              </div>
              <el-button type="primary" plain @click="refreshAll">刷新</el-button>
            </div>
            <div class="points-overview">
              <div>
                <span>当前积分</span>
                <strong>{{ currentPoints }}</strong>
              </div>
              <div>
                <span>链上余额</span>
                <strong>{{ pointsBalance?.chainBalance ?? '-' }}</strong>
              </div>
              <div>
                <span>数据一致性</span>
                <strong :class="{ danger: pointsBalance && !pointsBalance.consistent }">
                  {{ pointsBalance ? (pointsBalance.consistent ? '一致' : '不一致') : '-' }}
                </strong>
              </div>
              <div>
                <span>最近检查</span>
                <strong>{{ pointsBalance?.checkedAt ? formatDateTime(pointsBalance.checkedAt) : '-' }}</strong>
              </div>
            </div>
            <el-table class="points-table" :data="pointsRecords" empty-text="暂无积分流水">
              <el-table-column label="来源" min-width="220">
                <template #default="{ row }">
                  <div class="points-source">
                    <strong>{{ row.source || '-' }}</strong>
                    <span>{{ row.organizationName || '平台积分账户' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="积分变动" width="120">
                <template #default="{ row }">
                  <span class="points-amount" :class="{ spent: row.transactionType === 'spent' }">{{ formatPoints(row) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="110">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('points', row.transactionType).label" :tone="getStatusMeta('points', row.transactionType).tone" />
                </template>
              </el-table-column>
              <el-table-column label="链上状态" min-width="160">
                <template #default="{ row }">
                  <div class="chain-cell">
                    <StatusBadge :label="getStatusMeta('evidence', row.onchainStatus || 'success').label" :tone="getStatusMeta('evidence', row.onchainStatus || 'success').tone" />
                    <span>{{ shortHash(row.txHash || row.digest, 8, 6) }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="chainBalanceAfter" label="变动后余额" width="120" />
              <el-table-column label="时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else-if="activeMenu === 'redemptions'" class="profile-card">
            <h2>兑换记录</h2>
            <el-table :data="myRedemptions" empty-text="暂无兑换记录">
              <el-table-column prop="productName" label="商品" min-width="180" />
              <el-table-column prop="organizationName" label="履约组织" min-width="160" />
              <el-table-column prop="pointsCost" label="消耗积分" width="100" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('redemption', row.status).label" :tone="getStatusMeta('redemption', row.status).tone" />
                </template>
              </el-table-column>
              <el-table-column label="申请时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else class="profile-card">
            <h2>{{ currentMenuLabel }}</h2>
            <EmptyState mark="建" title="模块建设中" description="该模块会随业务接口补充继续完善。" />
          </section>
        </main>
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import {
  clearAuth,
  getCachedUser,
  getCurrentUser,
  getToken,
  redirectToLogin,
  saveAuth,
  changeCurrentUserPassword,
  updateCurrentUser,
  uploadCurrentUserAvatar
} from '../api/auth'
import {
  getMyCompletions,
  getMyJoinRequests,
  getMyOrganization,
  getMyParticipations,
  getMyPointsBalance,
  getMyPointsRecords,
  getMyRedemptions,
  leaveMyOrganization
} from '../api/platform'
import { formatDateTime, getStatusMeta, shortHash } from '../utils/ui'

const router = useRouter()
const pageLoading = ref(false)
const activeMenu = ref('home')
const user = ref(getCachedUser())
const myJoinRequests = ref([])
const myOrganization = ref(null)
const myParticipations = ref([])
const myCompletions = ref([])
const pointsRecords = ref([])
const pointsBalance = ref(null)
const myRedemptions = ref([])
const profileFormRef = ref(null)
const profileSaving = ref(false)
const passwordFormRef = ref(null)
const passwordSaving = ref(false)
const avatarUploading = ref(false)
const leavingOrganization = ref(false)
const profileForm = reactive({
  username: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const confirmPasswordRule = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const profileRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 100, message: '用户名长度必须在 2 到 100 之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 6, max: 50, message: '原密码长度必须在 6 到 50 之间', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '新密码长度必须在 6 到 50 之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: confirmPasswordRule, trigger: 'blur' }]
}

const profileMenus = [
  {
    title: '个人中心',
    type: 'group',
    children: [
      { key: 'home', title: '个人主页', path: '/profile' },
      { key: 'password', title: '修改个人信息', path: '/profile/profile-edit' },
      { key: 'change-password', title: '修改密码', path: '/profile/password' }
    ]
  },
  {
    title: '我的活动',
    type: 'group',
    children: [
      { key: 'registrations', title: '我的报名', path: '/profile/registrations' },
      { key: 'join', title: '我的组织', path: '/profile/organization' },
      { key: 'services', title: '服务明细', path: '/profile/service-records' },
      { key: 'points', title: '积分流水', path: '/profile/points' },
      { key: 'redemptions', title: '兑换记录', path: '/profile/exchanges' }
    ]
  }
]

const currentMenuLabel = computed(() => {
  const menus = profileMenus.flatMap((group) => group.children)
  return menus.find((item) => item.key === activeMenu.value)?.title || '个人中心'
})
const avatarUrl = computed(() => resolveImage(user.value?.avatarPath))
const myOrganizationAvatarUrl = computed(() => resolveImage(myOrganization.value?.avatarPath))
const participationCount = computed(() => myParticipations.value.length)
const approvedCompletions = computed(() => myCompletions.value.filter((item) => item.status === 'approved'))
const serviceHours = computed(() =>
  approvedCompletions.value.reduce((total, item) => total + calculateServiceHours(item), 0)
)
const serviceHoursDisplay = computed(() => {
  const value = serviceHours.value
  return Number.isInteger(value) ? value : value.toFixed(1)
})
const volunteerDays = computed(() => {
  if (!user.value?.joinDate) {
    return 1
  }
  const diff = Date.now() - new Date(user.value.joinDate).getTime()
  return Math.max(1, Math.floor(diff / 86400000))
})
const organizationText = computed(() => user.value?.organizationName || '暂未加入组织')
const currentPoints = computed(() => {
  if (pointsBalance.value?.cachedBalance !== undefined && pointsBalance.value?.cachedBalance !== null) {
    return pointsBalance.value.cachedBalance
  }
  if (user.value?.totalPoints !== undefined && user.value?.totalPoints !== null) {
    return user.value.totalPoints
  }
  return pointsRecords.value.reduce((total, item) => {
    const points = Number(item.points || 0)
    return item.transactionType === 'spent' ? total - points : total + points
  }, 0)
})

onMounted(initializePage)

async function initializePage() {
  pageLoading.value = true
  try {
    await loadProfile()
    await refreshAll()
  } finally {
    pageLoading.value = false
  }
}

async function loadProfile() {
  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('volunteer')
    return
  }
  try {
    const profile = await getCurrentUser(token)
    user.value = profile
    saveAuth({ token, user: profile })
    fillProfileForm(profile)
  } catch (error) {
    clearAuth()
    ElMessage.error(error.message || '登录状态已失效')
    redirectToLogin('volunteer')
  }
}

async function refreshAll() {
  try {
    const [joinData, participationData, completionData, pointsData, balanceData, redemptionData] = await Promise.all([
      getMyJoinRequests().catch(() => []),
      getMyParticipations().catch(() => []),
      getMyCompletions().catch(() => []),
      getMyPointsRecords().catch(() => []),
      getMyPointsBalance().catch(() => null),
      getMyRedemptions().catch(() => [])
    ])
    myJoinRequests.value = joinData
    myOrganization.value = await getMyOrganization().catch(() => null)
    myParticipations.value = participationData
    myCompletions.value = completionData
    pointsRecords.value = pointsData
    pointsBalance.value = balanceData
    myRedemptions.value = redemptionData
  } catch (error) {
    ElMessage.error(error.message || '数据加载失败')
  }
}

function fillProfileForm(profile = user.value) {
  profileForm.username = profile?.username || ''
  profileForm.email = profile?.email || ''
}

function resetProfileForm() {
  fillProfileForm()
  profileFormRef.value?.clearValidate()
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

async function handleSaveProfile() {
  if (!profileFormRef.value) {
    return
  }

  try {
    await profileFormRef.value.validate()
    await ElMessageBox.confirm('确认修改个人信息吗？', '修改确认', {
      confirmButtonText: '确认修改',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('volunteer')
    return
  }

  profileSaving.value = true
  try {
    const payload = { username: profileForm.username, email: profileForm.email }
    const profile = await updateCurrentUser(token, payload)
    user.value = profile
    saveAuth({ token, user: profile })
    fillProfileForm(profile)
    ElMessage.success('个人信息已更新')
  } catch (error) {
    ElMessage.error(error.message || '个人信息更新失败')
  } finally {
    profileSaving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) {
    return
  }
  try {
    await passwordFormRef.value.validate()
    await ElMessageBox.confirm('确认修改密码吗？修改后请使用新密码登录。', '修改确认', {
      confirmButtonText: '确认修改',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('volunteer')
    return
  }

  passwordSaving.value = true
  try {
    await changeCurrentUserPassword(token, {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    resetPasswordForm()
    ElMessage.success('密码已修改')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

function beforeAvatarUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('头像文件必须是图片')
    return false
  }
  if (file.size / 1024 / 1024 > 3) {
    ElMessage.error('头像图片不能超过 3MB')
    return false
  }
  return true
}

async function handleAvatarUpload({ file, onSuccess, onError }) {
  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('volunteer')
    return
  }

  avatarUploading.value = true
  try {
    const profile = await uploadCurrentUserAvatar(token, file)
    user.value = profile
    saveAuth({ token, user: profile })
    onSuccess?.(profile)
    ElMessage.success('头像已更新')
  } catch (error) {
    onError?.(error)
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function handleLeaveOrganization() {
  try {
    await ElMessageBox.confirm(
      `确认退出“${myOrganization.value?.organizationName || '当前组织'}”吗？`,
      '退出组织',
      {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  leavingOrganization.value = true
  try {
    await leaveMyOrganization()
    await loadProfile()
    await refreshAll()
    ElMessage.success('已退出组织')
  } catch (error) {
    ElMessage.error(error.message || '退出组织失败')
  } finally {
    leavingOrganization.value = false
  }
}

function calculateServiceHours(item) {
  const start = new Date(item.serviceStartTime).getTime()
  const end = new Date(item.serviceEndTime).getTime()
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) {
    return 0
  }
  return Math.round(((end - start) / 3600000) * 10) / 10
}

function formatPoints(row) {
  const points = Number(row.points || 0)
  return `${row.transactionType === 'spent' ? '-' : '+'}${points}`
}

function resolveImage(path) {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//.test(path)) {
    return path
  }
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return path.startsWith('/') ? `${baseUrl}${path}` : `${baseUrl}/${path}`
}
</script>

<style scoped>
.profile-page {
  padding-bottom: 72px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 22px;
}

.profile-sidebar {
  overflow: hidden;
  border: 1px solid #f1eeee;
  background: #fff;
}

.profile-menu-group + .profile-menu-group {
  border-top: 1px solid #f6f2f2;
}

.profile-menu-group-title,
.profile-menu-item {
  width: 100%;
  border: 0;
  background: #fff;
  text-align: left;
}

.profile-menu-group-title {
  display: flex;
  align-items: center;
  min-height: 42px;
  border-left: 3px solid #d92727;
  padding: 0 22px;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.profile-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 40px;
  border-left: 3px solid transparent;
  padding: 0 24px 0 38px;
  color: #333;
  cursor: pointer;
  font-size: 15px;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.profile-menu-item:hover {
  background: #fff7f7;
  color: #d92727;
}

.profile-menu-item.active {
  border-left-color: #d92727;
  background: #fff1f1;
  color: #d92727;
  font-weight: 500;
}

.profile-main {
  min-height: 520px;
}

.profile-card {
  padding: 30px;
  border: 1px solid rgba(223, 0, 27, 0.08);
  border-radius: 8px;
  background: #fff;
}

.profile-card h2 {
  margin: 0 0 22px;
  font-size: 22px;
}

.profile-hero {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 28px;
}

.avatar-box {
  display: grid;
  justify-items: center;
}

.avatar-circle {
  width: 150px;
  height: 150px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(180deg, #ffc29f, #ffe0d3);
  color: #fff;
  font-size: 58px;
  font-weight: 700;
  box-shadow: 0 10px 22px rgba(161, 43, 61, 0.16);
}

.avatar-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload {
  margin-top: 14px;
}

.avatar-upload :deep(.el-button) {
  min-width: 116px;
  border-color: #df001b;
  color: #df001b;
}

.avatar-upload :deep(.el-button:hover) {
  border-color: #d92727;
  background: #fff1f1;
  color: #d92727;
}

.profile-info h1 {
  margin: 14px 0 10px;
  font-size: 26px;
}

.profile-info p {
  margin: 8px 0;
  color: #666;
}

.profile-info p span {
  padding: 2px 6px;
  margin-right: 8px;
  border-radius: 4px;
  background: #ffdee2;
  color: #df001b;
  font-size: 12px;
}

.quick-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.profile-stats {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  padding-top: 28px;
  border-top: 1px dashed #e6e6e6;
}

.profile-stats div,
.balance-row div {
  padding: 18px;
  border-radius: 8px;
  background: #fff7f8;
}

.profile-stats span,
.balance-row strong {
  display: block;
  color: #555;
  font-size: 34px;
  font-weight: 700;
}

.profile-stats p,
.balance-row span {
  margin: 8px 0 0;
  color: #777;
}

.service-list {
  display: grid;
  gap: 16px;
}

.service-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(223, 0, 27, 0.08);
  border-radius: 8px;
  background: #fffafa;
}

.service-card h3 {
  margin: 0 0 10px;
}

.service-card p {
  margin: 8px 0 0;
  color: #666;
  line-height: 1.7;
}

.service-side {
  min-width: 180px;
  display: grid;
  align-content: start;
  gap: 10px;
  justify-items: end;
}

.profile-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.profile-section-head h2 {
  margin-bottom: 8px;
}

.profile-section-head p {
  margin: 0;
  color: #777;
  line-height: 1.6;
}

.profile-edit-card {
  max-width: 760px;
}

.profile-form {
  margin-top: 24px;
  padding: 24px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: linear-gradient(180deg, #fffafa 0%, #fff 48%);
}

.profile-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #df001b inset;
}

.profile-form :deep(.el-button--primary) {
  border-color: #df001b;
  background: #df001b;
}

.organization-card {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
  padding: 24px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: linear-gradient(180deg, #fffafa 0%, #fff 58%);
}

.organization-avatar {
  width: 132px;
  height: 132px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe2e5, #fff3e8);
  color: #df001b;
  font-size: 46px;
  font-weight: 700;
}

.organization-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.organization-main h3 {
  margin: 0;
  color: #2f1e23;
  font-size: 22px;
}

.organization-main p {
  margin: 12px 0 0;
  color: #666;
  line-height: 1.8;
}

.organization-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 160px));
  gap: 14px;
  margin-top: 20px;
}

.organization-stats div {
  padding: 14px 16px;
  border-radius: 8px;
  background: #fff7f8;
}

.organization-stats strong {
  display: block;
  color: #333;
  font-size: 26px;
}

.organization-stats span {
  display: block;
  margin-top: 6px;
  color: #777;
}

.balance-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.points-card {
  padding: 28px;
}

.points-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.points-overview div {
  min-height: 108px;
  padding: 18px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: linear-gradient(180deg, #fff7f7, #fff);
}

.points-overview span {
  display: block;
  color: #7b6a6a;
  font-size: 14px;
}

.points-overview strong {
  display: block;
  margin-top: 12px;
  color: #2f1e23;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
}

.points-overview strong.danger {
  color: #d92727;
}

.points-table {
  border: 1px solid #f1eeee;
  border-radius: 8px;
  overflow: hidden;
}

.points-table :deep(.el-table__header th) {
  background: #fff7f7;
  color: #5c4448;
  font-weight: 600;
}

.points-source {
  display: grid;
  gap: 6px;
}

.points-source strong {
  color: #2f1e23;
  font-weight: 600;
}

.points-source span,
.chain-cell span {
  color: #8a7a7d;
  font-size: 13px;
}

.points-amount {
  color: #178a47;
  font-size: 18px;
  font-weight: 700;
}

.points-amount.spent {
  color: #d92727;
}

.chain-cell {
  display: grid;
  gap: 6px;
}

.points-card :deep(.el-button--primary.is-plain) {
  border-color: #f2b7b7;
  background: #fff7f7;
  color: #d92727;
}

.points-card :deep(.el-button--primary.is-plain:hover) {
  border-color: #d92727;
  background: #d92727;
  color: #fff;
}

@media (max-width: 980px) {
  .profile-layout,
  .profile-hero,
  .profile-stats,
  .balance-row,
  .points-overview {
    grid-template-columns: 1fr;
  }

  .service-card {
    flex-direction: column;
  }

  .service-side {
    justify-items: start;
  }

  .organization-card,
  .organization-stats {
    grid-template-columns: 1fr;
  }
}
</style>
