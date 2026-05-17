<template>
  <div class="model-list">
    <header class="page-header">
      <h2>模型管理</h2>
      <button @click="openCreate" class="btn btn-primary">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新增模型
      </button>
    </header>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="models.length === 0" class="empty">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="3"/>
          <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
        </svg>
      </div>
      <p>暂无模型配置</p>
      <span class="empty-hint">点击右上角添加你的第一个 AI 模型</span>
    </div>

    <!-- Table -->
    <div v-else class="table-wrapper">
      <table class="model-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>提供商</th>
            <th>模型名</th>
            <th>API 地址</th>
            <th>参数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in models" :key="m.id">
            <td class="cell-name">{{ m.name }}</td>
            <td>
              <span class="provider-badge" :class="m.provider">
                {{ m.provider === 'ollama' ? 'Ollama' : 'OpenAI' }}
              </span>
            </td>
            <td><code>{{ m.modelName }}</code></td>
            <td class="cell-url">{{ m.baseUrl || '-' }}</td>
            <td class="cell-url">{{ m.options || '-' }}</td>
            <td>
              <span :class="['status-dot', m.enabled ? 'enabled' : 'disabled']"></span>
              {{ m.enabled ? '已启用' : '已禁用' }}
            </td>
            <td class="cell-actions">
              <button @click="editModel(m)" class="btn btn-sm">编辑</button>
              <button @click="toggleModel(m)" class="btn btn-sm" :class="m.enabled ? 'btn-warn' : 'btn-ok'">
                {{ m.enabled ? '禁用' : '启用' }}
              </button>
              <button @click="deleteModel(m)" class="btn btn-sm btn-danger">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Form Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal">
        <h3>{{ editingModel ? '编辑模型' : '新增模型' }}</h3>

        <div class="form-divider">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          基本信息
        </div>
        <div class="form-grid">
          <div class="form-group">
            <label>名称</label>
            <input v-model="form.name" class="input" placeholder="例如：我的 GPT-4" />
          </div>
          <div class="form-group">
            <label>提供商</label>
            <select v-model="form.provider" class="input">
              <option value="openai">OpenAI（兼容协议）</option>
              <option value="ollama">Ollama（本地）</option>
            </select>
          </div>
        </div>

        <div class="form-divider">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.21 15.89A10 10 0 118 2.83M22 12A10 10 0 0012 2v10z"/>
          </svg>
          连接配置
        </div>
        <div class="form-grid">
          <div class="form-group">
            <label>模型名</label>
            <input v-model="form.modelName" class="input"
              :placeholder="form.provider === 'ollama' ? 'qwen3-max / llama3.2 / deepseek-r1' : 'gpt-4o / deepseek-chat / claude-sonnet-4'" />
          </div>
          <div class="form-group">
            <label>API 地址</label>
            <input v-model="form.baseUrl" class="input"
              :placeholder="form.provider === 'ollama' ? 'http://localhost:11434' : 'https://api.openai.com（留空使用默认）'" />
          </div>
          <div class="form-group">
            <label>API Key</label>
            <input v-model="form.apiKey" type="password" class="input"
              :placeholder="form.provider === 'ollama' ? 'Ollama 通常不需要' : 'sk-...'" />
          </div>
        </div>

        <div class="form-divider">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="20" x2="12" y2="10"/>
            <line x1="18" y1="20" x2="18" y2="4"/>
            <line x1="6" y1="20" x2="6" y2="16"/>
          </svg>
          生成参数
        </div>
        <div class="form-grid">
          <div class="form-group">
            <label>温度 (temperature)</label>
            <input v-model.number="form.temperature" type="number" step="0.1" min="0" max="2" class="input" placeholder="0.7" />
          </div>
          <div class="form-group" v-if="form.provider === 'ollama'">
            <label>上下文窗口 (numCtx)</label>
            <input v-model.number="form.numCtx" type="number" step="512" min="2048" class="input" placeholder="4096" />
          </div>
          <div class="form-group">
            <label>最大 Token</label>
            <input v-model.number="form.maxTokens" type="number" step="100" min="1" class="input" placeholder="2048" />
          </div>
        </div>

        <div class="modal-actions">
          <button @click="showForm = false" class="btn">取消</button>
          <button @click="saveModel" class="btn btn-primary">{{ editingModel ? '保存' : '创建' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const models = ref([])
const loading = ref(false)
const showForm = ref(false)
const editingModel = ref(null)
const form = ref({
  name: '', provider: 'openai', modelName: '', baseUrl: '', apiKey: '',
  temperature: null, maxTokens: null, numCtx: null
})

onMounted(loadModels)

async function loadModels() {
  loading.value = true
  try {
    const res = await axios.get('/api/models')
    models.value = res.data.data || []
  } catch (e) {
    console.error('load failed', e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingModel.value = null
  form.value = { name: '', provider: 'openai', modelName: '', baseUrl: '', apiKey: '',
    temperature: null, maxTokens: null, numCtx: null }
  showForm.value = true
}

function editModel(m) {
  editingModel.value = m
  const opts = parseOptions(m.options)
  form.value = {
    name: m.name,
    provider: m.provider,
    modelName: m.modelName,
    baseUrl: m.baseUrl || '',
    apiKey: m.apiKey || '',
    temperature: opts.temperature ?? null,
    maxTokens: opts.maxTokens ?? null,
    numCtx: opts.numCtx ?? null
  }
  showForm.value = true
}

function parseOptions(str) {
  if (!str) return {}
  try { return JSON.parse(str) } catch { return {} }
}

async function saveModel() {
  if (!form.value.name || !form.value.modelName) return alert('请填写名称和模型名')

  const opts = {}
  if (form.value.temperature != null) opts.temperature = form.value.temperature
  if (form.value.maxTokens != null) opts.maxTokens = form.value.maxTokens
  if (form.value.numCtx != null) opts.numCtx = form.value.numCtx

  const payload = {
    name: form.value.name,
    provider: form.value.provider,
    modelName: form.value.modelName,
    baseUrl: form.value.baseUrl || null,
    apiKey: form.value.apiKey || null,
    options: Object.keys(opts).length > 0 ? JSON.stringify(opts) : null
  }

  try {
    if (editingModel.value) {
      await axios.put(`/api/models/${editingModel.value.id}`, payload)
    } else {
      await axios.post('/api/models', payload)
    }
    showForm.value = false
    loadModels()
  } catch (e) {
    console.error('save failed', e)
  }
}

async function toggleModel(m) {
  try {
    await axios.put(`/api/models/${m.id}/toggle`)
    m.enabled = !m.enabled
  } catch (e) {
    console.error('toggle failed', e)
  }
}

async function deleteModel(m) {
  if (!confirm(`确认删除模型"${m.name}"？`)) return
  try {
    await axios.delete(`/api/models/${m.id}`)
    loadModels()
  } catch (e) {
    console.error('delete failed', e)
  }
}
</script>

<style scoped>
.model-list {
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== Table Wrapper ===== */
.table-wrapper {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.model-table {
  width: 100%;
  border-collapse: collapse;
}
.model-table th {
  text-align: left;
  padding: 0.85rem 1rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}
.model-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.9rem;
  color: var(--color-text);
}
.model-table tbody tr:last-child td {
  border-bottom: none;
}
.model-table tbody tr:hover td {
  background: #f8fafc;
}

/* ===== Cells ===== */
.cell-name {
  font-weight: 600;
}
.cell-url {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-secondary) !important;
  font-size: 0.85rem !important;
}
.cell-actions {
  white-space: nowrap;
}
.cell-actions .btn {
  margin-right: 4px;
}
.cell-actions .btn:last-child {
  margin-right: 0;
}

/* ===== Provider Badge ===== */
.provider-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 100px;
  font-size: 0.8rem;
  font-weight: 500;
}
.provider-badge.openai {
  background: #e8f4fd;
  color: #0369a1;
}
.provider-badge.ollama {
  background: #f3e8ff;
  color: #7c3aed;
}

/* ===== Status Dot ===== */
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.status-dot.enabled {
  background: var(--color-success);
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.4);
}
.status-dot.disabled {
  background: var(--color-border);
}

/* ===== Code ===== */
code {
  background: var(--color-bg);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

/* ===== Form Modal Tweaks ===== */
.form-divider {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-primary);
  margin: 1.25rem 0 0.75rem;
  padding-bottom: 0.4rem;
  border-bottom: 1px solid var(--color-border);
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem 1rem;
}

/* ===== Action Button Variants ===== */
.btn-ok {
  color: var(--color-success);
  border-color: var(--color-success);
}
.btn-ok:hover {
  background: var(--color-success);
  color: white;
}
.btn-warn {
  color: var(--color-warning);
  border-color: var(--color-warning);
}
.btn-warn:hover {
  background: var(--color-warning);
  color: white;
}

/* ===== Empty ===== */
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
