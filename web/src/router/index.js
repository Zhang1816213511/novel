import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/novel',
    name: 'NovelList',
    component: () => import('../views/NovelList.vue')
  },
  {
    path: '/novel/:id',
    name: 'NovelDetail',
    component: () => import('../views/NovelDetail.vue')
  },
  {
    path: '/models',
    name: 'ModelList',
    component: () => import('../views/ModelList.vue')
  },
  {
    path: '/settings',
    name: 'SystemConfig',
    component: () => import('../views/SystemConfig.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router