<script setup lang="ts">
import { ref, watch } from 'vue'
import CountdownBar from '../components/CountdownBar.vue'
import GoodsGrid from '../components/GoodsGrid.vue'
import { useCountdown } from '../composables/useCountdown'
import { usePolling } from '../composables/usePolling'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { fetchGoods, fetchGoodsDetail, fetchRecentOrders } from '../api'
import type { Goods } from '../api/types'
import { WINDOW_DURATION, INTERVAL_DURATION } from '../api/constants'

const router = useRouter()

const { remaining, status, isWindowOpen } = useCountdown(WINDOW_DURATION, INTERVAL_DURATION)
const { data: goodsList } = usePolling<Goods[]>(fetchGoods, 3000)
const { username, logout } = useAuth()

const initialStocks = ref<Record<number, number>>({})
const buyerList = ref<{ userId: string; goodsName: string; createTime: string }[]>([])

// Load initial stocks for progress bar reference
watch(goodsList, (list) => {
  if (!list) return
  list.forEach(g => {
    if (!(g.id in initialStocks.value)) {
      fetchGoodsDetail(g.id).then(d => {
        initialStocks.value = { ...initialStocks.value, [g.id]: d.initialStock }
      }).catch(() => {})
    }
  })
})

async function refreshOrders() {
  const all: { userId: string; goodsName: string; createTime: string }[] = []
  if (goodsList.value) {
    for (const g of goodsList.value) {
      try {
        const orders = await fetchRecentOrders(g.id, 5)
        all.push(...orders.map(o => ({ ...o, goodsName: g.name })))
      } catch { /* ignore */ }
    }
  }
  all.sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
  buyerList.value = all.slice(0, 24)
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

    <div class="toolbar">
      <span class="user-label">{{ username }}</span>
      <div class="toolbar-actions">
        <span class="goods-count" v-if="goodsList">{{ goodsList.length }} 款商品</span>
        <button class="nav-btn" @click="refreshOrders">刷新记录</button>
        <button class="nav-btn" @click="router.push('/orders')">我的订单</button>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </div>

    <GoodsGrid
      :goods-list="goodsList ?? []"
      :initial-stocks="initialStocks"
      :disabled="!isWindowOpen"
      @sold="onSold"
    />

    <div v-if="buyerList.length" class="buyer-panel">
      <h3>最新抢购记录</h3>
      <div class="buyer-marquee">
        <div class="buyer-list" v-for="round in 2" :key="round">
          <span
            v-for="(b, idx) in buyerList"
            :key="round + '-' + idx"
            class="buyer-tag"
          >{{ b.goodsName }} · {{ b.userId.slice(0, 8) }}***</span>
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

.toolbar {
  max-width: 960px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-label {
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.goods-count {
  font-size: 12px;
  color: #999;
  margin-right: 4px;
}

.nav-btn {
  padding: 6px 16px;
  border: 1px solid #e74c3c;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #e74c3c;
  cursor: pointer;
  transition: all 0.15s;
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
  max-width: 960px;
  margin: 30px auto;
  padding: 0 16px;
}

.buyer-panel h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 12px;
}

.buyer-marquee {
  background: #fff;
  border-radius: 10px;
  padding: 10px 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
  white-space: nowrap;
}

.buyer-list {
  display: inline-flex;
  gap: 10px;
  animation: marquee 30s linear infinite;
  padding: 0 10px;
}

.buyer-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  white-space: nowrap;
}

@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}
</style>
