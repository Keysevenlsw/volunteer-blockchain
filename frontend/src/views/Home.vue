<template>
  <div class="home-page">
    <header class="banner" :style="bannerStyle">
      <div class="banner-actions">
        <span v-if="user" class="user-welcome">欢迎，{{ user.username }}</span>
        <router-link v-if="!isLoggedIn" class="login-link" to="/login">请登录</router-link>
        <router-link v-if="!isLoggedIn" to="/register">志愿者注册</router-link>
        <router-link v-if="!isLoggedIn" to="/register">公益组织注册</router-link>
        <button v-if="isLoggedIn" class="link-btn" @click="handleLogout">退出</button>
      </div>
      <div class="banner-content">
        <div class="logo-block">
          <img class="logo-img" :src="logoUrl" alt="链信公益" />
          <div>
            <div class="brand-cn">链信公益</div>
            <div class="brand-en">Lianxin Gongyi</div>
          </div>
          <span class="region-badge">全国</span>
        </div>
      </div>
    </header>

    <nav class="main-nav">
      <div class="nav-container">
        <a class="active">首页</a>
        <a>志愿项目</a>
        <a>志愿队伍</a>
        <a>信息动态</a>
        <a>政策文件</a>
        <a>使用指南</a>
      </div>
    </nav>

    <section class="headline">
      <span class="headline-tag">头条</span>
      <span class="headline-text">传递真善美传播正能量，志愿服务让城市更温暖</span>
    </section>

    <section class="content-grid">
      <div class="carousel-card">
        <el-carousel height="280px" indicator-position="outside">
          <el-carousel-item v-for="slide in bannerSlides" :key="slide.title">
            <div class="slide" :style="{ background: slide.background }">
              <div class="slide-title">{{ slide.title }}</div>
              <div class="slide-sub">{{ slide.subtitle }}</div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <div class="info-card">
        <div class="info-header">
          <h3>信息动态</h3>
          <button class="more-btn">更多</button>
        </div>
        <ul class="info-list">
          <li v-for="item in infoList" :key="item.title" class="info-item">
            <span class="info-tag">{{ item.tag }}</span>
            <span class="info-title">{{ item.title }}</span>
            <span class="info-date">{{ item.date }}</span>
          </li>
        </ul>
      </div>
    </section>

    <section class="project-section">
      <div class="section-title">
        <h2>已完成的支援项目</h2>
        <span>公开展示，未登录也可查看</span>
      </div>
      <el-row :gutter="18">
        <el-col v-for="project in projects" :key="project.id" :xs="24" :sm="12" :md="8">
          <el-card class="project-card" shadow="hover">
            <div class="project-cover" :style="projectCover(project.image)">
              <div class="project-label">完成</div>
            </div>
            <div class="project-body">
              <h4>{{ project.title }}</h4>
              <p>{{ project.description || '暂无项目描述' }}</p>
              <div class="project-meta">
                <span>{{ project.location || '地点待更新' }}</span>
                <span>{{ project.endDate || '日期待更新' }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { clearAuth, getCachedUser, getCurrentUser, getToken, saveAuth } from '../api/auth'
import { getCompletedProjects, getPublicInfo } from '../api/public'

export default {
  name: 'Home',
  data() {
    return {
      user: getCachedUser(),
      isLoggedIn: !!getToken(),
      infoList: [],
      projects: [],
      logoUrl: '/assets/branding/logo.png',
      bannerUrl: '/assets/branding/home-banner.png',
      bannerSlides: [
        {
          title: '百千万志愿者 结核病防治知识传播活动',
          subtitle: '你我共同行动 终结结核流行',
          background: 'linear-gradient(135deg, #9ae6ff, #b8e2ff 45%, #9fe7cf)'
        },
        {
          title: '学雷锋志愿服务月',
          subtitle: '让公益行动成为城市温度',
          background: 'linear-gradient(135deg, #ffe29f, #ffa99f 45%, #ff719a)'
        },
        {
          title: '社区关怀行动',
          subtitle: '守护银龄，传递爱心',
          background: 'linear-gradient(135deg, #c2ffd8, #465efb 50%, #6a82fb)'
        }
      ]
    }
  },
  computed: {
    bannerStyle() {
      return {
        backgroundImage: `linear-gradient(110deg, rgba(255, 232, 204, 0.9), rgba(255, 246, 236, 0.9)), url(${this.bannerUrl})`
      }
    }
  },
  async mounted() {
    const token = getToken()
    if (!token) {
      await this.loadPublicData()
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
    } finally {
      await this.loadPublicData()
    }
  },
  methods: {
    async loadPublicData() {
      try {
        const [info, projects] = await Promise.all([getPublicInfo(), getCompletedProjects(6)])
        this.infoList = info
        this.projects = projects
      } catch (error) {
        this.infoList = [
          { title: '关于提升志愿服务认证质量的通知', tag: '通知', date: '2026-03-05' },
          { title: '春季城市关爱行动志愿者招募启动', tag: '动态', date: '2026-02-26' },
          { title: '公益组织积分商城规则更新说明', tag: '政策', date: '2026-02-12' }
        ]
        this.projects = [
          { id: 1, title: '社区关怀行动', description: '关爱独居老人，提供陪伴与服务', location: '贵阳', endDate: '2026-02-20' },
          { id: 2, title: '校园环保志愿行', description: '绿色行动倡议，净化校园环境', location: '贵安新区', endDate: '2026-02-18' },
          { id: 3, title: '应急科普宣传', description: '普及应急救护知识', location: '遵义', endDate: '2026-02-10' }
        ]
      }
    },
    handleLogout() {
      clearAuth()
      this.user = null
      this.isLoggedIn = false
      ElMessage.success('已退出登录')
      this.$router.push('/login')
    },
    projectCover(image) {
      if (image) {
        return { backgroundImage: `url(${image})` }
      }
      return {
        backgroundImage: 'linear-gradient(135deg, rgba(255, 99, 71, 0.7), rgba(255, 193, 7, 0.7))'
      }
    }
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #fff5f4;
  color: #2b2b2b;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}

.banner-actions {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 14px;
  padding: 12px 28px 0;
  font-size: 13px;
  color: #9a2c2c;
}

.banner-actions a,
.link-btn {
  color: #d12c2c;
  text-decoration: none;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
}

.user-welcome {
  font-weight: 600;
  color: #b22b2b;
}

.login-link {
  color: #e60012;
  font-weight: 700;
}

.banner {
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  min-height: 180px;
  padding: 10px 28px 26px;
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  max-width: 1200px;
  margin: 0 auto;
}

.logo-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-img {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
  object-fit: contain;
  padding: 6px;
}

.brand-cn {
  font-size: 26px;
  font-weight: 700;
  color: #d60012;
}

.brand-en {
  font-size: 12px;
  color: #b35b5b;
}

.region-badge {
  background: #e60012;
  color: #fff;
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  margin-left: 6px;
}

.main-nav {
  background: #e60012;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 26px;
  padding: 12px 28px;
  color: #fff;
  font-size: 15px;
}

.nav-container a {
  color: #fff;
  text-decoration: none;
  font-weight: 600;
}

.nav-container a.active {
  background: #fff;
  color: #e60012;
  padding: 6px 16px;
  border-radius: 18px;
}

.headline {
  max-width: 1200px;
  margin: 20px auto 0;
  background: #fff7f2;
  padding: 12px 18px;
  border-radius: 10px;
  border: 1px dashed #f2b4b4;
  display: flex;
  align-items: center;
  gap: 12px;
}

.headline-tag {
  background: #e60012;
  color: #fff;
  font-weight: 700;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 6px;
}

.headline-text {
  font-size: 16px;
  font-weight: 600;
  color: #b22828;
}

.content-grid {
  max-width: 1200px;
  margin: 18px auto 0;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
  padding: 0 28px;
}

.carousel-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
}

.slide {
  height: 260px;
  border-radius: 10px;
  padding: 28px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.slide-title {
  font-size: 24px;
  font-weight: 700;
}

.slide-sub {
  margin-top: 6px;
  font-size: 14px;
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.info-header h3 {
  margin: 0;
  font-size: 18px;
  color: #c72828;
}

.more-btn {
  border: none;
  background: #ffeaea;
  color: #c72828;
  padding: 4px 10px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 12px;
}

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0d0d0;
  align-items: center;
}

.info-item:last-child {
  border-bottom: none;
}

.info-tag {
  background: #f9dcdc;
  color: #b74a4a;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 6px;
}

.info-title {
  font-size: 13px;
  color: #4a4a4a;
}

.info-date {
  font-size: 12px;
  color: #9b7b7b;
}

.project-section {
  max-width: 1200px;
  margin: 28px auto 0;
  padding: 0 28px 40px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}

.section-title h2 {
  margin: 0;
  font-size: 22px;
  color: #c72828;
}

.section-title span {
  color: #8f6c6c;
  font-size: 13px;
}

.project-card {
  border-radius: 14px;
  overflow: hidden;
}

.project-cover {
  height: 140px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.project-label {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(230, 0, 18, 0.9);
  color: #fff;
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 12px;
}

.project-body {
  padding: 12px 14px 16px;
}

.project-body h4 {
  margin: 0;
  font-size: 16px;
  color: #2b2b2b;
}

.project-body p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #6e6e6e;
  line-height: 1.5;
}

.project-meta {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9a7a7a;
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .nav-container {
    flex-wrap: wrap;
  }
  .banner-content {
    flex-direction: column;
    align-items: flex-start;
  }
  .banner-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>

