<template>
  <PortalLayout
    active-key="organizations"
    :breadcrumb-items="[
      { label: '志愿组织', path: '/organizations' },
      { label: '志愿组织详情' }
    ]"
    breadcrumb-back-text="返回上一页"
  >
    <section class="detail-page">
      <div class="portal-shell">
        <div v-if="loading" class="portal-loading">正在加载组织详情...</div>

        <template v-else-if="organization">
          <section class="org-hero">
            <div class="org-hero__avatar">
              <img v-if="organization.avatarPath" :src="resolveImage(organization.avatarPath)" :alt="organization.organizationName" />
              <span v-else>{{ organization.organizationName?.slice(0, 1) || '志' }}</span>
            </div>

            <div class="org-hero__main">
              <h1>{{ organization.organizationName }}</h1>
              <div class="org-hero__facts">
                <p>组织编号：{{ organization.organizationId }}</p>
                <p>志愿活动数：{{ organization.publicActivityCount ?? 0 }}</p>
                <p>志愿者数：{{ organization.volunteerCount ?? 0 }}</p>
              </div>
            </div>

            <div class="org-hero__action">
              <el-button type="primary" size="large" :loading="joining" @click="openJoinDialog">加入组织</el-button>
            </div>
          </section>

          <section class="info-grid">
            <article class="info-card">
              <h2>组织负责人</h2>
              <div class="info-card__content">
                <strong>{{ organizationLeader }}</strong>
              </div>
            </article>
            <article class="info-card">
              <h2>组织地址</h2>
              <div class="info-card__content">
                <strong>{{ organizationAddress }}</strong>
              </div>
            </article>
          </section>

          <section class="tabs-section">
            <div class="tabs-header">
              <button type="button" :class="{ active: activeTab === 'intro' }" @click="activeTab = 'intro'">组织简介</button>
              <button type="button" :class="{ active: activeTab === 'activities' }" @click="activeTab = 'activities'">发布活动</button>
            </div>

            <div class="tabs-body" v-if="activeTab === 'intro'">
              <h3>组织简介</h3>
              <div
                class="tab-panel"
                v-html="organization.organizationDescription || '该组织已参与平台志愿服务认证，长期开展公益志愿服务活动。'"
              ></div>
            </div>

            <div class="tabs-body" v-else>
              <div v-if="activityLoading" class="portal-loading">正在加载组织活动...</div>
              <div v-else class="org-activity-grid">
                <article
                  v-for="activity in activities"
                  :key="activity.activityId"
                  class="org-activity-card"
                  role="button"
                  tabindex="0"
                  @click="router.push(`/activities/${activity.activityId}`)"
                  @keyup.enter="router.push(`/activities/${activity.activityId}`)"
                >
                  <h3 :title="activity.activityName">{{ activity.activityName }}</h3>
                  <p :title="activity.description || '暂无活动说明'">{{ activity.description || '暂无活动说明' }}</p>
                  <div class="org-activity-meta">
                    <span>积分：{{ activity.approvedRewardPoints ?? activity.requestedRewardPoints ?? 0 }}</span>
                    <span :title="activity.location || '待公布'">地点：{{ activity.location || '待公布' }}</span>
                  </div>
                </article>
              </div>
              <EmptyState v-if="!activityLoading && !activities.length" mark="活" title="暂无公开活动" description="该组织当前还没有公开活动。" />
            </div>
          </section>
        </template>

        <EmptyState v-else mark="组" title="组织不存在" description="组织可能已下线或暂未公开。" />
      </div>
    </section>

    <el-dialog v-model="joinDialogVisible" title="加入组织申请" width="min(520px, calc(100vw - 24px))" @closed="resetJoinForm">
      <el-form ref="joinFormRef" :model="joinForm" :rules="joinRules" label-position="top">
        <el-form-item label="申请组织">
          <el-input :model-value="organization?.organizationName || '-'" disabled />
        </el-form-item>
        <el-form-item label="申请说明" prop="applyReason">
          <el-input
            v-model.trim="joinForm.applyReason"
            type="textarea"
            :rows="4"
            maxlength="50"
            show-word-limit
            placeholder="请填写加入该组织的原因，50字以内"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="joining" @click="submitJoinOrganization">提交申请</el-button>
      </template>
    </el-dialog>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import { getPublicActivities, getPublicOrganization } from '../api/public'
import { createJoinRequest } from '../api/platform'
import { getCachedUser, getToken, hasRole, redirectToLogin } from '../api/auth'

const route = useRoute()
const router = useRouter()
const organization = ref(null)
const activities = ref([])
const loading = ref(false)
const activityLoading = ref(false)
const joining = ref(false)
const joinDialogVisible = ref(false)
const joinFormRef = ref(null)
const activeTab = ref('intro')
const user = ref(getCachedUser())
const joinForm = reactive({
  applyReason: ''
})

const joinRules = {
  applyReason: [
    { required: true, message: '请填写申请说明', trigger: 'blur' },
    { max: 50, message: '申请说明不能超过 50 个字', trigger: ['blur', 'change'] }
  ]
}

const organizationLeader = computed(() => {
  if (!organization.value?.organizationName) {
    return '组织负责人待公布'
  }
  return `${organization.value.organizationName}负责人`
})

const organizationAddress = computed(() => {
  if (!organization.value?.organizationName) {
    return '组织地址待公布'
  }
  return `${organization.value.organizationName}服务中心`
})

onMounted(loadPageData)

async function loadPageData() {
  await Promise.all([loadOrganization(), loadActivities()])
}

async function loadOrganization() {
  loading.value = true
  try {
    organization.value = await getPublicOrganization(route.params.id)
  } catch (error) {
    organization.value = null
    ElMessage.error(error.message || '组织详情加载失败')
  } finally {
    loading.value = false
  }
}

async function loadActivities() {
  activityLoading.value = true
  try {
    activities.value = await getPublicActivities({ organizationId: route.params.id, limit: 18 })
  } catch (error) {
    activities.value = []
  } finally {
    activityLoading.value = false
  }
}

function openJoinDialog() {
  user.value = getCachedUser()
  if (!getToken() || !hasRole('volunteer', user.value)) {
    redirectToLogin('volunteer')
    return
  }
  joinDialogVisible.value = true
}

function resetJoinForm() {
  joinForm.applyReason = ''
  joinFormRef.value?.clearValidate()
}

async function submitJoinOrganization() {
  if (!joinFormRef.value) {
    return
  }
  try {
    await joinFormRef.value.validate()
  } catch {
    return
  }

  joining.value = true
  try {
    await createJoinRequest({
      organizationId: organization.value.organizationId,
      applyReason: joinForm.applyReason
    })
    ElMessage.success('加入申请已提交，等待组织管理员审核')
    joinDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '加入组织失败')
  } finally {
    joining.value = false
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
.detail-page {
  padding-bottom: 70px;
}

.portal-loading {
  color: #777;
}

.org-hero {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 180px;
  gap: 22px;
  align-items: start;
  padding: 24px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(40, 40, 70, 0.08);
}

.org-hero__avatar {
  width: 220px;
  height: 220px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe2e5, #fff3e8);
  color: #df001b;
  font-size: 72px;
  font-weight: 700;
}

.org-hero__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-hero__main h1 {
  margin: 0;
  color: #25405b;
  font-size: 24px;
  font-weight: 500;
}

.org-hero__facts {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.org-hero__facts p {
  margin: 0;
  color: #6b5960;
  font-size: 15px;
}

.org-hero__action {
  padding-top: 16px;
  text-align: right;
}

.org-hero__action :deep(.el-button--primary) {
  min-width: 140px;
  height: 46px;
  border-color: #e60012;
  background: #e60012;
  border-radius: 4px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
  margin-top: 38px;
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

.info-card__content {
  margin-top: 26px;
}

.info-card__content strong {
  color: #25405b;
  font-size: 18px;
  line-height: 1.7;
}

.tabs-section {
  margin-top: 36px;
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

.tab-panel {
  color: #334455;
  font-size: 16px;
  line-height: 2;
}

.tab-panel :deep(p),
.tab-panel p {
  margin: 0;
}

.org-activity-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.org-activity-card {
  min-height: 180px;
  padding: 18px;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(40, 40, 70, 0.08);
  cursor: pointer;
}

.org-activity-card h3 {
  margin: 0;
  color: #2f1e23;
  font-size: 18px;
  line-height: 1.45;
}

.org-activity-card p {
  min-height: 72px;
  margin: 12px 0 0;
  display: -webkit-box;
  overflow: hidden;
  color: #666;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.org-activity-meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  margin-top: 16px;
  color: #888;
  font-size: 13px;
}

@media (max-width: 980px) {
  .org-hero {
    grid-template-columns: 1fr;
  }

  .org-hero__avatar {
    width: 180px;
    height: 180px;
  }

  .info-grid,
  .org-activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .info-grid,
  .org-activity-grid {
    grid-template-columns: 1fr;
  }
}
</style>
