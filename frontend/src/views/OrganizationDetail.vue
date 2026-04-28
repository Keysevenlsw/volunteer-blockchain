<template>
  <PortalLayout
    active-key="organizations"
    :breadcrumb-items="[
      { label: '志愿组织', path: '/organizations' },
      { label: '组织详情' }
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
              <p>{{ organization.organizationDescription || '该组织已参与平台志愿服务认证。' }}</p>
              <div class="org-hero__metrics">
                <div>
                  <span>志愿者数</span>
                  <strong>{{ organization.volunteerCount ?? 0 }}</strong>
                </div>
                <div>
                  <span>志愿活动数</span>
                  <strong>{{ organization.publicActivityCount ?? 0 }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section class="org-section">
            <h2>公开活动</h2>
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
                <h3>{{ activity.activityName }}</h3>
                <p>{{ activity.description || '暂无活动说明' }}</p>
                <div class="org-activity-meta">
                  <span>积分：{{ activity.approvedRewardPoints ?? activity.requestedRewardPoints ?? 0 }}</span>
                  <span>地点：{{ activity.location || '待公布' }}</span>
                </div>
              </article>
            </div>
            <EmptyState v-if="!activityLoading && !activities.length" mark="活" title="暂无公开活动" description="该组织当前还没有公开活动。" />
          </section>
        </template>

        <EmptyState v-else mark="组" title="组织不存在" description="组织可能已下线或暂未公开。" />
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import { getPublicActivities, getPublicOrganization } from '../api/public'

const route = useRoute()
const router = useRouter()
const organization = ref(null)
const activities = ref([])
const loading = ref(false)
const activityLoading = ref(false)

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
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 22px;
  padding: 26px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(40, 40, 70, 0.08);
}

.org-hero__avatar {
  width: 120px;
  height: 120px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe2e5, #fff3e8);
  color: #df001b;
  font-size: 48px;
  font-weight: 700;
}

.org-hero__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-hero__main h1 {
  margin: 0;
  color: #2f1e23;
  font-size: 30px;
}

.org-hero__main p {
  margin: 16px 0 0;
  color: #666;
  line-height: 1.8;
}

.org-hero__metrics {
  display: grid;
  grid-template-columns: repeat(2, 220px);
  gap: 16px;
  margin-top: 22px;
}

.org-hero__metrics div {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 8px;
  background: #fff7f7;
}

.org-hero__metrics span {
  color: #888;
  font-size: 13px;
}

.org-hero__metrics strong {
  color: #df001b;
  font-size: 26px;
}

.org-section {
  margin-top: 34px;
}

.org-section h2 {
  margin: 0 0 18px;
  color: #2f1e23;
  font-size: 22px;
}

.org-activity-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.org-activity-card {
  min-height: 170px;
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
  margin: 12px 0 0;
  color: #666;
  line-height: 1.7;
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
  .org-activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .org-hero,
  .org-hero__metrics,
  .org-activity-grid {
    grid-template-columns: 1fr;
  }
}
</style>
