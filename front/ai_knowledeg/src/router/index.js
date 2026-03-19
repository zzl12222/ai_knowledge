import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/layout',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'square',
        name: 'Square',
        component: () => import('../views/Square.vue')
      },
      {
        path: 'create',
        name: 'Create',
        component: () => import('../views/Create.vue')
      },
      {
        path: 'ai-chat',
        name: 'AIChat',
        component: () => import('../views/AIChat.vue')
      },
      {
        path: 'graph/:id',
        name: 'GraphDetail',
        component: () => import('../views/GraphDetail.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router