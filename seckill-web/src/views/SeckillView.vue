<script setup lang="ts">
import { ref } from 'vue'
import CountdownBar from '../components/CountdownBar.vue'
import GoodsGrid from '../components/GoodsGrid.vue'
import { useCountdown } from '../composables/useCountdown'
import { usePolling } from '../composables/usePolling'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { fetchGoods, fetchRecentOrders } from '../api'
import type { Goods } from '../api/types'

const router = useRouter()
const WINDOW_DURATION = 10
const INTERVAL_DURATION = 25

const { remaining, status, isWindowOpen } = useCountdown(WINDOW_DURATION, INTERVAL_DURATION)
const { data: goodsList } = usePolling<Goods[]>(fetchGoods, 5000)
const { username, logout } = useAuth()

const buyerList = ref<{ userId: string; createTime: string }[]>([])

async function refreshOrders() {
  const all: { userId: string; createTime: string }[] = []
  if (goodsList.value) {
    for (const g of goodsList.value) {
      try {
        const orders = await fetchRecentOrders(g.id, 10)
        all.push(...orders.map(o => ({ ...o, goodsName: g.name } as any)))
      } catch { /* ignore */ }
    }
  }
  all.sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
  buyerList.value = all.slice(0, 20)
}

async function handleLogout() {
  await logout()
  router.push({ name: 'login', query: { reason: 'logout' } })
}

function onSold() {
  fetchGoods().then(list => {
    if (list) goodsList.value = list
  })
  refreshOrders()
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
      <div class="user-actions">
        <button class="nav-btn" @click="refreshOrders">刷新记录</button>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </div>

    <GoodsGrid
      :goods-list="goodsList ?? []"
      :disabled="!isWindowOpen"
      @sold="onSold"
    />

    <div v-if="buyerList.length" class="buyer-panel">
      <h3>抢购记录</h3>
      <div class="buyer-list">
        <div v-for="(b, idx) in buyerList" :key="idx" class="buyer-row">
          <span class="buyer-idx">{{ idx + 1 }}</span>
          <span class="buyer-goods">{{ (b as any).goodsName || '' }}</span>
          <span class="buyer-user">{{ b.userId }}</span>
          <span class="buyer-time">{{ new Date(b.createTime).toLocaleTimeString() }}</span>
        </div>
      </div>
    </div>
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

.user-actions {
  display: flex;
  gap: 8px;
}

.nav-btn {
  padding: 6px 16px;
  border: 1px solid #e74c3c;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #e74c3c;
  cursor: pointer;
}

.nav-btn:hover {
  background: #e74c3c;
  color: #fff;
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

.buyer-panel {
  max-width: 720px;
  margin: 30px auto;
  padding: 0 16px;
}

.buyer-panel h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 12px;
}

.buyer-list {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
}

.buyer-row {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.buyer-row:last-child {
  border-bottom: none;
}

.buyer-idx {
  width: 24px;
  font-weight: 700;
  color: #e74c3c;
  font-size: 13px;
}

.buyer-goods {
  flex: 1;
  color: #333;
  font-weight: 500;
}

.buyer-user {
  color: #666;
  margin-right: 16px;
}

.buyer-time {
  color: #999;
  font-size: 13px;
}
</style>
