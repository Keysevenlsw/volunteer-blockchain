<template>
  <PortalLayout active-key="home" breadcrumb="个人中心">
    <section class="profile-page">
      <div class="portal-shell profile-layout" v-loading="pageLoading">
        <aside class="profile-sidebar">
          <div class="sidebar-title">个人中心</div>
          <button
            v-for="item in menuItems"
            :key="item.key"
            type="button"
            :class="{ active: activeMenu === item.key, section: item.section }"
            @click="activeMenu = item.key"
          >
            {{ item.label }}
          </button>
        </aside>

        <main class="profile-main">
          <section v-if="activeMenu === 'home'" class="profile-card profile-hero">
            <div class="avatar-box">
              <div class="avatar-circle">{{ user?.username?.slice(0, 1) || '志' }}</div>
              <button type="button">编辑头像</button>
            </div>
            <div class="profile-info">
              <h1>{{ user?.username || '志愿者' }}</h1>
              <p><span>账号</span> 志愿者号：{{ user?.userId || user?.email || '-' }}</p>
              <p>{{ user?.email || '-' }} · 归属组织：{{ user?.organizationName || '暂未加入组织' }}</p>
              <div class="quick-actions">
                <el-button type="primary" @click="router.push('/activities')">去参与活动</el-button>
                <el-button @click="router.push('/organizations')">申请加入组织</el-button>
              </div>
            </div>
            <div class="profile-stats">
              <div><span>{{ myParticipations.length }}</span><p>参与活动</p></div>
              <div><span>{{ serviceHours }}</span><p>总服务时长（小时）</p></div>
              <div><span>{{ volunteerDays }}</span><p>成为志愿者（天）</p></div>
              <div><span>{{ user?.totalPoints ?? 0 }}</span><p>公益积分</p></div>
            </div>
          </section>

          <section v-else-if="activeMenu === 'profile'" class="profile-card">
            <h2>志愿者档案</h2>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户名">{{ user?.username || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ user?.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="归属组织">{{ user?.organizationName || '暂未加入组织' }}</el-descriptions-item>
              <el-descriptions-item label="当前积分">{{ user?.totalPoints ?? 0 }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section v-else-if="activeMenu === 'join'" class="profile-card">
            <h2>我的组织申请</h2>
            <el-table :data="myJoinRequests" empty-text="暂无组织申请">
              <el-table-column prop="organizationName" label="组织" min-width="160" />
              <el-table-column prop="applyReason" label="申请说明" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('joinRequest', row.status).label" :tone="getStatusMeta('joinRequest', row.status).tone" />
                </template>
              </el-table-column>
              <el-table-column label="申请时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column prop="reviewNote" label="审核备注" min-width="180" show-overflow-tooltip />
            </el-table>
          </section>

          <section v-else-if="activeMenu === 'registrations'" class="profile-card">
            <h2>我的报名</h2>
            <el-table :data="myParticipations" empty-text="暂无报名记录">
              <el-table-column prop="activityName" label="活动" min-width="180" />
              <el-table-column prop="organizationName" label="组织" min-width="160" />
              <el-table-column label="报名时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.participationDate) }}</template>
              </el-table-column>
              <el-table-column label="报告状态" width="130">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('completion', row.completionStatus || 'pending').label" :tone="getStatusMeta('completion', row.completionStatus || 'pending').tone" />
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else-if="activeMenu === 'services'" class="profile-card">
            <h2>服务明细与链上存证</h2>
            <div class="service-list">
              <article v-for="item in myCompletions" :key="item.completionId" class="service-card">
                <div>
                  <h3>{{ item.activityName }}</h3>
                  <p>{{ item.serviceLocation || '-' }} · {{ formatDateTime(item.submissionDate) }}</p>
                  <p>{{ item.reportText || '暂无报告总结' }}</p>
                </div>
                <div class="service-side">
                  <StatusBadge :label="getStatusMeta('completion', item.status).label" :tone="getStatusMeta('completion', item.status).tone" />
                  <StatusBadge :label="getStatusMeta('evidence', item.evidenceStatus || 'pending').label" :tone="getStatusMeta('evidence', item.evidenceStatus || 'pending').tone" />
                  <span>{{ shortHash(item.txHash || item.digest, 10, 8) }}</span>
                </div>
              </article>
            </div>
            <EmptyState v-if="!myCompletions.length" mark="服" title="暂无服务明细" description="完成报告审核后会在这里形成服务记录。" />
          </section>

          <section v-else-if="activeMenu === 'points'" class="profile-card">
            <div class="profile-section-head">
              <h2>积分流水</h2>
              <el-button plain @click="refreshAll">刷新</el-button>
            </div>
            <div class="balance-row" v-if="pointsBalance">
              <div><span>链上余额</span><strong>{{ pointsBalance.chainBalance }}</strong></div>
              <div><span>缓存余额</span><strong>{{ pointsBalance.cachedBalance }}</strong></div>
              <div><span>一致性</span><strong>{{ pointsBalance.consistent ? '一致' : '不一致' }}</strong></div>
              <div><span>检查时间</span><strong>{{ formatDateTime(pointsBalance.checkedAt) }}</strong></div>
            </div>
            <el-table :data="pointsRecords" empty-text="暂无积分流水">
              <el-table-column prop="source" label="来源" min-width="180" />
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('points', row.transactionType).label" :tone="getStatusMeta('points', row.transactionType).tone" />
                </template>
              </el-table-column>
              <el-table-column prop="points" label="积分" width="100" />
              <el-table-column label="链上状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('evidence', row.onchainStatus || 'success').label" :tone="getStatusMeta('evidence', row.onchainStatus || 'success').tone" />
                </template>
              </el-table-column>
              <el-table-column label="时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else-if="activeMenu === 'redemptions'" class="profile-card">
            <h2>兑换记录</h2>
            <el-table :data="myRedemptions" empty-text="暂无兑换记录">
              <el-table-column prop="productName" label="商品" min-width="180" />
              <el-table-column prop="organizationName" label="履约组织" min-width="160" />
              <el-table-column prop="pointsCost" label="消耗积分" width="100" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('redemption', row.status).label" :tone="getStatusMeta('redemption', row.status).tone" />
                </template>
              </el-table-column>
              <el-table-column label="申请时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else class="profile-card">
            <h2>{{ currentMenuLabel }}</h2>
            <EmptyState mark="建" title="模块建设中" description="该模块会随业务接口补充继续完善。" />
          </section>
        </main>
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
import StatusBadge from '../components/StatusBadge.vue'
import { clearAuth, getCachedUser, getCurrentUser, getToken, redirectToLogin, saveAuth } from '../api/auth'
import { getMyCompletions, getMyJoinRequests, getMyParticipations, getMyPointsBalance, getMyPointsRecords, getMyRedemptions } from '../api/platform'
import { formatDateTime, getStatusMeta, shortHash } from '../utils/ui'

const router = useRouter()
const pageLoading = ref(false)
const activeMenu = ref('home')
const user = ref(getCachedUser())
const myJoinRequests = ref([])
const myParticipations = ref([])
const myCompletions = ref([])
const pointsRecords = ref([])
const pointsBalance = ref(null)
const myRedemptions = ref([])

const menuItems = [
  { key: 'home', label: '个人主页' },
  { key: 'profile', label: '志愿者档案' },
  { key: 'preferences', label: '我的偏好' },
  { key: 'password', label: '修改密码' },
  { key: 'honors', label: '我的荣誉' },
  { key: 'activity-section', label: '我的活动', section: true },
  { key: 'registrations', label: '我的报名' },
  { key: 'join', label: '我的组织申请' },
  { key: 'services', label: '服务明细' },
  { key: 'training', label: '在线培训' },
  { key: 'points', label: '积分流水' },
  { key: 'redemptions', label: '兑换记录' }
]

const currentMenuLabel = computed(() => menuItems.find((item) => item.key === activeMenu.value)?.label || '个人中心')
const serviceHours = computed(() => myCompletions.value.filter((item) => item.status === 'approved').length * 2)
const volunteerDays = computed(() => {
  if (!user.value?.joinDate) {
    return 1
  }
  const diff = Date.now() - new Date(user.value.joinDate).getTime()
  return Math.max(1, Math.floor(diff / 86400000))
})

onMounted(initializePage)

async function initializePage() {
  pageLoading.value = true
  try {
    await loadProfile()
    await refreshAll()
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

async function refreshAll() {
  try {
    const [joinData, participationData, completionData, pointsData, balanceData, redemptionData] = await Promise.all([
      getMyJoinRequests().catch(() => []),
      getMyParticipations().catch(() => []),
      getMyCompletions().catch(() => []),
      getMyPointsRecords().catch(() => []),
      getMyPointsBalance().catch(() => null),
      getMyRedemptions().catch(() => [])
    ])
    myJoinRequests.value = joinData
    myParticipations.value = participationData
    myCompletions.value = completionData
    pointsRecords.value = pointsData
    pointsBalance.value = balanceData
    myRedemptions.value = redemptionData
  } catch (error) {
    ElMessage.error(error.message || '数据加载失败')
  }
}
</script>

<style scoped>
.profile-page {
  padding-bottom: 72px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 22px;
}

.profile-sidebar {
  border: 1px solid rgba(223, 0, 27, 0.08);
  background: #fff;
}

.sidebar-title,
.profile-sidebar button {
  width: 100%;
  min-height: 44px;
  border: 0;
  border-left: 4px solid transparent;
  background: #fff;
  color: #333;
  cursor: pointer;
  text-align: left;
  padding: 0 28px;
  font-size: 16px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  border-left-color: #df001b;
  background: #fff2f3;
}

.profile-sidebar button.active,
.profile-sidebar button.section {
  border-left-color: #df001b;
  background: #fff0f2;
  color: #df001b;
}

.profile-sidebar button.section {
  color: #333;
}

.profile-main {
  min-height: 520px;
}

.profile-card {
  padding: 30px;
  border: 1px solid rgba(223, 0, 27, 0.08);
  border-radius: 8px;
  background: #fff;
}

.profile-card h2 {
  margin: 0 0 22px;
  font-size: 22px;
}

.profile-hero {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 28px;
}

.avatar-box {
  display: grid;
  justify-items: center;
}

.avatar-circle {
  width: 150px;
  height: 150px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(180deg, #ffc29f, #ffe0d3);
  color: #fff;
  font-size: 58px;
  font-weight: 700;
  box-shadow: 0 10px 22px rgba(161, 43, 61, 0.16);
}

.avatar-box button {
  width: 120px;
  height: 30px;
  margin-top: -30px;
  border: 0;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
}

.profile-info h1 {
  margin: 14px 0 10px;
  font-size: 26px;
}

.profile-info p {
  margin: 8px 0;
  color: #666;
}

.profile-info p span {
  padding: 2px 6px;
  margin-right: 8px;
  border-radius: 4px;
  background: #ffdee2;
  color: #df001b;
  font-size: 12px;
}

.quick-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.profile-stats {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  padding-top: 28px;
  border-top: 1px dashed #e6e6e6;
}

.profile-stats div,
.balance-row div {
  padding: 18px;
  border-radius: 8px;
  background: #fff7f8;
}

.profile-stats span,
.balance-row strong {
  display: block;
  color: #555;
  font-size: 34px;
  font-weight: 700;
}

.profile-stats p,
.balance-row span {
  margin: 8px 0 0;
  color: #777;
}

.service-list {
  display: grid;
  gap: 16px;
}

.service-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(223, 0, 27, 0.08);
  border-radius: 8px;
  background: #fffafa;
}

.service-card h3 {
  margin: 0 0 10px;
}

.service-card p {
  margin: 8px 0 0;
  color: #666;
  line-height: 1.7;
}

.service-side {
  min-width: 180px;
  display: grid;
  align-content: start;
  gap: 10px;
  justify-items: end;
}

.profile-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.balance-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

@media (max-width: 980px) {
  .profile-layout,
  .profile-hero,
  .profile-stats,
  .balance-row {
    grid-template-columns: 1fr;
  }

  .service-card {
    flex-direction: column;
  }

  .service-side {
    justify-items: start;
  }
}
</style>
