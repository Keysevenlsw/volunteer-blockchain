<template>
  <PortalLayout :active-key="pageActiveKey" :breadcrumb="isRewardsPanel ? '积分商城' : '志愿活动'">
    <section class="activity-page">
      <div class="portal-shell">
        <template v-if="isRewardsPanel">
          <div class="page-title-row">
            <div>
              <h1>积分商城</h1>
              <p>登录志愿者账号后可使用公益积分兑换。</p>
            </div>
            <div class="page-actions">
              <el-button @click="router.push('/activities')">志愿活动</el-button>
              <el-button type="primary" @click="router.push('/activities?panel=rewards')">积分商城</el-button>
            </div>
          </div>

          <div v-if="productLoading" class="portal-loading">正在加载积分商品...</div>
          <div v-else class="product-grid">
            <article v-for="product in products" :key="product.productId" class="product-card">
              <div class="product-cover">
                <img v-if="product.imagePath" :src="resolveImage(product.imagePath)" :alt="product.productName" />
                <span v-else>积分商品</span>
              </div>
              <div class="product-body">
                <StatusBadge :label="`${product.price} 积分`" tone="primary" />
                <h2>{{ product.productName }}</h2>
                <p>{{ product.productDescription || '暂无商品说明' }}</p>
                <el-button v-if="isVolunteer" type="primary" :disabled="product.stock <= 0" @click="handleRedeem(product)">立即兑换</el-button>
                <el-button v-else type="primary" plain @click="router.push('/login?role=volunteer')">登录后兑换</el-button>
              </div>
            </article>
          </div>
          <EmptyState v-if="!productLoading && !products.length" mark="兑" title="暂无可兑换商品" description="商品审核通过后会进入统一积分商城。" />
        </template>

        <template v-else>
          <div class="filter-panel">
            <div class="filter-row">
              <strong>活动类别:</strong>
              <div class="filter-options">
                <button
                  v-for="category in categories"
                  :key="category"
                  type="button"
                  :class="{ active: filters.category === category }"
                  @click="filters.category = category"
                >
                  {{ category }}
                </button>
              </div>
            </div>

            <template v-if="moreExpanded">
              <div class="filter-row">
                <strong>报名情况:</strong>
                <div class="filter-options">
                  <button
                    v-for="status in signupStatuses"
                    :key="status"
                    type="button"
                    :class="{ active: filters.signupStatus === status }"
                    @click="filters.signupStatus = status"
                  >
                    {{ status }}
                  </button>
                </div>
              </div>

              <div class="filter-row filter-row--compact">
                <strong>积分范围:</strong>
                <div class="compact-group">
                  <el-input v-model="filters.minPoints" placeholder="最低积分" clearable />
                  <span>至</span>
                  <el-input v-model="filters.maxPoints" placeholder="最高积分" clearable />
                </div>
              </div>

              <div class="filter-row filter-row--compact">
                <strong>年龄:</strong>
                <div class="compact-group">
                  <el-input v-model="filters.minAge" placeholder="请输入" clearable />
                  <span>至</span>
                  <el-input v-model="filters.maxAge" placeholder="请输入" clearable />
                </div>
              </div>
            </template>

            <div class="more-line">
              <button type="button" @click="moreExpanded = !moreExpanded">
                {{ moreExpanded ? '收起' : '更多条件' }}
              </button>
            </div>

            <div class="query-panel">
              <div class="query-item">
                <label>活动日期</label>
                <div class="query-range">
                  <el-date-picker v-model="filters.startDate" type="date" value-format="YYYY-MM-DD" placeholder="" />
                  <span>至</span>
                  <el-date-picker v-model="filters.endDate" type="date" value-format="YYYY-MM-DD" placeholder="" />
                </div>
              </div>

              <div class="query-item query-item--keyword">
                <label>活动名称</label>
                <el-input
                  v-model="searchKeyword"
                  placeholder="请输入关键词"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </div>

              <div class="query-actions">
                <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
                <el-button class="reset-button" @click="resetSearch">重置</el-button>
              </div>
            </div>
          </div>

          <div v-if="loading" class="portal-loading">正在加载活动...</div>
          <div v-else class="project-grid">
            <article
              v-for="activity in pagedVisibleActivities"
              :key="activity.activityId"
              class="project-card"
              role="button"
              tabindex="0"
              @click="goDetail(activity)"
              @keyup.enter="goDetail(activity)"
            >
              <div class="project-card__top">
                <div class="project-image">
                  <span class="project-label">{{ firstTag(activity) }}</span>
                  <img v-if="activity.imagePath" :src="resolveImage(activity.imagePath)" :alt="activity.activityName" />
                  <div v-else class="project-placeholder" :class="placeholderClass(activity)">
                    <span>{{ placeholderIcon(activity) }}</span>
                  </div>
                </div>

                <div class="project-main">
                  <h2 :title="activity.activityName">{{ activity.activityName }}</h2>
                  <div class="project-stats">
                    <div>
                      <span>积分:</span>
                      <strong>{{ activity.approvedRewardPoints ?? activity.requestedRewardPoints ?? 0 }}</strong>
                    </div>
                    <div>
                      <span>岗位</span>
                      <strong>{{ availableSlots(activity) }}个</strong>
                    </div>
                  </div>
                </div>
              </div>

              <div class="project-meta">
                <p :title="`活动日期：${formatDateOnly(activity.startDate)} 至 ${formatDateOnly(activity.endDate)}`">
                  活动日期：{{ formatDateOnly(activity.startDate) }} 至 {{ formatDateOnly(activity.endDate) }}
                </p>
                <p :title="`招募时间：${formatDateTime(activity.publishDate)} 至 ${formatDateTime(activity.enrollDeadline)}`">
                  招募时间：{{ formatDateTime(activity.publishDate) }} 至 {{ formatDateTime(activity.enrollDeadline) }}
                </p>
                <p :title="`活动地点：${activity.location || '待公布'}`">
                  活动地点：{{ activity.location || '待公布' }}
                </p>
              </div>
            </article>
          </div>

          <div v-if="!loading && visibleActivities.length > pageSize" class="pagination-row">
            <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pageSize"
              :total="visibleActivities.length"
              :current-page="currentPage"
              @current-change="handlePageChange"
            />
          </div>

          <EmptyState v-if="!loading && !visibleActivities.length" mark="活" title="暂无匹配活动" description="换个条件试试，或等待组织发布新的公开活动。" />
        </template>
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { getPublicActivities } from '../api/public'
import { getVolunteerActivities, getVolunteerProducts, redeemProduct } from '../api/platform'
import { getCachedUser, getToken, hasRole } from '../api/auth'
import { formatDateTime } from '../utils/ui'

const router = useRouter()
const route = useRoute()
const user = ref(getCachedUser())
const loading = ref(false)
const productLoading = ref(false)
const activities = ref([])
const products = ref([])
const searchKeyword = ref('')
const searchedKeyword = ref('')
const moreExpanded = ref(false)
const currentPage = ref(1)
const pageSize = 9

const categories = ['全部', '文明实践', '法律援助', '心理健康', '禁毒防毒', '文旅服务', '赛事展会', '垃圾分类', '助老助弱', '帮困助残', '生态环保', '医疗卫生', '文体科教', '公共安全', '社区治理', '应急救援', '其他']
const signupStatuses = ['全部', '名额未满', '名额已满']
const filters = reactive({
  category: '全部',
  signupStatus: '全部',
  minPoints: '',
  maxPoints: '',
  minAge: '',
  maxAge: '',
  startDate: '',
  endDate: ''
})

const isVolunteer = computed(() => !!getToken() && hasRole('volunteer', user.value))
const isRewardsPanel = computed(() => route.query.panel === 'rewards')
const pageActiveKey = computed(() => (isRewardsPanel.value ? 'rewards' : 'activities'))
const visibleActivities = computed(() => activities.value.filter(matchFilters))
const pagedVisibleActivities = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return visibleActivities.value.slice(start, start + pageSize)
})

onMounted(loadPageData)

watch(
  () => route.query.panel,
  () => {
    loadPageData()
  }
)

async function handleSearch() {
  searchedKeyword.value = searchKeyword.value.trim()
  currentPage.value = 1
  await loadActivities()
}

async function resetSearch() {
  searchKeyword.value = ''
  searchedKeyword.value = ''
  currentPage.value = 1
  Object.assign(filters, {
    category: '全部',
    signupStatus: '全部',
    minPoints: '',
    maxPoints: '',
    minAge: '',
    maxAge: '',
    startDate: '',
    endDate: ''
  })
  await loadActivities()
}

function handlePageChange(page) {
  currentPage.value = page
}

async function loadActivities() {
  loading.value = true
  try {
    activities.value = isVolunteer.value
      ? await getVolunteerActivities()
      : await getPublicActivities({ keyword: searchedKeyword.value, limit: 80 })
  } catch (error) {
    activities.value = []
    ElMessage.error(error.message || '活动加载失败')
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

function matchFilters(activity) {
  if (filters.category !== '全部' && !splitTags(activity.categoryTags).includes(filters.category)) {
    return false
  }
  if (filters.signupStatus === '名额未满' && availableSlots(activity) <= 0) {
    return false
  }
  if (filters.signupStatus === '名额已满' && availableSlots(activity) > 0) {
    return false
  }
  const points = Number(activity.approvedRewardPoints ?? activity.requestedRewardPoints ?? 0)
  if (filters.minPoints && points < Number(filters.minPoints)) {
    return false
  }
  if (filters.maxPoints && points > Number(filters.maxPoints)) {
    return false
  }
  const startDate = toDateOnly(activity.startDate)
  if (filters.startDate && startDate && startDate < filters.startDate) {
    return false
  }
  if (filters.endDate && startDate && startDate > filters.endDate) {
    return false
  }
  return true
}

function goDetail(activity) {
  if (activity?.activityId) {
    router.push(`/activities/${activity.activityId}`)
  }
}

function splitTags(value) {
  return String(value || '')
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function firstTag(activity) {
  return splitTags(activity.categoryTags)[0] || '志愿服务'
}

function availableSlots(activity) {
  const max = Number(activity.maxParticipants ?? 0)
  const current = Number(activity.currentParticipants ?? 0)
  return Math.max(max - current, 0)
}

function toDateOnly(value) {
  return value ? String(value).slice(0, 10) : ''
}

function formatDateOnly(value) {
  return toDateOnly(value) || '-'
}

function placeholderClass(activity) {
  const tag = firstTag(activity)
  if (tag.includes('文体') || tag.includes('科教')) {
    return 'project-placeholder--blue'
  }
  if (tag.includes('文旅') || tag.includes('赛事')) {
    return 'project-placeholder--purple'
  }
  return 'project-placeholder--red'
}

function placeholderIcon(activity) {
  const tag = firstTag(activity)
  if (tag.includes('文体') || tag.includes('科教')) {
    return '书'
  }
  if (tag.includes('文旅') || tag.includes('赛事')) {
    return '行'
  }
  return '志'
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
.activity-page {
  padding-bottom: 70px;
}

.page-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
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

.page-title-row p,
.portal-loading {
  color: #777;
}

.filter-panel {
  margin-bottom: 28px;
  padding: 2px 0 0;
  background: linear-gradient(180deg, #fff8f4 0%, #ffffff 100%);
}

.filter-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 10px 0;
}

.filter-row strong {
  flex: 0 0 74px;
  color: #1f1f1f;
  font-size: 15px;
  line-height: 28px;
}

.filter-options {
  display: flex;
  flex: 1;
  gap: 12px 18px;
  flex-wrap: wrap;
}

.filter-row button {
  height: 28px;
  padding: 0 10px;
  border: 0;
  background: transparent;
  color: #333;
  cursor: pointer;
  font-size: 15px;
  line-height: 28px;
}

.filter-row button.active {
  background: #e60012;
  color: #fff;
}

.filter-row--compact {
  align-items: center;
}

.compact-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.compact-group :deep(.el-input) {
  width: 150px;
}

.more-line {
  position: relative;
  display: flex;
  justify-content: center;
  margin: 20px 0 18px;
  border-top: 1px solid #e9e9e9;
}

.more-line button {
  min-width: 92px;
  height: 30px;
  margin-top: -15px;
  border: 1px solid #cfcfcf;
  border-radius: 999px;
  background: #fff;
  color: #666;
  cursor: pointer;
}

.query-panel {
  display: grid;
  grid-template-columns: 430px 390px auto;
  gap: 16px;
  align-items: center;
  padding: 16px 28px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(40, 40, 70, 0.06);
}

.query-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.query-item label {
  color: #222;
  font-size: 15px;
  white-space: nowrap;
}

.query-item--keyword :deep(.el-input) {
  flex: 1;
}

.query-range {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.query-range :deep(.el-date-editor) {
  width: 152px;
}

.query-panel :deep(.el-input__wrapper) {
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.query-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.query-actions :deep(.el-button--primary) {
  border-color: #e60012;
  background: #e60012;
}

.reset-button {
  border-color: #d5d5d5;
  background: #c9c9c9;
  color: #fff;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 32px 40px;
}

.project-card {
  display: grid;
  grid-template-rows: auto auto;
  gap: 12px;
  min-height: 246px;
  padding: 14px;
  border: 1px solid #f2f2f2;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 6px 16px rgba(40, 40, 70, 0.08);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.project-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(40, 40, 70, 0.13);
}

.project-card__top {
  display: grid;
  grid-template-columns: 164px minmax(0, 1fr);
  gap: 16px;
}

.project-image {
  position: relative;
  height: 132px;
  overflow: hidden;
  border-radius: 8px;
}

.project-image img,
.project-placeholder {
  width: 100%;
  height: 132px;
}

.project-image img {
  object-fit: cover;
}

.project-label {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1;
  height: 24px;
  padding: 0 12px;
  border-radius: 0 0 8px 0;
  background: #e60012;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 24px;
}

.project-placeholder {
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 44px;
  font-weight: 800;
}

.project-placeholder--red {
  background: linear-gradient(135deg, #ff9ea8, #ffd6d8);
}

.project-placeholder--purple {
  background: linear-gradient(135deg, #bfb5ff, #e6ddff);
}

.project-placeholder--blue {
  background: linear-gradient(135deg, #75c8ff, #d5f1ff);
}

.project-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 132px;
}

.project-main h2 {
  height: 54px;
  margin: 2px 0 18px;
  display: -webkit-box;
  overflow: hidden;
  color: #171717;
  font-size: 18px;
  font-weight: 500;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.project-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: auto;
}

.project-stats div {
  min-height: 42px;
  display: grid;
  gap: 8px;
  justify-items: start;
}

.project-stats div + div {
  padding-left: 20px;
  border-left: 1px solid #eeeeee;
}

.project-stats span {
  color: #999;
  font-size: 13px;
}

.project-stats strong {
  color: #111;
  font-size: 14px;
}

.project-meta {
  display: grid;
  gap: 6px;
  width: 100%;
  padding-top: 2px;
}

.project-meta p {
  margin: 0;
  width: 100%;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

.product-card {
  overflow: hidden;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 30px rgba(161, 43, 61, 0.07);
}

.product-cover {
  height: 150px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #fff0d8, #fff7f7);
  color: #df001b;
  font-size: 24px;
  font-weight: 700;
}

.product-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-body {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.product-body h2,
.product-body p {
  margin: 0;
}

@media (max-width: 1180px) {
  .project-grid,
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-title-row,
  .project-grid,
  .product-grid,
  .query-panel,
  .query-range,
  .query-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .query-item {
    display: grid;
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .project-meta p {
    white-space: normal;
  }

  .project-card__top {
    grid-template-columns: 1fr;
  }

  .project-image {
    height: 160px;
  }

  .project-image img,
  .project-placeholder {
    height: 160px;
  }

  .query-actions {
    margin-left: 0;
  }
}
</style>
