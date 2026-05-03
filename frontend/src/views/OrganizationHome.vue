<template>
  <PortalLayout active-key="organizations" breadcrumb="组织工作台">
    <section class="org-workbench" v-loading="pageLoading">
      <div class="portal-shell workbench-layout">
        <aside class="workbench-side">
          <strong>组织工作台</strong>
          <button v-for="item in tabs" :key="item.key" type="button" :class="{ active: activeTab === item.key }" @click="activeTab = item.key">
            {{ item.label }}
          </button>
        </aside>

        <main class="workbench-main">
          <section class="summary-grid">
            <article><span>待审成员</span><strong>{{ pendingJoinCount }}</strong></article>
            <article><span>活动草稿/审核</span><strong>{{ activities.length }}</strong></article>
            <article><span>待审报告</span><strong>{{ pendingCompletionCount }}</strong></article>
            <article><span>链上异常</span><strong>{{ failedEvidenceCount }}</strong></article>
          </section>

          <section v-if="activeTab === 'overview'" class="panel">
            <h2>组织概览</h2>
            <p class="muted-text">{{ user?.organizationName || '当前组织' }} · 管理员 {{ user?.username || '-' }} · {{ user?.email || '-' }}</p>
            <div class="guide-grid">
              <div>创建活动草稿后提交平台审核，通过后公开展示。</div>
              <div>志愿者完成报告由组织审核，通过后触发积分和链上存证。</div>
              <div>商品草稿提交平台审核，通过后进入统一积分商城。</div>
            </div>
          </section>

          <section v-else-if="activeTab === 'members'" class="panel">
            <div class="panel-head"><h2>成员申请审核</h2><el-button @click="refreshAll">刷新</el-button></div>
            <el-table :data="joinRequests" empty-text="暂无成员申请">
              <el-table-column prop="username" label="申请人" width="130" />
              <el-table-column prop="applyReason" label="申请说明" min-width="240" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('joinRequest', row.status).label" :tone="getStatusMeta('joinRequest', row.status).tone" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210">
                <template #default="{ row }">
                  <el-button size="small" type="success" :disabled="row.status !== 'pending'" @click="reviewJoin(row, 'approved')">通过</el-button>
                  <el-button size="small" type="danger" plain :disabled="row.status !== 'pending'" @click="reviewJoin(row, 'rejected')">拒绝</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else-if="activeTab === 'activities'" class="panel">
            <div class="panel-head"><h2>活动草稿与提交</h2><el-button type="primary" @click="openActivityDialog()">新建活动</el-button></div>
            <div class="card-grid">
              <article v-for="activity in activities" :key="activity.activityId" class="work-card">
                <div class="card-topline">
                  <h3>{{ activity.activityName }}</h3>
                  <StatusBadge :label="getStatusMeta('review', activity.reviewStatus).label" :tone="getStatusMeta('review', activity.reviewStatus).tone" />
                </div>
                <p>{{ activity.location || '未设置地点' }} · {{ formatDateTime(activity.startDate) }}</p>
                <p>{{ activity.description || '暂无活动说明' }}</p>
                <div class="actions">
                  <el-button size="small" @click="openActivityDialog(activity)">编辑</el-button>
                  <el-button size="small" type="primary" plain :disabled="activity.reviewStatus === 'pending_review' || activity.reviewStatus === 'approved'" @click="handleSubmitActivity(activity)">提交审核</el-button>
                  <el-button size="small" type="danger" plain @click="handleDeleteActivity(activity)">删除</el-button>
                </div>
              </article>
            </div>
            <EmptyState v-if="!activities.length" mark="活" title="暂无活动" description="创建活动草稿后可提交平台审核。" />
          </section>

          <section v-else-if="activeTab === 'reviews'" class="panel">
            <div class="panel-head"><h2>完成报告审核</h2><el-button @click="refreshAll">刷新</el-button></div>
            <div class="card-grid">
              <article v-for="review in completionReviews" :key="review.completionId" class="work-card">
                <div class="card-topline">
                  <h3>{{ review.activityName }}</h3>
                  <StatusBadge :label="getStatusMeta('completion', review.status).label" :tone="getStatusMeta('completion', review.status).tone" />
                </div>
                <p>{{ review.username }} · {{ review.serviceLocation || '未填写地点' }}</p>
                <p>{{ review.reportText || '暂无工作总结' }}</p>
                <div class="actions">
                  <el-button size="small" type="success" :disabled="review.status !== 'pending'" @click="reviewCompletionItem(review, 'approved')">通过</el-button>
                  <el-button size="small" type="danger" plain :disabled="review.status !== 'pending'" @click="reviewCompletionItem(review, 'rejected')">驳回</el-button>
                </div>
              </article>
            </div>
            <EmptyState v-if="!completionReviews.length" mark="审" title="暂无完成报告" description="志愿者提交的服务报告会在这里审核。" />
          </section>

          <section v-else-if="activeTab === 'products'" class="panel">
            <div class="panel-head"><h2>商品草稿与提交</h2><el-button type="primary" @click="openProductDialog()">新建商品</el-button></div>
            <div class="card-grid">
              <article v-for="product in products" :key="product.productId" class="work-card">
                <div class="card-topline">
                  <h3>{{ product.productName }}</h3>
                  <StatusBadge :label="getStatusMeta('review', product.reviewStatus).label" :tone="getStatusMeta('review', product.reviewStatus).tone" />
                </div>
                <p>{{ product.productDescription || '暂无商品说明' }}</p>
                <p>{{ product.price }} 积分 · 库存 {{ product.stock }}</p>
                <div class="actions">
                  <el-button size="small" @click="openProductDialog(product)">编辑</el-button>
                  <el-button size="small" type="primary" plain :disabled="product.reviewStatus === 'pending_review' || product.reviewStatus === 'approved'" @click="handleSubmitProduct(product)">提交审核</el-button>
                  <el-button size="small" type="danger" plain @click="handleDeleteProduct(product)">删除</el-button>
                </div>
              </article>
            </div>
            <EmptyState v-if="!products.length" mark="商" title="暂无商品" description="创建商品草稿后可提交平台审核。" />
          </section>

          <section v-else-if="activeTab === 'redemptions'" class="panel">
            <div class="panel-head"><h2>兑换履约</h2><el-button @click="refreshAll">刷新</el-button></div>
            <el-table :data="redemptions" empty-text="暂无兑换记录">
              <el-table-column prop="username" label="志愿者" width="120" />
              <el-table-column prop="productName" label="商品" min-width="160" />
              <el-table-column prop="pointsCost" label="积分" width="90" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('redemption', row.status).label" :tone="getStatusMeta('redemption', row.status).tone" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="190">
                <template #default="{ row }">
                  <el-button size="small" type="success" :disabled="row.status !== 'pending'" @click="updateRedemption(row, 'fulfilled')">发放</el-button>
                  <el-button size="small" type="warning" plain :disabled="row.status !== 'pending'" @click="updateRedemption(row, 'cancelled')">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else-if="activeTab === 'evidences'" class="panel">
            <div class="panel-head"><h2>链上存证监控</h2><el-button @click="refreshAll">刷新</el-button></div>
            <el-table :data="evidences" empty-text="暂无存证记录">
              <el-table-column prop="bizId" label="业务 ID" width="100" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('evidence', row.onchainStatus).label" :tone="getStatusMeta('evidence', row.onchainStatus).tone" />
                </template>
              </el-table-column>
              <el-table-column prop="txHash" label="交易哈希" min-width="220" show-overflow-tooltip />
              <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button size="small" type="primary" plain :disabled="row.onchainStatus !== 'failed'" @click="handleRetry(row)">重试</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else class="panel">
            <h2>组织设置</h2>
            <el-alert type="info" show-icon :closable="false" title="当前后端未提供组织资料更新接口，本页先展示组织公开资料维护入口占位。" />
          </section>
        </main>
      </div>
    </section>

    <el-dialog v-model="activityDialogVisible" :title="activityForm.id ? '编辑活动' : '新建活动'" width="min(760px, calc(100vw - 24px))">
      <el-form label-position="top">
        <el-form-item label="活动名称"><el-input v-model="activityForm.activityName" /></el-form-item>
        <el-form-item label="活动描述"><el-input v-model="activityForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="开始时间"><el-date-picker v-model="activityForm.startDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="结束时间"><el-date-picker v-model="activityForm.endDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="地点"><el-input v-model="activityForm.location" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="申请积分"><el-input-number v-model="activityForm.requestedRewardPoints" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="最大人数"><el-input-number v-model="activityForm.maxParticipants" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="报名截止"><el-date-picker v-model="activityForm.enrollDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="联系人"><el-input v-model="activityForm.contactName" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="联系电话"><el-input v-model="activityForm.contactPhone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="标签"><el-input v-model="activityForm.categoryTags" placeholder="例如 GENERAL,环保" /></el-form-item>
        <el-form-item label="封面路径"><el-input v-model="activityForm.imagePath" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveActivity">保存活动</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="productDialogVisible" :title="productForm.id ? '编辑商品' : '新建商品'" width="min(640px, calc(100vw - 24px))">
      <el-form label-position="top">
        <el-form-item label="商品名称"><el-input v-model="productForm.productName" /></el-form-item>
        <el-form-item label="商品描述"><el-input v-model="productForm.productDescription" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="积分价格"><el-input-number v-model="productForm.price" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="库存"><el-input-number v-model="productForm.stock" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="图片路径"><el-input v-model="productForm.imagePath" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">保存商品</el-button>
      </template>
    </el-dialog>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { clearAuth, getCachedUser, getCurrentUser, getToken, redirectToLogin, saveAuth } from '../api/auth'
import {
  createActivity,
  createProduct,
  deleteActivity,
  deleteProduct,
  getCompletionReviews,
  getOrganizationActivities,
  getOrganizationEvidences,
  getOrganizationJoinRequests,
  getOrganizationProducts,
  getOrganizationRedemptions,
  retryEvidence,
  reviewCompletion,
  reviewJoinRequest,
  submitActivity,
  submitProduct,
  updateActivity,
  updateProduct,
  updateRedemptionStatus
} from '../api/platform'
import { formatDateTime, getStatusMeta, toDateTimeInputValue } from '../utils/ui'

const router = useRouter()
const pageLoading = ref(false)
const user = ref(getCachedUser())
const activeTab = ref('overview')
const joinRequests = ref([])
const activities = ref([])
const completionReviews = ref([])
const products = ref([])
const redemptions = ref([])
const evidences = ref([])
const activityDialogVisible = ref(false)
const productDialogVisible = ref(false)
const activityForm = reactive(createEmptyActivityForm())
const productForm = reactive(createEmptyProductForm())

const tabs = [
  { key: 'overview', label: '组织概览' },
  { key: 'members', label: '成员申请审核' },
  { key: 'activities', label: '活动草稿/提交' },
  { key: 'reviews', label: '完成报告审核' },
  { key: 'products', label: '商品草稿/提交' },
  { key: 'redemptions', label: '兑换履约' },
  { key: 'evidences', label: '链上存证监控' },
  { key: 'settings', label: '组织设置' }
]

const pendingJoinCount = computed(() => joinRequests.value.filter((item) => item.status === 'pending').length)
const pendingCompletionCount = computed(() => completionReviews.value.filter((item) => item.status === 'pending').length)
const failedEvidenceCount = computed(() => evidences.value.filter((item) => item.onchainStatus === 'failed').length)

onMounted(initializePage)

function createEmptyActivityForm() {
  return {
    id: null,
    activityName: '',
    description: '',
    startDate: '',
    endDate: '',
    location: '',
    contactName: '',
    contactPhone: '',
    categoryTags: '',
    imagePath: '',
    maxParticipants: 20,
    requestedRewardPoints: 10,
    enrollDeadline: ''
  }
}

function createEmptyProductForm() {
  return { id: null, productName: '', productDescription: '', price: 10, stock: 1, imagePath: '' }
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
    redirectToLogin('organization_admin')
    return
  }
  try {
    const profile = await getCurrentUser(token)
    user.value = profile
    saveAuth({ token, user: profile })
  } catch (error) {
    clearAuth()
    ElMessage.error(error.message || '登录状态已失效')
    redirectToLogin('organization_admin')
  }
}

async function refreshAll() {
  const [joinData, activityData, completionData, productData, redemptionData, evidenceData] = await Promise.all([
    getOrganizationJoinRequests().catch(() => []),
    getOrganizationActivities().catch(() => []),
    getCompletionReviews().catch(() => []),
    getOrganizationProducts().catch(() => []),
    getOrganizationRedemptions().catch(() => []),
    getOrganizationEvidences().catch(() => [])
  ])
  joinRequests.value = joinData
  activities.value = activityData
  completionReviews.value = completionData
  products.value = productData
  redemptions.value = redemptionData
  evidences.value = evidenceData
}

async function reviewJoin(row, status) {
  try {
    const action = status === 'approved' ? '通过' : '拒绝'
    const { value } = await ElMessageBox.prompt(`请输入${action}备注`, `${action}成员申请`, { inputPlaceholder: '可填写审核说明' })
    await reviewJoinRequest(row.id, { status, reviewNote: value || '' })
    ElMessage.success(`已${action}申请`)
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

function openActivityDialog(row) {
  Object.assign(activityForm, createEmptyActivityForm(), row ? {
    id: row.activityId,
    activityName: row.activityName,
    description: row.description,
    startDate: toDateTimeInputValue(row.startDate),
    endDate: toDateTimeInputValue(row.endDate),
    location: row.location,
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    categoryTags: row.categoryTags,
    imagePath: row.imagePath,
    maxParticipants: row.maxParticipants,
    requestedRewardPoints: row.requestedRewardPoints ?? row.approvedRewardPoints ?? 10,
    enrollDeadline: toDateTimeInputValue(row.enrollDeadline)
  } : {})
  activityDialogVisible.value = true
}

async function saveActivity() {
  try {
    const payload = { ...activityForm }
    delete payload.id
    if (activityForm.id) {
      await updateActivity(activityForm.id, payload)
      ElMessage.success('活动已更新')
    } else {
      await createActivity(payload)
      ElMessage.success('活动已创建')
    }
    activityDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '活动保存失败')
  }
}

async function handleSubmitActivity(row) {
  try {
    await submitActivity(row.activityId)
    ElMessage.success('活动已提交平台审核')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

async function handleDeleteActivity(row) {
  try {
    await ElMessageBox.confirm(`确认删除活动“${row.activityName}”吗？`, '删除活动', { type: 'warning' })
    await deleteActivity(row.activityId)
    ElMessage.success('活动已删除')
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function reviewCompletionItem(row, status) {
  try {
    const action = status === 'approved' ? '通过' : '驳回'
    const { value } = await ElMessageBox.prompt(`请输入${action}说明`, `${action}完成报告`, { inputPlaceholder: '可填写审核说明或驳回原因' })
    await reviewCompletion(row.completionId, { status, reviewNote: value || '' })
    ElMessage.success(`已${action}完成报告`)
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

function openProductDialog(row) {
  Object.assign(productForm, createEmptyProductForm(), row ? {
    id: row.productId,
    productName: row.productName,
    productDescription: row.productDescription,
    price: row.price,
    stock: row.stock,
    imagePath: row.imagePath
  } : {})
  productDialogVisible.value = true
}

async function saveProduct() {
  try {
    const payload = { ...productForm }
    delete payload.id
    if (productForm.id) {
      await updateProduct(productForm.id, payload)
      ElMessage.success('商品已更新')
    } else {
      await createProduct(payload)
      ElMessage.success('商品已创建')
    }
    productDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '商品保存失败')
  }
}

async function handleSubmitProduct(row) {
  try {
    await submitProduct(row.productId)
    ElMessage.success('商品已提交平台审核')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

async function handleDeleteProduct(row) {
  try {
    await ElMessageBox.confirm(`确认删除商品“${row.productName}”吗？`, '删除商品', { type: 'warning' })
    await deleteProduct(row.productId)
    ElMessage.success('商品已删除')
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function updateRedemption(row, status) {
  try {
    await updateRedemptionStatus(row.id, { status })
    ElMessage.success('兑换状态已更新')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '处理失败')
  }
}

async function handleRetry(row) {
  try {
    await retryEvidence(row.id)
    ElMessage.success('已发起重试')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '重试失败')
  }
}
</script>

<style scoped>
.org-workbench {
  padding-bottom: 72px;
}

.workbench-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 22px;
}

.workbench-side,
.panel,
.summary-grid article {
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fff;
}

.workbench-side {
  overflow: hidden;
}

.workbench-side strong,
.workbench-side button {
  width: 100%;
  min-height: 48px;
  display: flex;
  align-items: center;
  border: 0;
  border-left: 4px solid transparent;
  background: #fff;
  padding: 0 22px;
  color: #333;
  cursor: pointer;
  text-align: left;
  font-size: 15px;
}

.workbench-side strong,
.workbench-side button.active {
  border-left-color: #df001b;
  background: #fff0f2;
  color: #df001b;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.summary-grid article {
  padding: 20px;
}

.summary-grid span {
  color: #777;
}

.summary-grid strong {
  display: block;
  margin-top: 12px;
  font-size: 32px;
}

.panel {
  padding: 24px;
}

.panel h2 {
  margin: 0 0 20px;
  font-size: 23px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head h2 {
  margin: 0;
}

.muted-text,
.work-card p {
  color: #666;
  line-height: 1.75;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.guide-grid div,
.work-card {
  padding: 18px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fffafa;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.card-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-topline h3 {
  margin: 0;
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .workbench-layout {
    grid-template-columns: 1fr;
  }

  .summary-grid,
  .guide-grid,
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-grid,
  .guide-grid,
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
