<template>
  <div class="chat-overlay" v-if="visible" @click.self="$emit('close')">
    <div class="chat-panel" @click.stop>
      <!-- Header -->
      <div class="chat-header">
        <h3>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
          </svg>
          AI 对话
        </h3>
        <button class="chat-close" @click="$emit('close')" title="关闭">
          <svg viewBox="0 0 12 12" width="12" height="12">
            <line x1="1" y1="1" x2="11" y2="11" stroke="currentColor" stroke-width="1.5"/>
            <line x1="11" y1="1" x2="1" y2="11" stroke="currentColor" stroke-width="1.5"/>
          </svg>
        </button>
      </div>

      <!-- Messages -->
      <div class="chat-messages" ref="messagesRef">
        <!-- Welcome -->
        <div v-if="messages.length === 0" class="chat-welcome">
          <div class="welcome-icon">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
          </div>
          <p class="welcome-title">AI 创作助手</p>
          <p class="welcome-hint">
            输入 <code>@</code> 引用文件后提出修改要求<br/>
            例如：<span class="example">@简介 改成更简洁的风格</span>
          </p>
          <div class="welcome-tips">
            <div class="tip"><code>@简介</code> 作品简介</div>
            <div class="tip"><code>@大纲</code> 创作大纲</div>
            <div class="tip"><code>@1/正文</code> 第一章正文</div>
            <div class="tip"><code>@1/梗概:3-10</code> 指定行范围</div>
          </div>
        </div>

        <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
          <!-- Refs chips -->
          <div v-if="msg.refs?.length" class="msg-refs">
            <span v-for="(ref, ri) in msg.refs" :key="ri" class="ref-chip">
              {{ formatRefLabel(ref) }}
            </span>
          </div>
          <!-- Content -->
          <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
          <!-- Changes (apply buttons) -->
          <div v-if="msg.changes?.length" class="msg-changes">
            <div v-for="(ch, ci) in msg.changes" :key="ci" class="change-item">
              <span class="change-label">{{ formatChangeLabel(ch) }}</span>
              <button class="btn btn-xs btn-primary" @click="applyChange(ch)">应用到编辑器</button>
            </div>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="msg assistant">
          <div class="msg-content loading-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input-area">
        <!-- Active refs chips in input -->
        <div v-if="activeRefs.length" class="active-refs">
          <span v-for="(ref, i) in activeRefs" :key="i" class="ref-chip">
            {{ formatRefLabel(ref) }}
            <button class="ref-remove" @click="removeRef(i)">&times;</button>
          </span>
        </div>

        <!-- Autocomplete dropdown -->
        <div v-if="showAutocomplete" class="autocomplete-dropdown" ref="autoRef">
          <div v-for="(item, i) in filteredAutocomplete" :key="i"
            :class="['auto-item', { active: i === autoIndex }]"
            @click="selectAuto(i)"
            @mouseenter="autoIndex = i">
            <span class="auto-icon">{{ item.icon }}</span>
            <div class="auto-info">
              <span class="auto-label">{{ item.label }}</span>
              <span class="auto-desc">{{ item.desc }}</span>
            </div>
          </div>
        </div>

        <div class="input-row">
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="chat-input"
            placeholder="输入消息，@ 引用文件..."
            rows="2"
            @keydown="onInputKeydown"
            @input="onInputChange"
            @compositionstart="composing = true"
            @compositionend="composing = false"
          ></textarea>
          <button class="btn-send" :disabled="!canSend" @click="sendMessage" title="发送">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

export default {
  name: 'ChatPanel',
  props: {
    visible: Boolean,
    novelId: { type: Number, required: true },
    chapters: { type: Array, default: () => [] },
    modelName: { type: String, default: '' },
  },
  emits: ['close', 'apply-synopsis', 'apply-outline', 'apply-chapter'],
  setup(props, { emit }) {
    const inputText = ref('')
    const messages = ref([])
    const loading = ref(false)
    const messagesRef = ref(null)
    const inputRef = ref(null)
    const composing = ref(false)
    const autoIndex = ref(0)
    const showAutocomplete = ref(false)
    const activeRefs = ref([])
    const autoRef = ref(null)

    // ─── Autocomplete items ───
    const autocompleteItems = computed(() => {
      const items = [
        { id: 'synopsis', label: '简介', icon: '📝', desc: '作品简介', type: 'synopsis' },
        { id: 'outline', label: '大纲', icon: '📋', desc: '创作大纲', type: 'outline' },
      ]
      props.chapters.forEach((ch, idx) => {
        const num = ch.chapterNumber || (idx + 1)
        items.push({
          id: `ch${num}/summary`, label: `${num}/梗概`, icon: '📄',
          desc: `第${num}章 梗概`, type: 'chapter',
          chapterNumber: num, section: 'summary',
        })
        items.push({
          id: `ch${num}/content`, label: `${num}/正文`, icon: '📃',
          desc: `第${num}章 正文`, type: 'chapter',
          chapterNumber: num, section: 'content',
        })
      })
      return items
    })

    const autoFilter = ref('')
    const filteredAutocomplete = computed(() => {
      if (!autoFilter.value) return autocompleteItems.value.slice(0, 20)
      const f = autoFilter.value.toLowerCase()
      return autocompleteItems.value.filter(item =>
        item.label.toLowerCase().includes(f) || item.desc.toLowerCase().includes(f)
      ).slice(0, 20)
    })

    // ─── @ 引用解析 ───
    function parseAtReferences(text) {
      const refs = []
      const regex = /@([^\s,，。；;:：]+)/g
      let match
      while ((match = regex.exec(text)) !== null) {
        const raw = match[1]
        // 尝试解析：数字/正文:行-行 或 简介/大纲
        if (raw === '简介' || raw === 'synopsis') {
          refs.push({ type: 'synopsis' })
        } else if (raw === '大纲' || raw === 'outline') {
          refs.push({ type: 'outline' })
        } else {
          // 尝试解析章节引用: 1/正文:5-10
          const chMatch = raw.match(/^(\d+)(?:\/(summary|content|梗概|正文))?(?::(\d+)(?:-(\d+))?)?$/)
          if (chMatch) {
            const ref = {
              type: 'chapter',
              chapterNumber: parseInt(chMatch[1]),
              section: normalizeSection(chMatch[2]),
              startLine: chMatch[3] ? parseInt(chMatch[3]) : null,
              endLine: chMatch[4] ? parseInt(chMatch[4]) : null,
            }
            // 去重
            const exists = refs.some(r =>
              r.type === 'chapter' && r.chapterNumber === ref.chapterNumber && r.section === ref.section
            )
            if (!exists) refs.push(ref)
          }
        }
      }
      return refs
    }

    function normalizeSection(s) {
      if (!s) return null
      if (s === '梗概' || s === 'summary') return 'summary'
      if (s === '正文' || s === 'content') return 'content'
      return s
    }

    // ─── Input handling ───
    function onInputChange() {
      const val = inputText.value
      // 检测 @ 触发 autocomplete
      const lastAtIndex = val.lastIndexOf('@')
      if (lastAtIndex >= 0) {
        const afterAt = val.slice(lastAtIndex + 1)
        // 如果后面有空格则关闭
        if (afterAt.includes(' ')) {
          showAutocomplete.value = false
        } else {
          autoFilter.value = afterAt
          showAutocomplete.value = true
          autoIndex.value = 0
        }
      } else {
        showAutocomplete.value = false
      }
    }

    function onInputKeydown(e) {
      if (showAutocomplete.value && !composing.value) {
        if (e.key === 'ArrowDown') {
          e.preventDefault()
          autoIndex.value = Math.min(autoIndex.value + 1, filteredAutocomplete.value.length - 1)
          scrollAutoVisible()
        } else if (e.key === 'ArrowUp') {
          e.preventDefault()
          autoIndex.value = Math.max(autoIndex.value - 1, 0)
          scrollAutoVisible()
        } else if (e.key === 'Enter' || e.key === 'Tab') {
          if (filteredAutocomplete.value.length > 0) {
            e.preventDefault()
            selectAuto(autoIndex.value)
          }
        }
      }
      // Enter without shift → send
      if (e.key === 'Enter' && !e.shiftKey && !showAutocomplete.value && !composing.value) {
        e.preventDefault()
        sendMessage()
      }
    }

    function selectAuto(index) {
      const item = filteredAutocomplete.value[index]
      if (!item) return

      // 找到最后一个 @ 并替换
      const lastAtIndex = inputText.value.lastIndexOf('@')
      if (lastAtIndex >= 0) {
        inputText.value = inputText.value.slice(0, lastAtIndex) + `@${item.label} `
      }

      showAutocomplete.value = false
      nextTick(() => inputRef.value?.focus())
    }

    function scrollAutoVisible() {
      nextTick(() => {
        const el = autoRef.value?.querySelector('.auto-item.active')
        el?.scrollIntoView({ block: 'nearest' })
      })
    }

    // ─── Send ───
    const canSend = computed(() => inputText.value.trim() && !loading.value)

    async function sendMessage() {
      if (!canSend.value) return

      const text = inputText.value.trim()
      const refs = parseAtReferences(text)

      // 添加用户消息
      messages.value.push({ role: 'user', content: text, refs: refs.length ? refs : undefined })
      inputText.value = ''
      showAutocomplete.value = false
      loading.value = true
      scrollToBottom()

      try {
        const res = await axios.post(`/api/chat/${props.novelId}`, {
          message: text,
          refs: refs,
          modelName: props.modelName,
        })
        if (res.data.code === 200) {
          const data = res.data.data
          messages.value.push({
            role: 'assistant',
            content: data.reply,
            changes: data.changes || undefined,
          })
        }
      } catch (e) {
        messages.value.push({
          role: 'assistant',
          content: '❌ 请求失败：' + (e.response?.data?.message || e.message),
        })
      } finally {
        loading.value = false
        scrollToBottom()
      }
    }

    // ─── Apply changes ───
    function applyChange(change) {
      if (change.type === 'synopsis') {
        emit('apply-synopsis', change.content)
      } else if (change.type === 'outline') {
        emit('apply-outline', change.content)
      } else if (change.type === 'chapter') {
        emit('apply-chapter', change.chapterNumber, change.section, change.content)
      }
    }

    // ─── Utils ───
    function formatRefLabel(ref) {
      if (ref.type === 'synopsis') return '@简介'
      if (ref.type === 'outline') return '@大纲'
      let label = `@${ref.chapterNumber}`
      if (ref.section === 'summary') label += '/梗概'
      else if (ref.section === 'content') label += '/正文'
      if (ref.startLine) label += `:${ref.startLine}${ref.endLine ? '-'+ref.endLine : ''}`
      return label
    }

    function formatChangeLabel(change) {
      if (change.type === 'synopsis') return '📝 简介已更新'
      if (change.type === 'outline') return '📋 大纲已更新'
      if (change.type === 'chapter') {
        let label = `📄 第${change.chapterNumber}章`
        label += change.section === 'summary' ? '梗概' : '正文'
        return label + ' 已更新'
      }
      return '已更新'
    }

    function removeRef(index) {
      activeRefs.value.splice(index, 1)
    }

    function renderMarkdown(text) {
      if (!text) return ''
      // 简单的 markdown 渲染（支持代码块、加粗、换行）
      let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        // 代码块
        .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
        // 行内代码
        .replace(/`([^`]+)`/g, '<code>$1</code>')
        // 加粗
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        // 换行
        .replace(/\n/g, '<br>')
      return html
    }

    function scrollToBottom() {
      nextTick(() => {
        if (messagesRef.value) {
          messagesRef.value.scrollTop = messagesRef.value.scrollHeight
        }
      })
    }

    // Watch for visibility changes
    watch(() => props.visible, (v) => {
      if (v) {
        nextTick(() => inputRef.value?.focus())
      }
    })

    return {
      inputText, messages, loading, messagesRef, inputRef, composing,
      showAutocomplete, autoIndex, autoRef, activeRefs,
      filteredAutocomplete, canSend,
      onInputChange, onInputKeydown, selectAuto,
      sendMessage, applyChange,
      formatRefLabel, formatChangeLabel, removeRef, renderMarkdown,
    }
  },
}
</script>

<style scoped>
/* ===== Overlay ===== */
.chat-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.2);
  z-index: 500;
  display: flex;
  justify-content: flex-end;
  animation: fadeIn 0.15s ease;
}

/* ===== Panel ===== */
.chat-panel {
  width: 420px;
  max-width: 90vw;
  height: 100%;
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0,0,0,0.12);
  animation: slideIn 0.2s ease;
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ===== Header ===== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.chat-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text);
}
.chat-close {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
}
.chat-close:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

/* ===== Messages ===== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Welcome */
.chat-welcome {
  text-align: center;
  padding: 2rem 0.5rem;
  color: var(--color-text-muted);
}
.welcome-icon {
  color: var(--color-primary);
  opacity: 0.6;
  margin-bottom: 0.75rem;
}
.welcome-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}
.welcome-hint {
  font-size: 0.82rem;
  line-height: 1.6;
  margin-bottom: 1rem;
}
.welcome-hint code {
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 1px 5px;
  border-radius: 3px;
}
.welcome-hint .example {
  color: var(--color-primary);
  font-weight: 500;
}
.welcome-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}
.tip {
  font-size: 0.78rem;
  background: var(--color-bg);
  padding: 4px 10px;
  border-radius: 6px;
  color: var(--color-text-secondary);
}
.tip code {
  color: var(--color-primary);
  font-weight: 500;
}

/* Message bubbles */
.msg {
  max-width: 100%;
}
.msg.user {
  align-self: flex-end;
}
.msg.assistant {
  align-self: flex-start;
}

.msg-content {
  padding: 0.6rem 0.85rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  line-height: 1.6;
  word-break: break-word;
}
.msg.user .msg-content {
  background: var(--color-primary);
  color: white;
  border-bottom-right-radius: 4px;
}
.msg.assistant .msg-content {
  background: var(--color-bg);
  color: var(--color-text);
  border-bottom-left-radius: 4px;
}
.msg-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.8rem;
  overflow-x: auto;
  margin: 0.5rem 0;
}
.msg-content :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 0.85rem;
}
.msg.user .msg-content :deep(code) {
  background: rgba(255,255,255,0.15);
}
.msg-content :deep(br) {
  content: '';
  display: block;
  margin: 4px 0;
}

/* Loading dots */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 0.6rem 1rem !important;
  align-items: center;
}
.loading-dots span {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--color-text-muted);
  animation: bounce 1.4s infinite ease-in-out both;
}
.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }
.loading-dots span:nth-child(3) { animation-delay: 0s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Ref chips */
.msg-refs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 4px;
}
.ref-chip {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.ref-remove {
  background: none;
  border: none;
  cursor: pointer;
  color: inherit;
  font-size: 14px;
  line-height: 1;
  padding: 0 2px;
  opacity: 0.6;
}
.ref-remove:hover {
  opacity: 1;
}

/* Changes */
.msg-changes {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.change-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  background: var(--color-bg);
  border-radius: 6px;
  font-size: 0.8rem;
}
.change-label {
  flex: 1;
  color: var(--color-text-secondary);
}

/* ===== Autocomplete ===== */
.autocomplete-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0.75rem;
  right: 0.75rem;
  max-height: 200px;
  overflow-y: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 10;
}
.auto-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  transition: background 0.1s;
}
.auto-item:hover, .auto-item.active {
  background: var(--color-primary-light);
}
.auto-icon { font-size: 1rem; }
.auto-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.auto-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text);
}
.auto-desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

/* ===== Input ===== */
.chat-input-area {
  border-top: 1px solid var(--color-border);
  padding: 0.75rem;
  position: relative;
  flex-shrink: 0;
}
.active-refs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}
.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.chat-input {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.55rem 0.75rem;
  font-size: 0.875rem;
  font-family: inherit;
  resize: none;
  outline: none;
  color: var(--color-text);
  background: var(--color-bg);
  transition: var(--transition);
  line-height: 1.5;
}
.chat-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}
.btn-send {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition);
}
.btn-send:hover:not(:disabled) {
  background: var(--color-primary-dark);
}
.btn-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
