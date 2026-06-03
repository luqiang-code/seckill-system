<script setup lang="ts">
defineProps<{
  remaining: number
  status: 'open' | 'waiting'
}>()

function formatTime(seconds: number): string {
  if (seconds <= 0) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
}
</script>

<template>
  <div class="hourglass-bar">
    <div class="hourglass-glow"></div>
    <div class="hourglass-inner">
      <div class="hourglass-label">⚗️ 距离本场结束</div>
      <div class="hourglass-timer" :class="{ expired: status === 'waiting' }">
        {{ formatTime(remaining) }}
      </div>
      <div class="hourglass-status" :class="status">
        <span class="status-icon">{{ status === 'open' ? '🪄' : '🔮' }}</span>
        {{ status === 'open' ? '魔法抢购进行中' : '下一轮魔法即将开启' }}
      </div>
    </div>
    <div class="hourglass-border top-left"></div>
    <div class="hourglass-border top-right"></div>
    <div class="hourglass-border bottom-left"></div>
    <div class="hourglass-border bottom-right"></div>
  </div>
</template>

<style scoped>
.hourglass-bar {
  position: relative;
  max-width: 600px;
  margin: 20px auto;
  padding: 28px 24px;
  text-align: center;
  background: linear-gradient(135deg, #1e1610 0%, #2c1f14 30%, #3d2820 60%, #2c1f14 100%);
  border: 2px solid #5c0000;
  border-radius: 4px;
  overflow: hidden;
}

.hourglass-glow {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: radial-gradient(ellipse at 50% 0%, rgba(212, 175, 55, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

.hourglass-inner {
  position: relative;
  z-index: 1;
}

.hourglass-border {
  position: absolute;
  width: 20px; height: 20px;
  border-color: #D4AF37;
  border-style: solid;
}
.hourglass-border.top-left    { top: 4px; left: 4px; border-width: 2px 0 0 2px; }
.hourglass-border.top-right   { top: 4px; right: 4px; border-width: 2px 2px 0 0; }
.hourglass-border.bottom-left { bottom: 4px; left: 4px; border-width: 0 0 2px 2px; }
.hourglass-border.bottom-right{ bottom: 4px; right: 4px; border-width: 0 2px 2px 0; }

.hourglass-label {
  font-family: 'Cormorant Garamond', serif;
  font-size: 15px;
  color: #b8a080;
  margin-bottom: 8px;
  font-style: italic;
}

.hourglass-timer {
  font-family: 'Pirata One', serif;
  font-size: 48px;
  font-weight: 400;
  color: #D4AF37;
  text-shadow: 0 0 20px rgba(212, 175, 55, 0.3);
  letter-spacing: 0.06em;
}

.hourglass-timer.expired {
  color: #665540;
  text-shadow: none;
}

.hourglass-status {
  font-family: 'Pirata One', serif;
  font-size: 16px;
  margin-top: 8px;
  letter-spacing: 0.05em;
}

.hourglass-status .status-icon {
  margin-right: 6px;
}

.hourglass-status.open {
  color: #D4AF37;
}

.hourglass-status.waiting {
  color: #8b7355;
}
</style>
