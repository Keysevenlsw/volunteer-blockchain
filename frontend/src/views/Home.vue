<template>
  <PortalLayout home active-key="home">
    <section class="home-entry">
      <div class="portal-shell">
        <div class="entry-grid">
          <button
            v-for="item in entryCards"
            :key="item.title"
            type="button"
            class="entry-card"
            @click="router.push(item.path)"
          >
            <span class="entry-card__icon" :class="item.tone">{{ item.icon }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.subtitle }}</small>
          </button>
        </div>
      </div>
    </section>

    <section class="home-section">
      <div class="portal-shell home-two-col">
        <article class="home-panel">
          <div class="section-head">
            <h2>通知公告</h2>
            <RouterLink to="/notices">更多</RouterLink>
          </div>
          <div v-if="infoLoading" class="portal-loading">正在加载公告...</div>
          <ul v-else class="notice-list">
            <li v-for="item in infoList" :key="`${item.title}-${item.date}`">
              <span>{{ item.tag || '公告' }}</span>
              <strong>{{ item.title }}</strong>
              <time>{{ formatDate(item.date) }}</time>
            </li>
          </ul>
          <EmptyState v-if="!infoLoading && !infoList.length" mark="告" title="暂无公告" description="平台公告会在这里公开展示。" />
        </article>

        <article class="home-panel">
          <div class="section-head">
            <h2>链上存证</h2>
            <RouterLink to="/publicity">更多</RouterLink>
          </div>
          <div v-if="evidenceLoading" class="portal-loading">正在加载存证...</div>
          <div v-else class="evidence-list">
            <div v-for="item in evidences" :key="item.id || `${item.bizType}-${item.bizId}`" class="evidence-row">
              <div>
                <strong>{{ item.activityName || '志愿服务记录' }}</strong>
                <p>{{ item.organizationName || '公益组织' }} · {{ item.username || '志愿者' }}</p>
              </div>
              <StatusBadge :label="getStatusMeta('evidence', item.onchainStatus).label" :tone="getStatusMeta('evidence', item.onchainStatus).tone" />
            </div>
          </div>
          <EmptyState v-if="!evidenceLoading && !evidences.length" mark="链" title="暂无存证" description="服务记录审核通过后会在这里展示链上摘要。" />
        </article>
      </div>
    </section>

    <section class="home-section home-section--soft">
      <div class="portal-shell">
        <div class="section-head">
          <div>
            <h2>服务公示</h2>
            <p>展示已审核通过并完成可信存证的志愿服务成果。</p>
          </div>
          <RouterLink to="/publicity">查看全部</RouterLink>
        </div>
        <div v-if="projectLoading" class="portal-loading">正在加载公示...</div>
        <div v-else class="project-grid">
          <article v-for="project in projects" :key="project.id" class="project-card">
            <div class="project-cover">
              <img v-if="project.image" :src="resolveImage(project.image)" :alt="project.title" />
              <span v-else>志愿服务</span>
            </div>
            <div class="project-content">
              <StatusBadge :label="getStatusMeta('evidence', project.evidenceStatus).label" :tone="getStatusMeta('evidence', project.evidenceStatus).tone" />
              <h3>{{ project.title }}</h3>
              <p>{{ project.description || '暂无服务说明' }}</p>
              <div class="project-meta">
                <span>{{ project.organizationName || '公益组织' }}</span>
                <span>{{ formatDate(project.endDate) }}</span>
              </div>
              <div class="hash-line">摘要：{{ shortHash(project.digest, 12, 8) }}</div>
            </div>
          </article>
        </div>
        <EmptyState v-if="!projectLoading && !projects.length" mark="公" title="暂无服务公示" description="完成报告审核通过后会在这里展示。" />
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { getCompletedProjects, getPublicEvidences, getPublicInfo } from '../api/public'
import { formatDate, getStatusMeta, shortHash } from '../utils/ui'

const router = useRouter()
const infoList = ref([])
const projects = ref([])
const evidences = ref([])
const infoLoading = ref(false)
const projectLoading = ref(false)
const evidenceLoading = ref(false)

const entryCards = [
  { title: '志愿者注册', subtitle: '我要加入志愿者', icon: '+', tone: 'orange', path: '/register?role=volunteer' },
  { title: '志愿组织注册', subtitle: '注册成为组织', icon: '组', tone: 'purple', path: '/register?role=organization_admin' },
  { title: '志愿服务活动', subtitle: '我想参加志愿服务活动', icon: '活', tone: 'blue', path: '/activities' },
  { title: '志愿服务组织', subtitle: '查看加入志愿服务组织', icon: '织', tone: 'pink', path: '/organizations' },
  { title: '时长公示', subtitle: '查看服务认证结果', icon: '时', tone: 'red', path: '/publicity' },
  { title: '积分商城', subtitle: '登录后兑换公益积分', icon: '兑', tone: 'orange', path: '/activities?panel=rewards' },
  { title: '通知公告', subtitle: '平台动态和通知', icon: '告', tone: 'blue', path: '/notices' },
  { title: '帮助中心', subtitle: '了解平台使用流程', icon: '?', tone: 'purple', path: '/help' }
]

onMounted(async () => {
  await Promise.all([loadInfo(), loadProjects(), loadEvidences()])
})

async function loadInfo() {
  infoLoading.value = true
  try {
    infoList.value = await getPublicInfo()
  } catch (error) {
    infoList.value = []
  } finally {
    infoLoading.value = false
  }
}

async function loadProjects() {
  projectLoading.value = true
  try {
    projects.value = await getCompletedProjects(6)
  } catch (error) {
    projects.value = []
  } finally {
    projectLoading.value = false
  }
}

async function loadEvidences() {
  evidenceLoading.value = true
  try {
    evidences.value = await getPublicEvidences({ limit: 5, status: 'success' })
  } catch (error) {
    evidences.value = []
  } finally {
    evidenceLoading.value = false
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
.home-entry {
  padding: 42px 0 18px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.entry-card {
  min-height: 176px;
  padding: 26px 20px;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 14px 30px rgba(174, 36, 50, 0.06);
  text-align: center;
}

.entry-card:hover {
  border-color: rgba(223, 0, 27, 0.32);
}

.entry-card__icon {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  margin: 0 auto 20px;
  border-radius: 8px;
  color: #fff;
  font-size: 26px;
  font-weight: 700;
}

.entry-card__icon.orange { background: linear-gradient(135deg, #ffb657, #ff6a1a); }
.entry-card__icon.purple { background: linear-gradient(135deg, #c0a4ff, #7049e6); }
.entry-card__icon.blue { background: linear-gradient(135deg, #70d0ff, #2275d6); }
.entry-card__icon.pink { background: linear-gradient(135deg, #ff9ab4, #e52861); }
.entry-card__icon.red { background: linear-gradient(135deg, #ff8b9a, #df001b); }

.entry-card strong {
  display: block;
  font-size: 22px;
  color: #111;
}

.entry-card small {
  display: block;
  margin-top: 8px;
  color: #777;
  font-size: 14px;
}

.home-section {
  padding: 34px 0;
}

.home-section--soft {
  background: linear-gradient(180deg, rgba(255, 241, 241, 0.58), rgba(255, 255, 255, 0.88));
}

.home-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
}

.home-panel {
  min-height: 330px;
  padding: 26px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fff;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-head h2 {
  margin: 0;
  color: #171717;
  font-size: 26px;
}

.section-head p {
  margin: 8px 0 0;
  color: #777;
}

.section-head a {
  color: #df001b;
  font-weight: 700;
}

.portal-loading {
  padding: 24px 0;
  color: #777;
}

.notice-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-list li {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) 96px;
  gap: 14px;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px dashed rgba(223, 0, 27, 0.14);
}

.notice-list span {
  color: #df001b;
  font-weight: 700;
}

.notice-list strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-list time {
  color: #888;
}

.evidence-list {
  display: grid;
  gap: 14px;
}

.evidence-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fffafa;
}

.evidence-row p {
  margin: 8px 0 0;
  color: #777;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.project-card {
  overflow: hidden;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
}

.project-cover {
  height: 168px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ffd1d7, #fff1e8);
  color: #df001b;
  font-size: 22px;
  font-weight: 700;
}

.project-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.project-content {
  padding: 20px;
}

.project-content h3 {
  margin: 14px 0 0;
  font-size: 20px;
}

.project-content p {
  min-height: 50px;
  margin: 10px 0 0;
  color: #666;
  line-height: 1.7;
}

.project-meta,
.hash-line {
  margin-top: 14px;
  color: #777;
  font-size: 13px;
}

.project-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 1100px) {
  .entry-grid,
  .project-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .entry-grid,
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
