<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { doSeckill, fetchResult, ApiError } from '../api'
import type { Goods, OrderInfo } from '../api/types'

const router = useRouter()

const props = defineProps<{
  goods: Goods
  disabled: boolean
}>()

const emit = defineEmits<{
  sold: []
}>()

const PAYMENT_DEADLINE_MS = 15 * 60 * 1000
const DEBOUNCE_MS = 1000

const resultType = ref<'success' | 'fail' | 'info' | ''>('')
const resultMsg = ref('')
const orderInfo = ref<OrderInfo | null>(null)
const paymentLeft = ref(0)
let cooldownUntil = 0
let paymentTimer: ReturnType<typeof setInterval> | null = null

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
  if (paymentTimer) {
    clearInterval(paymentTimer)
    paymentTimer = null
  }
}

function handlePay() {
  resultType.value = 'success'
  resultMsg.value = '支付成功！订单号: ' + (orderInfo.value?.id ?? '')
  stopPaymentCountdown()
  paymentLeft.value = 0
}

async function handleSeckill() {
  const now = Date.now()
  if (now < cooldownUntil) return
  cooldownUntil = now + DEBOUNCE_MS

  clearResult()

  try {
    await doSeckill(props.goods.id)
    resultType.value = 'success'
    resultMsg.value = '抢到了！'
    pollResult()
    emit('sold')
  } catch (e) {
    if (e instanceof ApiError) {
      switch (e.code) {
        case 429:
          resultType.value = 'info'
          resultMsg.value = '请求太频繁'
          break
        case 2:
          resultType.value = 'info'
          resultMsg.value = '已抢过'
          break
        case 0:
          resultType.value = 'fail'
          resultMsg.value = '已售罄'
          break
        default:
          resultType.value = 'fail'
          resultMsg.value = e.message
      }
    } else {
      resultType.value = 'fail'
      resultMsg.value = '网络错误'
    }
  }
}

function pollResult() {
  const check = () => {
    fetchResult(props.goods.id)
      .then(order => {
        orderInfo.value = order
        resultMsg.value = '订单号: ' + order.id
        startPaymentCountdown()
      })
      .catch(err => {
        if (err instanceof ApiError && err.code === 3) {
          setTimeout(check, 500)
        }
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

function goDetail() {
  router.push(`/goods/${props.goods.id}`)
}

onUnmounted(() => stopPaymentCountdown())
</script>

<template>
  <div class="goods-card">
    <div class="icon clickable" :class="goods.id === 1 ? 'phone' : 'laptop'" @click="goDetail">
      {{ goods.id === 1 ? '📱' : '💻' }}
    </div>
    <div class="info clickable" @click="goDetail">
      <div class="name">{{ goods.name }}</div>
      <div class="view-hint">点击查看详情 →</div>
      <div class="price"><span class="unit">¥</span>{{ goods.price.toLocaleString() }}</div>
      <div class="stock-info">
        剩余库存 <span class="num">{{ goods.stock }}</span> 件
      </div>
    </div>
    <div class="action">
      <button
        class="btn"
        :disabled="disabled"
        @click="handleSeckill"
      >{{ disabled ? '等待开场' : '立即秒杀' }}</button>
      <div v-if="resultType" class="result" :class="resultType">{{ resultMsg }}</div>
      <div v-if="orderInfo && paymentLeft > 0" class="payment-bar">
        <div class="payment-countdown">请在 <span class="countdown-num">{{ formatCountdown(paymentLeft) }}</span> 内完成支付</div>
        <button class="pay-btn" @click="handlePay">立即支付</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.goods-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  flex-shrink: 0;
}

.icon.phone { background: #e8f5e9; }
.icon.laptop { background: #e3f2fd; }

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.view-hint {
  font-size: 12px;
  color: #ccc;
  margin-bottom: 2px;
}

.clickable { cursor: pointer; }
.clickable:hover .name { color: #e74c3c; }
.clickable:hover .view-hint { color: #e74c3c; }

.price {
  font-size: 22px;
  font-weight: 700;
  color: #e74c3c;
  margin-bottom: 2px;
}

.price .unit {
  font-size: 14px;
}

.stock-info {
  font-size: 13px;
  color: #999;
}

.stock-info .num {
  color: #e74c3c;
  font-weight: 600;
}

.action {
  flex-shrink: 0;
}

.btn {
  width: 120px;
  padding: 12px 0;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  color: #fff;
  background: #e74c3c;
}

.btn:active:not(:disabled) {
  transform: scale(0.96);
}

.btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.result {
  font-size: 13px;
  margin-top: 6px;
  text-align: center;
  font-weight: 600;
}

.result.success { color: #27ae60; }
.result.fail { color: #e74c3c; }
.result.info { color: #2980b9; }

.payment-bar {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fff8e1;
  border: 1px solid #ffc107;
  border-radius: 8px;
  text-align: center;
}

.payment-countdown {
  font-size: 13px;
  color: #e65100;
  margin-bottom: 8px;
}

.countdown-num {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.pay-btn {
  padding: 6px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  color: #fff;
  background: #ff9800;
  transition: all 0.15s;
}

.pay-btn:active {
  transform: scale(0.96);
}

@media (max-width: 500px) {
  .goods-card {
    flex-direction: column;
    text-align: center;
  }

  .action {
    width: 100%;
  }

  .btn {
    width: 100%;
  }
}
</style>
