<template>
  <PortalLayout active-key="home" breadcrumb="登录">
    <section class="auth-page">
      <div class="portal-shell auth-card">
        <div class="auth-visual">
          <div class="auth-visual__sun"></div>
          <div class="auth-visual__people">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div class="auth-visual__city"></div>
        </div>

        <div class="auth-panel">
          <div class="auth-tabs">
            <button
              v-for="item in loginTabs"
              :key="item.key"
              type="button"
              :class="{ active: activeRole === item.key }"
              @click="setActiveRole(item.key)"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="auth-title">
            <strong>{{ activeRole === 'organization_admin' ? '志愿组织登录' : '志愿者登录' }}</strong>
          </div>

          <el-form class="auth-form" label-position="top" @submit.prevent>
            <el-form-item label="登录邮箱">
              <el-input
                v-model="form.email"
                placeholder="请输入登录邮箱"
                clearable
                autocomplete="username"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
                autocomplete="current-password"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-button type="primary" class="auth-submit" :loading="loading" @click="handleLogin">
              登录
            </el-button>
          </el-form>

          <div class="auth-links">
            <RouterLink :to="registerLink">立即注册</RouterLink>
            <button type="button" @click="ElMessage.info('请联系平台管理员重置密码')">忘记密码？</button>
          </div>
        </div>
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PortalLayout from '../components/PortalLayout.vue'
import { clearAuth, getWorkspaceRoute, hasRole, login, saveAuth } from '../api/auth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeRole = ref(normalizeRole(route.query.role))
const form = reactive({
  email: '',
  password: ''
})

const loginTabs = [
  { key: 'volunteer', label: '志愿者登录' },
  { key: 'organization_admin', label: '志愿组织登录' }
]

const registerLink = computed(() => ({
  path: '/register',
  query: { role: activeRole.value === 'organization_admin' ? 'organization_admin' : 'volunteer' }
}))

watch(
  () => route.query.role,
  (value) => {
    activeRole.value = normalizeRole(value)
  }
)

function normalizeRole(value) {
  if (value === 'organization_admin') {
    return 'organization_admin'
  }
  return 'volunteer'
}

function setActiveRole(role) {
  activeRole.value = normalizeRole(role)
  router.replace({
    path: '/login',
    query: activeRole.value === 'organization_admin' ? { role: 'organization_admin' } : {}
  })
}

function isAdminLike(user) {
  return hasRole('system_admin', user) || hasRole('activity_reviewer', user) || hasRole('product_reviewer', user)
}

function isVolunteerLike(user) {
  return hasRole('volunteer', user)
}

function roleAllowed(user) {
  if (activeRole.value === 'organization_admin') {
    return hasRole('organization_admin', user)
  }
  return isAdminLike(user) || isVolunteerLike(user)
}

async function handleLogin() {
  if (!form.email || !form.password) {
    ElMessage.warning('请填写登录邮箱和密码')
    return
  }

  loading.value = true
  try {
    const authData = await login({
      email: form.email,
      password: form.password
    })
    if (!roleAllowed(authData.user)) {
      clearAuth()
      ElMessage.error('当前账号角色与所选登录入口不一致')
      return
    }
    saveAuth(authData)
    ElMessage.success('登录成功')
    router.push(getWorkspaceRoute(authData.user))
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  padding: 0 0 70px;
}

.auth-card {
  min-height: 508px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 440px;
  overflow: hidden;
  border: 1px solid rgba(223, 0, 27, 0.06);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 22px 48px rgba(144, 47, 56, 0.1);
}

.auth-visual {
  position: relative;
  min-height: 508px;
  overflow: hidden;
  background:
    linear-gradient(180deg, #f6f5ff 0%, #fff8f1 76%),
    linear-gradient(90deg, rgba(223, 0, 27, 0.06), rgba(255, 149, 83, 0.08));
}

.auth-visual::before {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 150px;
  background: linear-gradient(150deg, rgba(255, 121, 129, 0.42), rgba(255, 176, 100, 0.3));
  clip-path: polygon(0 44%, 35% 20%, 68% 46%, 100% 26%, 100% 100%, 0 100%);
}

.auth-visual__sun {
  position: absolute;
  left: 42%;
  top: 24%;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: rgba(255, 158, 143, 0.34);
  box-shadow: 0 22px 60px rgba(223, 0, 27, 0.08);
}

.auth-visual__city {
  position: absolute;
  left: 22%;
  right: 8%;
  bottom: 92px;
  height: 132px;
  background:
    linear-gradient(#ff74ba, #7e78ff) 0 40px / 42px 92px no-repeat,
    linear-gradient(#ff74ba, #7e78ff) 86px 0 / 46px 132px no-repeat,
    linear-gradient(#ff74ba, #7e78ff) 164px 46px / 56px 86px no-repeat,
    linear-gradient(#ff74ba, #7e78ff) 280px 12px / 50px 120px no-repeat,
    linear-gradient(#ff74ba, #7e78ff) 430px 52px / 60px 80px no-repeat,
    linear-gradient(#ff74ba, #7e78ff) 570px 36px / 44px 96px no-repeat;
  opacity: 0.72;
}

.auth-visual__people {
  position: absolute;
  left: 38%;
  bottom: 112px;
  display: flex;
  align-items: flex-end;
  gap: 22px;
  z-index: 2;
}

.auth-visual__people span {
  width: 42px;
  height: 104px;
  border-radius: 22px 22px 8px 8px;
  background: linear-gradient(180deg, #ff3e7a, #ffb23d);
  box-shadow: 0 16px 28px rgba(223, 0, 27, 0.14);
}

.auth-visual__people span:nth-child(1) {
  height: 88px;
  background: linear-gradient(180deg, #ffd334, #ff8a3a);
}

.auth-visual__people span:nth-child(3) {
  height: 78px;
  background: linear-gradient(180deg, #ff7557, #d73d50);
}

.auth-panel {
  padding: 34px 38px 38px;
  background: linear-gradient(180deg, #ffffff 0%, #fffafa 100%);
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0 0 26px;
  padding: 6px;
  border: 1px solid rgba(223, 0, 27, 0.08);
  border-radius: 8px;
  background: #fff5f6;
}

.auth-tabs button {
  position: relative;
  height: 50px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #77656a;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  transition: background 0.2s ease, box-shadow 0.2s ease, color 0.2s ease;
}

.auth-tabs button.active {
  background: #fff;
  color: #1f1a1b;
  box-shadow: 0 12px 24px rgba(183, 45, 56, 0.12);
}

.auth-title {
  display: grid;
  margin-bottom: 18px;
}

.auth-title strong {
  color: #221719;
  font-size: 22px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 9px;
  color: #5c454b;
  font-size: 14px;
  font-weight: 700;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 46px;
  padding: 0 14px;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #fffafa 100%);
  box-shadow:
    inset 0 0 0 1px rgba(159, 83, 92, 0.18),
    0 10px 22px rgba(73, 34, 40, 0.04);
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow:
    inset 0 0 0 1px rgba(223, 0, 27, 0.28),
    0 12px 24px rgba(73, 34, 40, 0.06);
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 1px rgba(223, 0, 27, 0.5),
    0 0 0 4px rgba(223, 0, 27, 0.08),
    0 14px 28px rgba(73, 34, 40, 0.08);
  transform: translateY(-1px);
}

.auth-submit {
  width: 100%;
  height: 48px;
  margin-top: 4px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(135deg, #df001b 0%, #f04b42 100%);
  box-shadow: 0 16px 28px rgba(223, 0, 27, 0.22);
}

.auth-submit:hover,
.auth-submit:focus {
  background: linear-gradient(135deg, #c90018 0%, #e43e35 100%);
}

.auth-links {
  display: flex;
  justify-content: center;
  gap: 34px;
  margin-top: 22px;
}

.auth-links a,
.auth-links button {
  border: 0;
  background: transparent;
  color: #6f6064;
  cursor: pointer;
  font-size: 15px;
}

.auth-links a:hover,
.auth-links button:hover {
  color: #df001b;
}

@media (max-width: 960px) {
  .auth-card {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 260px;
  }
}

@media (max-width: 640px) {
  .auth-panel {
    padding: 26px 20px 30px;
  }
}
</style>
