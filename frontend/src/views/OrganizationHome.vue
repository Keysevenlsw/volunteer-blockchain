<template>
  <div class="dashboard">
    <el-card class="hero-card" shadow="hover">
      <div class="hero-content">
        <div>
          <h2>公益组织主页</h2>
          <p>发布活动、审核志愿者、管理积分商城</p>
        </div>
        <el-button type="success" @click="goHome">返回首页</el-button>
      </div>
      <el-divider />
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8">
          <el-card class="feature-card" shadow="never">
            <h3>活动发布</h3>
            <p>创建与管理志愿服务活动。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-card class="feature-card" shadow="never">
            <h3>审核管理</h3>
            <p>审核志愿者加入申请与完成记录。</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8">
          <el-card class="feature-card" shadow="never">
            <h3>积分商城</h3>
            <p>上架商品与管理兑换记录。</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'OrganizationHome',
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
  background: linear-gradient(135deg, #f7fff4 0%, #f0f8ff 100%);
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
