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
  <div class="potion-bar">
    <div class="potion-label">
      <span>🧪 {{ label }}</span>
      <span class="potion-val">{{ current }}{{ suffix }}</span>
    </div>
    <div class="potion-track">
      <div class="potion-liquid"></div>
      <div
        class="potion-fill"
        :class="colorClass"
        :style="{ width: Math.min(100, (Number(current) / max) * 100) + '%' }"
      >
        <div class="potion-bubbles"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.potion-bar {
  margin-bottom: 16px;
}

.potion-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #c8b898;
  font-style: italic;
}

.potion-val {
  color: #D4AF37;
  font-weight: 700;
  font-style: normal;
}

.potion-track {
  height: 28px;
  background: #1a1008;
  border: 1px solid #3d2820;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.potion-liquid {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(212,175,55,0.05) 0%, transparent 100%);
  pointer-events: none;
}

.potion-fill {
  height: 100%;
  border-radius: 3px;
  position: relative;
  transition: width 0.4s ease;
  overflow: hidden;
}

.potion-fill::after {
  content: '';
  position: absolute;
  top: 2px; left: 4px; right: 4px; height: 4px;
  background: rgba(255,255,255,0.2);
  border-radius: 2px;
}

.potion-bubbles {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 70%, rgba(255,255,255,0.15) 1px, transparent 1px),
    radial-gradient(circle at 60% 40%, rgba(255,255,255,0.1) 2px, transparent 2px),
    radial-gradient(circle at 80% 60%, rgba(255,255,255,0.12) 1px, transparent 1px);
}

.potion-fill.sold   { background: linear-gradient(90deg, #740001, #c0392b); }
.potion-fill.remain { background: linear-gradient(90deg, #1a5632, #2ecc71); }
.potion-fill.rate   { background: linear-gradient(90deg, #0d1b3e, #5dade2); }
</style>
