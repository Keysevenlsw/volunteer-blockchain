<template>
  <div class="home-page">
    <header class="hero">
      <h1>志愿者服务认证系统</h1>
      <p>服务申报、审核认证、链上存证、积分激励的一体化平台</p>
      <div class="hero-actions">
        <router-link v-if="!isLoggedIn" to="/login">
          <el-button type="primary" size="large">立即登录</el-button>
        </router-link>
        <router-link v-if="!isLoggedIn" to="/register">
          <el-button plain size="large">注册账号</el-button>
        </router-link>
        <el-button v-if="isLoggedIn" type="primary" plain size="large" @click="handleLogout">退出登录</el-button>
      </div>

      <el-alert
        v-if="isLoggedIn && user"
        class="user-alert"
        type="success"
        :closable="false"
        show-icon
        :title="`当前登录：${user.username}（${user.email}）`"
      />
      <el-alert
        v-else
        class="user-alert"
        type="info"
        :closable="false"
        show-icon
        title="当前未登录，请先注册或登录后体验完整功能"
      />
    </header>

    <section class="cards">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8">
          <el-card class="feature-card" shadow="hover">
            <h3>可信存证</h3>
            <p>活动完成记录上链，支持追溯与防篡改。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-card class="feature-card" shadow="hover">
            <h3>多角色协同</h3>
            <p>志愿者、公益组织、管理员分权协作，流程清晰。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8">
          <el-card class="feature-card" shadow="hover">
            <h3>积分激励</h3>
            <p>完成服务获取积分，可在积分商城兑换奖励。</p>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { clearAuth, getCachedUser, getCurrentUser, getToken, saveAuth } from '../api/auth'

export default {
  name: 'Home',
  data() {
    return {
      user: getCachedUser(),
      isLoggedIn: !!getToken()
    }
  },
  async mounted() {
    const token = getToken()
    if (!token) {
      return
    }

    try {
      const profile = await getCurrentUser(token)
      this.user = profile
      saveAuth({ token, user: profile })
      this.isLoggedIn = true
    } catch (error) {
      clearAuth()
      this.user = null
      this.isLoggedIn = false
      ElMessage.warning(error.message || '登录状态已失效，请重新登录')
    }
  },
  methods: {
    handleLogout() {
      clearAuth()
      this.user = null
      this.isLoggedIn = false
      ElMessage.success('已退出登录')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 32px 16px;
  background: linear-gradient(160deg, #f0f7ff 0%, #f7fffb 100%);
}

.hero {
  max-width: 900px;
  margin: 0 auto;
  text-align: center;
  padding: 56px 20px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(31, 45, 61, 0.08);
}

.hero h1 {
  margin: 0;
  font-size: 38px;
  color: #1f2d3d;
}

.hero p {
  margin: 16px auto 0;
  max-width: 620px;
  color: #5f6f85;
  line-height: 1.7;
}

.hero-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.user-alert {
  margin: 22px auto 0;
  max-width: 680px;
  text-align: left;
}

.cards {
  max-width: 1100px;
  margin: 24px auto 0;
}

.feature-card {
  border-radius: 14px;
  margin-top: 16px;
}

.feature-card h3 {
  margin: 0;
  color: #23364d;
}

.feature-card p {
  margin: 10px 0 0;
  color: #667a92;
  line-height: 1.65;
}
</style>
