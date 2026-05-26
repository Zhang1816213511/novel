<template>
  <div class="novel-detail">
    <!-- Header -->
    <header class="detail-header">
      <button @click="goBack" class="btn btn-sm btn-back">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12"/>
          <polyline points="12 19 5 12 12 5"/>
        </svg>
        返回
      </button>
      <div class="title-area">
        <input v-model="novel.title" class="title-input" placeholder="作品标题" @input="scheduleSave" />
      </div>
      <div class="header-right">
        <select v-model="selectedModel" class="input model-select">
          <option value="" disabled>选择模型...</option>
          <option v-for="m in models" :key="m.id" :value="m.name">{{ m.name }}</option>
        </select>
        <span class="stat">{{ wordCount }} 字</span>
        <span v-if="savedAt" class="stat saved">{{ savedAt }} 已保存</span>
        <span v-else class="stat unsaved">未保存</span>
      </div>
    </header>

    <div class="body-layout">
      <!-- Left: Chapter Sidebar -->
      <aside class="chapter-sidebar">
        <div class="sidebar-header">
          <h3>章节</h3>
          <button class="btn btn-sm" @click="addChapter">+ 添加</button>
        </div>
        <div class="chapter-nav-list">
          <div v-for="(ch, idx) in chapters" :key="ch.id"
            :class="['chapter-nav-item', { active: idx === selectedChapterIndex }]"
            @click="selectedChapterIndex = idx">
            <span class="chapter-nav-num">{{ idx + 1 }}</span>
            <span class="chapter-nav-title">{{ ch.title || `第${idx + 1}章` }}</span>
            <button class="chapter-nav-delete" @click.stop="deleteChapter(ch.id, idx)" title="删除章节">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div v-if="chapters.length === 0" class="chapter-nav-empty">
            暂无章节，点击「+ 添加」创建
          </div>
        </div>
      </aside>

      <!-- Right: Main Content -->
      <div class="main-content">
        <!-- Synopsis & Outline (collapsible) -->
        <div class="meta-sections">
          <section class="section-card">
            <div class="section-header" @click="toggleSection('synopsis')">
              <h3>
                <span class="section-arrow">{{ openSections.synopsis ? '▼' : '▶' }}</span>
                作品简介
                <span v-if="novel.synopsis" class="section-badge">已生成</span>
              </h3>
              <button v-if="openSections.synopsis" class="btn btn-sm btn-primary"
                :disabled="genLoading.synopsis || !selectedModel"
                @click.stop="generateSynopsis">
                <span v-if="genLoading.synopsis" class="spinner"></span>
                {{ novel.synopsis ? '重新生成' : '生成简介' }}
              </button>
            </div>
            <div v-show="openSections.synopsis" class="section-body">
              <textarea v-model="novel.synopsis" class="section-textarea"
                placeholder="作品简介将在生成后显示，也可手动编辑..."
                @input="scheduleSave"></textarea>
            </div>
          </section>

          <section class="section-card">
            <div class="section-header" @click="toggleSection('outline')">
              <h3>
                <span class="section-arrow">{{ openSections.outline ? '▼' : '▶' }}</span>
                创作大纲
                <span v-if="novel.outline" class="section-badge">已生成</span>
              </h3>
              <button v-if="openSections.outline" class="btn btn-sm btn-primary"
                :disabled="genLoading.outline || !selectedModel || !novel.synopsis"
                @click.stop="generateOutline">
                <span v-if="genLoading.outline" class="spinner"></span>
                {{ novel.outline ? '重新生成' : '生成大纲' }}
              </button>
            </div>
            <div v-show="openSections.outline" class="section-body">
              <textarea v-model="novel.outline" class="section-textarea"
                placeholder="大纲将在生成简介后生成，也可手动编辑..."
                @input="scheduleSave"></textarea>
            </div>
          </section>
        </div>

        <!-- Chapter Editor -->
        <div v-if="selectedChapter" class="chapter-editor">
          <div class="chapter-editor-header">
            <input v-model="selectedChapter.title" class="chapter-title-input"
              placeholder="章节标题" @input="scheduleChapterSave(selectedChapter)" />
          </div>
          <div class="chapter-editor-body">
            <div class="chapter-field">
              <div class="chapter-field-header">
                <label>梗概</label>
                <button class="btn btn-xs btn-primary"
                  :disabled="genLoading['summary-' + selectedChapter.id] || !selectedModel"
                  @click="generateSummary(selectedChapter)">
                  <span v-if="genLoading['summary-' + selectedChapter.id]" class="spinner"></span>
                  生成梗概
                </button>
              </div>
              <textarea v-model="selectedChapter.summary" class="chapter-textarea"
                placeholder="本章梗概，可手动编辑或点击生成"
                @input="scheduleChapterSave(selectedChapter)"></textarea>
            </div>
            <div class="chapter-field">
              <div class="chapter-field-header">
                <label>正文</label>
                <button class="btn btn-xs btn-primary"
                  :disabled="genLoading['content-' + selectedChapter.id] || !selectedModel || !selectedChapter.summary"
                  @click="generateContent(selectedChapter)">
                  <span v-if="genLoading['content-' + selectedChapter.id]" class="spinner"></span>
                  生成正文
                </button>
              </div>
              <textarea v-model="selectedChapter.content" class="chapter-textarea chapter-content"
                placeholder="本章正文，可手动编辑或先生成梗概再生成正文"
                @input="scheduleChapterSave(selectedChapter)"></textarea>
            </div>
          </div>
        </div>
        <div v-else class="chapter-editor-placeholder">
          <p>选择左侧章节开始编辑，或点击「+ 添加」创建新章节</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const novel = ref({ title: '', content: '', synopsis: '', outline: '' })
const chapters = ref([])
const models = ref([])
const selectedModel = ref('')
const savedAt = ref('')
const genLoading = ref({})
const wordCount = ref(0)
const selectedChapterIndex = ref(-1)

const openSections = ref({ synopsis: false, outline: false })

const selectedChapter = computed(() => {
  if (selectedChapterIndex.value >= 0 && selectedChapterIndex.value < chapters.value.length) {
    return chapters.value[selectedChapterIndex.value]
  }
  return null
})

let saveTimer = null
let chapterTimers = {}

onMounted(async () => {
  await Promise.all([loadNovel(), loadModels()])
  updateWordCount()
})

onUnmounted(() => {
  if (saveTimer) clearTimeout(saveTimer)
  Object.values(chapterTimers).forEach(t => clearTimeout(t))
  saveNow()
})

async function loadNovel() {
  try {
    const res = await axios.get(`/api/novels/${route.params.id}`)
    if (res.data.code === 200) {
      novel.value = res.data.data
      novel.value.synopsis = novel.value.synopsis || ''
      novel.value.outline = novel.value.outline || ''
    }
  } catch (e) {
    console.error('加载作品失败', e)
  }
  await loadChapters()
}

async function loadChapters() {
  try {
    const res = await axios.get(`/api/chapters/novel/${route.params.id}`)
    if (res.data.code === 200) {
      chapters.value = (res.data.data || []).map(ch => ({
        ...ch,
        summary: ch.summary || '',
        content: ch.content || ''
      }))
      if (chapters.value.length > 0) {
        selectedChapterIndex.value = 0
      }
    }
  } catch (e) {
    console.error('加载章节失败', e)
  }
}

async function loadModels() {
  try {
    const res = await axios.get('/api/models/enabled')
    models.value = res.data.data || []
    if (models.value.length > 0) {
      selectedModel.value = models.value[0].name
    }
  } catch (e) {
    console.error('加载模型失败', e)
  }
}

function updateWordCount() {
  const allContent = chapters.value.map(c => c.content || '').join('')
  wordCount.value = allContent.length
}

function toggleSection(name) {
  openSections.value[name] = !openSections.value[name]
}

// ========== Auto-Save ==========

function scheduleSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => saveNow(), 800)
}

async function saveNow() {
  try {
    await axios.put(`/api/novels/${route.params.id}`, {
      title: novel.value.title,
      content: novel.value.content,
      synopsis: novel.value.synopsis,
      outline: novel.value.outline
    })
    savedAt.value = new Date().toLocaleTimeString('zh-CN')
  } catch (e) {
    console.error('保存失败', e)
  }
}

function scheduleChapterSave(chapter) {
  if (chapterTimers[chapter.id]) clearTimeout(chapterTimers[chapter.id])
  chapterTimers[chapter.id] = setTimeout(async () => {
    try {
      await axios.put(`/api/chapters/${chapter.id}`, {
        id: chapter.id,
        novelId: chapter.novelId,
        chapterNumber: chapter.chapterNumber,
        title: chapter.title,
        summary: chapter.summary,
        content: chapter.content
      })
      updateWordCount()
      savedAt.value = new Date().toLocaleTimeString('zh-CN')
    } catch (e) {
      console.error('章节保存失败', e)
    }
    delete chapterTimers[chapter.id]
  }, 800)
}

function goBack() { router.push('/novel') }

// ========== Chapters ==========

async function addChapter() {
  try {
    const res = await axios.post(`/api/chapters/novel/${route.params.id}`)
    if (res.data.code === 200) {
      const ch = res.data.data
      ch.summary = ch.summary || ''
      ch.content = ch.content || ''
      chapters.value.push(ch)
      selectedChapterIndex.value = chapters.value.length - 1
    }
  } catch (e) {
    console.error('添加章节失败', e)
  }
}

async function deleteChapter(id, idx) {
  try {
    await axios.delete(`/api/chapters/${id}`)
    chapters.value.splice(idx, 1)
    if (selectedChapterIndex.value === idx) {
      selectedChapterIndex.value = idx < chapters.value.length ? idx : chapters.value.length - 1
    } else if (selectedChapterIndex.value > idx) {
      selectedChapterIndex.value--
    }
    updateWordCount()
  } catch (e) {
    console.error('删除章节失败', e)
  }
}

// ========== Generation ==========

async function generateSynopsis() {
  if (!selectedModel.value) return
  genLoading.value.synopsis = true
  try {
    const res = await axios.post(`/api/generate/${route.params.id}/synopsis`, {
      modelName: selectedModel.value
    })
    if (res.data.code === 200) {
      novel.value.synopsis = res.data.data
      await saveNow()
    }
  } catch (e) {
    console.error('生成简介失败', e)
    alert('生成简介失败：' + (e.response?.data?.message || e.message))
  } finally {
    genLoading.value.synopsis = false
  }
}

async function generateOutline() {
  if (!selectedModel.value) return
  genLoading.value.outline = true
  try {
    const res = await axios.post(`/api/generate/${route.params.id}/outline`, {
      modelName: selectedModel.value
    })
    if (res.data.code === 200) {
      novel.value.outline = res.data.data
      await saveNow()
    }
  } catch (e) {
    console.error('生成大纲失败', e)
    alert('生成大纲失败：' + (e.response?.data?.message || e.message))
  } finally {
    genLoading.value.outline = false
  }
}

async function generateSummary(chapter) {
  if (!selectedModel.value) return
  const key = 'summary-' + chapter.id
  genLoading.value[key] = true
  try {
    const res = await axios.post(`/api/generate/${route.params.id}/chapter-summary/${chapter.id}`, {
      modelName: selectedModel.value
    })
    if (res.data.code === 200) {
      chapter.summary = res.data.data
      scheduleChapterSave(chapter)
    }
  } catch (e) {
    console.error('生成梗概失败', e)
    alert('生成梗概失败：' + (e.response?.data?.message || e.message))
  } finally {
    genLoading.value[key] = false
  }
}

async function generateContent(chapter) {
  if (!selectedModel.value) return
  const key = 'content-' + chapter.id
  genLoading.value[key] = true
  try {
    const res = await axios.post(`/api/generate/${route.params.id}/chapter-content/${chapter.id}`, {
      modelName: selectedModel.value
    })
    if (res.data.code === 200) {
      chapter.content = res.data.data
      scheduleChapterSave(chapter)
      updateWordCount()
    }
  } catch (e) {
    console.error('生成正文失败', e)
    alert('生成正文失败：' + (e.response?.data?.message || e.message))
  } finally {
    genLoading.value[key] = false
  }
}
</script>

<style scoped>
.novel-detail {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 4rem);
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== Header ===== */
.detail-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 0.75rem;
  flex-shrink: 0;
}
.title-area { flex: 1; min-width: 0; }
.title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-text);
  background: transparent;
  padding: 0.25rem 0;
}
.title-input::placeholder { color: var(--color-text-muted); }
.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}
.model-select {
  width: 140px;
  font-size: 0.8rem;
  padding: 0.35rem 0.5rem;
}
.stat { font-size: 0.8rem; color: var(--color-text-muted); white-space: nowrap; }
.stat.saved { color: var(--color-success); }
.stat.unsaved { color: var(--color-warning); }
.btn-back { flex-shrink: 0; }

/* ===== Body Layout (sidebar + main) ===== */
.body-layout {
  flex: 1;
  display: flex;
  gap: 0.75rem;
  overflow: hidden;
}

/* ===== Chapter Sidebar ===== */
.chapter-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.sidebar-header h3 {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}
.chapter-nav-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.35rem;
}
.chapter-nav-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.5rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}
.chapter-nav-item:hover { background: var(--color-bg); }
.chapter-nav-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
}
.chapter-nav-num {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-text-muted);
  min-width: 18px;
  text-align: center;
}
.chapter-nav-item.active .chapter-nav-num { color: var(--color-primary); }
.chapter-nav-title {
  flex: 1;
  font-size: 0.82rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
}
.chapter-nav-delete {
  opacity: 0;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  padding: 2px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  transition: opacity 0.15s, color 0.15s;
  flex-shrink: 0;
}
.chapter-nav-item:hover .chapter-nav-delete { opacity: 1; }
.chapter-nav-delete:hover { color: var(--color-danger); }
.chapter-nav-empty {
  text-align: center;
  padding: 1.5rem 0.5rem;
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

/* ===== Main Content ===== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow: hidden;
}

/* ===== Meta Sections (synopsis & outline) ===== */
.meta-sections {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex-shrink: 0;
}
.section-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.6rem 1rem;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.section-header:hover { background: var(--color-bg); }
.section-header h3 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}
.section-arrow {
  font-size: 0.65rem;
  color: var(--color-text-muted);
  width: 12px;
}
.section-badge {
  font-size: 0.65rem;
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 0.1rem 0.45rem;
  border-radius: 10px;
  font-weight: 500;
}
.section-body {
  padding: 0 1rem 0.75rem;
}
.section-textarea {
  width: 100%;
  min-height: 80px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.6rem;
  font-size: 0.85rem;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  color: var(--color-text);
  background: var(--color-bg);
  transition: var(--transition);
}
.section-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

/* ===== Chapter Editor ===== */
.chapter-editor {
  flex: 1;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chapter-editor-header {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.chapter-title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  background: transparent;
  padding: 0.15rem 0;
}
.chapter-editor-body {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 1rem 1rem;
}
.chapter-field {
  margin-bottom: 0.75rem;
}
.chapter-field:last-child { margin-bottom: 0; }
.chapter-field-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
}
.chapter-field-header label {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}
.chapter-textarea {
  width: 100%;
  min-height: 80px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.6rem;
  font-size: 0.9rem;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  color: var(--color-text);
  background: var(--color-bg);
  transition: var(--transition);
}
.chapter-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}
.chapter-content {
  min-height: 300px;
}

/* ===== Placeholder ===== */
.chapter-editor-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

/* ===== Buttons ===== */
.btn-xs {
  padding: 0.25rem 0.6rem;
  font-size: 0.78rem;
}

/* ===== Spinner ===== */
.spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin-right: 2px;
  vertical-align: middle;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
