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
  <div ref="el" class="log-box">{{ lines.join('\n') }}</div>
</template>

<style scoped>
.log-box {
  max-width: 900px;
  margin: 0 auto;
  background: #0d1117;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #30363d;
  font-family: 'SF Mono', monospace;
  font-size: 13px;
  height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  color: #8b949e;
}
</style>
