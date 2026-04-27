import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import PublicActivities from '../views/PublicActivities.vue'
import ActivityDetail from '../views/ActivityDetail.vue'
import PublicOrganizations from '../views/PublicOrganizations.vue'
import Publicity from '../views/Publicity.vue'
import StaticPortalPage from '../views/StaticPortalPage.vue'
import VolunteerHome from '../views/VolunteerHome.vue'
import OrganizationHome from '../views/OrganizationHome.vue'
import ReviewerHome from '../views/ReviewerHome.vue'
import AdminHome from '../views/AdminHome.vue'
import { getCachedUser, getToken, getWorkspaceRoute, hasPermission, hasRole } from '../api/auth'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/activities', name: 'PublicActivities', component: PublicActivities },
  { path: '/activities/:id', name: 'ActivityDetail', component: ActivityDetail },
  { path: '/organizations', name: 'PublicOrganizations', component: PublicOrganizations },
  { path: '/publicity', name: 'Publicity', component: Publicity },
  { path: '/stations', name: 'Stations', component: StaticPortalPage, meta: { pageKey: 'stations' } },
  { path: '/notices', name: 'Notices', component: StaticPortalPage, meta: { pageKey: 'notices' } },
  { path: '/rules', name: 'Rules', component: StaticPortalPage, meta: { pageKey: 'rules' } },
  { path: '/help', name: 'Help', component: StaticPortalPage, meta: { pageKey: 'help' } },
  { path: '/profile', name: 'Profile', component: VolunteerHome, meta: { requiresAuth: true, role: 'volunteer' } },
  { path: '/volunteer', redirect: '/profile' },
  {
    path: '/organization-workbench',
    name: 'OrganizationWorkbench',
    component: OrganizationHome,
    meta: { requiresAuth: true, role: 'organization_admin' }
  },
  { path: '/organization', redirect: '/organization-workbench' },
  {
    path: '/reviewer',
    name: 'ReviewerHome',
    component: ReviewerHome,
    meta: { requiresAuth: true, roles: ['activity_reviewer', 'product_reviewer', 'system_admin'] }
  },
  { path: '/admin', name: 'AdminHome', component: AdminHome, meta: { requiresAuth: true, role: 'system_admin' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

function normalizeMetaCodes(value) {
  if (!value) {
    return []
  }
  if (Array.isArray(value)) {
    return value.filter(Boolean)
  }
  return [value]
}

router.beforeEach((to, from, next) => {
  const token = getToken()
  const user = getCachedUser()
  const requiredRoles = normalizeMetaCodes(to.meta.role || to.meta.roles)
  const requiredPermissions = normalizeMetaCodes(to.meta.permissions)

  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  if (to.meta.requiresAuth && token && user) {
    const roleAllowed =
      requiredRoles.length === 0 || requiredRoles.some((roleCode) => hasRole(roleCode, user))
    const permissionAllowed =
      requiredPermissions.length === 0 ||
      requiredPermissions.every((permissionCode) => hasPermission(permissionCode, user))

    if (!roleAllowed || !permissionAllowed) {
      next(getWorkspaceRoute(user))
      return
    }
  }

  if ((to.path === '/login' || to.path === '/register') && token && user) {
    next(getWorkspaceRoute(user))
    return
  }

  next()
})

export default router
