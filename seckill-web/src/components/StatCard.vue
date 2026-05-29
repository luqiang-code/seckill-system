<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  color: 'green' | 'red' | 'blue' | 'yellow'
}>()
</script>

<template>
  <div class="card">
    <div class="label">{{ label }}</div>
    <div class="value" :class="color">{{ value }}</div>
  </div>
</template>

<style scoped>
.card {
  background: #16213e;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  border: 1px solid #0f3460;
}

.label {
  font-size: 13px;
  color: #888;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.value {
  font-size: 42px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.value.green { color: #2ecc71; }
.value.red { color: #e74c3c; }
.value.blue { color: #3498db; }
.value.yellow { color: #f1c40f; }
</style>
