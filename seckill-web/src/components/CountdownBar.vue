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
  <div class="countdown-bar">
    <div class="label">距离本场结束</div>
    <div class="timer" :class="{ closed: status === 'waiting' }">{{ formatTime(remaining) }}</div>
    <div class="status" :class="status">
      {{ status === 'open' ? '秒杀进行中' : '等待下一轮' }}
    </div>
  </div>
</template>

<style scoped>
.countdown-bar {
  background: #fff;
  padding: 16px 20px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.label {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.timer {
  font-size: 36px;
  font-weight: 700;
  color: #e74c3c;
  font-variant-numeric: tabular-nums;
}

.timer.closed {
  color: #999;
}

.status {
  font-size: 14px;
  font-weight: 600;
}

.status.open {
  color: #e74c3c;
}

.status.waiting {
  color: #666;
}
</style>
