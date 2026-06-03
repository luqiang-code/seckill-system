<script setup lang="ts">
import { watch, ref, nextTick } from 'vue'

const props = defineProps<{
  lines: string[]
}>()

const el = ref<HTMLElement>()

watch(() => props.lines.length, () => {
  nextTick(() => {
    if (el.value) el.value.scrollTop = el.value.scrollHeight
  })
})
</script>

<template>
  <div ref="el" class="tome-box">
    <div class="tome-header">📜 魔法日志书</div>
    <div class="tome-content">{{ lines.join('\n') }}</div>
  </div>
</template>

<style scoped>
.tome-box {
  max-width: 900px;
  margin: 0 auto;
  background: linear-gradient(180deg, #0d0a06 0%, #1a1008 100%);
  border: 2px solid #3d2820;
  border-radius: 4px;
  overflow: hidden;
}

.tome-header {
  font-family: 'Pirata One', serif;
  font-size: 15px;
  color: #D4AF37;
  padding: 12px 20px;
  border-bottom: 1px solid #3d2820;
  background: #1a1008;
  letter-spacing: 0.08em;
}

.tome-content {
  padding: 16px 20px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  color: #8b8b6b;
  line-height: 1.6;
}
</style>
