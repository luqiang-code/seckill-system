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
  { label: '全部', value: STATUS_ALL },
  { label: '待支付', value: STATUS_PENDING },
  { label: '已支付', value: STATUS_PAID },
  { label: '已取消', value: STATUS_CANCELLED },
] as const

const activeTab = ref(STATUS_ALL)
const orders = ref<OrderInfo[]>([])
const loading = ref(true)
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

const statusLabel = (s: number) => {
  switch (s) {
    case STATUS_PENDING: return '待支付'
    case STATUS_PAID: return '已支付'
    case STATUS_CANCELLED: return '已取消'
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
    alert(e instanceof ApiError ? e.message : '支付失败')
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
  <div class="page">
    <header class="header">
      <button class="back-btn" @click="router.push('/')">← 返回</button>
      <h1>我的订单</h1>
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

    <div v-if="loading" class="state-box">加载中...</div>
    <div v-else-if="orders.length === 0" class="state-box empty">暂无订单</div>

    <div v-else class="order-list">
      <div v-for="o in orders" :key="o.id" class="order-card">
        <div class="order-head">
          <span class="goods-name">{{ o.goodsName }}</span>
          <span class="status-badge" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span>
        </div>
        <div class="order-body">
          <div class="order-info">
            <span>订单号: {{ o.id }}</span>
            <span>时间: {{ formatTime(o.createTime) }}</span>
            <span v-if="o.status === 0 && paymentLeft(o) > 0" class="countdown-tip">
              剩余 <strong>{{ formatCountdown(paymentLeft(o)) }}</strong>
            </span>
          </div>
          <div class="order-actions">
            <button
              v-if="o.status === 0"
              class="pay-btn"
              @click="handlePay(o)"
            >立即支付</button>
            <button
              class="detail-btn"
              @click="router.push(`/goods/${o.goodsId}`)"
            >查看商品</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40px; }

.header {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header h1 { font-size: 18px; margin: 0; }
.spacer { width: 60px; }
.back-btn {
  background: none; border: none; color: #fff; font-size: 15px;
  cursor: pointer; padding: 4px 0;
}

.tabs {
  max-width: 600px; margin: 16px auto 0; padding: 0 16px;
  display: flex; gap: 8px;
}
.tab-btn {
  flex: 1; padding: 10px 0; border: none; border-radius: 8px;
  font-size: 14px; font-weight: 600; cursor: pointer;
  background: #fff; color: #666; transition: all 0.15s;
}
.tab-btn.active {
  background: #e74c3c; color: #fff;
}

.state-box { text-align: center; padding: 60px 20px; color: #999; font-size: 15px; }
.state-box.empty { color: #ccc; }

.order-list {
  max-width: 600px; margin: 16px auto 0; padding: 0 16px;
  display: flex; flex-direction: column; gap: 12px;
}
.order-card {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.order-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 10px;
}
.goods-name { font-size: 16px; font-weight: 600; color: #333; }
.status-badge {
  font-size: 12px; padding: 3px 10px; border-radius: 10px; font-weight: 600;
}
.status-badge.pending { background: #fff3e0; color: #e65100; }
.status-badge.paid { background: #e8f5e9; color: #2e7d32; }
.status-badge.cancelled { background: #f5f5f5; color: #999; }

.order-body {
  display: flex; justify-content: space-between; align-items: center;
}
.order-info { font-size: 13px; color: #999; display: flex; flex-direction: column; gap: 2px; }
.countdown-tip { color: #e65100; }
.countdown-tip strong { font-weight: 700; }
.order-actions { display: flex; gap: 8px; align-items: center; }
.pay-btn {
  padding: 6px 16px; border: none; border-radius: 6px;
  background: #ff9800; font-size: 13px; font-weight: 600;
  color: #fff; cursor: pointer; white-space: nowrap;
}
.pay-btn:active { transform: scale(0.96); }
.detail-btn {
  padding: 6px 16px; border: 1px solid #e74c3c; border-radius: 6px;
  background: #fff; font-size: 13px; color: #e74c3c; cursor: pointer;
  white-space: nowrap;
}
</style>
