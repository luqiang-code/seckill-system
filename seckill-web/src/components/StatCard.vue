<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  color: 'green' | 'red' | 'blue' | 'yellow'
}>()
</script>

<template>
  <div class="stat-card">
    <div class="stat-inner">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value" :class="color">{{ value }}</div>
    </div>
    <div class="stat-ornament top-left"></div>
    <div class="stat-ornament bottom-right"></div>
  </div>
</template>

<style scoped>
.stat-card {
  position: relative;
  background: linear-gradient(135deg, #1e1610, #2a1f14);
  border: 1px solid #3d2820;
  border-radius: 2px;
  padding: 24px;
  text-align: center;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23D4AF37' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
}

.stat-inner { position: relative; z-index: 1; }

.stat-label {
  font-family: 'Pirata One', serif;
  font-size: 14px;
  color: #b8a080;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  margin-bottom: 10px;
}

.stat-value {
  font-family: 'Pirata One', serif;
  font-size: 44px;
  font-weight: 400;
  letter-spacing: 0.04em;
  text-shadow: 0 0 12px currentColor;
}

.stat-value.green  { color: #2ecc71; }
.stat-value.red    { color: #e74c3c; }
.stat-value.blue   { color: #5dade2; }
.stat-value.yellow { color: #D4AF37; }

.stat-ornament {
  position: absolute;
  width: 12px; height: 12px;
  border-color: #D4AF37;
  border-style: solid;
  opacity: 0.4;
}
.stat-ornament.top-left     { top: 6px; left: 6px; border-width: 1px 0 0 1px; }
.stat-ornament.bottom-right { bottom: 6px; right: 6px; border-width: 0 1px 1px 0; }
</style>
