import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import VolunteerHome from '../views/VolunteerHome.vue'
import OrganizationHome from '../views/OrganizationHome.vue'
import { getCachedUser, TOKEN_KEY } from '../api/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/volunteer',
    name: 'VolunteerHome',
    component: VolunteerHome,
    meta: { requiresAuth: true }
  },
  {
    path: '/organization',
    name: 'OrganizationHome',
    component: OrganizationHome,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem(TOKEN_KEY)
  const user = getCachedUser()

  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && token) {
    if (user && user.role === 'organization_admin') {
      next('/organization')
    } else {
      next('/volunteer')
    }
    return
  }
  next()
})

export default router
