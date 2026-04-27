<template>
  <div class="portal-layout" :class="{ 'portal-layout--home': props.home }">
    <header
      class="portal-hero"
      :class="{
        'portal-hero--home': props.home,
        'portal-hero--compact': heroMode === 'compact',
        'portal-hero--flipping': isFlipping,
        'portal-hero--flip-up': flipDirection === 'up',
        'portal-hero--flip-down': flipDirection === 'down'
      }"
    >
      <span class="portal-hero__bg portal-hero__bg--full"></span>
      <span class="portal-hero__bg portal-hero__bg--compact"></span>
      <div class="portal-shell portal-hero__inner">
        <h1 class="sr-only">志愿服务认证平台</h1>
        <img class="portal-logo-mark" src="/assets/branding/lianxin-gongyi-logo.png" alt="链信公益" />
        <div class="portal-userbar">
          <template v-if="user">
            <span class="portal-userbar__hello">您好，{{ user.username }}</span>
            <button type="button" class="portal-link" @click="goWorkspace">{{ workspaceText }}</button>
            <button type="button" class="portal-link" @click="handleLogout">退出</button>
          </template>
          <template v-else>
            <button type="button" class="portal-link portal-link--strong" @click="router.push('/login')">请登录</button>
            <button type="button" class="portal-link" @click="router.push('/register?role=volunteer')">志愿者注册</button>
            <span class="portal-separator"></span>
            <button type="button" class="portal-link" @click="router.push('/register?role=organization_admin')">志愿组织注册</button>
          </template>
          <button type="button" class="portal-search" aria-label="搜索">搜</button>
        </div>
      </div>
    </header>

    <nav class="portal-nav">
      <div class="portal-shell portal-nav__inner">
        <RouterLink
          v-for="item in navItems"
          :key="item.key"
          class="portal-nav__item"
          :class="{ active: props.activeKey === item.key }"
          :to="item.path"
        >
          {{ item.label }}
        </RouterLink>
      </div>
    </nav>

    <div class="portal-sticky" :class="{ visible: stickyVisible }">
      <header class="portal-hero portal-hero--compact portal-hero--sticky">
        <span class="portal-hero__bg portal-hero__bg--full"></span>
        <span class="portal-hero__bg portal-hero__bg--compact"></span>
        <div class="portal-shell portal-hero__inner">
          <h1 class="sr-only">志愿服务认证平台</h1>
          <img class="portal-logo-mark" src="/assets/branding/lianxin-gongyi-logo.png" alt="链信公益" />
          <div class="portal-userbar">
            <template v-if="user">
              <span class="portal-userbar__hello">您好，{{ user.username }}</span>
              <button type="button" class="portal-link" @click="goWorkspace">{{ workspaceText }}</button>
              <button type="button" class="portal-link" @click="handleLogout">退出</button>
            </template>
            <template v-else>
              <button type="button" class="portal-link portal-link--strong" @click="router.push('/login')">请登录</button>
              <button type="button" class="portal-link" @click="router.push('/register?role=volunteer')">志愿者注册</button>
              <span class="portal-separator"></span>
              <button type="button" class="portal-link" @click="router.push('/register?role=organization_admin')">志愿组织注册</button>
            </template>
            <button type="button" class="portal-search" aria-label="搜索">搜</button>
          </div>
        </div>
      </header>
      <nav class="portal-nav">
        <div class="portal-shell portal-nav__inner">
          <RouterLink
            v-for="item in navItems"
            :key="`sticky-${item.key}`"
            class="portal-nav__item"
            :class="{ active: props.activeKey === item.key }"
            :to="item.path"
          >
            {{ item.label }}
          </RouterLink>
        </div>
      </nav>
    </div>

    <main class="portal-body">
      <div v-if="hasBreadcrumb" class="portal-shell portal-breadcrumb">
        <div class="portal-breadcrumb__trail">
          <span>当前位置：</span>
          <RouterLink to="/">首页</RouterLink>
          <template v-for="(item, index) in resolvedBreadcrumbItems" :key="`${item}-${index}`">
            <span class="portal-breadcrumb__sep">></span>
            <strong v-if="index === resolvedBreadcrumbItems.length - 1">{{ item }}</strong>
            <span v-else>{{ item }}</span>
          </template>
        </div>
        <el-button
          v-if="props.breadcrumbBackText"
          text
          class="portal-breadcrumb__back"
          @click="router.back()"
        >
          {{ props.breadcrumbBackText }}
        </el-button>
      </div>
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAuth, getCachedUser, getCurrentUser, getToken, getWorkspaceRoute, hasRole, saveAuth } from '../api/auth'

const props = defineProps({
  home: {
    type: Boolean,
    default: false
  },
  activeKey: {
    type: String,
    default: 'home'
  },
  breadcrumb: {
    type: String,
    default: ''
  },
  breadcrumbItems: {
    type: Array,
    default: () => []
  },
  breadcrumbBackText: {
    type: String,
    default: ''
  }
})

const BANNER_MODE_KEY = '__volunteerPortalBannerMode'
const FLIP_DURATION = 520

const router = useRouter()
const user = ref(getCachedUser())
const stickyVisible = ref(false)
const targetHeroMode = props.home ? 'full' : 'compact'
const previousHeroMode = getLastBannerMode()
const shouldFlip = previousHeroMode && previousHeroMode !== targetHeroMode
const heroMode = ref(shouldFlip ? previousHeroMode : targetHeroMode)
const flipDirection = ref(shouldFlip ? (targetHeroMode === 'compact' ? 'up' : 'down') : '')
const isFlipping = ref(false)
let flipTimer = null

const navItems = [
  { key: 'home', label: '首页', path: '/' },
  { key: 'activities', label: '志愿活动', path: '/activities' },
  { key: 'organizations', label: '志愿组织', path: '/organizations' },
  { key: 'publicity', label: '时长公示', path: '/publicity' },
  { key: 'rewards', label: '积分商城', path: '/activities?panel=rewards' },
  { key: 'notices', label: '通知公告', path: '/notices' },
  { key: 'rules', label: '制度规范', path: '/rules' }
]

const workspaceText = computed(() => {
  if (!user.value) {
    return '工作台'
  }
  if (hasRole('system_admin', user.value)) {
    return '后台管理'
  }
  if (hasRole('activity_reviewer', user.value) || hasRole('product_reviewer', user.value)) {
    return '审核工作台'
  }
  if (hasRole('organization_admin', user.value)) {
    return '组织工作台'
  }
  return '个人中心'
})

const resolvedBreadcrumbItems = computed(() => {
  if (props.breadcrumbItems.length) {
    return props.breadcrumbItems
  }
  return props.breadcrumb ? [props.breadcrumb] : []
})

const hasBreadcrumb = computed(() => resolvedBreadcrumbItems.value.length > 0)

onMounted(() => {
  loadSession()
  if (shouldFlip) {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        isFlipping.value = true
        heroMode.value = targetHeroMode
        setLastBannerMode(targetHeroMode)
        flipTimer = window.setTimeout(() => {
          isFlipping.value = false
          flipDirection.value = ''
        }, FLIP_DURATION)
      })
    })
  } else {
    setLastBannerMode(targetHeroMode)
  }
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (flipTimer) {
    window.clearTimeout(flipTimer)
  }
})

async function loadSession() {
  const token = getToken()
  if (!token) {
    user.value = null
    return
  }
  try {
    const profile = await getCurrentUser(token)
    user.value = profile
    saveAuth({ token, user: profile })
  } catch (error) {
    clearAuth()
    user.value = null
  }
}

function goWorkspace() {
  router.push(getWorkspaceRoute(user.value))
}

function handleLogout() {
  clearAuth()
  user.value = null
  ElMessage.success('已退出登录')
  router.push('/')
}

function handleScroll() {
  const threshold = props.home ? 260 : 88
  stickyVisible.value = window.scrollY > threshold
}

function getLastBannerMode() {
  if (typeof window === 'undefined') {
    return null
  }
  const value = window[BANNER_MODE_KEY]
  return value === 'full' || value === 'compact' ? value : null
}

function setLastBannerMode(mode) {
  if (typeof window !== 'undefined') {
    window[BANNER_MODE_KEY] = mode
  }
}
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(255, 246, 238, 0.86), rgba(255, 255, 255, 0.96) 36%),
    #fff;
}

.portal-shell {
  width: min(1320px, calc(100vw - 48px));
  margin: 0 auto;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

.portal-hero {
  position: relative;
  overflow: hidden;
  perspective: 1200px;
  height: clamp(260px, 25.47vw, 489px);
  background: #fff6f4;
  transition: height 0.52s cubic-bezier(0.25, 0.9, 0.25, 1);
  will-change: height;
}

.portal-hero--compact {
  height: clamp(132px, 10.42vw, 200px);
}

.portal-hero__bg {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  background-position: center top;
  background-size: 100% 100%;
  background-repeat: no-repeat;
  transform-origin: top center;
  transition:
    opacity 0.46s ease,
    transform 0.52s cubic-bezier(0.25, 0.9, 0.25, 1),
    filter 0.52s ease;
  will-change: opacity, transform;
}

.portal-hero__bg--full {
  background-image: url('/assets/branding/home-banner-1.png');
  opacity: 1;
  transform: translateY(0) scaleY(1);
}

.portal-hero__bg--compact {
  background-image: url('/assets/branding/home-banner-2.png');
  opacity: 0;
  transform: translateY(18px) scaleY(1.05);
}

.portal-hero--compact .portal-hero__bg--full {
  opacity: 0;
  filter: blur(0.5px);
  transform: translateY(-22px) scaleY(0.88);
}

.portal-hero--compact .portal-hero__bg--compact {
  opacity: 1;
  transform: translateY(0) scaleY(1);
}

.portal-hero--flipping {
  transform-style: preserve-3d;
}

.portal-hero--flipping .portal-hero__bg {
  transition-duration: 0.52s;
}

.portal-hero__inner {
  position: relative;
  z-index: 1;
  height: 100%;
}

.portal-logo-mark {
  position: absolute;
  left: 0;
  top: clamp(16px, 1.9vw, 36px);
  width: clamp(238px, 18.2vw, 372px);
  height: auto;
  z-index: 2;
  object-fit: contain;
}

.portal-hero--compact .portal-logo-mark {
  top: clamp(10px, 1.05vw, 20px);
  width: clamp(220px, 15.6vw, 320px);
}

.portal-userbar {
  position: absolute;
  top: clamp(22px, 2.1vw, 40px);
  right: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  color: #222;
  font-size: 15px;
  z-index: 3;
}

.portal-hero--compact .portal-userbar {
  top: clamp(14px, 1.25vw, 24px);
}

.portal-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #d8001b;
  cursor: pointer;
  font-size: 15px;
}

.portal-link--strong,
.portal-userbar__hello {
  color: #111;
  font-weight: 700;
}

.portal-separator {
  width: 1px;
  height: 18px;
  background: rgba(216, 0, 27, 0.45);
}

.portal-search {
  width: 58px;
  height: 38px;
  border: 0;
  border-radius: 19px;
  background: #df001b;
  color: #fff;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.portal-nav {
  background: #df001b;
}

.portal-nav__inner {
  display: flex;
  align-items: stretch;
  min-height: 58px;
}

.portal-nav__item {
  min-width: 118px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}

.portal-nav__item.active {
  background: #fff;
  color: #d8001b;
}

.portal-body {
  min-height: 520px;
}

.portal-sticky {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-100%);
  transition:
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.24s ease;
  box-shadow: 0 12px 30px rgba(125, 18, 28, 0.16);
}

.portal-sticky.visible {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

.portal-hero--sticky {
  height: clamp(132px, 10.42vw, 200px);
}

.portal-breadcrumb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 0 20px;
  color: #555;
  font-size: 15px;
}

.portal-breadcrumb__trail {
  display: flex;
  align-items: center;
  gap: 10px;
}

.portal-breadcrumb__sep {
  color: #bdbdbd;
}

.portal-breadcrumb strong {
  color: #555;
}

.portal-breadcrumb__back {
  flex: 0 0 auto;
  color: #d8001b;
}

@media (max-width: 1200px) {
  .portal-shell {
    width: min(100vw - 28px, 1320px);
  }

  .portal-nav__inner {
    overflow-x: auto;
  }
}

@media (max-width: 760px) {
  .portal-hero,
  .portal-hero--compact {
    height: 120px;
  }

  .portal-userbar {
    left: 0;
    right: auto;
    top: 68px;
    gap: 10px;
    flex-wrap: wrap;
    font-size: 13px;
  }

  .portal-logo-mark {
    top: 12px;
    width: 190px;
  }

  .portal-search {
    display: none;
  }

  .portal-nav__item {
    min-width: 96px;
    font-size: 15px;
  }

  .portal-breadcrumb {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
