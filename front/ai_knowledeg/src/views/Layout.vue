<template>
  <div class="layout-container">
    <el-aside width="240px" class="sidebar">
      <div class="sidebar-header">
        <h2 class="logo">AI知识图谱</h2>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="square">
          <el-icon><Grid /></el-icon>
          <span>知识图谱广场</span>
        </el-menu-item>

        <el-menu-item index="create">
          <el-icon><Plus /></el-icon>
          <span>创建知识图谱</span>
        </el-menu-item>

        <el-menu-item index="ai-chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI对话框</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar :size="40" class="user-avatar">
            {{ userStore.username.charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="user-details">
            <div class="user-name">{{ userStore.username }}</div>
            <div class="user-role">用户</div>
          </div>
        </div>
        <el-button type="danger" text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </el-aside>

    <el-main class="main-content">
      <router-view />
    </el-main>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Grid, Plus, ChatDotRound, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = ref('square')

watch(() => route.path, (newPath) => {
  const pathMap = {
    '/layout/square': 'square',
    '/layout/create': 'create',
    '/layout/ai-chat': 'ai-chat'
  }
  activeMenu.value = pathMap[newPath] || 'square'
}, { immediate: true })

const handleMenuSelect = (index) => {
  const routeMap = {
    square: '/layout/square',
    create: '/layout/create',
    'ai-chat': '/layout/ai-chat'
  }
  router.push(routeMap[index])
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  border-right: none;
}

.sidebar-header {
  padding: 30px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  color: white;
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  text-align: center;
}

.sidebar-menu {
  flex: 1;
  border: none;
  background: transparent;
  padding: 20px 0;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.8);
  margin: 5px 15px;
  border-radius: 8px;
  transition: all 0.3s;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: 600;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 1.2rem;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.user-avatar {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: 600;
}

.user-details {
  flex: 1;
}

.user-name {
  color: white;
  font-weight: 600;
  font-size: 0.95rem;
  margin-bottom: 2px;
}

.user-role {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.8rem;
}

:deep(.el-button--danger.is-text) {
  color: rgba(255, 255, 255, 0.8);
  width: 100%;
  justify-content: center;
  padding: 10px;
}

:deep(.el-button--danger.is-text:hover) {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.main-content {
  padding: 30px;
  overflow-y: auto;
}
</style>