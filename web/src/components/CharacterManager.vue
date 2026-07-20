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

    <div v-else class="cm-list">
      <template v-for="group in groupedCharacters" :key="group.faction">
        <div class="cm-faction-header">
          <span class="cm-faction-name">{{ group.faction || '未分类' }}</span>
          <span class="cm-faction-count">{{ group.chars.length }}人</span>
        </div>
        <div class="cm-grid">
          <div v-for="ch in group.chars" :key="ch.id" class="cm-card">
            <div class="cm-card-avatar" @click="editCharacter(ch)">
              <img v-if="ch.imagePath" :src="ch.imagePath" :alt="ch.name" />
              <div v-else class="cm-avatar-placeholder">{{ ch.name.charAt(0) }}</div>
            </div>
            <div class="cm-card-body">
              <div class="cm-card-name-row">
                <span class="cm-card-name" @click="editCharacter(ch)">{{ ch.name }}</span>
                <span v-if="ch.roleType" class="cm-role-tag" :class="roleTagClass(ch.roleType)">{{ ch.roleType }}</span>
              </div>
              <div v-if="ch.alias" class="cm-card-alias">{{ ch.alias }}</div>
              <div v-if="ch.description" class="cm-card-desc">{{ ch.description }}</div>
            </div>
            <div class="cm-card-actions">
              <button class="btn btn-sm" @click="editCharacter(ch)" title="编辑">编辑</button>
              <button class="btn btn-sm btn-danger" @click="deleteCharacter(ch)" title="删除">删除</button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 表单弹窗 -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal modal-wide">
        <h3>{{ editingCharacter ? '编辑角色' : '新增角色' }}</h3>

        <div class="cf-layout">
          <!-- 左侧：头像 -->
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

          <!-- 右侧：字段 -->
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
            <div class="form-group">
              <label>势力</label>
              <select v-model="form.factionId" class="input">
                <option :value="null">未分类</option>
                <option v-for="f in factions" :key="f.id" :value="f.id">{{ f.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>角色类型</label>
              <div class="cf-role-select">
                <label class="cf-role-opt" :class="{ active: form.roleType === '主角' }">
                  <input type="radio" v-model="form.roleType" value="主角" /> 主角
                </label>
                <label class="cf-role-opt" :class="{ active: form.roleType === '配角' }">
                  <input type="radio" v-model="form.roleType" value="配角" /> 配角
                </label>
                <label class="cf-role-opt" :class="{ active: form.roleType === 'NPC' }">
                  <input type="radio" v-model="form.roleType" value="NPC" /> NPC
                </label>
              </div>
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
import { ref, onMounted, watch, computed } from 'vue'
import axios from 'axios'

const props = defineProps({
  novelId: { type: Number, required: true },
  factions: { type: Array, default: () => [] }
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
  appearance: '', background: '', factionId: null, roleType: '', imagePath: '', sortOrder: 0
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

const groupedCharacters = computed(() => {
  const map = new Map()
  for (const ch of characters.value) {
    const key = ch.factionName || ''
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(ch)
  }
  const groups = []
  for (const [faction, chars] of map) {
    groups.push({ faction, chars })
  }
  groups.sort((a, b) => {
    if (!a.faction) return 1
    if (!b.faction) return -1
    return a.faction.localeCompare(b.faction)
  })
  return groups
})

function roleTagClass(roleType) {
  switch (roleType) {
    case '主角': return 'cm-role-main'
    case '配角': return 'cm-role-support'
    case 'NPC': return 'cm-role-npc'
    default: return ''
  }
}

function openCreate() {
  editingCharacter.value = null
  form.value = { name: '', alias: '', description: '', personality: '',
    appearance: '', background: '', factionId: null, roleType: '', imagePath: '', sortOrder: 0 }
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
    factionId: ch.factionId || null,
    roleType: ch.roleType || '',
    imagePath: ch.imagePath || '',
    sortOrder: ch.sortOrder || 0
  }
  previewUrl.value = ''
  showForm.value = true
}

// 上传图片为 base64（简单方式，不使用 multipart）
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
    factionId: form.value.factionId || null,
    roleType: form.value.roleType || null,
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

/* ===== 卡片网格 ===== */
.cm-list {
  overflow-y: auto;
  flex: 1;
  padding-bottom: 1rem;
}

.cm-faction-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 1rem 0 0.5rem;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--color-border);
}
.cm-faction-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text);
}
.cm-faction-count {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.cm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.75rem;
  align-content: start;
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

/* 头像 */
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
.cm-card-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}
.cm-card-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text);
  cursor: pointer;
}
.cm-card-name:hover {
  color: var(--color-primary);
}

.cm-role-tag {
  font-size: 0.7rem;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
  flex-shrink: 0;
}
.cm-role-main {
  background: #fff3cd;
  color: #856404;
}
.cm-role-support {
  background: #d1ecf1;
  color: #0c5460;
}
.cm-role-npc {
  background: #e2e3e5;
  color: #383d41;
}

.cf-role-select {
  display: flex;
  gap: 0.5rem;
}
.cf-role-opt {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 0.85rem;
  transition: border-color 0.15s, background 0.15s;
}
.cf-role-opt input {
  display: none;
}
.cf-role-opt:hover {
  border-color: var(--color-primary);
}
.cf-role-opt.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 500;
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

/* ===== 弹窗 ===== */
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
