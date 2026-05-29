<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCountdown } from '../composables/useCountdown'
import { useAuth } from '../composables/useAuth'
import { fetchGoodsDetail, fetchRecentOrders, doSeckill, fetchResult, payOrder, ApiError } from '../api'
import { PRODUCT_META } from '../api/types'
import type { OrderInfo } from '../api/types'

const route = useRoute()
const router = useRouter()
const WINDOW_DURATION = 10
const INTERVAL_DURATION = 25

const { isWindowOpen } = useCountdown(WINDOW_DURATION, INTERVAL_DURATION)
const { username } = useAuth()

const goodsId = Number(route.params.id)

const detail = ref<{ id: number; name: string; price: number; initialStock: number; currentStock: number; sold: number } | null>(null)
const buyerList = ref<any[]>([])
const loading = ref(true)
const errorMsg = ref('')

const PAYMENT_DEADLINE_MS = 15 * 60 * 1000
const DEBOUNCE_MS = 1000
const resultType = ref<'success' | 'fail' | 'info' | ''>('')
const resultMsg = ref('')
const orderInfo = ref<OrderInfo | null>(null)
const paymentLeft = ref(0)
let cooldownUntil = 0
let paymentTimer: ReturnType<typeof setInterval> | null = null

const soldPct = ref(0)
const stockPct = ref(100)
const meta = computed(() => PRODUCT_META[goodsId] ?? { emoji: '📦', color: '#f5f5f5' })

async function loadDetail() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [d, orders] = await Promise.all([
      fetchGoodsDetail(goodsId),
      fetchRecentOrders(goodsId, 15),
    ])
    detail.value = d
    buyerList.value = orders
    const total = d.initialStock
    soldPct.value = total > 0 ? Math.round((d.sold / total) * 100) : 0
    stockPct.value = total > 0 ? Math.round((d.currentStock / total) * 100) : 0
  } catch (e) {
    errorMsg.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function refreshOrders() {
  fetchRecentOrders(goodsId, 15).then(orders => {
    buyerList.value = orders
  }).catch(() => {})
}

function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m + '分' + s + '秒'
}

function startPaymentCountdown() {
  if (!orderInfo.value) return
  const deadline = new Date(orderInfo.value.createTime).getTime() + PAYMENT_DEADLINE_MS
  stopPaymentCountdown()
  paymentTimer = setInterval(() => {
    const remain = Math.max(0, Math.floor((deadline - Date.now()) / 1000))
    paymentLeft.value = remain
    if (remain <= 0) {
      stopPaymentCountdown()
      resultType.value = 'fail'
      resultMsg.value = '订单已超时取消'
      orderInfo.value = null
    }
  }, 200)
}

function stopPaymentCountdown() {
  if (paymentTimer) { clearInterval(paymentTimer); paymentTimer = null }
}

async function handlePay() {
  if (!orderInfo.value) return
  try {
    await payOrder(orderInfo.value.id)
    resultType.value = 'success'
    resultMsg.value = '支付成功！订单号: ' + (orderInfo.value.id ?? '')
    stopPaymentCountdown()
    paymentLeft.value = 0
  } catch (e) {
    resultType.value = 'fail'
    resultMsg.value = e instanceof ApiError ? e.message : '支付失败'
  }
}

async function handleSeckill() {
  const now = Date.now()
  if (now < cooldownUntil) return
  cooldownUntil = now + DEBOUNCE_MS
  clearResult()

  try {
    await doSeckill(goodsId)
    resultType.value = 'success'
    resultMsg.value = '抢到了！'
    pollResult()
    loadDetail()
    refreshOrders()
  } catch (e) {
    if (e instanceof ApiError) {
      switch (e.code) {
        case 429: resultType.value = 'info'; resultMsg.value = '请求太频繁'; break
        case 2: resultType.value = 'info'; resultMsg.value = '已抢过'; break
        case 0: resultType.value = 'fail'; resultMsg.value = '已售罄'; break
        default: resultType.value = 'fail'; resultMsg.value = e.message
      }
    } else {
      resultType.value = 'fail'; resultMsg.value = '网络错误'
    }
  }
}

function pollResult() {
  const check = () => {
    fetchResult(goodsId)
      .then(order => {
        orderInfo.value = order
        resultMsg.value = '订单号: ' + order.id
        startPaymentCountdown()
      })
      .catch(err => {
        if (err instanceof ApiError && err.code === 3) setTimeout(check, 500)
      })
  }
  setTimeout(check, 300)
}

function clearResult() {
  resultType.value = ''
  resultMsg.value = ''
  orderInfo.value = null
  paymentLeft.value = 0
  stopPaymentCountdown()
}

onMounted(loadDetail)
onUnmounted(() => stopPaymentCountdown())
</script>

<template>
  <div class="page">
    <header class="header">
      <button class="back-btn" @click="router.push('/')">← 返回</button>
      <h1>商品详情</h1>
      <div class="user-info">{{ username }}</div>
    </header>

    <div v-if="loading" class="state-box">加载中...</div>
    <div v-else-if="errorMsg" class="state-box error">{{ errorMsg }}</div>

    <template v-else-if="detail">
      <div class="detail-card">
        <div class="hero">
          <div class="hero-icon" :style="{ background: meta.color }">
            {{ meta.emoji }}
          </div>
        </div>

        <div class="info-section">
          <h2 class="goods-name">{{ detail.name }}</h2>
          <div class="price-row">
            <span class="current-price"><span class="unit">¥</span>{{ detail.price.toLocaleString() }}</span>
            <span class="original-price">¥{{ (detail.price * 1.3).toFixed(0).replace(/\B(?=(\d{3})+(?!\d))/g, ',') }}</span>
            <span class="discount-tag">7折</span>
          </div>

          <div class="stock-section">
            <div class="stock-header">
              <span>库存进度</span>
              <span class="stock-num">已售 {{ detail.sold }} / {{ detail.initialStock }}</span>
            </div>
            <div class="progress-track">
              <div class="progress-fill sold" :style="{ width: soldPct + '%' }"></div>
              <div class="progress-fill remain" :style="{ width: stockPct + '%' }"></div>
            </div>
            <div class="stock-legend">
              <span class="legend-sold">已售 {{ soldPct }}%</span>
              <span class="legend-remain">剩余 {{ detail.currentStock }} 件</span>
            </div>
          </div>

          <div class="seckill-section">
            <button class="seckill-btn" :disabled="!isWindowOpen" @click="handleSeckill">
              {{ isWindowOpen ? '⚡ 立即秒杀' : '等待开场' }}
            </button>
            <div v-if="resultType" class="result" :class="resultType">{{ resultMsg }}</div>
            <div v-if="orderInfo && paymentLeft > 0" class="payment-bar">
              <div class="payment-countdown">请在 <span class="countdown-num">{{ formatCountdown(paymentLeft) }}</span> 内完成支付</div>
              <button class="pay-btn" @click="handlePay">立即支付</button>
            </div>
          </div>
        </div>
      </div>

      <div class="rules-card">
        <h3>秒杀规则</h3>
        <ul>
          <li>每个用户限购 <strong>1</strong> 件</li>
          <li>下单后 <strong>15 分钟</strong> 内完成支付，超时自动取消</li>
          <li>库存有限，先到先得</li>
        </ul>
      </div>

      <div class="buyer-panel">
        <h3>最新抢购记录</h3>
        <div v-if="buyerList.length === 0" class="empty-buyers">暂无记录</div>
        <div v-else class="buyer-list">
          <div v-for="(b, idx) in buyerList" :key="idx" class="buyer-row">
            <span class="buyer-idx">{{ idx + 1 }}</span>
            <span class="buyer-user">{{ b.userId }}</span>
            <span class="buyer-time">{{ new Date(b.createTime).toLocaleTimeString() }}</span>
          </div>
        </div>
      </div>
    </template>
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
.user-info { font-size: 13px; opacity: 0.9; }
.back-btn {
  background: none; border: none; color: #fff; font-size: 15px;
  cursor: pointer; padding: 4px 0;
}

.state-box { text-align: center; padding: 60px 20px; color: #999; font-size: 15px; }
.state-box.error { color: #e74c3c; }

/* --- Detail Card --- */
.detail-card {
  max-width: 500px; margin: 20px auto 0; background: #fff;
  border-radius: 16px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.hero { background: linear-gradient(135deg, #fafafa, #f0f0f0); padding: 40px 0; text-align: center; }
.hero-icon { width: 100px; height: 100px; border-radius: 20px; display: inline-flex; align-items: center; justify-content: center; font-size: 48px; }

.info-section { padding: 24px; }

.goods-name { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 12px; }

.price-row { display: flex; align-items: baseline; gap: 10px; margin-bottom: 20px; }
.current-price { font-size: 28px; font-weight: 800; color: #e74c3c; }
.current-price .unit { font-size: 16px; }
.original-price { font-size: 14px; color: #bbb; text-decoration: line-through; }
.discount-tag { font-size: 12px; background: #ffeaea; color: #e74c3c; padding: 2px 8px; border-radius: 4px; font-weight: 600; }

/* --- Stock Progress --- */
.stock-section { margin-bottom: 24px; }
.stock-header { display: flex; justify-content: space-between; font-size: 13px; color: #888; margin-bottom: 8px; }
.stock-num { color: #333; font-weight: 600; }
.progress-track { height: 8px; border-radius: 4px; background: #eee; display: flex; overflow: hidden; }
.progress-fill.sold { background: linear-gradient(90deg, #e74c3c, #f39c12); }
.progress-fill.remain { background: linear-gradient(90deg, #27ae60, #2ecc71); }
.stock-legend { display: flex; justify-content: space-between; font-size: 12px; margin-top: 6px; }
.legend-sold { color: #e74c3c; }
.legend-remain { color: #27ae60; }

/* --- Seckill Button --- */
.seckill-section { text-align: center; }
.seckill-btn {
  width: 100%; padding: 14px 0; border: none; border-radius: 12px;
  font-size: 18px; font-weight: 700; cursor: pointer; color: #fff;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  transition: all 0.15s;
}
.seckill-btn:active:not(:disabled) { transform: scale(0.97); }
.seckill-btn:disabled { background: #ccc; cursor: not-allowed; }

.result { font-size: 14px; margin-top: 10px; font-weight: 600; }
.result.success { color: #27ae60; }
.result.fail { color: #e74c3c; }
.result.info { color: #2980b9; }

.payment-bar {
  margin-top: 12px; padding: 12px; background: #fff8e1;
  border: 1px solid #ffc107; border-radius: 10px;
}
.payment-countdown { font-size: 14px; color: #e65100; margin-bottom: 10px; }
.countdown-num { font-weight: 700; }
.pay-btn {
  padding: 8px 32px; border: none; border-radius: 8px; font-size: 15px;
  font-weight: 600; cursor: pointer; color: #fff; background: #ff9800;
}

/* --- Rules --- */
.rules-card {
  max-width: 500px; margin: 16px auto 0; background: #fff;
  border-radius: 16px; padding: 20px 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.rules-card h3 { font-size: 15px; color: #333; margin: 0 0 10px; }
.rules-card ul { margin: 0; padding-left: 18px; }
.rules-card li { font-size: 13px; color: #666; line-height: 1.8; }

/* --- Buyers --- */
.buyer-panel {
  max-width: 500px; margin: 16px auto 0; background: #fff;
  border-radius: 16px; padding: 20px 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.buyer-panel h3 { font-size: 15px; color: #333; margin: 0 0 12px; }
.empty-buyers { color: #ccc; font-size: 13px; text-align: center; padding: 20px 0; }
.buyer-list { max-height: 300px; overflow-y: auto; }
.buyer-row { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #f5f5f5; font-size: 14px; }
.buyer-row:last-child { border-bottom: none; }
.buyer-idx { width: 28px; font-weight: 700; color: #e74c3c; font-size: 13px; }
.buyer-user { flex: 1; color: #333; }
.buyer-time { color: #999; font-size: 13px; }
</style>
