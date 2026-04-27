<template>
  <PortalLayout active-key="home" breadcrumb="注册">
    <section class="register-page">
      <div class="portal-shell register-card">
        <div class="register-body">
          <div class="register-heading">
            <h2>{{ form.role === 'organization_admin' ? '填写组织信息' : '填写个人信息' }}</h2>
            <span>{{ form.role === 'organization_admin' ? '志愿组织注册' : '志愿者注册' }}</span>
          </div>

          <el-form label-position="top" class="register-form" @submit.prevent>
            <div class="register-grid">
              <el-form-item label="用户名" required>
                <el-input
                  v-model="form.username"
                  placeholder="请输入用户名"
                  clearable
                  autocomplete="nickname"
                />
              </el-form-item>

              <el-form-item label="邮箱" required>
                <el-input
                  v-model="form.email"
                  placeholder="请输入邮箱"
                  clearable
                  autocomplete="email"
                />
              </el-form-item>

              <el-form-item
                v-if="form.role === 'organization_admin'"
                label="组织名称"
                required
                class="register-grid__full"
              >
                <el-input
                  v-model="form.organizationName"
                  placeholder="请输入公益组织名称"
                  clearable
                />
              </el-form-item>

              <el-form-item
                v-if="form.role === 'organization_admin'"
                label="组织简介"
                class="register-grid__full"
              >
                <el-input
                  v-model="form.organizationDescription"
                  type="textarea"
                  :rows="4"
                  resize="none"
                  placeholder="请介绍组织服务方向、覆盖区域和特色"
                />
              </el-form-item>

              <el-form-item label="密码" required>
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  show-password
                  clearable
                  autocomplete="new-password"
                />
              </el-form-item>

              <el-form-item label="确认密码" required>
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  show-password
                  clearable
                  autocomplete="new-password"
                  @keyup.enter="handleRegister"
                />
              </el-form-item>
            </div>

            <div class="register-actions">
              <el-button
                type="primary"
                class="register-submit"
                :loading="loading"
                @click="handleRegister"
              >
                完成注册
              </el-button>
              <button type="button" class="register-login" @click="router.push('/login')">
                已有账号，去登录
              </button>
            </div>
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
  if (form.role !== 'organization_admin') {
    form.organizationName = ''
    form.organizationDescription = ''
  }
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
  padding: 0 0 72px;
}

.register-card {
  border: 1px solid rgba(223, 0, 27, 0.06);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 250, 250, 0.98)),
    linear-gradient(90deg, rgba(223, 0, 27, 0.04), rgba(255, 143, 79, 0.05));
  box-shadow: 0 24px 58px rgba(144, 47, 56, 0.1);
}

.register-body {
  width: min(820px, calc(100% - 40px));
  margin: 0 auto;
  padding: 46px 0 60px;
}

.register-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(223, 0, 27, 0.1);
}

.register-heading h2 {
  margin: 0;
  padding-left: 14px;
  border-left: 5px solid #df001b;
  color: #221719;
  font-size: 24px;
  line-height: 1.25;
}

.register-heading span {
  color: #8c757d;
  font-size: 14px;
  font-weight: 700;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.register-grid__full {
  grid-column: 1 / -1;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.register-form :deep(.el-form-item__label) {
  padding-bottom: 9px;
  color: #5c454b;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.register-form :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 15px;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #fffafa 100%);
  box-shadow:
    inset 0 0 0 1px rgba(159, 83, 92, 0.18),
    0 12px 22px rgba(73, 34, 40, 0.04);
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow:
    inset 0 0 0 1px rgba(223, 0, 27, 0.28),
    0 14px 26px rgba(73, 34, 40, 0.06);
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 1px rgba(223, 0, 27, 0.5),
    0 0 0 4px rgba(223, 0, 27, 0.08),
    0 16px 30px rgba(73, 34, 40, 0.08);
  transform: translateY(-1px);
}

.register-form :deep(.el-input__inner) {
  color: #24191c;
  font-size: 15px;
}

.register-form :deep(.el-textarea__inner) {
  min-height: 128px;
  padding: 14px 15px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #fffafa 100%);
  box-shadow:
    inset 0 0 0 1px rgba(159, 83, 92, 0.18),
    0 12px 22px rgba(73, 34, 40, 0.04);
  color: #24191c;
  font-size: 15px;
  line-height: 1.8;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.register-form :deep(.el-textarea__inner:hover) {
  box-shadow:
    inset 0 0 0 1px rgba(223, 0, 27, 0.28),
    0 14px 26px rgba(73, 34, 40, 0.06);
}

.register-form :deep(.el-textarea__inner:focus) {
  box-shadow:
    inset 0 0 0 1px rgba(223, 0, 27, 0.5),
    0 0 0 4px rgba(223, 0, 27, 0.08),
    0 16px 30px rgba(73, 34, 40, 0.08);
  transform: translateY(-1px);
}

.register-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 4px;
}

.register-submit {
  min-width: 184px;
  height: 48px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(135deg, #df001b 0%, #f04b42 100%);
  box-shadow: 0 16px 28px rgba(223, 0, 27, 0.22);
}

.register-submit:hover,
.register-submit:focus {
  background: linear-gradient(135deg, #c90018 0%, #e43e35 100%);
}

.register-login {
  height: 40px;
  padding: 0 18px;
  border: 1px solid rgba(159, 83, 92, 0.18);
  border-radius: 8px;
  background: #fff;
  color: #6f6064;
  cursor: pointer;
  font-size: 14px;
  transition: border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.register-login:hover {
  border-color: rgba(223, 0, 27, 0.32);
  color: #df001b;
  box-shadow: 0 10px 22px rgba(73, 34, 40, 0.06);
}

@media (max-width: 900px) {
  .register-body {
    width: min(100% - 32px, 820px);
    padding: 34px 0 46px;
  }

  .register-grid {
    grid-template-columns: 1fr;
  }

  .register-grid__full {
    grid-column: auto;
  }
}

@media (max-width: 640px) {
  .register-heading,
  .register-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .register-submit,
  .register-login {
    width: 100%;
  }
}
</style>
