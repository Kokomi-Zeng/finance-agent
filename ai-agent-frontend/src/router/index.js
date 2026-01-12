import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - AI理财管理助手',
      description: 'AI理财管理助手提供专业的投资建议、财务规划和理财咨询服务'
    }
  },
  {
    path: '/agent',
    name: 'FinancialAgent',
    component: () => import('../views/FinancialAgent.vue'),
    meta: {
      title: 'AI理财智能体 - AI理财管理助手',
      description: 'AI理财智能体整合专业知识库，自主规划任务，搜索市场信息，生成专业理财报告'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局导航守卫，设置文档标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router
