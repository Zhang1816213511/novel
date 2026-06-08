<template>
  <div class="novel-list">
    <header class="page-header">
      <h2>我的作品</h2>
      <button @click="tryCreate" class="btn btn-primary">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新建作品
      </button>
    </header>

    <!-- 未配置工作目录提示 -->
    <div v-if="!configStore.configured" class="config-warning">
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span>工作区根目录未配置，请先前往 <router-link to="/settings" class="link">系统设置</router-link> 配置工作目录</span>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="novels.length === 0" class="empty">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M4 19.5A2.5 2.5 0 016.5 17H20"/>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
        </svg>
      </div>
      <p>还没有作品</p>
      <span class="empty-hint">点击右上角创建你的第一个作品</span>
    </div>
    <div v-else class="novel-grid">
      <article v-for="novel in novels" :key="novel.id"
        class="novel-card"
        @click="$router.push(`/novel/${novel.id}`)">
        <div class="card-top">
          <h3>{{ novel.title || '未命名作品' }}</h3>
          <span class="date">{{ formatDate(novel.update_time) }}</span>
        </div>
        <p class="summary">{{ novel.synopsis ? truncate(novel.synopsis, 120) : '暂无内容' }}</p>
        <div class="card-footer">
          <span class="read-more">点击编辑 &rarr;</span>
        </div>
      </article>
    </div>

    <!-- 新建作品 Modal -->
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal">
        <h3>新建作品</h3>
        <div class="form-group">
          <label>作品标题</label>
          <input v-model="newNovel.title" placeholder="输入作品标题" class="input"
            @input="updatePreviewPath" />
        </div>
        <div class="form-group">
          <label>保存位置</label>
          <div class="preview-path">
            <code>{{ previewPath || (configStore.workspaceRoot ? configStore.workspaceRoot + '/{作品标题}/' : '请先配置工作目录') }}</code>
          </div>
          <span class="form-hint">作品数据将自动保存到此目录下的 md 文件中</span>
        </div>
        <div class="modal-actions">
          <button @click="showCreateModal = false" class="btn">取消</button>
          <button @click="createNovel" class="btn btn-primary">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { useConfigStore } from '../stores/configStore'

const configStore = useConfigStore()

const novels = ref([])
const loading = ref(false)
const showCreateModal = ref(false)
const newNovel = ref({ title: '' })

const previewPath = computed(() => {
  if (!newNovel.value.title.trim()) return ''
  const root = configStore.workspaceRoot
  if (!root) return ''
  const dirName = newNovel.value.title
    .replace(/[\\/:*?"<>|]/g, '_')
    .replace(/\s+/g, '_')
    .replace(/^_+|_+$/g, '')
    .trim() || 'untitled'
  return root + '/' + dirName + '/'
})

onMounted(async () => {
  await configStore.checkStatus()
  await loadNovels()
})

function updatePreviewPath() {
  // computed 已自动响应
}

function tryCreate() {
  if (!configStore.configured) {
    alert('请先在系统设置中配置工作区根目录')
    return
  }
  showCreateModal.value = true
}

async function loadNovels() {
  loading.value = true
  try {
    const res = await axios.get('/api/novels')
    novels.value = res.data.data || []
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

async function createNovel() {
  if (!newNovel.value.title.trim()) return alert('请输入标题')
  try {
    await axios.post('/api/novels', {
      title: newNovel.value.title
    })
    showCreateModal.value = false
    newNovel.value = { title: '' }
    loadNovels()
  } catch (e) {
    alert('创建失败：' + (e.response?.data?.message || e.message))
  }
}

function truncate(str, len) {
  return str?.length > len ? str.substring(0, len) + '...' : str
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
</script>

<style scoped>
.novel-list {
  max-width: 1000px;
  margin: 0 auto;
}

/* ===== Grid ===== */
.novel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

/* ===== Card ===== */
.novel-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  cursor: pointer;
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}
.novel-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}
.card-top h3 {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.4;
  flex: 1;
  margin-right: 1rem;
}
.date {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  padding-top: 2px;
}
.summary {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  line-height: 1.7;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}
.word-count {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.read-more {
  font-size: 0.85rem;
  color: var(--color-primary);
  font-weight: 500;
}

/* ===== Config Warning ===== */
.config-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0.75rem 1rem;
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: var(--radius-md);
  color: #f57f17;
  font-size: 0.88rem;
  margin-bottom: 1rem;
}
.config-warning .link {
  color: var(--color-primary);
  font-weight: 500;
  text-decoration: none;
}
.config-warning .link:hover {
  text-decoration: underline;
}

/* ===== Preview Path ===== */
.preview-path {
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.75rem;
}
.preview-path code {
  font-size: 0.82rem;
  color: var(--color-text-secondary);
  word-break: break-all;
}
.form-hint {
  display: block;
  font-size: 0.78rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}

/* ===== Empty State ===== */
.empty {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--color-text-muted);
}
.empty-icon {
  margin-bottom: 1rem;
  opacity: 0.3;
}
.empty p {
  font-size: 1rem;
  margin-bottom: 0.5rem;
  color: var(--color-text-secondary);
}
.empty-hint {
  font-size: 0.85rem;
}
</style>
