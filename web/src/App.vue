<template>
  <div class="app-layout">
    <!-- Electron 自定义标题栏 -->
    <div class="titlebar" v-if="isElectron">
      <div class="titlebar-drag">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
        </svg>
        <span class="titlebar-title">写作助手</span>
      </div>
      <div class="titlebar-controls">
        <button class="tb-btn tb-btn-minimize" @click="minimize" title="最小化">
          <svg viewBox="0 0 12 12" width="12" height="12"><rect y="5" width="12" height="1.5" fill="currentColor"/></svg>
        </button>
        <button class="tb-btn tb-btn-maximize" @click="toggleMax" title="最大化">
          <svg v-if="!isMaxed" viewBox="0 0 12 12" width="12" height="12">
            <rect x="1" y="1" width="10" height="10" rx="1" fill="none" stroke="currentColor" stroke-width="1.3"/>
          </svg>
          <svg v-else viewBox="0 0 12 12" width="12" height="12">
            <rect x="3" y="0.5" width="8" height="8" rx="1" fill="none" stroke="currentColor" stroke-width="1.3"/>
            <rect x="0.5" y="3" width="8" height="8" rx="1" fill="var(--color-sidebar-bg)" stroke="currentColor" stroke-width="1.3"/>
          </svg>
        </button>
        <button class="tb-btn tb-btn-close" @click="closeWin" title="关闭">
          <svg viewBox="0 0 12 12" width="12" height="12">
            <line x1="1" y1="1" x2="11" y2="11" stroke="currentColor" stroke-width="1.5"/>
            <line x1="11" y1="1" x2="1" y2="11" stroke="currentColor" stroke-width="1.5"/>
          </svg>
        </button>
      </div>
    </div>

    <aside class="sidebar" :class="{ 'has-titlebar': isElectron }">
      <div class="sidebar-header">
        <div class="logo">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
          <span class="logo-text">写作助手</span>
        </div>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" exact-active-class="active">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          <span>首页</span>
        </router-link>
        <router-link to="/novel" class="nav-item" active-class="active">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
          </svg>
          <span>我的作品</span>
        </router-link>
        <router-link to="/models" class="nav-item" active-class="active">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
          </svg>
          <span>模型管理</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <router-link to="/settings" class="nav-item settings-link" active-class="active">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
          </svg>
          <span>系统设置</span>
        </router-link>
        <span class="version">v1.0.0</span>
      </div>
    </aside>
    <main class="main-content" :class="{ 'has-titlebar': isElectron }">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';

export default {
  name: 'App',
  setup() {
    const isElectron = ref(!!window.electronAPI);
    const isMaxed = ref(false);

    const minimize = () => window.electronAPI?.minimize();
    const toggleMax = () => window.electronAPI?.maximize();
    const closeWin = () => window.electronAPI?.close();

    onMounted(async () => {
      if (window.electronAPI) {
        window.electronAPI.onMaximizeChange((v) => { isMaxed.value = v; });
        isMaxed.value = await window.electronAPI.isMaximized();
      }
    });

    return { isElectron, isMaxed, minimize, toggleMax, closeWin };
  },
};
</script>

<style>
/* ===== CSS Variables ===== */
:root {
  --color-primary: #42b983;
  --color-primary-dark: #35a371;
  --color-primary-light: #e8f8f0;
  --color-sidebar-bg: #1e293b;
  --color-sidebar-text: #94a3b8;
  --color-sidebar-text-active: #ffffff;
  --color-sidebar-hover: #334155;
  --color-bg: #f1f5f9;
  --color-surface: #ffffff;
  --color-text: #1e293b;
  --color-text-secondary: #64748b;
  --color-text-muted: #94a3b8;
  --color-border: #e2e8f0;
  --color-danger: #ef4444;
  --color-warning: #f59e0b;
  --color-success: #22c55e;
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.08);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.12);
  --transition: all 0.2s ease;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei',
    Roboto, Oxygen, Ubuntu, sans-serif;
  background: var(--color-bg);
  color: var(--color-text);
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
}

/* ===== Common Button Styles ===== */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0.5rem 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  background: var(--color-surface);
  color: var(--color-text);
  transition: var(--transition);
  text-decoration: none;
  white-space: nowrap;
}
.btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.btn-primary {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}
.btn-primary:hover {
  background: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
  color: white;
}
.btn-sm {
  padding: 0.35rem 0.85rem;
  font-size: 0.85rem;
}
.btn-danger {
  color: var(--color-danger);
  border-color: var(--color-danger);
}
.btn-danger:hover {
  background: var(--color-danger);
  color: white;
}

/* ===== Common Modal Styles ===== */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.15s ease;
}
.modal {
  background: var(--color-surface);
  padding: 2rem;
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 560px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: var(--shadow-lg);
  animation: slideUp 0.2s ease;
}
.modal h3 {
  font-size: 1.2rem;
  margin-bottom: 1.25rem;
  color: var(--color-text);
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
}

/* ===== Common Form Styles ===== */
.input, .textarea {
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  transition: var(--transition);
  background: var(--color-surface);
  color: var(--color-text);
}
.input:focus, .textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}
.textarea { resize: vertical; }
select.input { background: var(--color-surface); }
.form-group { margin-bottom: 0.75rem; }
.form-group label {
  display: block;
  margin-bottom: 0.35rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  font-size: 0.85rem;
}

/* ===== Animations ===== */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* ===== 自定义滚动条 ===== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
  transition: background 0.2s;
}
::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* Firefox 滚动条 */
* {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

/* ===== Loading / Empty States ===== */
.loading, .empty {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

/* ===== Page Header ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.page-header h2 {
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--color-text);
}
</style>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

/* ===== Titlebar (Electron) ===== */
.titlebar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 36px;
  display: flex;
  align-items: center;
  background: var(--color-sidebar-bg);
  z-index: 200;
  user-select: none;
  flex-shrink: 0;
}
.titlebar-drag {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 14px;
  -webkit-app-region: drag;
  color: var(--color-sidebar-text);
  font-size: 0.8rem;
}
.titlebar-title {
  color: var(--color-sidebar-text);
  font-weight: 500;
  letter-spacing: 0.3px;
}
.titlebar-controls {
  display: flex;
  height: 100%;
  -webkit-app-region: no-drag;
}
.tb-btn {
  width: 46px;
  border: none;
  background: transparent;
  color: var(--color-sidebar-text);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.tb-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.tb-btn-close:hover {
  background: #e81123;
  color: #fff;
}

/* ===== Sidebar ===== */
.sidebar {
  width: 220px;
  background: var(--color-sidebar-bg);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}
.sidebar.has-titlebar {
  top: 36px;
}
.sidebar-header {
  padding: 1.5rem 1.25rem;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-primary);
}
.logo-text {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-sidebar-text-active);
  letter-spacing: 0.5px;
}

/* ===== Nav ===== */
.sidebar-nav {
  flex: 1;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-sm);
  color: var(--color-sidebar-text);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: var(--transition);
}
.nav-item:hover {
  background: var(--color-sidebar-hover);
  color: var(--color-sidebar-text-active);
}
.nav-item.active {
  background: rgba(66, 185, 131, 0.15);
  color: var(--color-primary);
}
.nav-item.active svg {
  stroke: var(--color-primary);
}

/* ===== Footer ===== */
.sidebar-footer {
  padding: 0.5rem 0.75rem;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sidebar-footer .nav-item {
  padding: 0.5rem 0.75rem;
  font-size: 0.85rem;
  border-radius: var(--radius-sm);
  color: var(--color-sidebar-text);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: var(--transition);
}
.sidebar-footer .nav-item:hover {
  background: var(--color-sidebar-hover);
  color: var(--color-sidebar-text-active);
}
.sidebar-footer .nav-item.active {
  background: rgba(66, 185, 131, 0.15);
  color: var(--color-primary);
}
.sidebar-footer .nav-item.active svg {
  stroke: var(--color-primary);
}
.version {
  font-size: 0.75rem;
  color: var(--color-sidebar-text);
  opacity: 0.5;
  padding: 0.25rem 0.75rem;
}

/* ===== Main Content ===== */
.main-content {
  flex: 1;
  margin-left: 220px;
  padding: 2rem;
  min-height: 100vh;
}
.main-content.has-titlebar {
  margin-top: 36px;
}
</style>
