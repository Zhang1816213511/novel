<template>
  <div class="home">
    <!-- 未配置工作目录提示 -->
    <div v-if="showConfigWarning" class="config-banner">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span>请先设置工作区根目录，然后开始创作</span>
      <router-link to="/settings" class="btn btn-sm config-btn">前往设置</router-link>
    </div>

    <section class="hero">
      <div class="hero-content">
        <div class="hero-icon">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
        </div>
        <h1>本地写作智能助手</h1>
        <p class="hero-subtitle">简洁的界面，专注写作本身。所有数据本地存储，安全可控。</p>
        <div class="hero-actions">
          <router-link to="/novel" class="btn btn-primary">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            新建作品
          </router-link>
          <router-link to="/models" class="btn">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
            </svg>
            配置模型
          </router-link>
        </div>
      </div>
    </section>

    <section class="features">
      <div class="feature-card">
        <div class="feature-icon" style="background: #e8f8f0; color: #42b983;">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
        </div>
        <h3>本地存储</h3>
        <p>所有数据保存在本地 SQLite 数据库，无需担心隐私泄露，数据安全可控</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fff7ed; color: #f97316;">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
        </div>
        <h3>离线可用</h3>
        <p>无需联网即可随时编辑和查看内容，专注创作不受网络限制</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #f0f0ff; color: #6366f1;">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </div>
        <h3>快速检索</h3>
        <p>通过缓存加速内容检索，即使作品再多也能瞬间找到</p>
      </div>
      <div class="feature-card">
        <div class="feature-icon" style="background: #fef2f2; color: #ef4444;">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="16 3 21 3 21 8"/>
            <line x1="4" y1="20" x2="21" y2="3"/>
            <polyline points="21 16 21 21 16 21"/>
            <line x1="15" y1="15" x2="21" y2="21"/>
            <line x1="4" y1="4" x2="9" y2="9"/>
          </svg>
        </div>
        <h3>AI 辅助</h3>
        <p>集成 Ollama 和 OpenAI 兼容 API，智能辅助写作，激发创作灵感</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useConfigStore } from '../stores/configStore'

const configStore = useConfigStore()
const showConfigWarning = ref(false)

onMounted(async () => {
  await configStore.checkStatus()
  showConfigWarning.value = !configStore.configured
})
</script>

<style scoped>
.home {
  max-width: 900px;
  margin: 0 auto;
}

/* ===== Config Banner ===== */
.config-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.75rem 1.25rem;
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: var(--radius-md);
  color: #f57f17;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}

.config-btn {
  margin-left: auto;
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
  text-decoration: none;
}

/* ===== Hero Section ===== */
.hero {
  text-align: center;
  padding: 3rem 2rem;
  margin-bottom: 3rem;
}
.hero-content {
  max-width: 600px;
  margin: 0 auto;
}
.hero-icon {
  color: var(--color-primary);
  margin-bottom: 1.5rem;
  opacity: 0.8;
}
.hero h1 {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 0.75rem;
}
.hero-subtitle {
  color: var(--color-text-secondary);
  font-size: 1.05rem;
  line-height: 1.7;
  margin-bottom: 2rem;
}
.hero-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

/* ===== Features ===== */
.features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}
.feature-card {
  background: var(--color-surface);
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
}
.feature-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}
.feature-card h3 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: var(--color-text);
}
.feature-card p {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
</style>
