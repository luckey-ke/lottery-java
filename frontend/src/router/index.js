import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
  { path: '/admin', name: 'Admin', component: () => import('../views/Admin.vue') },
  { path: '/history', name: 'FetchHistory', component: () => import('../views/FetchHistory.vue') },
  { path: '/analysis', name: 'Analysis', component: () => import('../views/Analysis.vue') },
  { path: '/trend', name: 'Trend', component: () => import('../views/Trend.vue') },
]

export default createRouter({
  history: createWebHistory(),
  routes
})
