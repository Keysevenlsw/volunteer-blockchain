<template>
  <PortalLayout
    active-key="activities"
    :breadcrumb-items="[
      { label: '志愿活动', path: '/activities' },
      { label: '志愿活动详情' }
    ]"
    breadcrumb-back-text="返回上一页"
  >
    <section class="detail-page">
      <div class="portal-shell">
        <div v-if="loading" class="portal-loading">正在加载活动详情...</div>

        <template v-else-if="activity">
          <section class="detail-hero">
            <div class="detail-image">
              <span class="detail-label">{{ firstTag(activity) }}</span>
              <img v-if="activity.imagePath" :src="resolveImage(activity.imagePath)" :alt="activity.activityName" />
              <div v-else class="detail-placeholder" :class="placeholderClass(activity)">
                <span>{{ placeholderIcon(activity) }}</span>
              </div>
            </div>

            <div class="detail-summary">
              <h1>{{ activity.activityName }}</h1>
              <p class="activity-code">
                活动编号：{{ activity.activityId }}
                <button type="button" @click="copyActivityId">复制</button>
              </p>
              <div class="tag-row">
                <el-tag v-for="tag in splitTags(activity.categoryTags)" :key="tag" effect="plain">{{ tag }}</el-tag>
              </div>

              <div class="summary-list">
                <div class="summary-item"><label>积分</label><span>{{ activity.approvedRewardPoints ?? activity.requestedRewardPoints ?? 0 }}</span></div>
                <div class="summary-item"><label>参与对象</label><span>全部志愿者</span></div>
                <div class="summary-item"><label>服务对象</label><span>社区居民、公益服务对象</span></div>
                <div class="summary-item"><label>服务保障</label><span>基础保障</span></div>
                <div class="summary-item"><label>活动时间</label><span>{{ formatDateOnly(activity.startDate) }} 至 {{ formatDateOnly(activity.endDate) }}</span></div>
                <div class="summary-item"><label>招募时间</label><span>{{ formatDateTime(activity.publishDate) }} 至 {{ formatDateTime(activity.enrollDeadline) }}</span></div>
              </div>
            </div>

            <div class="detail-action">
              <el-button type="primary" size="large" :loading="joining" @click="handleJoin">参与活动</el-button>
            </div>
          </section>

          <section class="info-grid">
            <article class="info-card">
              <h2>活动发起人</h2>
              <strong>{{ activity.organizationName || '公益组织' }}</strong>
              <p>地址：{{ activity.location || '待公布' }}</p>
            </article>
            <article class="info-card">
              <h2>活动联系人</h2>
              <strong>{{ activity.contactName || '活动联系人' }}</strong>
              <p>{{ maskPhone(activity.contactPhone) }}</p>
            </article>
            <article class="info-card">
              <h2>活动地址</h2>
              <strong>{{ activity.location || '待公布' }}</strong>
            </article>
          </section>

          <section class="position-section">
            <h2>岗位信息</h2>
            <div class="position-table">
              <div class="position-head">
                <span>岗位名称</span>
                <span>岗位描述</span>
                <span>岗位条件</span>
              </div>
              <div class="position-row">
                <span>志愿服务岗</span>
                <p>{{ activity.description || '参与现场服务、秩序维护、咨询引导及相关公益协助工作。' }}</p>
                <p>在“链信公益”完成实名认证的志愿者。</p>
              </div>
            </div>
          </section>

          <section class="tabs-section">
            <div class="tabs-header">
              <button type="button" :class="{ active: activeTab === 'intro' }" @click="activeTab = 'intro'">活动简介</button>
              <button type="button" :class="{ active: activeTab === 'signup' }" @click="activeTab = 'signup'">报名信息</button>
            </div>

            <div class="tabs-body" v-if="activeTab === 'intro'">
              <h3>活动内容</h3>
              <div class="intro-content">
                <p>【活动内容】{{ activity.description || '围绕社区公益服务开展现场协助、秩序维护、咨询引导和便民服务。' }}</p>
                <p>【签到地点】{{ activity.location || '活动地点待公布' }}</p>
                <p>【其他事项】请提前 10 分钟到场，服从活动安排，文明沟通，保持良好服务形象。</p>
                <p>【注意事项】报名后如因特殊原因无法到岗，请尽早联系活动负责人，避免影响服务安排。</p>
              </div>
            </div>

            <div class="tabs-body" v-else>
              <div v-if="registrationLoading" class="portal-loading">正在加载报名信息...</div>
              <div v-else class="signup-table">
                <div class="signup-head">
                  <span>活动日期</span>
                  <span>场次时间</span>
                  <span>姓名</span>
                  <span>岗位</span>
                  <span>服务时长（小时）</span>
                  <span>报名时间</span>
                  <span>报名方式</span>
                </div>
                <div v-for="item in registrations" :key="item.participationId" class="signup-row">
                  <span>{{ item.activityDate || '-' }}</span>
                  <span>{{ item.sessionTime || '-' }}</span>
                  <span>{{ item.username || '-' }}</span>
                  <span>{{ item.positionName || '-' }}</span>
                  <span>{{ item.serviceHours ?? '-' }}</span>
                  <span>{{ item.signupTime || '-' }}</span>
                  <span>{{ item.signupMethod || '-' }}</span>
                </div>
                <EmptyState v-if="!registrations.length" mark="报" title="暂无报名信息" description="当前活动还没有公开报名记录。" />
              </div>
            </div>
          </section>
        </template>

        <EmptyState v-else mark="活" title="活动不存在" description="活动可能已下线或暂未公开。" />
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import { getPublicActivity, getPublicActivityRegistrations } from '../api/public'
import { joinActivity } from '../api/platform'
import { getCachedUser, getToken, hasRole, redirectToLogin } from '../api/auth'
import { formatDateTime } from '../utils/ui'

const route = useRoute()
const router = useRouter()
const activity = ref(null)
const registrations = ref([])
const loading = ref(false)
const registrationLoading = ref(false)
const joining = ref(false)
const activeTab = ref('intro')
const user = ref(getCachedUser())

onMounted(loadPageData)

async function loadPageData() {
  await Promise.all([loadActivity(), loadRegistrations()])
}

async function loadActivity() {
  loading.value = true
  try {
    activity.value = await getPublicActivity(route.params.id)
  } catch (error) {
    activity.value = null
    ElMessage.error(error.message || '活动详情加载失败')
  } finally {
    loading.value = false
  }
}

async function loadRegistrations() {
  registrationLoading.value = true
  try {
    registrations.value = await getPublicActivityRegistrations(route.params.id)
  } catch (error) {
    registrations.value = []
  } finally {
    registrationLoading.value = false
  }
}

async function handleJoin() {
  user.value = getCachedUser()
  if (!getToken() || !hasRole('volunteer', user.value)) {
    redirectToLogin('volunteer')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认参与“${activity.value.activityName}”吗？提交后将由该组织管理员审核。`,
      '参与活动',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  joining.value = true
  try {
    await joinActivity(activity.value.activityId)
    ElMessage.success('参与申请已提交，等待组织管理员审核')
    activeTab.value = 'signup'
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '参与活动失败')
  } finally {
    joining.value = false
  }
}

async function copyActivityId() {
  try {
    await navigator.clipboard.writeText(String(activity.value?.activityId || ''))
    ElMessage.success('已复制活动编号')
  } catch (error) {
    ElMessage.warning('当前浏览器不支持复制')
  }
}

function splitTags(value) {
  return String(value || '')
    .split(/[,，/]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function firstTag(item) {
  return splitTags(item.categoryTags)[0] || '志愿服务'
}

function formatDateOnly(value) {
  return value ? String(value).slice(0, 10) : '-'
}

function maskPhone(value) {
  if (!value) {
    return '联系电话待公布'
  }
  return String(value).replace(/^(\d{3})\d{4}(\d+)/, '$1****$2')
}

function placeholderClass(item) {
  const tag = firstTag(item)
  if (tag.includes('文体') || tag.includes('科教')) {
    return 'detail-placeholder--blue'
  }
  if (tag.includes('文旅') || tag.includes('赛事')) {
    return 'detail-placeholder--purple'
  }
  return 'detail-placeholder--red'
}

function placeholderIcon(item) {
  const tag = firstTag(item)
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
.detail-page {
  padding-bottom: 76px;
}

.portal-loading {
  padding: 30px 0;
  color: #777;
}

.detail-hero {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) 170px;
  gap: 22px;
  align-items: start;
  padding: 24px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(40, 40, 70, 0.08);
}

.detail-image {
  position: relative;
  height: 200px;
  overflow: hidden;
  border-radius: 8px;
}

.detail-image img,
.detail-placeholder {
  width: 100%;
  height: 100%;
}

.detail-image img {
  object-fit: cover;
}

.detail-label {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1;
  height: 28px;
  padding: 0 14px;
  border-radius: 0 0 8px 0;
  background: #e60012;
  color: #fff;
  line-height: 28px;
  font-weight: 700;
}

.detail-placeholder {
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 58px;
  font-weight: 800;
}

.detail-placeholder--red {
  background: linear-gradient(135deg, #ff9ea8, #ffd6d8);
}

.detail-placeholder--purple {
  background: linear-gradient(135deg, #bfb5ff, #e6ddff);
}

.detail-placeholder--blue {
  background: linear-gradient(135deg, #75c8ff, #d5f1ff);
}

.detail-summary h1 {
  margin: 2px 0 12px;
  color: #25405b;
  font-size: 22px;
  font-weight: 500;
}

.activity-code {
  margin: 0 0 10px;
  color: #555;
}

.activity-code button {
  margin-left: 10px;
  border: 0;
  background: transparent;
  color: #e60012;
  cursor: pointer;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag-row :deep(.el-tag) {
  border-color: #e60012;
  color: #e60012;
}

.summary-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 28px;
}

.summary-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
  white-space: nowrap;
  color: #7d6268;
}

.summary-item label {
  flex: 0 0 auto;
}

.summary-item span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-action {
  padding-top: 26px;
  text-align: center;
}

.detail-action :deep(.el-button--primary) {
  width: 140px;
  height: 46px;
  border-color: #e60012;
  background: #e60012;
  border-radius: 4px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 44px;
  margin-top: 40px;
}

.info-card {
  min-height: 160px;
  padding: 24px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(40, 40, 70, 0.08);
}

.info-card h2 {
  margin: 0;
  color: #e60012;
  font-size: 18px;
}

.info-card strong {
  display: block;
  margin-top: 26px;
  color: #25405b;
  font-size: 18px;
  line-height: 1.6;
}

.info-card p {
  margin: 12px 0 0;
  color: #6b5960;
  line-height: 1.7;
}

.position-section {
  margin-top: 38px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(40, 40, 70, 0.08);
}

.position-section h2 {
  margin: 0;
  padding: 18px 22px;
  background: #fff2f2;
  color: #333;
  font-size: 18px;
  font-weight: 500;
}

.position-head,
.position-row {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
  padding: 18px 22px;
}

.position-head {
  color: #9aa3ad;
  background: #fafafa;
  font-weight: 600;
}

.position-row {
  color: #334455;
  line-height: 1.9;
}

.position-row p,
.position-row span {
  margin: 0;
}

.tabs-section {
  margin-top: 38px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(40, 40, 70, 0.08);
}

.tabs-header {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0 18px;
  background: #e60012;
}

.tabs-header button {
  min-width: 132px;
  height: 56px;
  padding: 0 18px;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  font-size: 18px;
  cursor: pointer;
}

.tabs-header button.active {
  position: relative;
  color: #fff;
  font-weight: 600;
}

.tabs-header button.active::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 0;
  height: 4px;
  background: #ffbf2f;
}

.tabs-body {
  padding: 28px 32px 36px;
}

.tabs-body h3 {
  margin: 0 0 26px;
  color: #2d3f56;
  font-size: 18px;
}

.intro-content {
  color: #334455;
  line-height: 2;
  font-size: 16px;
}

.intro-content p {
  margin: 0 0 8px;
}

.signup-table {
  color: #334455;
}

.signup-head,
.signup-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1.1fr 1.2fr 1fr;
  gap: 16px;
  align-items: center;
}

.signup-head {
  padding: 16px 22px;
  background: #fafafa;
  color: #9aa3ad;
  font-weight: 600;
}

.signup-row {
  padding: 18px 22px;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 1100px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .position-head,
  .position-row,
  .signup-head,
  .signup-row {
    grid-template-columns: 1fr;
  }
}
</style>
