<template>
  <div class="admin-shell" v-loading="pageLoading">
    <aside class="admin-side">
      <div class="admin-brand">
        <strong>后台管理系统</strong>
        <span>志愿服务认证平台</span>
      </div>
      <button v-for="item in menuItems" :key="item.key" type="button" :class="{ active: activeMenu === item.key }" @click="activeMenu = item.key">
        {{ item.label }}
      </button>
    </aside>

    <main class="admin-main">
      <header class="admin-top">
        <div>
          <h1>{{ currentMenuLabel }}</h1>
          <p>后台管理界面，用于平台级审核、权限、积分和链上维护。</p>
        </div>
        <div class="admin-actions">
          <span>{{ user?.username || '-' }}</span>
          <el-button @click="router.push('/')">返回门户</el-button>
          <el-button type="primary" @click="handleLogout">退出</el-button>
        </div>
      </header>

      <section v-if="activeMenu === 'dashboard'" class="admin-section">
        <div class="admin-metrics">
          <article><span>用户数</span><strong>{{ users.length }}</strong></article>
          <article><span>角色数</span><strong>{{ roles.length }}</strong></article>
          <article><span>待审活动</span><strong>{{ activityReviews.filter((item) => item.reviewStatus === 'pending_review').length }}</strong></article>
          <article><span>待审商品</span><strong>{{ productReviews.filter((item) => item.reviewStatus === 'pending_review').length }}</strong></article>
          <article><span>积分规则</span><strong>{{ pointsRules.length }}</strong></article>
          <article><span>链上异常</span><strong>{{ publicEvidences.filter((item) => item.onchainStatus === 'failed').length }}</strong></article>
        </div>
      </section>

      <section v-else-if="activeMenu === 'users'" class="admin-section">
        <div class="section-toolbar"><h2>用户管理</h2><el-button @click="refreshAll">刷新</el-button></div>
        <el-table :data="users" empty-text="暂无用户">
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="organizationName" label="所属组织" min-width="160" />
          <el-table-column label="主角色" width="140"><template #default="{ row }">{{ getRoleLabel(row.primaryRole) }}</template></el-table-column>
          <el-table-column label="角色列表" min-width="220"><template #default="{ row }">{{ (row.roles || []).map(getRoleLabel).join('、') || '暂无角色' }}</template></el-table-column>
          <el-table-column prop="totalPoints" label="积分" width="90" />
          <el-table-column label="操作" width="130"><template #default="{ row }"><el-button size="small" type="primary" plain @click="openRoleDialog(row)">分配角色</el-button></template></el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeMenu === 'roles'" class="admin-section">
        <div class="role-layout">
          <article class="admin-panel">
            <h2>角色权限</h2>
            <button v-for="role in roles" :key="role.code" type="button" class="role-item" :class="{ active: selectedRoleCode === role.code }" @click="showRolePermissions(role.code)">
              <strong>{{ role.name }}</strong>
              <span>{{ role.code }} · {{ role.dataScope }}</span>
            </button>
          </article>
          <article class="admin-panel">
            <h2>权限明细</h2>
            <div class="permission-grid">
              <span v-for="code in rolePermissions" :key="code">{{ code }}</span>
            </div>
            <EmptyState v-if="!rolePermissions.length" mark="权" title="请选择角色" description="点击左侧角色后查看权限编码。" />
          </article>
        </div>
      </section>

      <section v-else-if="activeMenu === 'permissions'" class="admin-section">
        <div class="section-toolbar"><h2>权限字典</h2><el-button @click="refreshAll">刷新</el-button></div>
        <el-table :data="permissions" empty-text="暂无权限">
          <el-table-column prop="code" label="权限编码" min-width="220" />
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="httpMethod" label="方法" width="100" />
          <el-table-column prop="apiPattern" label="接口模式" min-width="240" />
        </el-table>
      </section>

      <section v-else-if="activeMenu === 'activityReviews'" class="admin-section">
        <div class="section-toolbar"><h2>活动审核</h2><el-button @click="loadReviews">刷新</el-button></div>
        <div class="review-grid">
          <article v-for="item in activityReviews" :key="item.activityId" class="review-card">
            <div class="card-topline"><h3>{{ item.activityName }}</h3><StatusBadge :label="getStatusMeta('review', item.reviewStatus).label" :tone="getStatusMeta('review', item.reviewStatus).tone" /></div>
            <p>{{ item.organizationName }} · 申请 {{ item.requestedRewardPoints }} 积分 · 建议 {{ item.recommendedPoints ?? '-' }}</p>
            <el-alert v-if="item.escalationRequired" type="warning" :closable="false" title="高积分活动，建议管理员复审。" />
            <div class="actions">
              <el-button size="small" type="success" :disabled="item.reviewStatus !== 'pending_review'" @click="handleActivityReview(item, 'approved')">通过</el-button>
              <el-button size="small" type="warning" plain :disabled="item.reviewStatus !== 'pending_review'" @click="handleActivityReview(item, 'escalated')">升级复审</el-button>
              <el-button size="small" type="danger" plain :disabled="item.reviewStatus !== 'pending_review'" @click="handleActivityReview(item, 'rejected')">驳回</el-button>
            </div>
          </article>
        </div>
      </section>

      <section v-else-if="activeMenu === 'productReviews'" class="admin-section">
        <div class="section-toolbar"><h2>商品审核</h2><el-button @click="loadReviews">刷新</el-button></div>
        <div class="review-grid">
          <article v-for="item in productReviews" :key="item.productId" class="review-card">
            <div class="card-topline"><h3>{{ item.productName }}</h3><StatusBadge :label="getStatusMeta('review', item.reviewStatus).label" :tone="getStatusMeta('review', item.reviewStatus).tone" /></div>
            <p>{{ item.organizationName }} · {{ item.price }} 积分 · 库存 {{ item.stock }}</p>
            <p>{{ item.productDescription || '暂无商品说明' }}</p>
            <div class="actions">
              <el-button size="small" type="success" :disabled="item.reviewStatus !== 'pending_review'" @click="handleProductReview(item, 'approved')">通过</el-button>
              <el-button size="small" type="danger" plain :disabled="item.reviewStatus !== 'pending_review'" @click="handleProductReview(item, 'rejected')">驳回</el-button>
              <el-button size="small" plain :disabled="item.reviewStatus !== 'approved'" @click="handleProductReview(item, 'off_shelf')">下架</el-button>
            </div>
          </article>
        </div>
      </section>

      <section v-else-if="activeMenu === 'pointsRules'" class="admin-section">
        <div class="section-toolbar"><h2>积分规则</h2><el-button type="primary" @click="openRuleDialog()">新增规则</el-button></div>
        <el-table :data="pointsRules" empty-text="暂无积分规则">
          <el-table-column prop="activityCategory" label="分类" width="120" />
          <el-table-column label="服务时长" min-width="160"><template #default="{ row }">{{ row.minServiceHours }} - {{ row.maxServiceHours }} 小时</template></el-table-column>
          <el-table-column prop="suggestedPoints" label="建议积分" width="110" />
          <el-table-column prop="maxPoints" label="最高积分" width="110" />
          <el-table-column prop="escalationThreshold" label="复审阈值" width="110" />
          <el-table-column label="状态" width="90"><template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{ row }"><el-button size="small" @click="openRuleDialog(row)">编辑</el-button></template></el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeMenu === 'chainMaintenance'" class="admin-section">
        <div class="section-toolbar"><h2>链上积分维护</h2></div>
        <div class="maintenance-grid">
          <article>
            <h3>同步链上余额</h3>
            <p>批量读取链上余额并修正用户积分缓存。</p>
            <el-button type="primary" :loading="maintenanceLoading" @click="runSync">执行同步</el-button>
          </article>
          <article>
            <h3>历史积分迁移</h3>
            <p>将历史积分初始化迁移到积分合约，按业务键幂等处理。</p>
            <el-button type="primary" plain :loading="maintenanceLoading" @click="runMigration">执行迁移</el-button>
          </article>
        </div>
        <el-alert v-if="maintenanceResult" class="maintenance-result" type="success" :closable="false" show-icon :title="formatMaintenanceResult(maintenanceResult)" />
      </section>

      <section v-else class="admin-section">
        <div class="section-toolbar"><h2>链上存证监控</h2><el-button @click="loadPublicEvidences">刷新</el-button></div>
        <el-table :data="publicEvidences" empty-text="暂无公开存证">
          <el-table-column prop="activityName" label="活动" min-width="180" />
          <el-table-column prop="organizationName" label="组织" min-width="160" />
          <el-table-column label="状态" width="120"><template #default="{ row }"><StatusBadge :label="getStatusMeta('evidence', row.onchainStatus).label" :tone="getStatusMeta('evidence', row.onchainStatus).tone" /></template></el-table-column>
          <el-table-column prop="txHash" label="交易哈希" min-width="220" show-overflow-tooltip />
        </el-table>
      </section>
    </main>

    <el-dialog v-model="roleDialogVisible" title="分配用户角色" width="min(620px, calc(100vw - 24px))">
      <div v-if="selectedUser" class="dialog-user">
        <strong>{{ selectedUser.username }}</strong>
        <p>{{ selectedUser.email }} · 当前主角色 {{ getRoleLabel(selectedUser.primaryRole) }}</p>
      </div>
      <el-checkbox-group v-model="selectedRoleCodes" class="role-checkbox-group">
        <el-checkbox v-for="role in roles" :key="role.code" :label="role.code">{{ role.name }}（{{ role.code }}）</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUserRoles">保存角色</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? '编辑积分规则' : '新增积分规则'" width="min(640px, calc(100vw - 24px))">
      <el-form label-position="top">
        <el-form-item label="活动分类"><el-input v-model="ruleForm.activityCategory" /></el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="最小时长"><el-input-number v-model="ruleForm.minServiceHours" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="最大时长"><el-input-number v-model="ruleForm.maxServiceHours" :min="0.01" :precision="2" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="8"><el-form-item label="建议积分"><el-input-number v-model="ruleForm.suggestedPoints" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="8"><el-form-item label="最高积分"><el-input-number v-model="ruleForm.maxPoints" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="8"><el-form-item label="复审阈值"><el-input-number v-model="ruleForm.escalationThreshold" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态"><el-radio-group v-model="ruleForm.status"><el-radio-button :label="1">启用</el-radio-button><el-radio-button :label="0">停用</el-radio-button></el-radio-group></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存规则</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import StatusBadge from '../components/StatusBadge.vue'
import {
  assignAdminUserRoles,
  createPointsRule,
  getActivityReviews,
  getAdminPermissions,
  getAdminRolePermissions,
  getAdminRoles,
  getAdminUserRoles,
  getAdminUsers,
  getPointsRules,
  getProductReviews,
  migratePointsToChain,
  reviewActivity,
  reviewProduct,
  syncPointBalances,
  updatePointsRule
} from '../api/admin'
import { getPublicEvidences } from '../api/public'
import { clearAuth, getCachedUser, getCurrentUser, getToken, redirectToLogin, saveAuth } from '../api/auth'
import { getRoleLabel, getStatusMeta } from '../utils/ui'

const router = useRouter()
const user = ref(getCachedUser())
const pageLoading = ref(false)
const activeMenu = ref('dashboard')
const users = ref([])
const roles = ref([])
const permissions = ref([])
const selectedRoleCode = ref('')
const rolePermissions = ref([])
const roleDialogVisible = ref(false)
const selectedUser = ref(null)
const selectedRoleCodes = ref([])
const activityReviews = ref([])
const productReviews = ref([])
const pointsRules = ref([])
const publicEvidences = ref([])
const maintenanceLoading = ref(false)
const maintenanceResult = ref(null)
const ruleDialogVisible = ref(false)
const ruleForm = reactive(createEmptyRuleForm())

const menuItems = [
  { key: 'dashboard', label: '控制台概览' },
  { key: 'users', label: '用户管理' },
  { key: 'roles', label: '角色权限' },
  { key: 'permissions', label: '权限字典' },
  { key: 'activityReviews', label: '活动审核' },
  { key: 'productReviews', label: '商品审核' },
  { key: 'pointsRules', label: '积分规则' },
  { key: 'chainMaintenance', label: '链上积分维护' },
  { key: 'evidences', label: '链上存证监控' }
]

const currentMenuLabel = computed(() => menuItems.find((item) => item.key === activeMenu.value)?.label || '后台管理')

onMounted(initializePage)

function createEmptyRuleForm() {
  return {
    id: null,
    activityCategory: 'GENERAL',
    minServiceHours: 0,
    maxServiceHours: 2,
    suggestedPoints: 10,
    maxPoints: 20,
    escalationThreshold: 15,
    status: 1
  }
}

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
  } catch (error) {
    clearAuth()
    ElMessage.error(error.message || '登录状态已失效')
    redirectToLogin('volunteer')
  }
}

async function refreshAll() {
  const [userData, roleData, permissionData, ruleData] = await Promise.all([
    getAdminUsers().catch(() => []),
    getAdminRoles().catch(() => []),
    getAdminPermissions().catch(() => []),
    getPointsRules().catch(() => [])
  ])
  users.value = userData
  roles.value = roleData
  permissions.value = permissionData
  pointsRules.value = ruleData
  if (roles.value.length) {
    await showRolePermissions(selectedRoleCode.value || roles.value[0].code)
  }
  await Promise.all([loadReviews(), loadPublicEvidences()])
}

async function loadReviews() {
  const [activityData, productData] = await Promise.all([
    getActivityReviews().catch(() => []),
    getProductReviews().catch(() => [])
  ])
  activityReviews.value = activityData
  productReviews.value = productData
}

async function loadPublicEvidences() {
  publicEvidences.value = await getPublicEvidences({ limit: 50 }).catch(() => [])
}

async function showRolePermissions(roleCode) {
  selectedRoleCode.value = roleCode
  const detail = await getAdminRolePermissions(roleCode).catch(() => ({ permissions: [] }))
  rolePermissions.value = detail.permissions || []
}

async function openRoleDialog(row) {
  selectedUser.value = row
  const detail = await getAdminUserRoles(row.userId).catch(() => ({ roles: row.roles || [] }))
  selectedRoleCodes.value = detail.roles || []
  roleDialogVisible.value = true
}

async function submitUserRoles() {
  if (!selectedUser.value || !selectedRoleCodes.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  try {
    await assignAdminUserRoles(selectedUser.value.userId, { roleCodes: selectedRoleCodes.value })
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

async function handleActivityReview(item, status) {
  try {
    const action = status === 'approved' ? '通过' : status === 'escalated' ? '升级复审' : '驳回'
    const { value } = await ElMessageBox.prompt(`确认${action}“${item.activityName}”吗？`, `${action}活动`, {
      inputPlaceholder: status === 'approved' ? '请输入批准积分' : '请输入审核说明',
      inputValue: status === 'approved' ? String(item.recommendedPoints ?? item.requestedRewardPoints ?? 0) : ''
    })
    await reviewActivity(item.activityId, {
      status,
      approvedRewardPoints: status === 'approved' ? Number(value || 0) : null,
      reviewNote: status === 'approved' ? `批准 ${Number(value || 0)} 积分` : value || ''
    })
    ElMessage.success(`活动审核已${action}`)
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

async function handleProductReview(item, status) {
  try {
    const action = status === 'approved' ? '通过' : status === 'off_shelf' ? '下架' : '驳回'
    const { value } = await ElMessageBox.prompt(`确认${action}“${item.productName}”吗？`, `${action}商品`, {
      inputPlaceholder: '请输入审核说明'
    })
    await reviewProduct(item.productId, { status, reviewNote: value || '' })
    ElMessage.success(`商品审核已${action}`)
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

function openRuleDialog(row) {
  Object.assign(ruleForm, createEmptyRuleForm(), row ? {
    id: row.id,
    activityCategory: row.activityCategory,
    minServiceHours: Number(row.minServiceHours),
    maxServiceHours: Number(row.maxServiceHours),
    suggestedPoints: row.suggestedPoints,
    maxPoints: row.maxPoints,
    escalationThreshold: row.escalationThreshold,
    status: row.status
  } : {})
  ruleDialogVisible.value = true
}

async function saveRule() {
  try {
    const payload = { ...ruleForm }
    delete payload.id
    if (ruleForm.id) {
      await updatePointsRule(ruleForm.id, payload)
      ElMessage.success('积分规则已更新')
    } else {
      await createPointsRule(payload)
      ElMessage.success('积分规则已创建')
    }
    ruleDialogVisible.value = false
    pointsRules.value = await getPointsRules()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

async function runSync() {
  maintenanceLoading.value = true
  try {
    maintenanceResult.value = await syncPointBalances()
    ElMessage.success('链上余额同步完成')
  } catch (error) {
    ElMessage.error(error.message || '同步失败')
  } finally {
    maintenanceLoading.value = false
  }
}

async function runMigration() {
  maintenanceLoading.value = true
  try {
    maintenanceResult.value = await migratePointsToChain()
    ElMessage.success('历史积分迁移完成')
  } catch (error) {
    ElMessage.error(error.message || '迁移失败')
  } finally {
    maintenanceLoading.value = false
  }
}

function formatMaintenanceResult(result) {
  return `扫描 ${result.scannedUsers ?? 0}，迁移 ${result.migratedUsers ?? 0}，同步 ${result.syncedUsers ?? 0}，跳过 ${result.skippedUsers ?? 0}，失败 ${result.failedUsers ?? 0}`
}

function handleLogout() {
  clearAuth()
  router.push('/')
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  background: #fff7f7;
}

.admin-side {
  min-height: 100vh;
  background: linear-gradient(180deg, #b60018, #8e0014);
  color: #fff;
}

.admin-brand {
  padding: 28px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);
}

.admin-brand strong {
  display: block;
  font-size: 22px;
}

.admin-brand span {
  display: block;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.72);
}

.admin-side button {
  width: 100%;
  height: 48px;
  border: 0;
  border-left: 4px solid transparent;
  background: transparent;
  color: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  padding: 0 24px;
  text-align: left;
  font-size: 15px;
}

.admin-side button.active {
  border-left-color: #fff;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-weight: 700;
}

.admin-main {
  min-width: 0;
}

.admin-top {
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 30px;
  background: #fff;
  border-bottom: 1px solid rgba(223, 0, 27, 0.1);
}

.admin-top h1 {
  margin: 0;
  font-size: 26px;
}

.admin-top p {
  margin: 8px 0 0;
  color: #777;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-section {
  margin: 24px 30px;
  padding: 24px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fff;
}

.admin-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.admin-metrics article,
.maintenance-grid article,
.admin-panel,
.review-card {
  padding: 20px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fffafa;
}

.admin-metrics span {
  color: #777;
}

.admin-metrics strong {
  display: block;
  margin-top: 12px;
  font-size: 34px;
}

.section-toolbar,
.card-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-toolbar h2,
.admin-panel h2,
.review-card h3 {
  margin: 0;
}

.role-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
}

.role-item {
  width: 100%;
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 14px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.role-item.active {
  border-color: rgba(223, 0, 27, 0.38);
  color: #df001b;
}

.role-item span {
  color: #777;
}

.permission-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.permission-grid span {
  padding: 9px 12px;
  border-radius: 8px;
  background: #fff0f2;
  color: #9d0014;
}

.review-grid,
.maintenance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.review-card p,
.maintenance-grid p,
.dialog-user p {
  color: #666;
  line-height: 1.7;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.maintenance-result {
  margin-top: 18px;
}

.role-checkbox-group {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-side {
    min-height: auto;
  }

  .admin-side button {
    display: inline-flex;
    width: auto;
  }

  .admin-metrics,
  .role-layout,
  .review-grid,
  .maintenance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
