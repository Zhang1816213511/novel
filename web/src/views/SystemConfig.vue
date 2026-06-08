<template>
  <div class="settings-page">
    <header class="page-header">
      <h2>系统设置</h2>
    </header>

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else>
      <!-- 工作区根目录 -->
      <section class="setting-card">
        <div class="setting-card-header">
          <h3>工作区根目录</h3>
          <p class="setting-desc">所有作品将自动在此目录下以作品名称创建子文件夹</p>
        </div>
        <div class="setting-body">
          <div class="dir-input-row">
            <input
              v-model="dirPath"
              :placeholder="configStore.configured ? currentRoot : '请选择或输入工作区根目录路径'"
              class="input"
              readonly
            />
            <button @click="selectDirectory" class="btn btn-sm" title="选择文件夹">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/>
              </svg>
              选择
            </button>
          </div>

          <div v-if="configStore.configured && currentRoot" class="current-config">
            <span class="label">当前路径：</span>
            <code>{{ currentRoot }}</code>
          </div>

          <div v-if="dirPath" class="save-area">
            <button
              class="btn btn-primary"
              :disabled="saving || dirPath === currentRoot"
              @click="saveConfig"
            >
              {{ saving ? '保存中...' : '保存配置' }}
            </button>
            <span v-if="saved" class="saved-hint">✓ 已保存</span>
          </div>
        </div>
      </section>

    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useConfigStore } from '../stores/configStore'

const configStore = useConfigStore()

const loading = ref(true)
const currentRoot = ref('')
const dirPath = ref('')
const saving = ref(false)
const saved = ref(false)

onMounted(async () => {
  await configStore.checkStatus()
  currentRoot.value = configStore.workspaceRoot || ''
  loading.value = false
})

async function selectDirectory() {
  let dir = null
  if (window.electronAPI?.selectDirectory) {
    dir = await window.electronAPI.selectDirectory({ title: '选择工作区根目录' })
  } else {
    dir = prompt('请输入工作区根目录的绝对路径：', dirPath.value || '')
  }
  if (dir) dirPath.value = dir
}

async function saveConfig() {
  if (!dirPath.value) return
  saving.value = true
  saved.value = false
  try {
    await configStore.setWorkspaceRoot(dirPath.value)
    currentRoot.value = configStore.workspaceRoot
    saved.value = true
    setTimeout(() => { saved.value = false }, 3000)
  } catch (e) {
    alert('保存失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

</script>

<style scoped>
.settings-page {
  max-width: 800px;
  margin: 0 auto;
}

.setting-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 1.25rem;
  overflow: hidden;
}

.setting-card-header {
  padding: 1.25rem 1.5rem 0.5rem;
}

.setting-card-header h3 {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.25rem;
}

.setting-desc {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.setting-body {
  padding: 0.75rem 1.5rem 1.25rem;
}

.dir-input-row {
  display: flex;
  gap: 6px;
}

.dir-input-row .input {
  flex: 1;
}

.current-config {
  margin-top: 0.75rem;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

.current-config .label {
  font-weight: 500;
}

.current-config code {
  background: var(--color-bg);
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  font-size: 0.82rem;
  word-break: break-all;
}

.save-area {
  margin-top: 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.saved-hint {
  color: var(--color-success);
  font-size: 0.85rem;
  font-weight: 500;
}

</style>
