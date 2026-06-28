<template>
  <div class="character-manager">
    <div class="cm-header">
      <h3>角色管理</h3>
      <button class="btn btn-sm btn-primary" @click="openCreate">+ 添加角色</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="characters.length === 0" class="cm-empty">
      <p>暂无角色，点击右上角添加</p>
    </div>

    <div v-else class="cm-grid">
      <div v-for="ch in characters" :key="ch.id" class="cm-card">
        <div class="cm-card-avatar" @click="editCharacter(ch)">
          <img v-if="ch.imagePath" :src="ch.imagePath" :alt="ch.name" />
          <div v-else class="cm-avatar-placeholder">{{ ch.name.charAt(0) }}</div>
        </div>
        <div class="cm-card-body">
          <div class="cm-card-name" @click="editCharacter(ch)">{{ ch.name }}</div>
          <div v-if="ch.alias" class="cm-card-alias">{{ ch.alias }}</div>
          <div v-if="ch.description" class="cm-card-desc">{{ ch.description }}</div>
        </div>
        <div class="cm-card-actions">
          <button class="btn btn-sm" @click="editCharacter(ch)" title="编辑">编辑</button>
          <button class="btn btn-sm btn-danger" @click="deleteCharacter(ch)" title="删除">删除</button>
        </div>
      </div>
    </div>

    <!-- Form Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal modal-wide">
        <h3>{{ editingCharacter ? '编辑角色' : '新增角色' }}</h3>

        <div class="cf-layout">
          <!-- Left: Avatar -->
          <div class="cf-avatar-section">
            <div class="cf-avatar" @click="triggerUpload">
              <img v-if="form.imagePath || previewUrl" :src="previewUrl || form.imagePath" />
              <div v-else class="cf-avatar-empty">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                <span>上传图片</span>
              </div>
            </div>
            <input ref="fileInput" type="file" accept="image/*" hidden @change="onFileChange" />
            <button v-if="form.imagePath || previewUrl" class="btn btn-sm" @click="removeImage">移除图片</button>
          </div>

          <!-- Right: Fields -->
          <div class="cf-fields">
            <div class="form-group">
              <label>角色名称 *</label>
              <input v-model="form.name" class="input" placeholder="如：林轩" />
            </div>
            <div class="form-group">
              <label>别名</label>
              <input v-model="form.alias" class="input" placeholder="如：轩儿、林少" />
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea v-model="form.description" class="input cf-textarea" placeholder="简短的角色描述" rows="2"></textarea>
            </div>
            <div class="form-group">
              <label>性格特征</label>
              <textarea v-model="form.personality" class="input cf-textarea" placeholder="如：冷静睿智、外冷内热" rows="2"></textarea>
            </div>
            <div class="form-group">
              <label>外貌描述</label>
              <textarea v-model="form.appearance" class="input cf-textarea" placeholder="如：身高185cm，黑色短发" rows="2"></textarea>
            </div>
            <div class="form-group">
              <label>背景故事</label>
              <textarea v-model="form.background" class="input cf-textarea" placeholder="角色的背景经历" rows="3"></textarea>
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button @click="showForm = false" class="btn">取消</button>
          <button @click="saveCharacter" class="btn btn-primary" :disabled="!form.name.trim()">
            {{ editingCharacter ? '保存' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  novelId: { type: Number, required: true }
})

const emit = defineEmits(['characters-updated'])

const characters = ref([])
const loading = ref(false)
const showForm = ref(false)
const editingCharacter = ref(null)
const fileInput = ref(null)
const previewUrl = ref('')

const form = ref({
  name: '', alias: '', description: '', personality: '',
  appearance: '', background: '', imagePath: '', sortOrder: 0
})

onMounted(loadCharacters)

watch(() => props.novelId, loadCharacters)

async function loadCharacters() {
  if (!props.novelId) return
  loading.value = true
  try {
    const res = await axios.get(`/api/characters/novel/${props.novelId}`)
    characters.value = res.data.data || []
    emit('characters-updated', characters.value)
  } catch (e) {
    console.error('加载角色失败', e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingCharacter.value = null
  form.value = { name: '', alias: '', description: '', personality: '',
    appearance: '', background: '', imagePath: '', sortOrder: 0 }
  previewUrl.value = ''
  showForm.value = true
}

function editCharacter(ch) {
  editingCharacter.value = ch
  form.value = {
    name: ch.name,
    alias: ch.alias || '',
    description: ch.description || '',
    personality: ch.personality || '',
    appearance: ch.appearance || '',
    background: ch.background || '',
    imagePath: ch.imagePath || '',
    sortOrder: ch.sortOrder || 0
  }
  previewUrl.value = ''
  showForm.value = true
}

// Upload image as base64 (simple approach, no multipart)
function triggerUpload() {
  fileInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    previewUrl.value = ev.target.result
    form.value.imagePath = ev.target.result
  }
  reader.readAsDataURL(file)
}

function removeImage() {
  form.value.imagePath = ''
  previewUrl.value = ''
}

async function saveCharacter() {
  if (!form.value.name.trim()) return

  const payload = {
    novelId: props.novelId,
    name: form.value.name.trim(),
    alias: form.value.alias || null,
    description: form.value.description || null,
    personality: form.value.personality || null,
    appearance: form.value.appearance || null,
    background: form.value.background || null,
    imagePath: form.value.imagePath || null,
    sortOrder: form.value.sortOrder || 0
  }

  try {
    if (editingCharacter.value) {
      await axios.put(`/api/characters/${editingCharacter.value.id}`, payload)
    } else {
      await axios.post('/api/characters', payload)
    }
    showForm.value = false
    await loadCharacters()
  } catch (e) {
    console.error('保存角色失败', e)
    alert('保存失败：' + (e.response?.data?.message || e.message))
  }
}

async function deleteCharacter(ch) {
  if (!confirm(`确认删除角色"${ch.name}"？`)) return
  try {
    await axios.delete(`/api/characters/${ch.id}`)
    await loadCharacters()
  } catch (e) {
    console.error('删除角色失败', e)
  }
}
</script>

<style scoped>
.character-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.cm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  flex-shrink: 0;
}
.cm-header h3 {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

.cm-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

/* ===== Grid ===== */
.cm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.75rem;
  overflow-y: auto;
  flex: 1;
  align-content: start;
  padding-bottom: 1rem;
}

.cm-card {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.15s;
}
.cm-card:hover {
  box-shadow: var(--shadow-md);
}

/* Avatar */
.cm-card-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
}
.cm-card-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cm-avatar-placeholder {
  width: 100%;
  height: 100%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  font-weight: 700;
}

.cm-card-body {
  flex: 1;
  min-width: 0;
}
.cm-card-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text);
  cursor: pointer;
  margin-bottom: 2px;
}
.cm-card-name:hover {
  color: var(--color-primary);
}
.cm-card-alias {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  margin-bottom: 4px;
}
.cm-card-desc {
  font-size: 0.82rem;
  color: var(--color-text-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cm-card-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  justify-content: center;
}

/* ===== Modal ===== */
.modal-wide {
  max-width: 640px;
}

.cf-layout {
  display: flex;
  gap: 1.25rem;
  margin: 1rem 0;
}

.cf-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.cf-avatar {
  width: 120px;
  height: 150px;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  border: 2px dashed var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s;
}
.cf-avatar:hover {
  border-color: var(--color-primary);
}
.cf-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cf-avatar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.cf-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.cf-textarea {
  resize: vertical;
  font-family: inherit;
  font-size: 0.85rem;
}

.form-group label {
  display: block;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 3px;
}
</style>
