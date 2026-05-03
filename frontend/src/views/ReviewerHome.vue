<template>
  <PortalLayout active-key="home" breadcrumb="审核工作台">
    <section class="reviewer-page" v-loading="pageLoading">
      <div class="portal-shell reviewer-layout">
        <aside class="reviewer-side">
          <strong>审核工作台</strong>
          <button v-if="canReviewActivities" type="button" :class="{ active: activeTab === 'activities' }" @click="activeTab = 'activities'">活动审核</button>
          <button v-if="canReviewProducts" type="button" :class="{ active: activeTab === 'products' }" @click="activeTab = 'products'">商品审核</button>
        </aside>

        <main class="reviewer-main">
          <section class="review-summary">
            <article><span>待审活动</span><strong>{{ activityReviews.filter((item) => item.reviewStatus === 'pending_review').length }}</strong></article>
            <article><span>待审商品</span><strong>{{ productReviews.filter((item) => item.reviewStatus === 'pending_review').length }}</strong></article>
            <article><span>需复审活动</span><strong>{{ activityReviews.filter((item) => item.escalationRequired).length }}</strong></article>
          </section>

          <section v-if="activeTab === 'activities'" class="review-panel">
            <div class="panel-head">
              <h1>活动审核</h1>
              <el-button @click="loadReviews">刷新</el-button>
            </div>
            <div class="review-grid">
              <article v-for="item in activityReviews" :key="item.activityId" class="review-card">
                <div class="card-topline">
                  <h2>{{ item.activityName }}</h2>
                  <StatusBadge :label="getStatusMeta('review', item.reviewStatus).label" :tone="getStatusMeta('review', item.reviewStatus).tone" />
                </div>
                <p>{{ item.organizationName }} · {{ item.location || '未设置地点' }}</p>
                <dl>
                  <div><dt>申请积分</dt><dd>{{ item.requestedRewardPoints }}</dd></div>
                  <div><dt>建议积分</dt><dd>{{ item.recommendedPoints ?? '-' }}</dd></div>
                  <div><dt>最高积分</dt><dd>{{ item.maxPoints ?? '-' }}</dd></div>
                  <div><dt>复审阈值</dt><dd>{{ item.escalationThreshold ?? '-' }}</dd></div>
                </dl>
                <el-alert v-if="item.escalationRequired" type="warning" :closable="false" title="该活动申请积分较高，可升级复审或由系统管理员处理。" />
                <div class="actions">
                  <el-button type="success" :disabled="item.reviewStatus !== 'pending_review'" @click="openActivityReview(item, 'approved')">通过</el-button>
                  <el-button type="warning" plain :disabled="item.reviewStatus !== 'pending_review'" @click="openActivityReview(item, 'escalated')">升级复审</el-button>
                  <el-button type="danger" plain :disabled="item.reviewStatus !== 'pending_review'" @click="openActivityReview(item, 'rejected')">驳回</el-button>
                </div>
              </article>
            </div>
            <EmptyState v-if="!activityReviews.length" mark="审" title="暂无活动审核" description="组织提交活动审核后会在这里出现。" />
          </section>

          <section v-else class="review-panel">
            <div class="panel-head">
              <h1>商品审核</h1>
              <el-button @click="loadReviews">刷新</el-button>
            </div>
            <div class="review-grid">
              <article v-for="item in productReviews" :key="item.productId" class="review-card">
                <div class="card-topline">
                  <h2>{{ item.productName }}</h2>
                  <StatusBadge :label="getStatusMeta('review', item.reviewStatus).label" :tone="getStatusMeta('review', item.reviewStatus).tone" />
                </div>
                <p>{{ item.organizationName }} · {{ item.price }} 积分 · 库存 {{ item.stock }}</p>
                <p>{{ item.productDescription || '暂无商品说明' }}</p>
                <div class="actions">
                  <el-button type="success" :disabled="item.reviewStatus !== 'pending_review'" @click="handleProductReview(item, 'approved')">通过</el-button>
                  <el-button type="danger" plain :disabled="item.reviewStatus !== 'pending_review'" @click="handleProductReview(item, 'rejected')">驳回</el-button>
                  <el-button plain :disabled="item.reviewStatus !== 'approved'" @click="handleProductReview(item, 'off_shelf')">下架</el-button>
                </div>
              </article>
            </div>
            <EmptyState v-if="!productReviews.length" mark="商" title="暂无商品审核" description="组织提交商品审核后会在这里出现。" />
          </section>
        </main>
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { getActivityReviews, getProductReviews, reviewActivity, reviewProduct } from '../api/admin'
import { clearAuth, getCachedUser, getCurrentUser, getToken, hasRole, redirectToLogin, saveAuth } from '../api/auth'
import { getStatusMeta } from '../utils/ui'

const router = useRouter()
const user = ref(getCachedUser())
const pageLoading = ref(false)
const activeTab = ref('activities')
const activityReviews = ref([])
const productReviews = ref([])

const canReviewActivities = computed(() => hasRole('activity_reviewer', user.value) || hasRole('system_admin', user.value))
const canReviewProducts = computed(() => hasRole('product_reviewer', user.value) || hasRole('system_admin', user.value))

onMounted(initializePage)

async function initializePage() {
  pageLoading.value = true
  try {
    await loadProfile()
    if (!canReviewActivities.value && canReviewProducts.value) {
      activeTab.value = 'products'
    }
    await loadReviews()
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

async function loadReviews() {
  const [activityData, productData] = await Promise.all([
    canReviewActivities.value ? getActivityReviews().catch(() => []) : Promise.resolve([]),
    canReviewProducts.value ? getProductReviews().catch(() => []) : Promise.resolve([])
  ])
  activityReviews.value = activityData
  productReviews.value = productData
}

async function openActivityReview(item, status) {
  try {
    const action = status === 'approved' ? '通过' : status === 'escalated' ? '升级复审' : '驳回'
    const promptOptions = status === 'approved'
      ? { inputPlaceholder: '请输入批准积分', inputValue: String(item.recommendedPoints ?? item.requestedRewardPoints ?? 0) }
      : { inputPlaceholder: '请输入审核说明' }
    const { value } = await ElMessageBox.prompt(`确认${action}“${item.activityName}”吗？`, `${action}活动`, promptOptions)
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
    await reviewProduct(item.productId, {
      status,
      reviewNote: value || ''
    })
    ElMessage.success(`商品审核已${action}`)
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}
</script>

<style scoped>
.reviewer-page {
  padding-bottom: 72px;
}

.reviewer-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 22px;
}

.reviewer-side,
.review-panel,
.review-summary article {
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fff;
}

.reviewer-side {
  overflow: hidden;
}

.reviewer-side strong,
.reviewer-side button {
  width: 100%;
  min-height: 50px;
  display: flex;
  align-items: center;
  border: 0;
  border-left: 4px solid transparent;
  background: #fff;
  color: #333;
  cursor: pointer;
  padding: 0 22px;
  font-size: 16px;
}

.reviewer-side strong,
.reviewer-side button.active {
  border-left-color: #df001b;
  background: #fff0f2;
  color: #df001b;
}

.review-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.review-summary article {
  padding: 20px;
}

.review-summary span {
  color: #777;
}

.review-summary strong {
  display: block;
  margin-top: 10px;
  font-size: 32px;
}

.review-panel {
  padding: 24px;
}

.panel-head,
.card-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-head {
  margin-bottom: 20px;
}

.panel-head h1,
.review-card h2 {
  margin: 0;
}

.review-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.review-card {
  padding: 18px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fffafa;
}

.review-card p {
  color: #666;
  line-height: 1.7;
}

.review-card dl {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.review-card dt {
  color: #888;
  font-size: 12px;
}

.review-card dd {
  margin: 6px 0 0;
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

@media (max-width: 1000px) {
  .reviewer-layout,
  .review-summary,
  .review-grid {
    grid-template-columns: 1fr;
  }
}
</style>
