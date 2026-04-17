<template>
  <PortalLayout active-key="organizations" breadcrumb="志愿组织">
    <section class="list-page">
      <div class="portal-shell">
        <div class="page-title-row">
          <div>
            <h1>志愿服务组织</h1>
            <p>未登录可浏览公益组织信息；登录志愿者账号后可申请加入默认归属组织。</p>
          </div>
          <el-button v-if="!isVolunteer" type="primary" @click="router.push('/login?role=volunteer')">志愿者登录后申请加入</el-button>
        </div>

        <div v-if="loading" class="portal-loading">正在加载组织...</div>
        <div v-else class="org-grid">
          <article v-for="org in organizations" :key="org.organizationId || org.organizationName" class="org-card">
            <div class="org-avatar">
              <img v-if="org.avatarPath" :src="resolveImage(org.avatarPath)" :alt="org.organizationName" />
              <span v-else>{{ org.organizationName?.slice(0, 1) || '志' }}</span>
            </div>
            <div class="org-main">
              <h2>{{ org.organizationName }}</h2>
              <p>{{ org.organizationDescription || '该组织已参与平台志愿服务认证，更多公开信息将持续完善。' }}</p>
              <div class="org-meta">
                <span>公开服务记录：{{ org.projectCount || 0 }}</span>
                <span>服务方向：社区服务 / 公益活动</span>
              </div>
              <el-button v-if="isVolunteer && org.organizationId" type="primary" plain @click="openJoinDialog(org)">申请加入组织</el-button>
              <el-button v-else type="primary" plain @click="router.push('/login?role=volunteer')">登录后申请加入</el-button>
            </div>
          </article>
        </div>
        <EmptyState v-if="!loading && !organizations.length" mark="组" title="暂无组织信息" description="公益组织注册并产生公开服务记录后会在这里展示。" />
      </div>
    </section>

    <el-dialog v-model="joinDialogVisible" title="申请加入组织" width="min(560px, calc(100vw - 24px))">
      <el-form label-position="top">
        <el-form-item label="目标组织">
          <el-input :model-value="selectedOrganization?.organizationName" disabled />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="joinReason" type="textarea" :rows="4" placeholder="可填写参与意向、服务经验、可参与时间等信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitJoin">提交申请</el-button>
      </template>
    </el-dialog>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import { getCompletedProjects } from '../api/public'
import { createJoinRequest, getOrganizations } from '../api/platform'
import { getCachedUser, getToken, hasRole } from '../api/auth'

const router = useRouter()
const user = ref(getCachedUser())
const loading = ref(false)
const submitting = ref(false)
const organizations = ref([])
const joinDialogVisible = ref(false)
const selectedOrganization = ref(null)
const joinReason = ref('')

const isVolunteer = computed(() => !!getToken() && hasRole('volunteer', user.value))

onMounted(loadOrganizations)

async function loadOrganizations() {
  loading.value = true
  try {
    if (getToken()) {
      organizations.value = await getOrganizations()
    } else {
      const projects = await getCompletedProjects(30)
      const grouped = new Map()
      projects.forEach((project) => {
        const name = project.organizationName || '公益组织'
        const current = grouped.get(name) || {
          organizationName: name,
          organizationDescription: '该组织已有公开志愿服务成果，登录后可查看更多参与入口。',
          projectCount: 0
        }
        current.projectCount += 1
        grouped.set(name, current)
      })
      organizations.value = Array.from(grouped.values())
    }
  } catch (error) {
    organizations.value = []
  } finally {
    loading.value = false
  }
}

function openJoinDialog(org) {
  selectedOrganization.value = org
  joinReason.value = ''
  joinDialogVisible.value = true
}

async function submitJoin() {
  if (!selectedOrganization.value?.organizationId) {
    ElMessage.warning('请选择目标组织')
    return
  }
  submitting.value = true
  try {
    await createJoinRequest({
      organizationId: selectedOrganization.value.organizationId,
      applyReason: joinReason.value
    })
    ElMessage.success('组织申请已提交')
    joinDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '提交申请失败')
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

.page-title-row h1 {
  margin: 0;
  font-size: 30px;
}

.page-title-row p,
.portal-loading {
  color: #777;
}

.org-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}

.org-card {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 20px;
  padding: 24px;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(161, 43, 61, 0.06);
}

.org-avatar {
  width: 88px;
  height: 88px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe2e5, #fff3e8);
  color: #df001b;
  font-size: 38px;
  font-weight: 700;
}

.org-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-main h2 {
  margin: 0;
  font-size: 22px;
}

.org-main p {
  min-height: 50px;
  margin: 12px 0 0;
  color: #666;
  line-height: 1.7;
}

.org-meta {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  margin: 16px 0;
  color: #777;
  font-size: 13px;
}

@media (max-width: 900px) {
  .page-title-row,
  .org-grid {
    display: grid;
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .org-card {
    grid-template-columns: 1fr;
  }
}
</style>
