<template>
  <PortalLayout :active-key="pageActiveKey" :breadcrumb="isRewardsPanel ? '积分商城' : '志愿活动'">
    <section class="list-page">
      <div class="portal-shell">
        <div class="page-title-row">
          <div>
            <h1>{{ isRewardsPanel ? '积分商城' : '志愿服务活动' }}</h1>
            <p>{{ isRewardsPanel ? '未登录可查看兑换说明；登录志愿者账号后可使用公益积分兑换。' : '未登录可浏览公开活动信息；登录志愿者账号后可报名、取消报名和提交服务报告。' }}</p>
          </div>
          <div class="page-actions">
            <el-button :type="isRewardsPanel ? 'default' : 'primary'" @click="router.push('/activities')">志愿活动</el-button>
            <el-button :type="isRewardsPanel ? 'primary' : 'default'" @click="router.push('/activities?panel=rewards')">积分商城</el-button>
            <el-button v-if="!isVolunteer" type="primary" plain @click="router.push('/login?role=volunteer')">志愿者登录后参与</el-button>
          </div>
        </div>

        <template v-if="isRewardsPanel">
          <div v-if="productLoading" class="portal-loading">正在加载积分商品...</div>
          <div v-else class="activity-grid">
            <article v-for="product in products" :key="product.productId" class="activity-card">
              <div class="activity-cover">
                <img v-if="product.imagePath" :src="resolveImage(product.imagePath)" :alt="product.productName" />
                <span v-else>积分商品</span>
              </div>
              <div class="activity-body">
                <div class="card-topline">
                  <StatusBadge :label="`${product.price} 积分`" tone="primary" />
                  <span>{{ product.organizationName || '公益组织' }}</span>
                </div>
                <h2>{{ product.productName }}</h2>
                <p>{{ product.productDescription || '暂无商品说明' }}</p>
                <dl class="meta-list">
                  <div><dt>库存</dt><dd>{{ product.stock }}</dd></div>
                  <div><dt>状态</dt><dd>{{ getStatusMeta('review', product.reviewStatus).label }}</dd></div>
                </dl>
                <div class="card-actions">
                  <el-button v-if="isVolunteer" type="primary" :disabled="product.stock <= 0" @click="handleRedeem(product)">立即兑换</el-button>
                  <el-button v-else type="primary" plain @click="router.push('/login?role=volunteer')">登录后兑换</el-button>
                </div>
              </div>
            </article>
          </div>
          <EmptyState v-if="!productLoading && !products.length" mark="兑" title="暂无可兑换商品" description="商品审核通过后会进入统一积分商城。" />
        </template>

        <div v-else-if="loading" class="portal-loading">正在加载活动...</div>
        <div v-else class="activity-grid">
          <article v-for="activity in activities" :key="activity.activityId || activity.id" class="activity-card">
            <div class="activity-cover">
              <img v-if="activity.imagePath || activity.image" :src="resolveImage(activity.imagePath || activity.image)" :alt="activity.activityName || activity.title" />
              <span v-else>志愿活动</span>
            </div>
            <div class="activity-body">
              <div class="card-topline">
                <StatusBadge :label="activity.reviewStatus ? getStatusMeta('review', activity.reviewStatus).label : '公开展示'" :tone="activity.reviewStatus ? getStatusMeta('review', activity.reviewStatus).tone : 'success'" />
                <span>{{ activity.organizationName || '公益组织' }}</span>
              </div>
              <h2>{{ activity.activityName || activity.title }}</h2>
              <p>{{ activity.description || '暂无活动说明' }}</p>
              <dl class="meta-list">
                <div><dt>地点</dt><dd>{{ activity.location || '待公布' }}</dd></div>
                <div><dt>时间</dt><dd>{{ formatDateTime(activity.startDate || activity.endDate) }}</dd></div>
                <div><dt>名额</dt><dd>{{ activity.currentParticipants ?? 0 }} / {{ activity.maxParticipants ?? '-' }}</dd></div>
                <div><dt>积分</dt><dd>{{ activity.approvedRewardPoints ?? activity.requestedRewardPoints ?? '-' }}</dd></div>
              </dl>
              <div class="card-actions">
                <template v-if="isVolunteer && activity.activityId">
                  <el-button type="primary" plain :disabled="activity.joined" @click="handleJoin(activity)">报名活动</el-button>
                  <el-button :disabled="!activity.joined" @click="handleCancel(activity)">取消报名</el-button>
                  <el-button type="success" :disabled="!activity.joined" @click="openCompletionDialog(activity)">提交服务报告</el-button>
                </template>
                <el-button v-else type="primary" plain @click="router.push('/login?role=volunteer')">登录后报名</el-button>
              </div>
            </div>
          </article>
        </div>
        <EmptyState v-if="!isRewardsPanel && !loading && !activities.length" mark="活" title="暂无公开活动" description="已审核活动或服务公示会在这里展示。" />
      </div>
    </section>

    <el-dialog v-model="completionDialogVisible" title="提交服务报告" width="min(760px, calc(100vw - 24px))">
      <el-form label-position="top">
        <el-form-item label="活动名称"><el-input :model-value="completionForm.activityName" disabled /></el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12">
            <el-form-item label="服务地点"><el-input v-model="completionForm.serviceLocation" placeholder="请输入服务地点" /></el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="附件上传">
              <el-upload action="#" :http-request="handleUpload" :file-list="uploadFiles" :on-remove="handleUploadRemove">
                <el-button type="primary" plain>上传附件</el-button>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="开始时间"><el-date-picker v-model="completionForm.serviceStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="结束时间"><el-date-picker v-model="completionForm.serviceEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="工作总结"><el-input v-model="completionForm.reportText" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="贡献内容"><el-input v-model="completionForm.contributionDetails" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReport">提交报告</el-button>
      </template>
    </el-dialog>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { getCompletedProjects } from '../api/public'
import { cancelParticipation, getVolunteerActivities, getVolunteerProducts, joinActivity, redeemProduct, submitCompletion, uploadFile } from '../api/platform'
import { getCachedUser, getToken, hasRole } from '../api/auth'
import { formatDateTime, getStatusMeta, toDateTimeInputValue } from '../utils/ui'

const router = useRouter()
const route = useRoute()
const user = ref(getCachedUser())
const loading = ref(false)
const productLoading = ref(false)
const activities = ref([])
const products = ref([])
const completionDialogVisible = ref(false)
const submitting = ref(false)
const uploadFiles = ref([])
const completionForm = reactive(createEmptyCompletionForm())

const isVolunteer = computed(() => !!getToken() && hasRole('volunteer', user.value))
const isRewardsPanel = computed(() => route.query.panel === 'rewards')
const pageActiveKey = computed(() => (isRewardsPanel.value ? 'rewards' : 'activities'))

onMounted(loadPageData)

watch(
  () => route.query.panel,
  () => {
    loadPageData()
  }
)

function createEmptyCompletionForm() {
  return {
    activityId: null,
    activityName: '',
    serviceLocation: '',
    serviceStartTime: '',
    serviceEndTime: '',
    reportText: '',
    contributionDetails: '',
    attachmentPaths: []
  }
}

async function loadActivities() {
  loading.value = true
  try {
    if (isVolunteer.value) {
      activities.value = await getVolunteerActivities()
    } else {
      const projects = await getCompletedProjects(12)
      activities.value = projects.map((project) => ({
        ...project,
        activityName: project.title,
        startDate: project.endDate,
        approvedRewardPoints: null
      }))
    }
  } catch (error) {
    activities.value = []
  } finally {
    loading.value = false
  }
}

async function loadProducts() {
  productLoading.value = true
  try {
    products.value = isVolunteer.value ? await getVolunteerProducts() : []
  } catch (error) {
    products.value = []
  } finally {
    productLoading.value = false
  }
}

async function loadPageData() {
  if (isRewardsPanel.value) {
    await loadProducts()
  } else {
    await loadActivities()
  }
}

async function handleJoin(activity) {
  try {
    await joinActivity(activity.activityId)
    ElMessage.success('报名成功')
    await loadActivities()
  } catch (error) {
    ElMessage.error(error.message || '报名失败')
  }
}

async function handleRedeem(product) {
  try {
    await ElMessageBox.confirm(`确认消耗 ${product.price} 积分兑换“${product.productName}”吗？`, '积分兑换', { type: 'warning' })
    await redeemProduct({ productId: product.productId })
    ElMessage.success('兑换申请已提交')
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '兑换失败')
    }
  }
}

async function handleCancel(activity) {
  try {
    await ElMessageBox.confirm(`确认取消活动“${activity.activityName}”的报名吗？`, '取消报名', { type: 'warning' })
    await cancelParticipation(activity.activityId)
    ElMessage.success('已取消报名')
    await loadActivities()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消报名失败')
    }
  }
}

function openCompletionDialog(activity) {
  Object.assign(completionForm, createEmptyCompletionForm(), {
    activityId: activity.activityId,
    activityName: activity.activityName,
    serviceLocation: activity.location || '',
    serviceStartTime: toDateTimeInputValue(activity.startDate),
    serviceEndTime: toDateTimeInputValue(activity.endDate)
  })
  uploadFiles.value = []
  completionDialogVisible.value = true
}

async function handleUpload(option) {
  try {
    const path = await uploadFile(option.file)
    completionForm.attachmentPaths.push(path)
    uploadFiles.value.push({ name: option.file.name, url: path, response: { path } })
    option.onSuccess({ path })
  } catch (error) {
    option.onError(error)
    ElMessage.error(error.message || '上传失败')
  }
}

function handleUploadRemove(file) {
  const path = file?.response?.path || file?.url
  completionForm.attachmentPaths = completionForm.attachmentPaths.filter((item) => item !== path)
  uploadFiles.value = uploadFiles.value.filter((item) => (item.response?.path || item.url) !== path)
}

async function submitReport() {
  if (!completionForm.activityId || !completionForm.serviceLocation || !completionForm.serviceStartTime || !completionForm.serviceEndTime || !completionForm.reportText || !completionForm.contributionDetails) {
    ElMessage.warning('请填写完整服务报告')
    return
  }
  submitting.value = true
  try {
    await submitCompletion({ ...completionForm })
    ElMessage.success('服务报告已提交')
    completionDialogVisible.value = false
    await loadActivities()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
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
.list-page {
  padding-bottom: 70px;
}

.page-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-title-row h1 {
  margin: 0;
  font-size: 30px;
}

.page-title-row p {
  margin: 10px 0 0;
  color: #777;
}

.portal-loading {
  padding: 28px 0;
  color: #777;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.activity-card {
  overflow: hidden;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(161, 43, 61, 0.06);
}

.activity-cover {
  height: 170px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ffe2e6, #fff3e8);
  color: #df001b;
  font-size: 24px;
  font-weight: 700;
}

.activity-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.activity-body {
  padding: 20px;
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #777;
  font-size: 13px;
}

.activity-body h2 {
  margin: 14px 0 0;
  font-size: 21px;
}

.activity-body p {
  min-height: 54px;
  margin: 10px 0 0;
  color: #666;
  line-height: 1.7;
}

.meta-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0 0;
}

.meta-list dt {
  color: #888;
  font-size: 12px;
}

.meta-list dd {
  margin: 6px 0 0;
  color: #333;
  font-weight: 700;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .page-title-row,
  .activity-grid {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
