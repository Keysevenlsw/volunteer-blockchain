<template>
  <PortalLayout active-key="publicity" breadcrumb="时长公示">
    <section class="publicity-page">
      <div class="portal-shell">
        <div class="page-title-row">
          <div>
            <h1>时长公示与链上存证</h1>
            <p>公开展示审核通过的志愿服务成果、链上摘要和校验状态。</p>
          </div>
          <el-input v-model="keyword" placeholder="搜索活动、组织、志愿者" clearable class="search-input" @keyup.enter="loadEvidences" />
        </div>

        <div class="publicity-grid">
          <article class="panel">
            <div class="panel-head">
              <h2>服务成果</h2>
              <el-button plain @click="loadProjects">刷新</el-button>
            </div>
            <div v-if="projectLoading" class="portal-loading">正在加载服务成果...</div>
            <div v-else class="project-list">
              <div v-for="project in projects" :key="project.id" class="project-row">
                <div>
                  <h3>{{ project.title }}</h3>
                  <p>{{ project.organizationName || '公益组织' }} · {{ project.volunteerName || '志愿者' }} · {{ formatDate(project.endDate) }}</p>
                </div>
                <StatusBadge :label="getStatusMeta('evidence', project.evidenceStatus).label" :tone="getStatusMeta('evidence', project.evidenceStatus).tone" />
              </div>
            </div>
            <EmptyState v-if="!projectLoading && !projects.length" mark="公" title="暂无服务成果" description="完成报告审核通过后会在这里展示。" />
          </article>

          <article class="panel">
            <div class="panel-head">
              <h2>公开存证</h2>
              <el-button plain @click="loadEvidences">查询</el-button>
            </div>
            <div v-if="evidenceLoading" class="portal-loading">正在加载存证记录...</div>
            <div v-else class="evidence-list">
              <button v-for="item in evidences" :key="item.id" type="button" class="evidence-row" @click="selectedEvidence = item">
                <div>
                  <strong>{{ item.activityName || '志愿服务记录' }}</strong>
                  <p>{{ item.organizationName || '公益组织' }} · {{ item.username || '志愿者' }}</p>
                  <span>{{ shortHash(item.txHash || item.digest, 14, 10) }}</span>
                </div>
                <StatusBadge :label="getStatusMeta('evidence', item.onchainStatus).label" :tone="getStatusMeta('evidence', item.onchainStatus).tone" />
              </button>
            </div>
            <EmptyState v-if="!evidenceLoading && !evidences.length" mark="链" title="暂无存证记录" description="链上记录生成后会在这里公开查询。" />
          </article>
        </div>
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="链上存证详情" width="min(720px, calc(100vw - 24px))">
      <el-descriptions v-if="selectedEvidence" :column="1" border>
        <el-descriptions-item label="活动名称">{{ selectedEvidence.activityName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公益组织">{{ selectedEvidence.organizationName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="志愿者">{{ selectedEvidence.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ selectedEvidence.bizType }}</el-descriptions-item>
        <el-descriptions-item label="业务 ID">{{ selectedEvidence.bizId }}</el-descriptions-item>
        <el-descriptions-item label="交易哈希">{{ selectedEvidence.txHash || '-' }}</el-descriptions-item>
        <el-descriptions-item label="摘要">{{ selectedEvidence.digest || '-' }}</el-descriptions-item>
        <el-descriptions-item label="区块高度">{{ selectedEvidence.blockNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="校验结果">{{ selectedEvidence.verified ? '通过' : '未通过' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { getCompletedProjects, getPublicEvidences } from '../api/public'
import { formatDate, getStatusMeta, shortHash } from '../utils/ui'

const keyword = ref('')
const projects = ref([])
const evidences = ref([])
const selectedEvidence = ref(null)
const projectLoading = ref(false)
const evidenceLoading = ref(false)

const detailVisible = computed({
  get: () => !!selectedEvidence.value,
  set: (value) => {
    if (!value) {
      selectedEvidence.value = null
    }
  }
})

onMounted(async () => {
  await Promise.all([loadProjects(), loadEvidences()])
})

async function loadProjects() {
  projectLoading.value = true
  try {
    projects.value = await getCompletedProjects(20)
  } catch (error) {
    projects.value = []
  } finally {
    projectLoading.value = false
  }
}

async function loadEvidences() {
  evidenceLoading.value = true
  try {
    evidences.value = await getPublicEvidences({ limit: 20, keyword: keyword.value })
  } catch (error) {
    evidences.value = []
  } finally {
    evidenceLoading.value = false
  }
}
</script>

<style scoped>
.publicity-page {
  padding-bottom: 70px;
}

.page-title-row,
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.page-title-row {
  margin-bottom: 24px;
}

.page-title-row h1,
.panel-head h2 {
  margin: 0;
}

.page-title-row h1 {
  font-size: 30px;
}

.page-title-row p,
.portal-loading {
  color: #777;
}

.search-input {
  width: 320px;
}

.publicity-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 22px;
}

.panel {
  padding: 24px;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
}

.project-list,
.evidence-list {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.project-row,
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

.evidence-row {
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.project-row h3,
.evidence-row strong {
  margin: 0;
  color: #222;
  font-size: 17px;
}

.project-row p,
.evidence-row p,
.evidence-row span {
  display: block;
  margin: 8px 0 0;
  color: #777;
  font-size: 13px;
}

@media (max-width: 1000px) {
  .page-title-row,
  .publicity-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .search-input {
    width: 100%;
  }
}
</style>
