<template>
  <div :class="['doc-editor-wrapper', { 'is-fullscreen': isFullscreen }]">
    <!-- 菜单栏（类似 WPS/Word） -->
    <div v-if="showToolbar" class="doc-menubar">
      <div class="menubar-row menubar-main">
        <!-- 格式组 -->
        <div class="menubar-group">
          <button type="button" class="menubar-btn" @click="toggleBold" :class="{ active: editor?.isActive('bold') }" title="粗体 Ctrl+B">
            <strong>B</strong>
          </button>
          <button type="button" class="menubar-btn" @click="toggleItalic" :class="{ active: editor?.isActive('italic') }" title="斜体 Ctrl+I">
            <em>I</em>
          </button>
          <button type="button" class="menubar-btn" @click="toggleUnderline" :class="{ active: editor?.isActive('underline') }" title="下划线 Ctrl+U">
            <span style="text-decoration:underline">U</span>
          </button>
          <button type="button" class="menubar-btn" @click="toggleStrike" :class="{ active: editor?.isActive('strike') }" title="删除线">
            <span style="text-decoration:line-through">S</span>
          </button>
          <button type="button" class="menubar-btn" @click="toggleHighlight" :class="{ active: editor?.isActive('highlight') }" title="高亮">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M6 20l4-4-4-4-4 4 4 4z"/><path d="M20 4l-8 8 4 4 8-8-4-4z"/></svg>
          </button>
        </div>

        <span class="menubar-sep"></span>

        <!-- 段落组 -->
        <div class="menubar-group">
          <select class="menubar-select" @change="setHeading" :value="headingLevel">
            <option value="0">正文</option>
            <option value="1">标题 1</option>
            <option value="2">标题 2</option>
            <option value="3">标题 3</option>
          </select>
          <button type="button" class="menubar-btn" @click="toggleBulletList" :class="{ active: editor?.isActive('bulletList') }" title="无序列表">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M8 6h13M8 12h13M8 18h13M3 6h1v1H3zM3 12h1v1H3zM3 18h1v1H3z"/></svg>
          </button>
          <button type="button" class="menubar-btn" @click="toggleOrderedList" :class="{ active: editor?.isActive('orderedList') }" title="有序列表">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M10 6h11M10 12h11M10 18h11M4 6h1v4H4zM4 10h2v2H3v-1h2v-1H4zM3 14h2.2L3 17.2V18h3v-1H4.2L6 14.8V14H3z"/></svg>
          </button>
          <button type="button" class="menubar-btn" @click="toggleBlockquote" :class="{ active: editor?.isActive('blockQuote') }" title="引用">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/></svg>
          </button>
        </div>

        <span class="menubar-sep"></span>

        <!-- 插入组 -->
        <div class="menubar-group">
          <button type="button" class="menubar-btn" @click="addTable" title="插入表格">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M3 3h18v18H3z"/><path d="M3 9h18M3 15h18M9 3v18M15 3v18" stroke="var(--color-surface)" stroke-width="2"/></svg>
          </button>
          <button type="button" class="menubar-btn" @click="setHorizontalRule" title="分割线">
            ―
          </button>
          <button type="button" class="menubar-btn" @click="addImage" title="插入图片">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
          </button>
        </div>

        <span class="menubar-sep"></span>

        <!-- 对齐组 -->
        <div class="menubar-group">
          <button type="button" class="menubar-btn" @click="setTextAlign('left')" :class="{ active: editor?.isActive({ textAlign: 'left' }) }" title="左对齐">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M3 6h18M3 10h14M3 14h18M3 18h14"/></svg>
          </button>
          <button type="button" class="menubar-btn" @click="setTextAlign('center')" :class="{ active: editor?.isActive({ textAlign: 'center' }) }" title="居中">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M3 6h18M5 10h14M3 14h18M5 18h14"/></svg>
          </button>
          <button type="button" class="menubar-btn" @click="setTextAlign('right')" :class="{ active: editor?.isActive({ textAlign: 'right' }) }" title="右对齐">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M3 6h18M7 10h14M3 14h18M7 18h14"/></svg>
          </button>
        </div>

        <span class="menubar-sep"></span>

        <!-- 历史 & 视图 -->
        <div class="menubar-group">
          <button type="button" class="menubar-btn" @click="undo" title="撤销 Ctrl+Z">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
          </button>
          <button type="button" class="menubar-btn" @click="redo" title="重做 Ctrl+Y">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg>
          </button>
        </div>

        <div class="menubar-spacer"></div>

        <!-- 全屏切换 -->
        <div class="menubar-group">
          <button type="button" class="menubar-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏 Esc' : '全屏'" :class="{ active: isFullscreen }">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <polyline v-if="!isFullscreen" points="15 3 21 3 21 9"/>
              <polyline v-if="!isFullscreen" points="9 21 3 21 3 15"/>
              <line v-if="!isFullscreen" x1="21" y1="3" x2="14" y2="10"/>
              <line v-if="!isFullscreen" x1="3" y1="21" x2="10" y2="14"/>
              <polyline v-if="isFullscreen" points="4 14 10 14 10 20"/>
              <polyline v-if="isFullscreen" points="20 10 14 10 14 4"/>
              <line v-if="isFullscreen" x1="14" y1="10" x2="21" y2="3"/>
              <line v-if="isFullscreen" x1="10" y1="14" x2="3" y2="21"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 编辑器内容（文档页面） -->
    <div class="doc-editor-content" ref="editorContainer">
      <div class="doc-page">
        <editor-content :editor="editor" class="doc-prose" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Placeholder } from '@tiptap/extension-placeholder'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import { TextAlign } from '@tiptap/extension-text-align'
import { Underline } from '@tiptap/extension-underline'
import { Highlight } from '@tiptap/extension-highlight'
import { Image } from '@tiptap/extension-image'
import { HorizontalRule } from '@tiptap/extension-horizontal-rule'
import { Link } from '@tiptap/extension-link'

// ProseMirror 装饰器：角色名称高亮
import { Plugin, PluginKey } from 'prosemirror-state'
import { Decoration, DecorationSet } from 'prosemirror-view'

function createCharacterPlugin(charNames) {
  if (!charNames || charNames.length === 0) return null

  const sorted = [...charNames].sort((a, b) => b.length - a.length)

  return new Plugin({
    key: new PluginKey('character-highlight'),
    state: {
      init(_, { doc }) { return buildDeco(doc, sorted) },
      apply(tr, old, _oldState, newState) {
        if (tr.docChanged || tr.getMeta('characters-updated')) {
          return buildDeco(newState.doc, sorted)
        }
        return old.map(tr, tr.doc)
      }
    },
    props: {
      decorations(state) { return this.getState(state) },
      handleClick(view, pos) {
        const { doc } = view.state
        const resolved = doc.resolve(pos)
        const node = resolved.nodeAfter || doc.nodeAt(pos)
        if (!node || !node.isText) {
          // 检查前后文本节点
          for (let d = 0; d <= 2; d++) {
            const n = doc.nodeAt(Math.max(0, pos - d))
            if (n && n.isText) {
              const charName = findCharacterAt(n.text, d, sorted)
              if (charName) {
                emitCharacterClick(charName)
                return true
              }
            }
          }
          return false
        }

        const relPos = pos - resolved.before(resolved.depth)
        const charName = findCharacterAt(node.text, relPos, sorted)
        if (charName) {
          emitCharacterClick(charName)
          return true
        }
        return false
      }
    }
  })
}

function buildDeco(doc, sorted) {
  const decorations = []
  const seen = new Set()

  doc.descendants((node, pos) => {
    if (!node.isText || !node.text) return
    const text = node.text
    for (const name of sorted) {
      let idx = 0
      while ((idx = text.indexOf(name, idx)) !== -1) {
        const from = pos + idx
        const to = from + name.length
        // 避免重叠装饰
        const key = `${from}-${to}`
        if (!seen.has(key)) {
          seen.add(key)
          decorations.push(Decoration.inline(from, to, {
            class: 'character-ref',
            'data-char': name,
          }))
        }
        idx += name.length
      }
    }
  })

  return DecorationSet.create(doc, decorations)
}

function findCharacterAt(text, pos, sorted) {
  if (!text || pos < 0) return null
  for (const name of sorted) {
    const idx = text.indexOf(name)
    if (idx >= 0 && pos >= idx && pos < idx + name.length) {
      return name
    }
  }
  return null
}

let _emitCharacterClick = null
function emitCharacterClick(name) {
  if (_emitCharacterClick) _emitCharacterClick(name)
}

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '开始写作...' },
  showToolbar: { type: Boolean, default: true },
  height: { type: String, default: null },
  characters: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'characterClick'])

// 连接角色点击处理器
_emitCharacterClick = (name) => emit('characterClick', name)

const isFullscreen = ref(false)
const editorContainer = ref(null)

// 监听角色属性以更新高亮插件
watch(() => props.characters, (chars) => {
  if (!editor.value) return
  const names = chars?.map(c => c.name).filter(Boolean) || []
  const plugin = createCharacterPlugin(names)
  if (plugin) {
    // 删除旧的，添加新的
    try { editor.value.unregisterPlugin('character-highlight') } catch {}
    editor.value.registerPlugin(plugin)
  }
}, { deep: true })

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
      codeBlock: false
    }),
    Placeholder.configure({ placeholder: props.placeholder }),
    Table.configure({ resizable: true }),
    TableRow, TableCell, TableHeader,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    Underline, Highlight, Image, HorizontalRule, Link,
  ],
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    emit('update:modelValue', html)
  },
  editorProps: {
    attributes: {
      class: 'doc-prose-content'
    }
  }
})

const headingLevel = computed(() => {
  if (!editor.value) return '0'
  for (let i = 1; i <= 3; i++) {
    if (editor.value.isActive('heading', { level: i })) return String(i)
  }
  return '0'
})

watch(() => props.modelValue, (val) => {
  if (editor.value && val !== editor.value.getHTML()) {
    editor.value.commands.setContent(val || '', false)
  }
})

// 格式化命令
function toggleBold() { editor.value?.chain().focus().toggleBold().run() }
function toggleItalic() { editor.value?.chain().focus().toggleItalic().run() }
function toggleUnderline() { editor.value?.chain().focus().toggleUnderline().run() }
function toggleStrike() { editor.value?.chain().focus().toggleStrike().run() }
function toggleHighlight() { editor.value?.chain().focus().toggleHighlight().run() }
function toggleBulletList() { editor.value?.chain().focus().toggleBulletList().run() }
function toggleOrderedList() { editor.value?.chain().focus().toggleOrderedList().run() }
function toggleBlockquote() { editor.value?.chain().focus().toggleBlockquote().run() }
function setTextAlign(align) { editor.value?.chain().focus().setTextAlign(align).run() }
function undo() { editor.value?.chain().focus().undo().run() }
function redo() { editor.value?.chain().focus().redo().run() }
function setHorizontalRule() { editor.value?.chain().focus().setHorizontalRule().run() }

function setHeading(e) {
  const val = parseInt(e.target.value)
  if (val === 0) {
    editor.value?.chain().focus().setParagraph().run()
  } else {
    editor.value?.chain().focus().toggleHeading({ level: val }).run()
  }
}

function addTable() {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

function addImage() {
  const url = prompt('输入图片 URL:')
  if (url) editor.value?.chain().focus().setImage({ src: url }).run()
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  setTimeout(() => editor.value?.commands.focus(), 100)
}

// 键盘：Esc 退出全屏
function onKeydown(e) {
  if (e.key === 'Escape' && isFullscreen.value) {
    isFullscreen.value = false
  }
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  editor.value?.destroy()
})
</script>

<style scoped>
/* ===== 容器 ===== */
.doc-editor-wrapper {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.doc-editor-wrapper:focus-within {
  border-color: var(--color-primary);
}

.doc-editor-wrapper.is-fullscreen {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 999;
  border-radius: 0;
  border: none;
  background: #e8e8e8;
}

/* ===== 菜单栏 ===== */
.doc-menubar {
  flex-shrink: 0;
  background: #f8f9fa;
  border-bottom: 1px solid var(--color-border);
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  user-select: none;
}
.menubar-main {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 4px 6px;
  flex-wrap: wrap;
}
.menubar-group {
  display: flex;
  align-items: center;
  gap: 1px;
}
.menubar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 5px;
  border: none;
  background: transparent;
  border-radius: 3px;
  cursor: pointer;
  color: #444;
  font-size: 0.82rem;
  transition: all 0.1s;
  white-space: nowrap;
}
.menubar-btn:hover { background: #e0e0e0; }
.menubar-btn.active { background: #d0e8ff; color: #1a73e8; }
.menubar-select {
  height: 28px;
  border: 1px solid transparent;
  border-radius: 3px;
  background: transparent;
  font-size: 0.8rem;
  color: #444;
  cursor: pointer;
  padding: 0 4px;
  outline: none;
}
.menubar-select:hover { background: #e0e0e0; }
.menubar-sep {
  width: 1px;
  height: 20px;
  background: #d0d0d0;
  margin: 0 3px;
  flex-shrink: 0;
}
.menubar-spacer { flex: 1; }

/* ===== 编辑器内容 ===== */
.doc-editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  justify-content: center;
}

/* ===== 文档页面 ===== */
.doc-page {
  width: 100%;
  max-width: 800px;
  min-height: 400px;
  background: white;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08), 0 0 0 1px rgba(0,0,0,0.04);
  border-radius: 2px;
  transition: all 0.2s;
}

/* 全屏：最大化页面 */
.is-fullscreen .doc-editor-content {
  background: #e8e8e8;
  padding: 30px 20px;
}
.is-fullscreen .doc-page {
  box-shadow: 0 2px 12px rgba(0,0,0,0.15), 0 0 0 1px rgba(0,0,0,0.05);
  min-height: 100%;
}

/* ===== 正文排版 ===== */
:deep(.ProseMirror) {
  padding: 28px 32px;
  outline: none;
  min-height: 360px;
  line-height: 1.9;
  font-size: 1rem;
  color: #1a1a1a;
}
:deep(.ProseMirror p) {
  margin: 0;
  padding: 3px 0;
}

/* 交替行背景（微妙的） */
:deep(.ProseMirror) {
  counter-reset: prose-line;
}
:deep(.ProseMirror p:nth-of-type(even)) {
  background: rgba(0, 20, 40, 0.03);
}

/* 占位符 */
:deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: #aaa;
  pointer-events: none;
  height: 0;
}

/* 标题 */
:deep(.ProseMirror h1) { font-size: 1.6rem; font-weight: 700; margin: 0.8rem 0 0.4rem; color: #111; }
:deep(.ProseMirror h2) { font-size: 1.3rem; font-weight: 600; margin: 0.6rem 0 0.3rem; color: #222; }
:deep(.ProseMirror h3) { font-size: 1.1rem; font-weight: 600; margin: 0.4rem 0 0.2rem; color: #333; }

/* 列表 */
:deep(.ProseMirror ul), :deep(.ProseMirror ol) { padding-left: 1.8rem; }
:deep(.ProseMirror li) { margin: 2px 0; }
:deep(.ProseMirror ul li) { list-style-type: disc; }
:deep(.ProseMirror ol li) { list-style-type: decimal; }

/* 引用 */
:deep(.ProseMirror blockquote) {
  border-left: 3px solid var(--color-primary);
  margin: 0.5rem 0;
  padding: 4px 12px;
  color: #555;
  background: #f8f9fa;
  border-radius: 0 3px 3px 0;
}

/* 行内代码 */
:deep(.ProseMirror code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.85rem;
  color: #d63384;
}

/* 分割线 */
:deep(.ProseMirror hr) {
  border: none;
  border-top: 1px solid #ddd;
  margin: 1rem 0;
}

/* 角色名称引用 */
:deep(.character-ref) {
  color: #1a73e8;
  cursor: pointer;
  border-bottom: 1px dashed #1a73e8;
  transition: background 0.15s;
}
:deep(.character-ref:hover) {
  background: #e8f0fe;
}

/* 表格 */
:deep(.ProseMirror table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5rem 0;
  overflow: hidden;
}
:deep(.ProseMirror th),
:deep(.ProseMirror td) {
  border: 1px solid #ccc;
  padding: 8px 12px;
  min-width: 60px;
  text-align: left;
  vertical-align: top;
}
:deep(.ProseMirror th) {
  background: #f5f5f5;
  font-weight: 600;
}
:deep(.ProseMirror td) {
  background: white;
}
:deep(.ProseMirror .selectedCell) {
  background: #e8f0fe;
}

/* 图片 */
:deep(.ProseMirror img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 0.5rem 0;
}

/* 链接 */
:deep(.ProseMirror a) {
  color: #1a73e8;
  cursor: pointer;
  text-decoration: underline;
}

/* 全屏更大字号 */
.is-fullscreen :deep(.ProseMirror) {
  min-height: calc(100vh - 140px);
  font-size: 1.1rem;
  line-height: 2;
  padding: 32px 40px;
}
</style>
