<template>
  <PortalLayout active-key="home" breadcrumb="注册">
    <section class="register-page">
      <div class="portal-shell register-card">
        <div class="register-tabs">
          <button type="button" :class="{ active: form.role === 'volunteer' }" @click="setRole('volunteer')">志愿者注册</button>
          <button type="button" :class="{ active: form.role === 'organization_admin' }" @click="setRole('organization_admin')">志愿组织注册</button>
        </div>

        <div class="register-steps">
          <div v-for="step in steps" :key="step.id" class="register-step" :class="{ done: step.id < 2, active: step.id === 2 }">
            <span>{{ step.id < 2 ? '✓' : step.id }}</span>
            <strong>{{ step.label }}</strong>
          </div>
        </div>

        <div class="register-body">
          <h2>{{ form.role === 'volunteer' ? '填写个人信息' : '填写组织信息' }}</h2>
          <el-form label-position="left" label-width="120px" class="register-form" @submit.prevent>
            <el-form-item label="用户名" required>
              <el-input v-model="form.username" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="邮箱" required>
              <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
            </el-form-item>
            <el-form-item v-if="form.role === 'organization_admin'" label="组织名称" required>
              <el-input v-model="form.organizationName" placeholder="请输入公益组织名称" clearable />
            </el-form-item>
            <el-form-item v-if="form.role === 'organization_admin'" label="组织简介">
              <el-input v-model="form.organizationDescription" type="textarea" :rows="4" placeholder="请介绍组织服务方向、覆盖区域和特色" />
            </el-form-item>
            <el-form-item label="密码" required>
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable />
            </el-form-item>
            <el-form-item label="确认密码" required>
              <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password clearable @keyup.enter="handleRegister" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="register-submit" :loading="loading" @click="handleRegister">完成注册</el-button>
              <el-button @click="router.push('/login')">已有账号，去登录</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </section>
  </PortalLayout>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PortalLayout from '../components/PortalLayout.vue'
import { register } from '../api/auth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const steps = [
  { id: 1, label: '阅读须知和协议' },
  { id: 2, label: '填写信息' },
  { id: 3, label: '注册实名和培训' },
  { id: 4, label: '获得志愿者号' }
]

const form = reactive(createEmptyForm(route.query.role))

watch(
  () => route.query.role,
  (value) => {
    setRole(value === 'organization_admin' ? 'organization_admin' : 'volunteer')
  }
)

function createEmptyForm(role = 'volunteer') {
  return {
    role: role === 'organization_admin' ? 'organization_admin' : 'volunteer',
    username: '',
    email: '',
    organizationName: '',
    organizationDescription: '',
    password: '',
    confirmPassword: ''
  }
}

function setRole(role) {
  form.role = role === 'organization_admin' ? 'organization_admin' : 'volunteer'
}

async function handleRegister() {
  if (!form.username || !form.email || !form.password || !form.confirmPassword) {
    ElMessage.warning('请填写完整注册信息')
    return
  }
  if (form.role === 'organization_admin' && !form.organizationName) {
    ElMessage.warning('志愿组织注册需要填写组织名称')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.error('两次输入密码不一致')
    return
  }

  loading.value = true
  try {
    await register({
      username: form.username,
      email: form.email,
      password: form.password,
      role: form.role,
      organizationName: form.role === 'organization_admin' ? form.organizationName : null,
      organizationDescription: form.role === 'organization_admin' ? form.organizationDescription : null
    })
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { role: form.role } })
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  padding: 0 0 70px;
}

.register-card {
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 36px rgba(161, 43, 61, 0.07);
}

.register-tabs {
  display: flex;
  height: 68px;
  background: #fff0f2;
}

.register-tabs button {
  width: 210px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: transparent;
  cursor: pointer;
  color: #333;
  font-size: 18px;
  font-weight: 700;
}

.register-tabs button.active {
  border-bottom-color: #df001b;
  color: #111;
}

.register-steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  padding: 34px 80px 28px;
  border-bottom: 1px solid #f0f0f0;
}

.register-step {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 12px;
  color: #888;
}

.register-step::before {
  content: "";
  position: absolute;
  top: 18px;
  left: -50%;
  width: 100%;
  height: 2px;
  background: #eee;
}

.register-step:first-child::before {
  display: none;
}

.register-step.done::before,
.register-step.active::before {
  background: #df001b;
}

.register-step span {
  position: relative;
  z-index: 1;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #bbb;
  color: #fff;
  font-weight: 700;
}

.register-step.done span,
.register-step.active span {
  background: #df001b;
}

.register-step strong {
  font-weight: 500;
}

.register-body {
  padding: 42px 110px 60px;
}

.register-body h2 {
  margin: 0 0 28px;
  padding-left: 16px;
  border-left: 5px solid #df001b;
  font-size: 21px;
}

.register-form {
  max-width: 720px;
}

.register-submit {
  min-width: 180px;
  background: #df001b;
  border-color: #df001b;
}

@media (max-width: 900px) {
  .register-steps {
    padding: 26px 18px;
  }

  .register-body {
    padding: 30px 20px 42px;
  }

  .register-tabs button {
    width: 50%;
  }
}
</style>
