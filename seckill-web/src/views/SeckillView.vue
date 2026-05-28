<script setup lang="ts">
import CountdownBar from '../components/CountdownBar.vue'
import GoodsGrid from '../components/GoodsGrid.vue'
import { useCountdown } from '../composables/useCountdown'
import { usePolling } from '../composables/usePolling'
import { useAuth } from '../composables/useAuth'
import { fetchGoods } from '../api'
import type { Goods } from '../api/types'

const WINDOW_DURATION = 10
const INTERVAL_DURATION = 25

const { remaining, status, isWindowOpen } = useCountdown(WINDOW_DURATION, INTERVAL_DURATION)
const { data: goodsList } = usePolling<Goods[]>(fetchGoods, 5000)
const { username, logout } = useAuth()

function onSold() {
  fetchGoods().then(list => {
    if (list) goodsList.value = list
  })
}
</script>

<template>
  <div class="page">
    <header class="header">
      <h1>秒杀抢购</h1>
      <div class="subtitle">限时特惠 · 手慢无</div>
    </header>

    <CountdownBar :remaining="remaining" :status="status" />

    <div class="user-bar">
      <span class="user-label">{{ username }}</span>
      <button class="logout-btn" @click="logout">退出</button>
    </div>

    <GoodsGrid
      :goods-list="goodsList ?? []"
      :disabled="!isWindowOpen"
      @sold="onSold"
    />
  </div>
</template>

<style scoped>
.header {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  padding: 30px 20px;
  text-align: center;
}

.header h1 {
  font-size: 28px;
  margin-bottom: 4px;
}

.header .subtitle {
  font-size: 13px;
  opacity: 0.8;
}

.user-bar {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-label {
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.logout-btn {
  padding: 6px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.logout-btn:hover {
  border-color: #e74c3c;
  color: #e74c3c;
}
</style>
