<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMyOrders, payOrder, ApiError } from '../api'
import type { OrderInfo } from '../api/types'
import { PAYMENT_DEADLINE_MS, formatCountdown } from '../api/constants'

const router = useRouter()

const STATUS_ALL = -1
const STATUS_PENDING = 0
const STATUS_PAID = 1
const STATUS_CANCELLED = 2

const tabs = [
  { label: '全部契约', value: STATUS_ALL },
  { label: '待献金币', value: STATUS_PENDING },
  { label: '金币已献', value: STATUS_PAID },
  { label: '已解除', value: STATUS_CANCELLED },
] as const

const activeTab = ref(STATUS_ALL)
const orders = ref<OrderInfo[]>([])
const loading = ref(true)
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const statusLabel = (s: number) => {
  switch (s) {
    case STATUS_PENDING: return '待献金币'
    case STATUS_PAID: return '金币已献'
    case STATUS_CANCELLED: return '已解除'
    default: return '未知'
  }
}

const statusClass = (s: number) => {
  switch (s) {
    case STATUS_PENDING: return 'pending'
    case STATUS_PAID: return 'paid'
    case STATUS_CANCELLED: return 'cancelled'
    default: return ''
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const status = activeTab.value === STATUS_ALL ? null : activeTab.value
    orders.value = await fetchMyOrders(status)
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(val: number) {
  activeTab.value = val
  loadOrders()
}

function formatTime(ts: string) {
  const d = new Date(ts)
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString()
}

function paymentLeft(order: OrderInfo) {
  const deadline = new Date(order.createTime).getTime() + PAYMENT_DEADLINE_MS
  return Math.max(0, Math.floor((deadline - now.value) / 1000))
}

async function handlePay(order: OrderInfo) {
  try {
    await payOrder(order.id)
    order.status = 1
  } catch (e) {
    alert(e instanceof ApiError ? e.message : '魔法支付失败')
  }
}

onMounted(() => {
  loadOrders()
  timer = setInterval(() => { now.value = Date.now() }, 200)
})

onUnmounted(() => {
  if (timer) { clearInterval(timer); timer = null }
})
</script>

<template>
  <div class="contracts-page">
    <header class="contracts-header">
      <button class="back-btn" @click="router.push('/')">← 返回对角巷</button>
      <h1>我的契约</h1>
      <div class="spacer"></div>
    </header>

    <div class="tabs">
      <button
        v-for="t in tabs"
        :key="t.value"
        class="tab-btn"
        :class="{ active: activeTab === t.value }"
        @click="switchTab(t.value)"
      >{{ t.label }}</button>
    </div>

    <div v-if="loading" class="state-box">✦ 翻阅契约中...</div>
    <div v-else-if="orders.length === 0" class="state-box empty">暂无魔法契约</div>

    <div v-else class="contract-list">
      <div v-for="o in orders" :key="o.id" class="contract-scroll">
        <div class="scroll-seal">⚡</div>
        <div class="scroll-head">
          <span class="goods-name">{{ o.goodsName }}</span>
          <span class="status-badge" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span>
        </div>
        <div class="scroll-body">
          <div class="scroll-info">
            <span>契约编号: {{ o.id }}</span>
            <span>立契时间: {{ formatTime(o.createTime) }}</span>
            <span v-if="o.status === 0 && paymentLeft(o) > 0" class="countdown-tip">
              余 <strong>{{ formatCountdown(paymentLeft(o)) }}</strong>
            </span>
          </div>
          <div class="scroll-actions">
            <button
              v-if="o.status === 0"
              class="pay-btn"
              @click="handlePay(o)"
            >🪙 献金币</button>
            <button
              class="detail-btn"
              @click="router.push(`/goods/${o.goodsId}`)"
            >查看物品</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contracts-page {
  min-height: 100vh;
  padding-bottom: 40px;
}

/* header */
.contracts-header {
  background: linear-gradient(180deg, #1e1610, #2c1f14);
  border-bottom: 2px solid #5c0000;
  color: #D4AF37;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.contracts-header h1 {
  font-family: 'Pirata One', serif;
  font-size: 22px;
  letter-spacing: 0.12em;
  margin: 0;
}

.spacer { width: 80px; }

.back-btn {
  background: none;
  border: 1px solid #5c0000;
  border-radius: 2px;
  color: #c8b898;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 14px;
  transition: all 0.15s;
}

.back-btn:hover {
  border-color: #D4AF37;
  color: #D4AF37;
}

/* tabs */
.tabs {
  max-width: 600px;
  margin: 16px auto 0;
  padding: 0 16px;
  display: flex;
  gap: 8px;
}

.tab-btn {
  flex: 1;
  padding: 10px 0;
  border: 1px solid #3d2820;
  border-radius: 2px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  cursor: pointer;
  background: #1e1610;
  color: #8b7355;
  transition: all 0.15s;
}

.tab-btn.active {
  background: #5c0000;
  border-color: #8b0000;
  color: #D4AF37;
}

.tab-btn:hover:not(.active) {
  border-color: #D4AF37;
  color: #c8b898;
}

/* state */
.state-box {
  text-align: center;
  padding: 60px 20px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 18px;
  color: #8b7355;
  font-style: italic;
}
.state-box.empty { color: #665540; }

/* contract list */
.contract-list {
  max-width: 600px;
  margin: 16px auto 0;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contract-scroll {
  background: linear-gradient(180deg, #2c1f14, #1e1610);
  border: 1px solid #3d2820;
  border-radius: 4px;
  padding: 18px;
  position: relative;
}

.scroll-seal {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 20px;
  opacity: 0.3;
}

.scroll-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.goods-name {
  font-family: 'Pirata One', serif;
  font-size: 18px;
  color: #D4AF37;
  letter-spacing: 0.05em;
}

.status-badge {
  font-family: 'Cormorant Garamond', serif;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 2px;
  font-weight: 600;
}

.status-badge.pending   { background: rgba(212, 175, 55, 0.1); color: #D4AF37; border: 1px solid rgba(212,175,55,0.3); }
.status-badge.paid      { background: rgba(46, 204, 113, 0.1); color: #2ecc71; border: 1px solid rgba(46,204,113,0.3); }
.status-badge.cancelled { background: rgba(102, 85, 64, 0.2); color: #665540; border: 1px solid #3d2820; }

.scroll-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.scroll-info {
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #8b7355;
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-style: italic;
}

.countdown-tip { color: #D4AF37; }
.countdown-tip strong { font-family: 'Pirata One', serif; font-style: normal; }

.scroll-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pay-btn {
  padding: 6px 16px;
  border: 1px solid #8b6914;
  border-radius: 2px;
  font-family: 'Pirata One', serif;
  font-size: 14px;
  letter-spacing: 0.04em;
  cursor: pointer;
  color: #3d1c00;
  background: linear-gradient(180deg, #D4AF37, #b8941a);
  white-space: nowrap;
  transition: all 0.15s;
}

.pay-btn:hover {
  box-shadow: 0 0 12px rgba(212, 175, 55, 0.3);
}

.pay-btn:active { transform: scale(0.96); }

.detail-btn {
  padding: 6px 16px;
  border: 1px solid #5c0000;
  border-radius: 2px;
  background: #1e1610;
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #c8b898;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}

.detail-btn:hover {
  border-color: #D4AF37;
  color: #D4AF37;
}
</style>
