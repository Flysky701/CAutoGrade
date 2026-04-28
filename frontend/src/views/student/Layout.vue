<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { notificationApi } from '@/api'
import NotificationBell from '@/components/NotificationBell/index.vue'

const router = useRouter()
const auth = useAuthStore()
const unreadCount = ref(0)
const sidebarCollapsed = ref(false)

const handleLogout = () => auth.logout()

const toggleSidebar = () => { sidebarCollapsed.value = !sidebarCollapsed.value }

onMounted(async () => {
  try {
    unreadCount.value = (await notificationApi.getUnreadCount()).data || 0
  } catch { /* fail silently */ }
})
</script>

<template>
  <el-container class="app-layout">
    <el-header class="layout-header">
      <div class="header-left">
        <el-button class="menu-toggle" :icon="sidebarCollapsed ? 'Expand' : 'Fold'" link @click="toggleSidebar" />
        <h2 class="header-title">C语言自动批阅系统 · 学生</h2>
      </div>
      <div class="header-right">
        <NotificationBell :unread="unreadCount" @click="router.push('/student')" />
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-container class="layout-body">
      <el-aside :class="['layout-sidebar', { collapsed: sidebarCollapsed }]">
        <el-menu :default-active="$route.path" router :collapse="sidebarCollapsed">
          <el-menu-item index="/student/dashboard">
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/student/courses">
            <span>我的课程</span>
          </el-menu-item>
          <el-menu-item index="/student/history">
            <span>提交历史</span>
          </el-menu-item>
          <el-menu-item index="/student/profile">
            <span>个人设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout { min-height: 100vh; }

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-header);
  color: #fff;
  height: var(--header-height);
  padding: 0 var(--space-4);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  box-shadow: var(--shadow-sm);
}
.header-left { display: flex; align-items: center; gap: var(--space-3); }
.header-title { color: #fff; font-size: var(--font-size-md); white-space: nowrap; }
.header-right { display: flex; align-items: center; gap: var(--space-2); }
.header-right .el-button { color: #fff; }
.menu-toggle { display: none; color: #fff; }

.layout-body { min-height: calc(100vh - var(--header-height)); }

.layout-sidebar {
  width: var(--sidebar-width);
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-base);
  transition: width var(--transition-base);
  overflow: hidden;
}
.layout-sidebar.collapsed { width: var(--sidebar-collapsed-width); }
.layout-sidebar :deep(.el-menu) {
  border-right: none;
  background: transparent;
}

.layout-main {
  padding: var(--space-6);
  background: var(--bg-page);
  max-width: var(--content-max-width);
  margin: 0 auto;
  width: 100%;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 1024px) {
  .layout-sidebar { width: 200px; }
  .layout-sidebar.collapsed { width: 60px; }
  .layout-main { padding: var(--space-4); }
}
@media (max-width: 768px) {
  .menu-toggle { display: inline-flex; }
  .layout-sidebar:not(.collapsed) { position: fixed; z-index: var(--z-overlay); height: calc(100vh - var(--header-height)); box-shadow: var(--shadow-lg); }
  .layout-sidebar.collapsed { width: 0; min-width: 0; }
  .header-title { font-size: var(--font-size-sm); }
  .layout-main { padding: var(--space-3); }
}
</style>
