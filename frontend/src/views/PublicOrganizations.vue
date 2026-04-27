<template>
  <PortalLayout active-key="organizations" breadcrumb="志愿组织">
    <section class="list-page">
      <div class="portal-shell">
        <div class="page-title-row">
          <div>
            <h1>志愿服务组织</h1>
            <p>浏览公益组织信息，登录志愿者账号后可申请加入默认归属组织。</p>
          </div>
          <el-button v-if="!isVolunteer" type="primary" @click="router.push('/login?role=volunteer')">登录后申请加入</el-button>
        </div>

        <div class="search-panel">
          <el-input
            v-model="searchKeyword"
            size="large"
            clearable
            placeholder="搜索组织名称或服务介绍"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" size="large" :loading="loading" @click="handleSearch">搜索</el-button>
        </div>

        <div v-if="loading" class="portal-loading">正在加载组织...</div>
        <div v-else class="org-grid">
          <article v-for="org in organizations" :key="org.organizationId || org.organizationName" class="org-card">
            <div class="org-card__top">
              <div class="org-avatar">
                <img v-if="org.avatarPath" :src="resolveImage(org.avatarPath)" :alt="org.organizationName" />
                <span v-else>{{ org.organizationName?.slice(0, 1) || '志' }}</span>
              </div>
              <div>
                <h2>{{ org.organizationName }}</h2>
                <span>{{ org.publicActivityCount || org.projectCount || 0 }} 个公开活动</span>
              </div>
            </div>
            <p>{{ org.organizationDescription || '该组织已参与平台志愿服务认证，更多公开信息将持续完善。' }}</p>
            <div class="org-meta">
              <el-tag effect="plain" size="small">社区服务</el-tag>
              <el-tag effect="plain" size="small">公益活动</el-tag>
              <el-tag effect="plain" size="small">志愿协同</el-tag>
            </div>
            <div class="org-actions">
              <el-button v-if="isVolunteer && org.organizationId" type="primary" plain @click="openJoinDialog(org)">申请加入组织</el-button>
              <el-button v-else type="primary" plain @click="router.push('/login?role=volunteer')">登录后申请加入</el-button>
            </div>
          </article>
        </div>
        <EmptyState v-if="!loading && !organizations.length" mark="组" title="暂无匹配组织" description="换个关键词试试，或等待新的公益组织入驻。" />
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
import { getPublicOrganizations } from '../api/public'
import { createJoinRequest } from '../api/platform'
import { getCachedUser, getToken, hasRole } from '../api/auth'

const router = useRouter()
const user = ref(getCachedUser())
const loading = ref(false)
const submitting = ref(false)
const organizations = ref([])
const searchKeyword = ref('')
const searchedKeyword = ref('')
const joinDialogVisible = ref(false)
const selectedOrganization = ref(null)
const joinReason = ref('')

const isVolunteer = computed(() => !!getToken() && hasRole('volunteer', user.value))

onMounted(loadOrganizations)

async function handleSearch() {
  searchedKeyword.value = searchKeyword.value.trim()
  await loadOrganizations()
}

async function loadOrganizations() {
  loading.value = true
  try {
    organizations.value = await getPublicOrganizations({ keyword: searchedKeyword.value })
  } catch (error) {
    organizations.value = []
    ElMessage.error(error.message || '组织加载失败')
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
  margin-bottom: 20px;
}

.page-title-row h1 {
  margin: 0;
  font-size: 30px;
}

.page-title-row p,
.portal-loading {
  color: #777;
}

.search-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
  margin-bottom: 22px;
  padding: 18px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(161, 43, 61, 0.06);
}

.org-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.org-card {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  padding: 22px;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 30px rgba(161, 43, 61, 0.07);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.org-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 20px 42px rgba(161, 43, 61, 0.12);
}

.org-card__top {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 16px;
  align-items: center;
}

.org-avatar {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe2e5, #fff3e8);
  color: #df001b;
  font-size: 32px;
  font-weight: 700;
}

.org-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-card h2 {
  margin: 0;
  color: #2f1e23;
  font-size: 21px;
  line-height: 1.35;
}

.org-card__top span {
  display: inline-block;
  margin-top: 8px;
  color: #888;
  font-size: 13px;
}

.org-card p {
  min-height: 78px;
  margin: 18px 0 0;
  color: #666;
  line-height: 1.7;
}

.org-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.org-actions {
  margin-top: auto;
  padding-top: 18px;
}

@media (max-width: 1100px) {
  .org-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .page-title-row,
  .org-grid,
  .search-panel {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
