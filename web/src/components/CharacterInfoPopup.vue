<template>
  <div v-if="character" class="char-popup-overlay" @click.self="$emit('close')">
    <div class="char-popup">
      <button class="char-popup-close" @click="$emit('close')">&times;</button>

      <div class="char-popup-header">
        <div class="char-popup-avatar">
          <img v-if="character.imagePath" :src="character.imagePath" :alt="character.name" />
          <div v-else class="char-popup-avatar-empty">{{ character.name.charAt(0) }}</div>
        </div>
        <div class="char-popup-title">
          <h3>{{ character.name }}</h3>
          <span v-if="character.alias" class="char-popup-alias">{{ character.alias }}</span>
          <div v-if="character.roleType || character.factionName" class="char-popup-tags">
            <span v-if="character.roleType" class="char-popup-role" :class="roleClass(character.roleType)">{{ character.roleType }}</span>
            <span v-if="character.factionName" class="char-popup-faction">{{ character.factionName }}</span>
          </div>
        </div>
      </div>

      <div class="char-popup-body">
        <div v-if="character.description" class="char-popup-section">
          <label>描述</label>
          <p>{{ character.description }}</p>
        </div>
        <div v-if="character.personality" class="char-popup-section">
          <label>性格特征</label>
          <p>{{ character.personality }}</p>
        </div>
        <div v-if="character.appearance" class="char-popup-section">
          <label>外貌描述</label>
          <p>{{ character.appearance }}</p>
        </div>
        <div v-if="character.background" class="char-popup-section">
          <label>背景故事</label>
          <p>{{ character.background }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  character: { type: Object, default: null }
})
defineEmits(['close'])

function roleClass(roleType) {
  switch (roleType) {
    case '主角': return 'role-main'
    case '配角': return 'role-support'
    case 'NPC': return 'role-npc'
    default: return ''
  }
}
</script>

<style scoped>
.char-popup-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.3);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.char-popup {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  max-width: 420px;
  width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}

.char-popup-close {
  position: absolute;
  top: 8px; right: 12px;
  border: none;
  background: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  line-height: 1;
}
.char-popup-close:hover { color: #333; }

.char-popup-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.char-popup-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.char-popup-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.char-popup-avatar-empty {
  width: 100%;
  height: 100%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  font-weight: 700;
}

.char-popup-title h3 {
  font-size: 1.15rem;
  font-weight: 700;
  color: #222;
  margin: 0;
}
.char-popup-alias {
  font-size: 0.85rem;
  color: #888;
  margin-top: 2px;
}

.char-popup-tags {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}
.char-popup-role,
.char-popup-faction {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.char-popup-faction {
  background: #e8daef;
  color: #6c3483;
}
.role-main { background: #fff3cd; color: #856404; }
.role-support { background: #d1ecf1; color: #0c5460; }
.role-npc { background: #e2e3e5; color: #383d41; }

.char-popup-section {
  margin-bottom: 1rem;
}
.char-popup-section label {
  display: block;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.char-popup-section p {
  font-size: 0.9rem;
  line-height: 1.6;
  color: #444;
  margin: 0;
  white-space: pre-wrap;
}
</style>
