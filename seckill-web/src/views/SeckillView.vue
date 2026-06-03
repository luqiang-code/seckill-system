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
  <div class="diagon-page">
    <!-- Header -->
    <header class="diagon-header">
      <div class="header-ornament left"></div>
      <div>
        <h1 class="header-title">对角巷</h1>
        <div class="header-sub">Diagon Alley · 魔法限时抢购</div>
      </div>
      <div class="header-ornament right"></div>
    </header>

    <CountdownBar :remaining="remaining" :status="status" />

    <!-- Toolbar -->
    <div class="diagon-toolbar">
      <span class="wizard-label">🧙 {{ username }}</span>
      <div class="toolbar-actions">
        <span class="goods-count" v-if="goodsList">{{ goodsList.length }} 件魔法物品</span>
        <button class="tool-btn" @click="refreshOrders">🪄 刷新记录</button>
        <button class="tool-btn" @click="router.push('/orders')">📜 我的契约</button>
        <button class="tool-btn danger" @click="handleLogout">离开对角巷</button>
      </div>
    </div>

    <!-- Goods -->
    <GoodsGrid
      :goods-list="goodsList ?? []"
      :initial-stocks="initialStocks"
      :disabled="!isWindowOpen"
      @sold="onSold"
    />

    <!-- Buyer scroll -->
    <div v-if="buyerList.length" class="buyer-section">
      <h3 class="buyer-title">✦ 最新魔法交易 ✦</h3>
      <div class="buyer-marquee">
        <div class="buyer-track" v-for="round in 2" :key="round">
          <span v-for="(b, idx) in buyerList" :key="round + '-' + idx" class="buyer-tag">
            {{ b.goodsName }} · {{ b.userId.slice(0, 8) }}...
          </span>
        </div>
      </div>
    </div>

    <footer class="diagon-footer">
      <span>⚡</span> 破釜酒吧 · 敲砖入巷 · 魔法世界由此开启 <span>⚡</span>
    </footer>
  </div>
</template>

<style scoped>
.diagon-page {
  min-height: 100vh;
  padding-bottom: 40px;
}

/* ---- header ---- */
.diagon-header {
  background: linear-gradient(180deg, #1e1610, #2c1f14, #1e1610);
  color: #D4AF37;
  padding: 28px 20px;
  text-align: center;
  border-bottom: 2px solid #5c0000;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.header-ornament {
  width: 60px; height: 2px;
  background: linear-gradient(90deg, transparent, #D4AF37, transparent);
}

.header-title {
  font-family: 'Pirata One', serif;
  font-size: 38px;
  letter-spacing: 0.2em;
  margin: 0;
  text-shadow: 0 0 20px rgba(212, 175, 55, 0.3);
}

.header-sub {
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #b8a080;
  margin-top: 4px;
  font-style: italic;
}

/* ---- toolbar ---- */
.diagon-toolbar {
  max-width: 1000px;
  margin: 0 auto;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.wizard-label {
  font-family: 'Pirata One', serif;
  font-size: 16px;
  color: #D4AF37;
  letter-spacing: 0.05em;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.goods-count {
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #8b7355;
  margin-right: 4px;
  font-style: italic;
}

.tool-btn {
  padding: 7px 16px;
  border: 1px solid #5c0000;
  border-radius: 2px;
  background: #1e1610;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #c8b898;
  cursor: pointer;
  transition: all 0.15s;
}

.tool-btn:hover {
  border-color: #D4AF37;
  color: #D4AF37;
}

.tool-btn.danger {
  border-color: #3d2820;
  color: #8b7355;
}

.tool-btn.danger:hover {
  border-color: #740001;
  color: #e74c3c;
}

/* ---- buyer scroll ---- */
.buyer-section {
  max-width: 1000px;
  margin: 30px auto 0;
  padding: 0 16px;
}

.buyer-title {
  font-family: 'Pirata One', serif;
  font-size: 16px;
  color: #b8a080;
  text-align: center;
  margin-bottom: 12px;
  letter-spacing: 0.1em;
}

.buyer-marquee {
  background: #1e1610;
  border: 1px solid #3d2820;
  border-radius: 2px;
  padding: 10px 0;
  overflow: hidden;
  white-space: nowrap;
}

.buyer-track {
  display: inline-flex;
  gap: 10px;
  animation: marquee 30s linear infinite;
  padding: 0 10px;
}

.buyer-tag {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 2px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #b8a080;
  background: #2c1f14;
  border: 1px solid #3d2820;
  white-space: nowrap;
  font-style: italic;
}

@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* ---- footer ---- */
.diagon-footer {
  text-align: center;
  margin-top: 40px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #665540;
  font-style: italic;
}

.diagon-footer span {
  color: #D4AF37;
  opacity: 0.4;
}
</style>
