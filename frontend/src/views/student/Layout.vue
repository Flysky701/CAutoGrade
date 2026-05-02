<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { notificationApi } from '@/api'
import {
  HomeFilled, Reading, Document, User, Expand, Fold
} from '@element-plus/icons-vue'
import NotificationBell from '@/components/NotificationBell/index.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const unreadCount = ref(0)
const sidebarCollapsed = ref(false)

const handleLogout = () => auth.logout()
const toggleSidebar = () => { sidebarCollapsed.value = !sidebarCollapsed.value }

const breadcrumbMap: Record<string, string> = {
  '/student/dashboard': '首页',
  '/student/courses': '我的课程',
  '/student/history': '提交历史',
  '/student/profile': '个人设置',
}

const breadcrumbs = computed(() => {
  const items: { label: string; to?: string }[] = [{ label: '学生', to: '/student/dashboard' }]
  const label = breadcrumbMap[route.path]
  if (label && route.path !== '/student/dashboard') {
    items.push({ label })
  }
  return items
})

onMounted(async () => {
  try {
    unreadCount.value = (await notificationApi.getUnreadCount()).data || 0
  } catch { /* */ }
})
</script>

<template>
  <el-container class="app-layout">
    <el-header class="layout-header">
      <div class="header-left">
        <el-button class="menu-toggle" :icon="sidebarCollapsed ? Expand : Fold" link @click="toggleSidebar" />
        <h2 class="header-title">C 语言自动批阅系统</h2>
        <span class="header-role">学生</span>
      </div>
      <div class="header-right">
        <span class="welcome-tag">{{ auth.username }}</span>
        <NotificationBell :unread="unreadCount" @click="router.push('/student')" />
        <el-button text @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-container class="layout-body">
      <el-aside :class="['layout-sidebar', { collapsed: sidebarCollapsed }]">
        <el-menu :default-active="route.path" router :collapse="sidebarCollapsed">
          <el-menu-item-group title="主页">
            <el-menu-item index="/student/dashboard">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="学习">
            <el-menu-item index="/student/courses">
              <el-icon><Reading /></el-icon>
              <span>我的课程</span>
            </el-menu-item>
            <el-menu-item index="/student/history">
              <el-icon><Document /></el-icon>
              <span>提交历史</span>
            </el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="账户">
            <el-menu-item index="/student/profile">
              <el-icon><User /></el-icon>
              <span>个人设置</span>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <el-breadcrumb v-if="breadcrumbs.length > 1" separator="/" class="layout-breadcrumb">
          <el-breadcrumb-item v-for="(b, i) in breadcrumbs" :key="i" :to="b.to">{{ b.label }}</el-breadcrumb-item>
        </el-breadcrumb>
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
  display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(135deg, var(--primary-dark), var(--primary));
  color: #fff; height: var(--header-height); padding: 0 var(--space-4);
  position: sticky; top: 0; z-index: var(--z-sticky); box-shadow: var(--shadow-md);
}
.header-left { display: flex; align-items: center; gap: var(--space-3); }
.header-title { color: #fff; font-size: var(--font-size-md); white-space: nowrap; }
.header-role {
  font-size: var(--font-size-xs); background: rgba(255,255,255,0.2);
  padding: 2px 10px; border-radius: var(--radius-round);
}
.header-right { display: flex; align-items: center; gap: var(--space-3); }
.header-right .el-button { color: #fff; }
.welcome-tag { font-size: var(--font-size-xs); color: rgba(255,255,255,0.75); }
.menu-toggle { display: none; color: #fff; }
.layout-body { min-height: calc(100vh - var(--header-height)); }
.layout-sidebar {
  width: var(--sidebar-width); background: var(--bg-sidebar);
  border-right: 1px solid var(--border-base);
  transition: width var(--transition-base); overflow: hidden;
}
.layout-sidebar.collapsed { width: var(--sidebar-collapsed-width); }
.layout-sidebar :deep(.el-menu) { border-right: none; background: transparent; }
.layout-sidebar :deep(.el-menu-item-group__title) {
  padding: var(--space-4) var(--space-4) var(--space-2);
  font-size: var(--font-size-xs); color: var(--text-placeholder);
  text-transform: uppercase; letter-spacing: 0.5px;
}
.layout-main {
  padding: var(--space-6); background: var(--bg-page);
  max-width: var(--content-max-width); margin: 0 auto; width: 100%;
}
.layout-breadcrumb { margin-bottom: var(--space-4); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
@media (max-width: 1024px) {
  .layout-sidebar { width: 200px; } .layout-sidebar.collapsed { width: 60px; }
  .layout-main { padding: var(--space-4); }
}
@media (max-width: 768px) {
  .menu-toggle { display: inline-flex; }
  .welcome-tag { display: none; }
  .layout-sidebar:not(.collapsed) { position: fixed; z-index: var(--z-overlay); height: calc(100vh - var(--header-height)); box-shadow: var(--shadow-lg); }
  .layout-sidebar.collapsed { width: 0; min-width: 0; }
  .header-title { font-size: var(--font-size-sm); }
  .layout-main { padding: var(--space-3); }
}
</style>
