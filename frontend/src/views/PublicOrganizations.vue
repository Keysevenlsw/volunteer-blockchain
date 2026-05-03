<template>
  <PortalLayout active-key="organizations" breadcrumb="志愿组织">
    <section class="list-page">
      <div class="portal-shell">
        <div class="search-panel">
          <div class="search-item search-item--name">
            <label>组织名称</label>
            <el-input
              v-model="searchKeyword"
              placeholder="请输入关键词"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </div>
          <div class="search-item search-item--id">
            <label>组织编号</label>
            <el-input
              v-model="searchOrganizationId"
              placeholder="请输入"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </div>
          <div class="search-actions">
            <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </div>
        </div>

        <div v-if="loading" class="portal-loading">正在加载组织...</div>
        <div v-else class="org-grid">
          <article
            v-for="org in pagedOrganizations"
            :key="org.organizationId || org.organizationName"
            class="org-card"
            role="button"
            tabindex="0"
            @click="goDetail(org)"
            @keyup.enter="goDetail(org)"
          >
            <div class="org-card__top">
              <div class="org-avatar">
                <img v-if="org.avatarPath" :src="resolveImage(org.avatarPath)" :alt="org.organizationName" />
                <span v-else>{{ org.organizationName?.slice(0, 1) || '志' }}</span>
              </div>
              <div class="org-headline">
                <h2 :title="org.organizationName">{{ org.organizationName }}</h2>
                <span>组织编号：{{ org.organizationId }}</span>
              </div>
            </div>

            <p :title="plainDescription(org.organizationDescription) || '该组织已参与平台志愿服务认证。'">
              {{ plainDescription(org.organizationDescription) || '该组织已参与平台志愿服务认证。' }}
            </p>

            <div class="org-metrics">
              <div>
                <span>志愿者数</span>
                <strong>{{ org.volunteerCount ?? 0 }}</strong>
              </div>
              <div>
                <span>志愿活动数</span>
                <strong>{{ org.publicActivityCount ?? 0 }}</strong>
              </div>
            </div>
          </article>
        </div>

        <div v-if="!loading && organizations.length > pageSize" class="pagination-row">
          <el-pagination
            background
            layout="prev, pager, next"
            :page-size="pageSize"
            :total="organizations.length"
            :current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>

        <EmptyState v-if="!loading && !organizations.length" mark="组" title="暂无匹配组织" description="换个关键词试试，或等待新的公益组织入驻。" />
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import { getPublicOrganizations } from '../api/public'

const router = useRouter()
const loading = ref(false)
const organizations = ref([])
const searchKeyword = ref('')
const searchOrganizationId = ref('')
const searchedKeyword = ref('')
const searchedOrganizationId = ref('')
const currentPage = ref(1)
const pageSize = 9

const pagedOrganizations = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return organizations.value.slice(start, start + pageSize)
})

onMounted(loadOrganizations)

async function handleSearch() {
  searchedKeyword.value = searchKeyword.value.trim()
  searchedOrganizationId.value = searchOrganizationId.value.trim()
  currentPage.value = 1
  await loadOrganizations()
}

async function resetSearch() {
  searchKeyword.value = ''
  searchOrganizationId.value = ''
  searchedKeyword.value = ''
  searchedOrganizationId.value = ''
  currentPage.value = 1
  await loadOrganizations()
}

function handlePageChange(page) {
  currentPage.value = page
}

async function loadOrganizations() {
  loading.value = true
  try {
    organizations.value = await getPublicOrganizations({
      keyword: searchedKeyword.value,
      organizationId: searchedOrganizationId.value
    })
  } catch (error) {
    organizations.value = []
    ElMessage.error(error.message || '组织加载失败')
  } finally {
    loading.value = false
  }
}

function goDetail(org) {
  if (org?.organizationId) {
    router.push(`/organizations/${org.organizationId}`)
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

function plainDescription(value) {
  return String(value || '')
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}
</script>

<style scoped>
.list-page {
  padding-bottom: 70px;
}

.portal-loading {
  color: #777;
}

.search-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px 26px;
  margin: 8px 0 30px;
  padding: 16px 0;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-item label {
  flex: 0 0 auto;
  color: #333;
  font-size: 15px;
  white-space: nowrap;
}

.search-item--name :deep(.el-input) {
  width: 274px;
}

.search-item--id :deep(.el-input) {
  width: 274px;
}

.search-panel :deep(.el-input__wrapper) {
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.search-actions {
  display: flex;
  gap: 10px;
}

.search-actions :deep(.el-button) {
  min-width: 64px;
}

.search-actions :deep(.el-button--primary) {
  border-color: #e60012;
  background: #e60012;
}

.search-actions :deep(.el-button:last-child) {
  border-color: #d5d5d5;
  background: #c9c9c9;
  color: #fff;
}

.org-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.org-card {
  min-height: 250px;
  display: flex;
  flex-direction: column;
  padding: 20px;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(40, 40, 70, 0.08);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.org-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(40, 40, 70, 0.12);
}

.org-card__top {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
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

.org-headline h2 {
  margin: 0;
  overflow: hidden;
  color: #2f1e23;
  font-size: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-headline span {
  display: inline-block;
  margin-top: 8px;
  color: #888;
  font-size: 13px;
}

.org-card p {
  min-height: 68px;
  margin: 18px 0 0;
  display: -webkit-box;
  overflow: hidden;
  color: #666;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.org-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: auto;
  padding-top: 18px;
}

.org-metrics div {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #fff7f7;
}

.org-metrics span {
  color: #888;
  font-size: 13px;
}

.org-metrics strong {
  color: #df001b;
  font-size: 24px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 1100px) {
  .org-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .search-panel,
  .search-item,
  .search-actions,
  .org-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .search-item--name :deep(.el-input),
  .search-item--id :deep(.el-input) {
    width: 100%;
  }
}
</style>
