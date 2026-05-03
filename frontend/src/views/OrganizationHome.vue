<template>
  <PortalLayout active-key="organizations" breadcrumb="组织工作台">
    <section class="org-workbench" v-loading="pageLoading">
      <div class="portal-shell workbench-layout">
        <aside class="workbench-side">
          <div v-for="group in workbenchMenus" :key="group.title" class="workbench-menu-group">
            <div class="workbench-menu-group-title">{{ group.title }}</div>
            <button
              v-for="item in group.children"
              :key="item.key"
              type="button"
              class="workbench-menu-item"
              :class="{ active: activeTab === item.key }"
              @click="activeTab = item.key"
            >
              {{ item.label }}
            </button>
          </div>
        </aside>

        <main class="workbench-main">
          <section v-if="activeTab === 'personal-home'" class="panel admin-home-panel">
            <div class="admin-hero">
              <div class="admin-avatar-box">
                <div class="admin-avatar">
                  <img v-if="avatarUrl" :src="avatarUrl" :alt="user?.username || '组织管理员头像'" />
                  <span v-else>{{ user?.username?.slice(0, 1) || '组' }}</span>
                </div>
                <el-upload
                  class="avatar-upload"
                  accept="image/*"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="handleAvatarUpload"
                >
                  <el-button type="primary" plain :loading="avatarUploading">编辑头像</el-button>
                </el-upload>
              </div>
              <div>
                <h2>{{ user?.username || '组织管理员' }}</h2>
                <p class="muted-text">{{ organizationProfile?.organizationName || user?.organizationName || '当前组织' }} · 组织管理员</p>
                <div class="admin-info-grid">
                  <div><span>用户编号</span><strong>{{ user?.userId || '-' }}</strong></div>
                  <div><span>邮箱</span><strong>{{ user?.email || '-' }}</strong></div>
                  <div><span>组织编号</span><strong>{{ user?.organizationId || '-' }}</strong></div>
                  <div><span>注册时间</span><strong>{{ formatDateTime(user?.joinDate) }}</strong></div>
                </div>
              </div>
            </div>
            <div class="summary-grid admin-summary">
              <article><span>待审成员</span><strong>{{ pendingJoinCount }}</strong></article>
              <article><span>活动草稿/审核</span><strong>{{ activities.length }}</strong></article>
              <article><span>待审报告</span><strong>{{ pendingCompletionCount }}</strong></article>
              <article><span>链上异常</span><strong>{{ failedEvidenceCount }}</strong></article>
            </div>
          </section>

          <section v-else-if="activeTab === 'profile-edit'" class="panel profile-edit-panel">
            <h2>修改个人信息</h2>
            <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="92px" class="account-form">
              <el-form-item label="用户名" prop="username">
                <el-input v-model.trim="profileForm.username" maxlength="100" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model.trim="profileForm.email" maxlength="100" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存修改</el-button>
                <el-button @click="resetProfileForm">重置</el-button>
              </el-form-item>
            </el-form>
          </section>

          <section v-else-if="activeTab === 'change-password'" class="panel profile-edit-panel">
            <h2>修改密码</h2>
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="92px" class="account-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model.trim="passwordForm.oldPassword" type="password" maxlength="50" show-password placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model.trim="passwordForm.newPassword" type="password" maxlength="50" show-password placeholder="请输入新密码" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model.trim="passwordForm.confirmPassword" type="password" maxlength="50" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">修改密码</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </section>

          <template v-else>
            <section v-if="activeTab === 'overview'" class="panel">
              <div class="overview-hero">
                <div class="overview-logo">
                  <img v-if="organizationAvatarUrl" :src="organizationAvatarUrl" :alt="organizationProfile?.organizationName || '组织头像'" />
                  <span v-else>{{ organizationProfile?.organizationName?.slice(0, 1) || '组' }}</span>
                </div>
                <div class="overview-main">
                  <p class="overview-kicker">组织工作台</p>
                  <h2>{{ organizationProfile?.organizationName || user?.organizationName || '当前组织' }}</h2>
                  <div class="overview-desc" v-html="organizationProfile?.organizationDescription || '该组织已参与平台志愿服务认证，长期开展公益志愿服务活动。'"></div>
                </div>
              </div>
              <div class="overview-metrics">
                <article><span>公开活动</span><strong>{{ organizationProfile?.publicActivityCount ?? 0 }}</strong><small>面向平台展示</small></article>
                <article><span>组织志愿者</span><strong>{{ organizationProfile?.volunteerCount ?? 0 }}</strong><small>归属与参与统计</small></article>
                <article><span>待审成员</span><strong>{{ pendingJoinCount }}</strong><small>需要及时处理</small></article>
                <article><span>待审报告</span><strong>{{ pendingCompletionCount }}</strong><small>通过后发放积分</small></article>
              </div>
              <div class="overview-grid">
                <article class="overview-card">
                  <h3>近期活动</h3>
                  <div v-if="recentActivities.length" class="overview-list">
                    <div v-for="activity in recentActivities" :key="activity.activityId">
                      <strong>{{ activity.activityName }}</strong>
                      <span>{{ formatDateTime(activity.startDate) }} · {{ activity.location || '未设置地点' }}</span>
                    </div>
                  </div>
                  <EmptyState v-else mark="活" title="暂无活动" description="创建活动草稿后可在这里查看近期安排。" />
                </article>
                <article class="overview-card">
                  <h3>运营提醒</h3>
                  <div class="notice-list">
                    <p>成员申请通过后，其他组织申请会自动作废删除。</p>
                    <p>活动草稿需提交平台审核，通过后才会公开展示。</p>
                    <p>完成报告通过后会触发积分和链上存证流程。</p>
                  </div>
                </article>
              </div>
            </section>

            <section v-else-if="activeTab === 'members'" class="panel">
              <div class="panel-head"><h2>成员申请审核</h2><el-button @click="refreshAll">刷新</el-button></div>
              <el-table :data="joinRequests" empty-text="暂无成员申请">
                <el-table-column prop="username" label="申请人" width="130" />
                <el-table-column prop="applyReason" label="申请说明" min-width="240" show-overflow-tooltip />
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <StatusBadge :label="getStatusMeta('joinRequest', row.status).label" :tone="getStatusMeta('joinRequest', row.status).tone" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="210">
                  <template #default="{ row }">
                    <el-button size="small" type="success" :disabled="row.status !== 'pending'" @click="reviewJoin(row, 'approved')">通过</el-button>
                    <el-button size="small" type="danger" plain :disabled="row.status !== 'pending'" @click="reviewJoin(row, 'rejected')">拒绝</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>

            <section v-else-if="activeTab === 'activities'" class="panel">
              <div class="panel-head"><h2>活动草稿与提交</h2><el-button type="primary" @click="openActivityDialog()">新建活动</el-button></div>
              <div class="card-grid">
                <article v-for="activity in activities" :key="activity.activityId" class="work-card">
                  <div class="card-topline">
                    <h3>{{ activity.activityName }}</h3>
                    <StatusBadge :label="getStatusMeta('review', activity.reviewStatus).label" :tone="getStatusMeta('review', activity.reviewStatus).tone" />
                  </div>
                  <p>{{ activity.location || '未设置地点' }} · {{ formatDateTime(activity.startDate) }}</p>
                  <p>{{ activity.description || '暂无活动说明' }}</p>
                  <div class="actions">
                    <el-button size="small" @click="openActivityDialog(activity)">编辑</el-button>
                    <el-button size="small" type="primary" plain :disabled="activity.reviewStatus === 'pending_review' || activity.reviewStatus === 'approved'" @click="handleSubmitActivity(activity)">提交审核</el-button>
                    <el-button size="small" type="danger" plain @click="handleDeleteActivity(activity)">删除</el-button>
                  </div>
                </article>
              </div>
              <EmptyState v-if="!activities.length" mark="活" title="暂无活动" description="创建活动草稿后可提交平台审核。" />
            </section>

            <section v-else-if="activeTab === 'reviews'" class="panel">
              <div class="panel-head"><h2>完成报告审核</h2><el-button @click="refreshAll">刷新</el-button></div>
              <div class="card-grid">
                <article v-for="review in completionReviews" :key="review.completionId" class="work-card">
                  <div class="card-topline">
                    <h3>{{ review.activityName }}</h3>
                    <StatusBadge :label="getStatusMeta('completion', review.status).label" :tone="getStatusMeta('completion', review.status).tone" />
                  </div>
                  <p>{{ review.username }} · {{ review.serviceLocation || '未填写地点' }}</p>
                  <p>{{ review.reportText || '暂无工作总结' }}</p>
                  <div class="actions">
                    <el-button size="small" type="success" :disabled="review.status !== 'pending'" @click="reviewCompletionItem(review, 'approved')">通过</el-button>
                    <el-button size="small" type="danger" plain :disabled="review.status !== 'pending'" @click="reviewCompletionItem(review, 'rejected')">驳回</el-button>
                  </div>
                </article>
              </div>
              <EmptyState v-if="!completionReviews.length" mark="审" title="暂无完成报告" description="志愿者提交的服务报告会在这里审核。" />
            </section>

            <section v-else-if="activeTab === 'org-info'" class="panel org-info-panel">
              <div class="panel-head">
                <h2>修改组织信息</h2>
                <el-button @click="resetOrganizationForm">重置</el-button>
              </div>
              <el-form ref="orgFormRef" :model="orgForm" :rules="orgRules" label-position="top" class="organization-form">
                <div class="organization-form-top">
                  <div class="organization-avatar-box">
                    <div class="organization-avatar-preview">
                      <img v-if="organizationAvatarUrl" :src="organizationAvatarUrl" :alt="orgForm.organizationName || '组织头像'" />
                      <span v-else>{{ orgForm.organizationName?.slice(0, 1) || '组' }}</span>
                    </div>
                    <el-upload
                      accept="image/*"
                      :show-file-list="false"
                      :before-upload="beforeAvatarUpload"
                      :http-request="handleOrganizationAvatarUpload"
                    >
                      <el-button type="primary" plain :loading="orgAvatarUploading">更换组织头像</el-button>
                    </el-upload>
                  </div>
                  <div class="organization-fields">
                    <el-form-item label="组织名称" prop="organizationName">
                      <el-input v-model.trim="orgForm.organizationName" maxlength="255" show-word-limit placeholder="请输入组织名称" />
                    </el-form-item>
                    <el-form-item label="组织编号">
                      <el-input :model-value="organizationProfile?.organizationId || '-'" disabled />
                    </el-form-item>
                  </div>
                </div>
                <el-form-item label="组织简介" prop="organizationDescription">
                  <div class="rich-editor">
                    <div class="rich-toolbar">
                      <el-button size="small" @click="formatRichText('bold')">加粗</el-button>
                      <el-button size="small" @click="formatRichText('italic')">斜体</el-button>
                      <el-button size="small" @click="formatRichText('insertUnorderedList')">列表</el-button>
                      <el-button size="small" @click="formatRichText('formatBlock', 'h3')">标题</el-button>
                      <el-button size="small" @click="insertRichLink">链接</el-button>
                      <el-button size="small" @click="formatRichText('removeFormat')">清除格式</el-button>
                    </div>
                    <div
                      ref="richEditorRef"
                      class="rich-editor-body"
                      contenteditable="true"
                      data-placeholder="请输入组织简介，可设置标题、加粗、列表和链接"
                      @input="handleRichInput"
                      @blur="handleRichInput"
                    ></div>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="orgSaving" @click="handleSaveOrganization">保存组织信息</el-button>
                  <el-button @click="resetOrganizationForm">取消修改</el-button>
                </el-form-item>
              </el-form>
            </section>

            <section v-else-if="activeTab === 'products'" class="panel">
            <div class="panel-head"><h2>商品草稿与提交</h2><el-button type="primary" @click="openProductDialog()">新建商品</el-button></div>
            <div class="card-grid">
              <article v-for="product in products" :key="product.productId" class="work-card">
                <div class="card-topline">
                  <h3>{{ product.productName }}</h3>
                  <StatusBadge :label="getStatusMeta('review', product.reviewStatus).label" :tone="getStatusMeta('review', product.reviewStatus).tone" />
                </div>
                <p>{{ product.productDescription || '暂无商品说明' }}</p>
                <p>{{ product.price }} 积分 · 库存 {{ product.stock }}</p>
                <div class="actions">
                  <el-button size="small" @click="openProductDialog(product)">编辑</el-button>
                  <el-button size="small" type="primary" plain :disabled="product.reviewStatus === 'pending_review' || product.reviewStatus === 'approved'" @click="handleSubmitProduct(product)">提交审核</el-button>
                  <el-button size="small" type="danger" plain @click="handleDeleteProduct(product)">删除</el-button>
                </div>
              </article>
            </div>
            <EmptyState v-if="!products.length" mark="商" title="暂无商品" description="创建商品草稿后可提交平台审核。" />
          </section>

          <section v-else-if="activeTab === 'redemptions'" class="panel">
            <div class="panel-head"><h2>兑换履约</h2><el-button @click="refreshAll">刷新</el-button></div>
            <el-table :data="redemptions" empty-text="暂无兑换记录">
              <el-table-column prop="username" label="志愿者" width="120" />
              <el-table-column prop="productName" label="商品" min-width="160" />
              <el-table-column prop="pointsCost" label="积分" width="90" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('redemption', row.status).label" :tone="getStatusMeta('redemption', row.status).tone" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="190">
                <template #default="{ row }">
                  <el-button size="small" type="success" :disabled="row.status !== 'pending'" @click="updateRedemption(row, 'fulfilled')">发放</el-button>
                  <el-button size="small" type="warning" plain :disabled="row.status !== 'pending'" @click="updateRedemption(row, 'cancelled')">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section v-else-if="activeTab === 'evidences'" class="panel">
            <div class="panel-head"><h2>链上存证监控</h2><el-button @click="refreshAll">刷新</el-button></div>
            <el-table :data="evidences" empty-text="暂无存证记录">
              <el-table-column prop="bizId" label="业务 ID" width="100" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <StatusBadge :label="getStatusMeta('evidence', row.onchainStatus).label" :tone="getStatusMeta('evidence', row.onchainStatus).tone" />
                </template>
              </el-table-column>
              <el-table-column prop="txHash" label="交易哈希" min-width="220" show-overflow-tooltip />
              <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button size="small" type="primary" plain :disabled="row.onchainStatus !== 'failed'" @click="handleRetry(row)">重试</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>

          <EmptyState v-else mark="工" title="请选择工作台模块" description="从左侧导航进入对应功能。" />
          </template>
        </main>
      </div>
    </section>

    <el-dialog v-model="activityDialogVisible" :title="activityForm.id ? '编辑活动' : '新建活动'" width="min(760px, calc(100vw - 24px))">
      <el-form label-position="top">
        <el-form-item label="活动名称"><el-input v-model="activityForm.activityName" /></el-form-item>
        <el-form-item label="活动描述"><el-input v-model="activityForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="开始时间"><el-date-picker v-model="activityForm.startDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="结束时间"><el-date-picker v-model="activityForm.endDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="地点"><el-input v-model="activityForm.location" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="申请积分"><el-input-number v-model="activityForm.requestedRewardPoints" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="最大人数"><el-input-number v-model="activityForm.maxParticipants" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="报名截止"><el-date-picker v-model="activityForm.enrollDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="联系人"><el-input v-model="activityForm.contactName" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="联系电话"><el-input v-model="activityForm.contactPhone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="标签"><el-input v-model="activityForm.categoryTags" placeholder="例如 GENERAL,环保" /></el-form-item>
        <el-form-item label="封面路径"><el-input v-model="activityForm.imagePath" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveActivity">保存活动</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="productDialogVisible" :title="productForm.id ? '编辑商品' : '新建商品'" width="min(640px, calc(100vw - 24px))">
      <el-form label-position="top">
        <el-form-item label="商品名称"><el-input v-model="productForm.productName" /></el-form-item>
        <el-form-item label="商品描述"><el-input v-model="productForm.productDescription" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="12"><el-form-item label="积分价格"><el-input-number v-model="productForm.price" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="库存"><el-input-number v-model="productForm.stock" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="图片路径"><el-input v-model="productForm.imagePath" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">保存商品</el-button>
      </template>
    </el-dialog>
  </PortalLayout>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '../components/EmptyState.vue'
import PortalLayout from '../components/PortalLayout.vue'
import StatusBadge from '../components/StatusBadge.vue'
import {
  changeCurrentUserPassword,
  clearAuth,
  getCachedUser,
  getCurrentUser,
  getToken,
  redirectToLogin,
  saveAuth,
  updateCurrentUser,
  uploadCurrentUserAvatar
} from '../api/auth'
import {
  createActivity,
  createProduct,
  deleteActivity,
  deleteProduct,
  getCompletionReviews,
  getOrganizationActivities,
  getOrganizationEvidences,
  getOrganizationJoinRequests,
  getOrganizationProducts,
  getOrganizationRedemptions,
  getWorkbenchOrganization,
  retryEvidence,
  reviewCompletion,
  reviewJoinRequest,
  submitActivity,
  submitProduct,
  updateActivity,
  updateWorkbenchOrganization,
  updateProduct,
  updateRedemptionStatus,
  uploadWorkbenchOrganizationAvatar
} from '../api/platform'
import { formatDateTime, getStatusMeta, toDateTimeInputValue } from '../utils/ui'

const pageLoading = ref(false)
const user = ref(getCachedUser())
const activeTab = ref('personal-home')
const joinRequests = ref([])
const activities = ref([])
const completionReviews = ref([])
const products = ref([])
const redemptions = ref([])
const evidences = ref([])
const organizationProfile = ref(null)
const activityDialogVisible = ref(false)
const productDialogVisible = ref(false)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const orgFormRef = ref(null)
const richEditorRef = ref(null)
const profileSaving = ref(false)
const passwordSaving = ref(false)
const avatarUploading = ref(false)
const orgSaving = ref(false)
const orgAvatarUploading = ref(false)
const activityForm = reactive(createEmptyActivityForm())
const productForm = reactive(createEmptyProductForm())
const profileForm = reactive({
  username: '',
  email: ''
})
const orgForm = reactive({
  organizationName: '',
  organizationDescription: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const confirmPasswordRule = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const profileRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 100, message: '用户名长度必须在 2 到 100 之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
  ]
}

const orgRules = {
  organizationName: [
    { required: true, message: '请输入组织名称', trigger: 'blur' },
    { min: 2, max: 255, message: '组织名称长度必须在 2 到 255 之间', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 6, max: 50, message: '原密码长度必须在 6 到 50 之间', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '新密码长度必须在 6 到 50 之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: confirmPasswordRule, trigger: 'blur' }]
}

const workbenchMenus = [
  {
    title: '个人中心',
    children: [
      { key: 'personal-home', label: '个人主页' },
      { key: 'profile-edit', label: '修改个人信息' },
      { key: 'change-password', label: '修改密码' }
    ]
  },
  {
    title: '组织工作台',
    children: [
      { key: 'overview', label: '组织概览' },
      { key: 'org-info', label: '修改组织信息' },
      { key: 'members', label: '成员申请审核' },
      { key: 'activities', label: '活动草稿/提交' },
      { key: 'reviews', label: '完成报告审核' }
    ]
  },
  {
    title: '运营管理',
    children: [
      { key: 'products', label: '商品草稿/提交' },
      { key: 'redemptions', label: '兑换履约' },
      { key: 'evidences', label: '链上存证监控' }
    ]
  }
]

const pendingJoinCount = computed(() => joinRequests.value.filter((item) => item.status === 'pending').length)
const pendingCompletionCount = computed(() => completionReviews.value.filter((item) => item.status === 'pending').length)
const failedEvidenceCount = computed(() => evidences.value.filter((item) => item.onchainStatus === 'failed').length)
const avatarUrl = computed(() => resolveImage(user.value?.avatarPath))
const organizationAvatarUrl = computed(() => resolveImage(organizationProfile.value?.avatarPath))
const recentActivities = computed(() => activities.value.slice(0, 3))

onMounted(initializePage)

function createEmptyActivityForm() {
  return {
    id: null,
    activityName: '',
    description: '',
    startDate: '',
    endDate: '',
    location: '',
    contactName: '',
    contactPhone: '',
    categoryTags: '',
    imagePath: '',
    maxParticipants: 20,
    requestedRewardPoints: 10,
    enrollDeadline: ''
  }
}

function createEmptyProductForm() {
  return { id: null, productName: '', productDescription: '', price: 10, stock: 1, imagePath: '' }
}

async function initializePage() {
  pageLoading.value = true
  try {
    await loadProfile()
    await refreshAll()
  } finally {
    pageLoading.value = false
  }
}

async function loadProfile() {
  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('organization_admin')
    return
  }
  try {
    const profile = await getCurrentUser(token)
    user.value = profile
    saveAuth({ token, user: profile })
    fillProfileForm(profile)
  } catch (error) {
    clearAuth()
    ElMessage.error(error.message || '登录状态已失效')
    redirectToLogin('organization_admin')
  }
}

async function refreshAll() {
  const [joinData, activityData, completionData, productData, redemptionData, evidenceData, orgData] = await Promise.all([
    getOrganizationJoinRequests().catch(() => []),
    getOrganizationActivities().catch(() => []),
    getCompletionReviews().catch(() => []),
    getOrganizationProducts().catch(() => []),
    getOrganizationRedemptions().catch(() => []),
    getOrganizationEvidences().catch(() => []),
    getWorkbenchOrganization().catch(() => null)
  ])
  joinRequests.value = joinData
  activities.value = activityData
  completionReviews.value = completionData
  products.value = productData
  redemptions.value = redemptionData
  evidences.value = evidenceData
  organizationProfile.value = orgData
  fillOrganizationForm(orgData)
}

function fillProfileForm(profile = user.value) {
  profileForm.username = profile?.username || ''
  profileForm.email = profile?.email || ''
}

function resetProfileForm() {
  fillProfileForm()
  profileFormRef.value?.clearValidate()
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

function fillOrganizationForm(organization = organizationProfile.value) {
  orgForm.organizationName = organization?.organizationName || ''
  orgForm.organizationDescription = organization?.organizationDescription || ''
  setRichEditorContent(orgForm.organizationDescription)
}

function resetOrganizationForm() {
  fillOrganizationForm()
  orgFormRef.value?.clearValidate()
}

function setRichEditorContent(value = '') {
  nextTick(() => {
    if (richEditorRef.value && richEditorRef.value.innerHTML !== value) {
      richEditorRef.value.innerHTML = value || ''
    }
  })
}

function handleRichInput() {
  orgForm.organizationDescription = richEditorRef.value?.innerHTML || ''
}

function formatRichText(command, value = null) {
  richEditorRef.value?.focus()
  document.execCommand(command, false, value)
  handleRichInput()
}

async function insertRichLink() {
  try {
    const { value } = await ElMessageBox.prompt('请输入链接地址', '插入链接', {
      inputPlaceholder: 'https://example.com',
      confirmButtonText: '插入',
      cancelButtonText: '取消'
    })
    const url = String(value || '').trim()
    if (url) {
      formatRichText('createLink', url)
    }
  } catch {
    // 取消插入链接。
  }
}

function beforeAvatarUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('头像文件必须是图片')
    return false
  }
  if (file.size / 1024 / 1024 > 3) {
    ElMessage.error('头像图片不能超过 3MB')
    return false
  }
  return true
}

async function handleAvatarUpload({ file, onSuccess, onError }) {
  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('organization_admin')
    return
  }

  avatarUploading.value = true
  try {
    const profile = await uploadCurrentUserAvatar(token, file)
    user.value = profile
    saveAuth({ token, user: profile })
    onSuccess?.(profile)
    ElMessage.success('头像已更新')
  } catch (error) {
    onError?.(error)
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function handleOrganizationAvatarUpload({ file, onSuccess, onError }) {
  orgAvatarUploading.value = true
  try {
    const organization = await uploadWorkbenchOrganizationAvatar(file)
    organizationProfile.value = organization
    fillOrganizationForm(organization)
    onSuccess?.(organization)
    ElMessage.success('组织头像已更新')
  } catch (error) {
    onError?.(error)
    ElMessage.error(error.message || '组织头像上传失败')
  } finally {
    orgAvatarUploading.value = false
  }
}

async function handleSaveProfile() {
  if (!profileFormRef.value) {
    return
  }
  try {
    await profileFormRef.value.validate()
    await ElMessageBox.confirm('确认修改个人信息吗？', '修改确认', {
      confirmButtonText: '确认修改',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('organization_admin')
    return
  }

  profileSaving.value = true
  try {
    const profile = await updateCurrentUser(token, {
      username: profileForm.username,
      email: profileForm.email
    })
    user.value = profile
    saveAuth({ token, user: profile })
    fillProfileForm(profile)
    ElMessage.success('个人信息已更新')
  } catch (error) {
    ElMessage.error(error.message || '个人信息更新失败')
  } finally {
    profileSaving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) {
    return
  }
  try {
    await passwordFormRef.value.validate()
    await ElMessageBox.confirm('确认修改密码吗？修改后请使用新密码登录。', '修改确认', {
      confirmButtonText: '确认修改',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  const token = getToken()
  if (!token) {
    clearAuth()
    redirectToLogin('organization_admin')
    return
  }

  passwordSaving.value = true
  try {
    await changeCurrentUserPassword(token, {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    resetPasswordForm()
    ElMessage.success('密码已修改')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

async function handleSaveOrganization() {
  if (!orgFormRef.value) {
    return
  }
  handleRichInput()
  try {
    await orgFormRef.value.validate()
  } catch {
    return
  }

  orgSaving.value = true
  try {
    const organization = await updateWorkbenchOrganization({
      organizationName: orgForm.organizationName,
      organizationDescription: orgForm.organizationDescription
    })
    organizationProfile.value = organization
    fillOrganizationForm(organization)
    if (user.value) {
      user.value = { ...user.value, organizationName: organization.organizationName }
      saveAuth({ token: getToken(), user: user.value })
    }
    ElMessage.success('组织信息已更新')
  } catch (error) {
    ElMessage.error(error.message || '组织信息保存失败')
  } finally {
    orgSaving.value = false
  }
}

async function reviewJoin(row, status) {
  try {
    const action = status === 'approved' ? '通过' : '拒绝'
    const { value } = await ElMessageBox.prompt(`请输入${action}备注`, `${action}成员申请`, { inputPlaceholder: '可填写审核说明' })
    await reviewJoinRequest(row.id, { status, reviewNote: value || '' })
    ElMessage.success(`已${action}申请`)
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

function openActivityDialog(row) {
  Object.assign(activityForm, createEmptyActivityForm(), row ? {
    id: row.activityId,
    activityName: row.activityName,
    description: row.description,
    startDate: toDateTimeInputValue(row.startDate),
    endDate: toDateTimeInputValue(row.endDate),
    location: row.location,
    contactName: row.contactName,
    contactPhone: row.contactPhone,
    categoryTags: row.categoryTags,
    imagePath: row.imagePath,
    maxParticipants: row.maxParticipants,
    requestedRewardPoints: row.requestedRewardPoints ?? row.approvedRewardPoints ?? 10,
    enrollDeadline: toDateTimeInputValue(row.enrollDeadline)
  } : {})
  activityDialogVisible.value = true
}

async function saveActivity() {
  try {
    const payload = { ...activityForm }
    delete payload.id
    if (activityForm.id) {
      await updateActivity(activityForm.id, payload)
      ElMessage.success('活动已更新')
    } else {
      await createActivity(payload)
      ElMessage.success('活动已创建')
    }
    activityDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '活动保存失败')
  }
}

async function handleSubmitActivity(row) {
  try {
    await submitActivity(row.activityId)
    ElMessage.success('活动已提交平台审核')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

async function handleDeleteActivity(row) {
  try {
    await ElMessageBox.confirm(`确认删除活动“${row.activityName}”吗？`, '删除活动', { type: 'warning' })
    await deleteActivity(row.activityId)
    ElMessage.success('活动已删除')
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function reviewCompletionItem(row, status) {
  try {
    const action = status === 'approved' ? '通过' : '驳回'
    const { value } = await ElMessageBox.prompt(`请输入${action}说明`, `${action}完成报告`, { inputPlaceholder: '可填写审核说明或驳回原因' })
    await reviewCompletion(row.completionId, { status, reviewNote: value || '' })
    ElMessage.success(`已${action}完成报告`)
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

function openProductDialog(row) {
  Object.assign(productForm, createEmptyProductForm(), row ? {
    id: row.productId,
    productName: row.productName,
    productDescription: row.productDescription,
    price: row.price,
    stock: row.stock,
    imagePath: row.imagePath
  } : {})
  productDialogVisible.value = true
}

async function saveProduct() {
  try {
    const payload = { ...productForm }
    delete payload.id
    if (productForm.id) {
      await updateProduct(productForm.id, payload)
      ElMessage.success('商品已更新')
    } else {
      await createProduct(payload)
      ElMessage.success('商品已创建')
    }
    productDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '商品保存失败')
  }
}

async function handleSubmitProduct(row) {
  try {
    await submitProduct(row.productId)
    ElMessage.success('商品已提交平台审核')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

async function handleDeleteProduct(row) {
  try {
    await ElMessageBox.confirm(`确认删除商品“${row.productName}”吗？`, '删除商品', { type: 'warning' })
    await deleteProduct(row.productId)
    ElMessage.success('商品已删除')
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function updateRedemption(row, status) {
  try {
    await updateRedemptionStatus(row.id, { status })
    ElMessage.success('兑换状态已更新')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '处理失败')
  }
}

async function handleRetry(row) {
  try {
    await retryEvidence(row.id)
    ElMessage.success('已发起重试')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '重试失败')
  }
}

function resolveImage(path) {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//.test(path)) {
    return path
  }
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return path.startsWith('/') ? `${baseUrl}${path}` : `${baseUrl}/${path}`
}
</script>

<style scoped>
.org-workbench {
  padding-bottom: 72px;
}

.workbench-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 22px;
}

.panel,
.summary-grid article {
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fff;
}

.workbench-side {
  overflow: hidden;
  border: 1px solid #f1eeee;
  background: #fff;
}

.workbench-menu-group + .workbench-menu-group {
  border-top: 1px solid #f6f2f2;
}

.workbench-menu-group-title,
.workbench-menu-item {
  width: 100%;
  border: 0;
  background: #fff;
  text-align: left;
}

.workbench-menu-group-title {
  display: flex;
  align-items: center;
  min-height: 42px;
  border-left: 3px solid #d92727;
  padding: 0 22px;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.workbench-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 40px;
  border-left: 3px solid transparent;
  padding: 0 24px 0 38px;
  color: #333;
  cursor: pointer;
  font-size: 15px;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.workbench-menu-item:hover {
  background: #fff7f7;
  color: #d92727;
}

.workbench-menu-item.active {
  border-left-color: #d92727;
  background: #fff1f1;
  color: #d92727;
  font-weight: 500;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.admin-summary {
  margin: 24px 0 0;
}

.summary-grid article {
  padding: 20px;
}

.summary-grid span {
  color: #777;
}

.summary-grid strong {
  display: block;
  margin-top: 12px;
  font-size: 32px;
}

.panel {
  padding: 24px;
}

.panel h2 {
  margin: 0 0 20px;
  font-size: 23px;
}

.admin-home-panel {
  padding: 30px;
}

.admin-hero {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.admin-avatar-box {
  display: grid;
  justify-items: center;
  gap: 14px;
}

.admin-avatar {
  width: 132px;
  height: 132px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(180deg, #ffddd8, #fff3ef);
  color: #d8001b;
  font-size: 46px;
  font-weight: 700;
  box-shadow: 0 10px 22px rgba(161, 43, 61, 0.16);
}

.admin-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload :deep(.el-button) {
  min-width: 116px;
  border-color: #df001b;
  color: #df001b;
}

.avatar-upload :deep(.el-button:hover) {
  border-color: #d92727;
  background: #fff1f1;
  color: #d92727;
}

.admin-hero h2 {
  margin: 8px 0 10px;
}

.admin-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.admin-info-grid div {
  min-height: 74px;
  padding: 14px 16px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: #fffafa;
}

.admin-info-grid span {
  display: block;
  color: #7b6a6a;
  font-size: 13px;
}

.admin-info-grid strong {
  display: block;
  margin-top: 8px;
  color: #2f1e23;
  font-size: 18px;
  line-height: 1.35;
  word-break: break-all;
}

.profile-edit-panel {
  max-width: 760px;
}

.account-form {
  margin-top: 24px;
  padding: 24px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: linear-gradient(180deg, #fffafa 0%, #fff 48%);
}

.account-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #df001b inset;
}

.account-form :deep(.el-button--primary) {
  border-color: #df001b;
  background: #df001b;
}

.overview-hero {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  gap: 22px;
  align-items: center;
  padding: 26px;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(223, 0, 27, 0.08), rgba(255, 247, 247, 0.96)),
    #fffafa;
}

.overview-logo {
  width: 118px;
  height: 118px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe2e5, #fff3e8);
  color: #df001b;
  font-size: 42px;
  font-weight: 700;
  box-shadow: 0 10px 22px rgba(161, 43, 61, 0.14);
}

.overview-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overview-kicker {
  margin: 0 0 8px;
  color: #d92727;
  font-weight: 700;
}

.overview-main h2 {
  margin: 0 0 12px;
  color: #2f1e23;
  font-size: 28px;
}

.overview-desc {
  max-width: 820px;
  color: #66575a;
  line-height: 1.8;
}

.overview-desc :deep(p) {
  margin: 0 0 8px;
}

.overview-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 18px 0;
}

.overview-metrics article {
  min-height: 126px;
  padding: 18px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: #fff;
}

.overview-metrics span,
.overview-metrics small {
  display: block;
  color: #7b6a6a;
}

.overview-metrics strong {
  display: block;
  margin: 10px 0 8px;
  color: #2f1e23;
  font-size: 34px;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 16px;
}

.overview-card {
  padding: 20px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: linear-gradient(180deg, #fffafa, #fff 62%);
}

.overview-card h3 {
  margin: 0 0 16px;
  color: #2f1e23;
  font-size: 18px;
}

.overview-list {
  display: grid;
  gap: 12px;
}

.overview-list div,
.notice-list p {
  margin: 0;
  padding: 14px 16px;
  border-radius: 8px;
  background: #fff;
  color: #66575a;
  line-height: 1.7;
}

.overview-list strong,
.overview-list span {
  display: block;
}

.overview-list span {
  margin-top: 5px;
  color: #8a7a7d;
  font-size: 13px;
}

.notice-list {
  display: grid;
  gap: 12px;
}

.org-info-panel {
  padding: 28px;
}

.organization-form {
  margin-top: 8px;
}

.organization-form-top {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.organization-avatar-box {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 20px;
  border: 1px solid #f1eeee;
  border-radius: 8px;
  background: #fffafa;
}

.organization-avatar-preview {
  width: 132px;
  height: 132px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffe2e5, #fff3e8);
  color: #df001b;
  font-size: 46px;
  font-weight: 700;
}

.organization-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.organization-fields {
  min-width: 0;
}

.rich-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
}

.rich-editor:focus-within {
  border-color: #df001b;
  box-shadow: 0 0 0 1px #df001b inset;
}

.rich-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border-bottom: 1px solid #f1eeee;
  background: #fffafa;
}

.rich-toolbar :deep(.el-button) {
  margin-left: 0;
}

.rich-editor-body {
  min-height: 220px;
  padding: 16px;
  color: #334455;
  line-height: 1.8;
  outline: none;
}

.rich-editor-body:empty::before {
  content: attr(data-placeholder);
  color: #a8abb2;
}

.organization-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #df001b inset;
}

.organization-form :deep(.el-button--primary) {
  border-color: #df001b;
  background: #df001b;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head h2 {
  margin: 0;
}

.muted-text,
.work-card p {
  color: #666;
  line-height: 1.75;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.guide-grid div,
.work-card {
  padding: 18px;
  border: 1px solid rgba(223, 0, 27, 0.1);
  border-radius: 8px;
  background: #fffafa;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.card-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-topline h3 {
  margin: 0;
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .workbench-layout {
    grid-template-columns: 1fr;
  }

  .summary-grid,
  .guide-grid,
  .card-grid,
  .admin-info-grid,
  .overview-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .summary-grid,
  .guide-grid,
  .card-grid,
  .admin-hero,
  .admin-info-grid,
  .overview-hero,
  .overview-metrics,
  .organization-form-top {
    grid-template-columns: 1fr;
  }
}
</style>
