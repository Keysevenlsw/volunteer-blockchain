<template>
  <PortalLayout :active-key="page.activeKey" :breadcrumb="page.title">
    <section class="static-page">
      <div class="portal-shell">
        <div class="static-hero">
          <h1>{{ page.title }}</h1>
          <p>{{ page.description }}</p>
        </div>

        <div class="static-grid">
          <article v-for="item in page.items" :key="item.title" class="static-card">
            <span>{{ item.mark }}</span>
            <h2>{{ item.title }}</h2>
            <p>{{ item.text }}</p>
          </article>
        </div>

        <article v-if="showNotices" class="notice-panel">
          <div class="panel-head">
            <h2>平台资讯</h2>
          </div>
          <div v-if="loading" class="portal-loading">正在加载资讯...</div>
          <ul v-else class="notice-list">
            <li v-for="item in infoList" :key="`${item.title}-${item.date}`">
              <span>{{ item.tag || '资讯' }}</span>
              <strong>{{ item.title }}</strong>
              <time>{{ formatDate(item.date) }}</time>
            </li>
          </ul>
          <EmptyState v-if="!loading && !infoList.length" mark="讯" title="暂无资讯" description="平台资讯会在这里展示。" />
        </article>
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import { getPublicInfo } from '../api/public'
import { formatDate } from '../utils/ui'

const route = useRoute()
const infoList = ref([])
const loading = ref(false)

const pages = {
  stations: {
    activeKey: 'stations',
    title: '志愿服务站',
    description: '集中展示平台服务入口、组织联络和志愿服务办理说明。',
    items: [
      { mark: '站', title: '服务咨询', text: '查看平台服务流程、活动参与规则和积分兑换说明。' },
      { mark: '联', title: '组织联络', text: '公益组织可通过工作台维护公开资料，志愿者可浏览组织信息。' },
      { mark: '证', title: '认证支持', text: '完成服务报告审核后，平台同步展示可信存证状态。' }
    ]
  },
  notices: {
    activeKey: 'notices',
    title: '通知公告',
    description: '公开平台通知、活动资讯和系统动态。',
    items: [
      { mark: '告', title: '平台公告', text: '发布平台运行、服务规则和业务办理通知。' },
      { mark: '动', title: '活动动态', text: '同步公益组织发布与审核通过的志愿活动信息。' },
      { mark: '链', title: '链上动态', text: '展示存证、积分和可信认证相关公开说明。' }
    ]
  },
  rules: {
    activeKey: 'rules',
    title: '制度规范',
    description: '说明活动参与、服务报告、组织审核、积分规则和链上存证规范。',
    items: [
      { mark: '1', title: '活动审核', text: '组织发布活动后需提交平台审核，通过后才进入公开活动列表。' },
      { mark: '2', title: '服务认证', text: '志愿者提交完成报告，组织审核通过后产生服务记录。' },
      { mark: '3', title: '积分上链', text: '积分获得、消费和余额一致性以链上记录为可信依据。' }
    ]
  },
  help: {
    activeKey: 'help',
    title: '帮助中心',
    description: '面向志愿者、公益组织、审核员和系统管理员的使用指引。',
    items: [
      { mark: '志', title: '志愿者如何参与', text: '注册登录后，在志愿活动报名，在志愿组织申请默认归属组织。' },
      { mark: '组', title: '组织如何运营', text: '登录组织工作台，维护成员、活动、报告审核、商品和兑换履约。' },
      { mark: '管', title: '管理员如何审核', text: '进入后台处理角色权限、活动审核、商品审核、积分规则和链上维护。' }
    ]
  }
}

const page = computed(() => pages[route.meta.pageKey] || pages.help)
const showNotices = computed(() => route.meta.pageKey === 'notices')

onMounted(loadInfo)

watch(
  () => route.meta.pageKey,
  () => {
    infoList.value = []
    loadInfo()
  }
)

async function loadInfo() {
  if (!showNotices.value) {
    return
  }
  loading.value = true
  try {
    infoList.value = await getPublicInfo()
  } catch (error) {
    infoList.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.static-page {
  padding-bottom: 70px;
}

.static-hero {
  padding: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #fff, #fff0f2);
  border: 1px solid rgba(223, 0, 27, 0.12);
}

.static-hero h1 {
  margin: 0;
  font-size: 30px;
}

.static-hero p {
  margin: 12px 0 0;
  color: #666;
}

.static-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin-top: 24px;
}

.static-card,
.notice-panel {
  padding: 24px;
  border: 1px solid rgba(223, 0, 27, 0.12);
  border-radius: 8px;
  background: #fff;
}

.static-card span {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #df001b;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
}

.static-card h2 {
  margin: 18px 0 0;
  font-size: 21px;
}

.static-card p {
  margin: 12px 0 0;
  color: #666;
  line-height: 1.8;
}

.notice-panel {
  margin-top: 24px;
}

.panel-head h2 {
  margin: 0 0 18px;
}

.notice-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-list li {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr) 110px;
  gap: 14px;
  padding: 15px 0;
  border-bottom: 1px dashed rgba(223, 0, 27, 0.14);
}

.notice-list span {
  color: #df001b;
  font-weight: 700;
}

.notice-list time {
  color: #777;
}

.portal-loading {
  color: #777;
}

@media (max-width: 880px) {
  .static-grid {
    grid-template-columns: 1fr;
  }

  .notice-list li {
    grid-template-columns: 1fr;
  }
}
</style>
