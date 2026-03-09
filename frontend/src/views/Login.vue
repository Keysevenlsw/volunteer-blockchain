<template>
  <div class="auth-page">
    <div class="bg-shape"></div>
    <div class="bg-shape shape-2"></div>

    <el-card class="auth-card" shadow="hover">
      <template #header>
        <div class="auth-header">
          <h2>登录系统</h2>
          <p>志愿者服务认证平台</p>
        </div>
      </template>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="请输入用户名" clearable />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="请输入密码" show-password clearable />
        </el-form-item>

        <el-button type="primary" class="full-btn" @click="handleLogin">登录</el-button>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <router-link to="/register">去注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'

export default {
  data() {
    return {
      username: '',
      password: ''
    }
  },
  methods: {
    handleLogin() {
      if (!this.username || !this.password) {
        ElMessage.warning('请填写完整登录信息')
        return
      }

      ElMessage.success('登录校验通过，待接入后端接口')
      console.log('登录信息：', this.username, this.password)
    }
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 16px;
  background: linear-gradient(135deg, #e8f1ff 0%, #f8fbff 45%, #e7fff6 100%);
}

.bg-shape {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.22) 0%, rgba(64, 158, 255, 0) 70%);
  top: -120px;
  left: -120px;
}

.shape-2 {
  width: 480px;
  height: 480px;
  top: auto;
  left: auto;
  right: -120px;
  bottom: -160px;
  background: radial-gradient(circle, rgba(103, 194, 58, 0.2) 0%, rgba(103, 194, 58, 0) 70%);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  z-index: 1;
  border-radius: 16px;
}

.auth-header h2 {
  margin: 0;
  font-size: 28px;
  color: #1f2d3d;
}

.auth-header p {
  margin: 8px 0 0;
  color: #6b7a90;
}

.full-btn {
  width: 100%;
  margin-top: 6px;
}

.auth-footer {
  margin-top: 16px;
  text-align: center;
  color: #73839b;
}

.auth-footer a {
  color: #2f7ef7;
  text-decoration: none;
}
</style>
