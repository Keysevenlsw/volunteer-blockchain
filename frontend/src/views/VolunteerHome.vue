<template>
  <div class="dashboard">
    <el-card class="hero-card" shadow="hover">
      <div class="hero-content">
        <div>
          <h2>志愿者主页</h2>
          <p>查看活动、提交服务记录、兑换积分</p>
        </div>
        <el-button type="primary" @click="goHome">返回首页</el-button>
      </div>
      <el-divider />
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8">
          <el-card class="feature-card" shadow="never">
            <h3>活动列表</h3>
            <p>浏览加入组织发布的志愿服务活动。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-card class="feature-card" shadow="never">
            <h3>服务申报</h3>
            <p>提交活动完成情况，等待审核上链。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8">
          <el-card class="feature-card" shadow="never">
            <h3>积分兑换</h3>
            <p>用积分兑换奖励与服务机会。</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'VolunteerHome',
  methods: {
    goHome() {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  padding: 32px 16px;
  background: linear-gradient(135deg, #f3f8ff 0%, #f7fff8 100%);
}

.hero-card {
  max-width: 1080px;
  margin: 0 auto;
  border-radius: 18px;
}

.hero-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-content h2 {
  margin: 0;
  font-size: 28px;
  color: #1f2d3d;
}

.hero-content p {
  margin: 8px 0 0;
  color: #607489;
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
