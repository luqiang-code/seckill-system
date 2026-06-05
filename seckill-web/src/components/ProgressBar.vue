<script setup lang="ts">
defineProps<{
  label: string
  current: string | number
  suffix: string
  max: number
  colorClass: string
}>()
</script>

<template>
  <div class="bar-wrap">
    <div class="bar-label">
      <span>{{ label }}</span>
      <span>{{ current }}{{ suffix }}</span>
    </div>
    <div class="bar-track">
      <div
        class="bar-fill"
        :class="colorClass"
        :style="{ width: Math.min(100, (Number(current) / max) * 100) + '%' }"
      />
    </div>
  </div>
</template>

<style scoped>
.bar-wrap {
  margin-bottom: 12px;
}

.bar-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 13px;
}

.bar-label span:last-child {
  color: #888;
}

.bar-track {
  height: 28px;
  background: #0f3460;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}

.bar-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s;
}

.bar-fill.sold { background: linear-gradient(90deg, #e74c3c, #c0392b); }
.bar-fill.remain { background: linear-gradient(90deg, #2ecc71, #27ae60); }
.bar-fill.rate { background: linear-gradient(90deg, #3498db, #2980b9); }
</style>
