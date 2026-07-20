<template>
  <div class="faction-manager">
    <div class="fm-header">
      <h3>势力管理</h3>
      <div class="fm-header-actions">
        <div class="fm-view-toggle">
          <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">列表</button>
          <button :class="{ active: viewMode === 'graph' }" @click="viewMode = 'graph'">图谱</button>
        </div>
        <button class="btn btn-sm btn-primary" @click="openCreateFaction">+ 添加势力</button>
        <button v-if="factions.length >= 2" class="btn btn-sm" @click="openCreateRelation">+ 添加关系</button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <!-- 列表视图 -->
    <div v-else-if="viewMode === 'list'" class="fm-list">
      <div v-if="factions.length === 0" class="fm-empty">
        <p>暂无势力，点击右上角添加</p>
      </div>
      <div v-for="f in factions" :key="f.id" class="fm-card">
        <div class="fm-card-body">
          <div class="fm-card-name">{{ f.name }}</div>
          <div v-if="f.description" class="fm-card-desc">{{ f.description }}</div>
          <div class="fm-card-stats">
            <span>{{ getCharCount(f.id) }} 个角色</span>
            <span v-if="getFactionRelations(f.id).length > 0">
              | {{ getFactionRelations(f.id).map(r => relationLabel(r, f.id)).join('、') }}
            </span>
          </div>
        </div>
        <div class="fm-card-actions">
          <button class="btn btn-sm" @click="editFaction(f)">编辑</button>
          <button class="btn btn-sm btn-danger" @click="deleteFaction(f)">删除</button>
        </div>
      </div>

      <!-- 关系列表 -->
      <div v-if="relations.length > 0" class="fm-relations">
        <h4>势力关系</h4>
        <div v-for="r in relations" :key="r.id" class="fm-rel-row">
          <span class="fm-rel-from">{{ getFactionName(r.sourceFactionId) }}</span>
          <span class="fm-rel-type" :style="{ background: relationColor(r.relationType) }">{{ r.relationType }}</span>
          <span class="fm-rel-to">{{ getFactionName(r.targetFactionId) }}</span>
          <span v-if="r.description" class="fm-rel-desc">{{ r.description }}</span>
          <button class="btn btn-sm" @click="editRelation(r)">编辑</button>
          <button class="btn btn-sm btn-danger" @click="deleteRelation(r)">删除</button>
        </div>
      </div>
    </div>

    <!-- 图谱视图 -->
    <div v-else-if="viewMode === 'graph'" class="fm-graph-container" ref="graphContainer"></div>

    <!-- 势力表单弹窗 -->
    <div v-if="showFactionForm" class="modal-overlay" @click.self="showFactionForm = false">
      <div class="modal">
        <h3>{{ editingFaction ? '编辑势力' : '新增势力' }}</h3>
        <div class="form-group">
          <label>势力名称 *</label>
          <input v-model="factionForm.name" class="input" placeholder="如：青云宗" />
        </div>
        <div class="form-group">
          <label>势力简介</label>
          <textarea v-model="factionForm.description" class="input" rows="3" placeholder="势力的背景和特点"></textarea>
        </div>
        <div class="modal-actions">
          <button @click="showFactionForm = false" class="btn">取消</button>
          <button @click="saveFaction" class="btn btn-primary" :disabled="!factionForm.name.trim()">保存</button>
        </div>
      </div>
    </div>

    <!-- 关系表单弹窗 -->
    <div v-if="showRelationForm" class="modal-overlay" @click.self="showRelationForm = false">
      <div class="modal">
        <h3>{{ editingRelation ? '编辑关系' : '新增关系' }}</h3>
        <div class="form-group">
          <label>源势力 *</label>
          <select v-model="relationForm.sourceFactionId" class="input">
            <option v-for="f in factions" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>目标势力 *</label>
          <select v-model="relationForm.targetFactionId" class="input">
            <option v-for="f in factions" :key="f.id" :value="f.id">{{ f.name }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>关系类型 *</label>
          <div class="fm-relation-types">
            <label v-for="rt in relationTypes" :key="rt" class="fm-rel-opt" :class="{ active: relationForm.relationType === rt }">
              <input type="radio" v-model="relationForm.relationType" :value="rt" /> {{ rt }}
            </label>
          </div>
        </div>
        <div class="form-group">
          <label>说明</label>
          <input v-model="relationForm.description" class="input" placeholder="可选，如：曾为同盟，后因利益决裂" />
        </div>
        <div class="modal-actions">
          <button @click="showRelationForm = false" class="btn">取消</button>
          <button @click="saveRelation" class="btn btn-primary" :disabled="!relationForm.sourceFactionId || !relationForm.targetFactionId || !relationForm.relationType">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import axios from 'axios'

const props = defineProps({
  novelId: { type: Number, required: true },
  characterCounts: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['factions-updated'])

const factions = ref([])
const relations = ref([])
const loading = ref(false)
const viewMode = ref('list')
const graphContainer = ref(null)
let network = null

const showFactionForm = ref(false)
const editingFaction = ref(null)
const factionForm = ref({ name: '', description: '' })

const showRelationForm = ref(false)
const editingRelation = ref(null)
const relationForm = ref({ sourceFactionId: null, targetFactionId: null, relationType: '同盟', description: '' })
const relationTypes = ['同盟', '敌对', '中立', '附属']

onMounted(loadData)

watch(() => props.novelId, loadData)

watch(viewMode, async (mode) => {
  if (mode === 'graph') {
    await nextTick()
    renderGraph()
  }
})

async function loadData() {
  if (!props.novelId) return
  loading.value = true
  try {
    const [fRes, rRes] = await Promise.all([
      axios.get(`/api/factions/novel/${props.novelId}`),
      axios.get(`/api/factions/${props.novelId}/relations`)
    ])
    factions.value = fRes.data.data || []
    relations.value = rRes.data.data || []
    emit('factions-updated', factions.value)
  } catch (e) {
    console.error('加载势力失败', e)
  } finally {
    loading.value = false
  }
}

function getFactionName(id) {
  const f = factions.value.find(f => f.id === id)
  return f ? f.name : '未知'
}

function getCharCount(factionId) {
  return props.characterCounts[factionId] || 0
}

function getFactionRelations(factionId) {
  return relations.value.filter(r => r.sourceFactionId === factionId || r.targetFactionId === factionId)
}

function relationLabel(r, myId) {
  const other = r.sourceFactionId === myId ? getFactionName(r.targetFactionId) : getFactionName(r.sourceFactionId)
  const dir = r.sourceFactionId === myId ? '→' : '←'
  return `${dir} ${r.relationType} ${other}`
}

function relationColor(type) {
  switch (type) {
    case '同盟': return '#d4edda'
    case '敌对': return '#f8d7da'
    case '中立': return '#e2e3e5'
    case '附属': return '#cce5ff'
    default: return '#e2e3e5'
  }
}

function edgeColor(type) {
  switch (type) {
    case '同盟': return { color: '#28a745' }
    case '敌对': return { color: '#dc3545', dashes: true }
    case '中立': return { color: '#6c757d', dashes: true }
    case '附属': return { color: '#007bff' }
    default: return { color: '#6c757d' }
  }
}

// ─── 势力 CRUD ───

function openCreateFaction() {
  editingFaction.value = null
  factionForm.value = { name: '', description: '' }
  showFactionForm.value = true
}

function editFaction(f) {
  editingFaction.value = f
  factionForm.value = { name: f.name, description: f.description || '' }
  showFactionForm.value = true
}

async function saveFaction() {
  if (!factionForm.value.name.trim()) return
  const payload = { novelId: props.novelId, name: factionForm.value.name.trim(), description: factionForm.value.description || null }
  try {
    if (editingFaction.value) {
      await axios.put(`/api/factions/${editingFaction.value.id}`, payload)
    } else {
      await axios.post('/api/factions', payload)
    }
    showFactionForm.value = false
    await loadData()
  } catch (e) {
    console.error('保存势力失败', e)
    alert('保存失败：' + (e.response?.data?.message || e.message))
  }
}

async function deleteFaction(f) {
  if (!confirm(`确认删除势力"${f.name}"？\n该势力下的角色将取消关联，所有关系也将删除。`)) return
  try {
    await axios.delete(`/api/factions/${f.id}`)
    await loadData()
  } catch (e) {
    console.error('删除势力失败', e)
  }
}

// ─── 关系 CRUD ───

function openCreateRelation() {
  editingRelation.value = null
  relationForm.value = { sourceFactionId: factions.value[0]?.id || null, targetFactionId: factions.value[1]?.id || factions.value[0]?.id || null, relationType: '同盟', description: '' }
  showRelationForm.value = true
}

function editRelation(r) {
  editingRelation.value = r
  relationForm.value = { sourceFactionId: r.sourceFactionId, targetFactionId: r.targetFactionId, relationType: r.relationType, description: r.description || '' }
  showRelationForm.value = true
}

async function saveRelation() {
  const payload = { novelId: props.novelId, sourceFactionId: relationForm.value.sourceFactionId, targetFactionId: relationForm.value.targetFactionId, relationType: relationForm.value.relationType, description: relationForm.value.description || null }
  try {
    if (editingRelation.value) {
      await axios.put(`/api/factions/relations/${editingRelation.value.id}`, payload)
    } else {
      await axios.post('/api/factions/relations', payload)
    }
    showRelationForm.value = false
    await loadData()
  } catch (e) {
    console.error('保存关系失败', e)
    alert('保存失败：' + (e.response?.data?.message || e.message))
  }
}

async function deleteRelation(r) {
  if (!confirm('确认删除该关系？')) return
  try {
    await axios.delete(`/api/factions/relations/${r.id}`)
    await loadData()
  } catch (e) {
    console.error('删除关系失败', e)
  }
}

// ─── 图谱渲染 ───

async function renderGraph() {
  if (!graphContainer.value || factions.value.length === 0) return

  try {
    const { DataSet } = await import('vis-data')
    const { Network } = await import('vis-network')

    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9']
    const nodes = new DataSet(factions.value.map((f, i) => ({
      id: f.id,
      label: f.name,
      title: f.description || f.name,
      color: { background: colors[i % colors.length], border: '#333' },
      font: { size: 14, color: '#333' }
    })))

    const edges = new DataSet(relations.value.map(r => ({
      from: r.sourceFactionId,
      to: r.targetFactionId,
      label: r.relationType,
      title: r.description || '',
      arrows: 'to',
      smooth: { type: 'curvedCW', roundness: 0.2 },
      ...edgeColor(r.relationType)
    })))

    const options = {
      physics: { solver: 'barnesHut', barnesHut: { gravitationalConstant: -3000, centralGravity: 0.3, springLength: 200 } },
      edges: { font: { size: 11, align: 'middle' } },
      interaction: { hover: true, tooltipDelay: 200 }
    }

    if (network) network.destroy()
    network = new Network(graphContainer.value, { nodes, edges }, options)

    network.on('click', (params) => {
      if (params.nodes.length > 0) {
        const fid = params.nodes[0]
        const f = factions.value.find(f => f.id === fid)
        if (f) editFaction(f)
      }
    })
  } catch (e) {
    console.error('渲染图谱失败', e)
  }
}
</script>

<style scoped>
.faction-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.fm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}
.fm-header h3 {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}
.fm-header-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.fm-view-toggle {
  display: flex;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.fm-view-toggle button {
  padding: 3px 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
.fm-view-toggle button.active {
  background: var(--color-primary);
  color: #fff;
}

.fm-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.fm-list {
  overflow-y: auto;
  flex: 1;
  padding-bottom: 1rem;
}

.fm-card {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  align-items: center;
}
.fm-card-body {
  flex: 1;
  min-width: 0;
}
.fm-card-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text);
}
.fm-card-desc {
  font-size: 0.82rem;
  color: var(--color-text-secondary);
  margin-top: 2px;
}
.fm-card-stats {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}
.fm-card-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fm-relations {
  margin-top: 1rem;
}
.fm-relations h4 {
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: var(--color-text);
}
.fm-rel-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.85rem;
}
.fm-rel-type {
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 500;
}
.fm-rel-desc {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  flex: 1;
}

.fm-relation-types {
  display: flex;
  gap: 0.5rem;
}
.fm-rel-opt {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 0.85rem;
}
.fm-rel-opt input { display: none; }
.fm-rel-opt.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 500;
}

.fm-graph-container {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  min-height: 400px;
}
</style>
