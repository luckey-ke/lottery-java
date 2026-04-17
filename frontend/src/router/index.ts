import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 公开页面 — 所有人可访问
const publicRoutes: RouteRecordRaw[] = [
  { path: '/', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
  { path: '/analysis', name: 'Analysis', component: () => import('../views/Analysis.vue') },
  { path: '/trend', name: 'Trend', component: () => import('../views/Trend.vue') },
  { path: '/recommend', name: 'Recommend', component: () => import('../views/Recommend.vue') },
]

// 后台管理 — 独立布局，仅管理员
const adminRoutes: RouteRecordRaw = {
  path: '/admin',
  component: () => import('../views/AdminLayout.vue'),
  meta: { requiresAdmin: true },
  children: [
    { path: '', name: 'AdminHome', component: () => import('../views/Admin.vue') },
    { path: 'history', name: 'AdminHistory', component: () => import('../views/FetchHistory.vue') },
    { path: 'users', name: 'AdminUsers', component: () => import('../views/Users.vue') },
    { path: 'roles', name: 'AdminRoles', component: () => import('../views/Roles.vue') },
    { path: 'menus', name: 'AdminMenus', component: () => import('../views/Menus.vue') },
  ],
}

// 认证页面
const authRoutes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guestOnly: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { guestOnly: true } },
]

const routes: RouteRecordRaw[] = [...publicRoutes, adminRoutes, ...authRoutes]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('lottery_token')
  const user = JSON.parse(localStorage.getItem('lottery_user') || 'null')
  const isLoggedIn = !!token
  const isAdmin = (user?.roles ?? []).some((r: string) => r.toLowerCase() === 'admin')

  // 需要管理员权限的页面
  if (to.meta.requiresAdmin || to.matched.some(r => r.meta.requiresAdmin)) {
    if (!isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
    if (!isAdmin) {
      return next('/')
    }
  }

  // 仅游客页面（已登录跳转首页）
  if (to.meta.guestOnly && isLoggedIn) {
    return next('/')
  }

  next()
})

export default router
