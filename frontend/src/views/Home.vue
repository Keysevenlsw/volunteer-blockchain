<template>
  <div class="home-page">
    <header class="hero">
      <h1>志愿者服务认证系统</h1>
      <p>服务申报、审核认证、链上存证、积分激励的一体化平台</p>
      <div class="hero-actions">
        <router-link to="/login">
          <el-button type="primary" size="large">立即登录</el-button>
        </router-link>
        <router-link to="/register">
          <el-button plain size="large">注册账号</el-button>
        </router-link>
      </div>
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
export default {
  name: 'Home'
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
