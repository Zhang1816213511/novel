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

    <!-- Navigation Bar: 简介大纲 / 章节编辑 / 角色管理 -->
    <nav class="detail-nav">
      <button :class="['nav-btn', { active: activeTab === 'synopsis-outline' }]" @click="switchTab('synopsis-outline')">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        简介大纲
      </button>
      <button :class="['nav-btn', { active: activeTab === 'chapters' }]" @click="switchTab('chapters')">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
        章节编辑
      </button>
      <button :class="['nav-btn', { active: activeTab === 'characters' }]" @click="switchTab('characters')">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>
        角色管理
      </button>
    </nav>

    <!-- Tab Content -->
    <div class="body-layout">
      <!-- 简介大纲 Tab (merged) -->
      <div v-if="activeTab === 'synopsis-outline'" class="tab-content synopsis-outline">
        <section class="so-section">
          <div class="so-section-header">
            <span class="so-section-title">作品简介</span>
            <span v-if="novel.synopsis" class="section-badge">已生成</span>
          </div>
          <RichEditor v-model="novel.synopsis" placeholder="在此编写作品简介..."
            @update:model-value="scheduleSave" />
        </section>
        <section class="so-section">
          <div class="so-section-header">
            <span class="so-section-title">创作大纲</span>
            <span v-if="novel.outline" class="section-badge">已生成</span>
          </div>
          <RichEditor v-model="novel.outline" placeholder="在此编写创作大纲..."
            @update:model-value="scheduleSave" />
        </section>
      </div>

      <!-- Chapters Tab -->
      <template v-if="activeTab === 'chapters'">
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

        <div class="main-content">
          <div v-if="selectedChapter" class="chapter-editor">
            <div class="chapter-editor-header">
              <input v-model="selectedChapter.title" class="chapter-title-input"
                placeholder="章节标题" @input="scheduleChapterSave(selectedChapter)" />
            </div>
            <div class="chapter-editor-body">
              <RichEditor v-model="selectedChapter.content" placeholder="本章正文..."
                :characters="allCharacters" @character-click="onCharacterClick"
                @update:model-value="scheduleChapterSave(selectedChapter)" />
            </div>
          </div>
          <div v-else class="chapter-editor-placeholder">
            <p>选择左侧章节开始编辑，或点击「+ 添加」创建新章节</p>
          </div>
        </div>
      </template>

      <!-- Characters Tab -->
      <CharacterManager v-if="activeTab === 'characters'" :novelId="Number(route.params.id)"
        @characters-updated="onCharactersUpdated" />
    </div>

    <!-- Character Info Popup -->
    <CharacterInfoPopup :character="clickedCharacter" @close="clickedCharacter = null" />

    <!-- AI 对话浮动按钮 -->
    <button class="chat-fab" :class="{ active: showChat }" @click="showChat = !showChat" title="AI 对话">
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
      </svg>
    </button>

    <!-- AI 对话面板 -->
    <ChatPanel
      :visible="showChat"
      :novelId="Number(route.params.id)"
      :chapters="chapters"
      :modelName="selectedModel"
      @close="showChat = false"
      @apply-synopsis="onApplySynopsis"
      @apply-outline="onApplyOutline"
      @apply-chapter="onApplyChapter"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import ChatPanel from '../components/ChatPanel.vue'
import RichEditor from '../components/RichEditor.vue'
import CharacterManager from '../components/CharacterManager.vue'
import CharacterInfoPopup from '../components/CharacterInfoPopup.vue'
import { novelStore } from '../stores/novelStore.js'

const props = defineProps({
  tab: { type: String, default: 'synopsis-outline' }
})

const route = useRoute()
const router = useRouter()
const novel = ref({ title: '', synopsis: '', outline: '' })
const chapters = ref([])
const models = ref([])
const selectedModel = ref('')
const savedAt = ref('')
const wordCount = ref(0)
const selectedChapterIndex = ref(-1)
const showChat = ref(false)
const activeTab = ref(props.tab || 'synopsis-outline')
const allCharacters = ref([])
const clickedCharacter = ref(null)

let saveTimer = null
let chapterTimers = {}

watch(() => props.tab, (t) => {
  if (t) activeTab.value = t
})

watch(activeTab, (t) => {
  const path = t === 'synopsis-outline' ? `/novel/${route.params.id}` : `/novel/${route.params.id}/${t}`
  router.replace(path)
})

watch(() => route.params.id, (id) => {
  if (id && id !== novelStore.currentNovelId) {
    Promise.all([loadNovel(), loadModels(), loadCharacters()])
    updateWordCount()
  }
})

onMounted(async () => {
  await Promise.all([loadNovel(), loadModels(), loadCharacters()])
  updateWordCount()
})

onUnmounted(() => {
  if (saveTimer) clearTimeout(saveTimer)
  Object.values(chapterTimers).forEach(t => clearTimeout(t))
  saveNow()
  novelStore.currentNovelId = null
  novelStore.currentNovelTitle = ''
})

const selectedChapter = computed(() => {
  if (selectedChapterIndex.value >= 0 && selectedChapterIndex.value < chapters.value.length) {
    return chapters.value[selectedChapterIndex.value]
  }
  return null
})

function switchTab(tab) {
  activeTab.value = tab
}

function goBack() {
  novelStore.currentNovelId = null
  novelStore.currentNovelTitle = ''
  router.push('/novel')
}

async function loadNovel() {
  try {
    const res = await axios.get(`/api/novels/${route.params.id}`)
    if (res.data.code === 200) {
      novel.value = res.data.data
      novel.value.synopsis = novel.value.synopsis || ''
      novel.value.outline = novel.value.outline || ''
      novelStore.currentNovelId = Number(route.params.id)
      novelStore.currentNovelTitle = novel.value.title
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

async function loadCharacters() {
  try {
    const res = await axios.get(`/api/characters/novel/${route.params.id}`)
    allCharacters.value = res.data.data || []
  } catch (e) {
    console.error('加载角色失败', e)
  }
}

function onCharacterClick(name) {
  const ch = allCharacters.value.find(c => c.name === name)
  if (ch) clickedCharacter.value = ch
}

function onCharactersUpdated(chars) {
  allCharacters.value = chars
}

function updateWordCount() {
  const allContent = chapters.value.map(c => c.content || '').join('')
  wordCount.value = allContent.length
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

// ========== AI 对话 ==========

function onApplySynopsis(content) {
  novel.value.synopsis = content
  scheduleSave()
}

function onApplyOutline(content) {
  novel.value.outline = content
  scheduleSave()
}

function onApplyChapter(chapterNumber, section, content) {
  const ch = chapters.value.find(c => c.chapterNumber === chapterNumber)
  if (!ch) return
  if (section === 'summary') {
    ch.summary = content
  } else if (section === 'content') {
    ch.content = content
  }
  scheduleChapterSave(ch)
  updateWordCount()
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
  margin-bottom: 0.5rem;
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

/* ===== Navigation Bar ===== */
.detail-nav {
  display: flex;
  gap: 0;
  margin-bottom: 0.75rem;
  flex-shrink: 0;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  border: 1px solid var(--color-border);
}
.nav-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0.7rem 1rem;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 0.88rem;
  font-weight: 500;
  color: var(--color-text-muted);
  transition: all 0.15s;
  position: relative;
}
.nav-btn:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 20%;
  height: 60%;
  width: 1px;
  background: var(--color-border);
}
.nav-btn:hover {
  color: var(--color-text);
  background: var(--color-bg);
}
.nav-btn.active {
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-weight: 600;
}
.nav-btn.active svg {
  stroke: var(--color-primary);
}

/* ===== Tab Content ===== */
.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* ===== 简介大纲 Merged Section ===== */
.synopsis-outline {
  gap: 0.75rem;
  overflow-y: auto;
  padding-right: 4px;
}
.so-section {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.so-section-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.so-section-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text);
}
.so-section .doc-editor-wrapper {
  border: none;
  border-radius: 0;
}
.so-section .doc-editor-toolbar {
  border-radius: 0;
  border-left: none;
  border-right: none;
}
.tab-toolbar-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}
.section-badge {
  font-size: 0.65rem;
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 0.1rem 0.45rem;
  border-radius: 10px;
  font-weight: 500;
}

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
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.chapter-editor-body .doc-editor-wrapper {
  flex: 1;
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-bottom: none;
}
.chapter-editor-body .doc-editor-content {
  max-height: none;
}
.chapter-editor-body .doc-page {
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

/* ===== Floating Chat Button ===== */
.chat-fab {
  position: fixed;
  right: 1.5rem;
  bottom: 1.5rem;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: none;
  background: var(--color-primary);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(66, 185, 131, 0.4);
  transition: var(--transition);
  z-index: 400;
}
.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(66, 185, 131, 0.5);
}
.chat-fab.active {
  background: var(--color-text-muted);
  box-shadow: 0 4px 14px rgba(0,0,0,0.15);
}
</style>
