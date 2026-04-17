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
              @click="activeRole = item.key"
            >
              {{ item.label }}
            </button>
          </div>

          <el-form class="auth-form" label-position="top" @submit.prevent>
            <el-form-item label="登录邮箱">
              <el-input v-model="form.email" placeholder="请输入登录邮箱" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item label="验证码">
              <div class="captcha-row">
                <el-input v-model="form.captcha" placeholder="请输入验证码" clearable />
                <div class="captcha-code">Y7M8</div>
              </div>
            </el-form-item>
            <el-button type="primary" class="auth-submit" :loading="loading" @click="handleLogin">登 录</el-button>
          </el-form>

          <div class="auth-links">
            <RouterLink to="/register?role=volunteer">立即注册</RouterLink>
            <button type="button" @click="ElMessage.info('账号查询功能需要后端接口支持')">账号查询</button>
            <button type="button" @click="ElMessage.info('忘记密码功能需要后端接口支持')">忘记密码?</button>
          </div>
        </div>
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
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
  password: '',
  captcha: ''
})

const loginTabs = [
  { key: 'volunteer', label: '志愿者登录' },
  { key: 'organization_admin', label: '志愿组织登录' },
  { key: 'admin', label: '管理员/审核员登录' }
]

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
  if (value === 'admin' || value === 'system_admin' || value === 'activity_reviewer' || value === 'product_reviewer') {
    return 'admin'
  }
  return 'volunteer'
}

function roleAllowed(user) {
  if (activeRole.value === 'admin') {
    return hasRole('system_admin', user) || hasRole('activity_reviewer', user) || hasRole('product_reviewer', user)
  }
  return hasRole(activeRole.value, user)
}

async function handleLogin() {
  if (!form.email || !form.password) {
    ElMessage.warning('请填写登录邮箱和密码')
    return
  }
  if (!form.captcha) {
    ElMessage.warning('请输入验证码')
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
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(161, 43, 61, 0.08);
}

.auth-visual {
  position: relative;
  min-height: 508px;
  overflow: hidden;
  background:
    radial-gradient(circle at 55% 18%, rgba(255, 205, 214, 0.42), transparent 10%),
    linear-gradient(180deg, #f3f4ff 0%, #fff7f0 78%);
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
  padding: 34px 38px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  margin: 0 -38px 26px;
  border-bottom: 1px solid #eee;
}

.auth-tabs button {
  position: relative;
  height: 54px;
  border: 0;
  background: #fff;
  color: #666;
  cursor: pointer;
  font-size: 17px;
}

.auth-tabs button.active {
  color: #111;
  font-weight: 700;
}

.auth-tabs button.active::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 34px;
  height: 3px;
  background: #df001b;
  transform: translateX(-50%);
}

.captcha-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 130px;
  gap: 12px;
}

.captcha-code {
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid #e2e2e2;
  border-radius: 8px;
  background: #f7fff5;
  color: #577352;
  font-size: 27px;
  font-weight: 700;
  letter-spacing: 4px;
}

.auth-submit {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  background: #df001b;
  border-color: #df001b;
}

.auth-links {
  display: flex;
  justify-content: center;
  gap: 36px;
  margin-top: 20px;
}

.auth-links a,
.auth-links button {
  border: 0;
  background: transparent;
  color: #666;
  cursor: pointer;
  font-size: 16px;
}

@media (max-width: 960px) {
  .auth-card {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 260px;
  }
}
</style>
