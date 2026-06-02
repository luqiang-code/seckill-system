<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSeckill } from '../composables/useSeckill'
import { formatCountdown } from '../api/constants'
import { PRODUCT_META } from '../api/types'
import type { Goods } from '../api/types'

const router = useRouter()

const props = defineProps<{
  goods: Goods
  disabled: boolean
  initialStock: number
}>()

const emit = defineEmits<{
  sold: []
}>()

const {
  resultType, resultMsg, orderInfo, paymentLeft,
  handleSeckill, handlePay,
} = useSeckill(() => props.goods.id)

const meta = computed(() => PRODUCT_META[props.goods.id] ?? { emoji: '📦', color: '#f5f5f5' })
const stockPct = computed(() => props.initialStock > 0
  ? Math.round((props.goods.stock / props.initialStock) * 100)
  : 0)
const isSoldOut = computed(() => props.goods.stock <= 0)

function onSeckill() {
  handleSeckill(() => emit('sold'))
}

function goDetail() {
  router.push(`/goods/${props.goods.id}`)
}
</script>

<template>
  <div class="goods-card" :class="{ 'sold-out': isSoldOut }">
    <div class="icon clickable" :style="{ background: meta.color }" @click="goDetail">
      {{ meta.emoji }}
    </div>
    <div class="info clickable" @click="goDetail">
      <div class="name">{{ goods.name }}</div>
      <div class="view-hint">点击查看详情 →</div>
      <div class="price"><span class="unit">¥</span>{{ goods.price.toLocaleString() }}</div>
      <div class="stock-section">
        <div class="stock-bar-bg">
          <div class="stock-bar-fill" :style="{ width: stockPct + '%' }" :class="{ low: stockPct < 20 }"></div>
        </div>
        <div class="stock-text">
          剩余 <span class="num">{{ goods.stock }}</span> 件
        </div>
      </div>
    </div>
    <div class="action">
      <button
        class="btn"
        :disabled="disabled || isSoldOut"
        @click="onSeckill"
      >{{ isSoldOut ? '已售罄' : disabled ? '等待开场' : '立即秒杀' }}</button>
      <div v-if="resultType" class="result" :class="resultType">{{ resultMsg }}</div>
      <div v-if="orderInfo && paymentLeft > 0" class="payment-bar">
        <div class="payment-countdown">请在 <span class="countdown-num">{{ formatCountdown(paymentLeft) }}</span> 内完成支付</div>
        <button class="pay-btn" @click="handlePay">立即支付</button>
      </div>
    </div>
    <div v-if="isSoldOut" class="soldout-overlay">
      <span>已售罄</span>
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
  position: relative;
  overflow: hidden;
  transition: transform 0.15s;
}

.goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.goods-card.sold-out {
  opacity: 0.7;
}

.icon {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  flex-shrink: 0;
}

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
  margin-bottom: 10px;
}

.price .unit {
  font-size: 14px;
}

.stock-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stock-bar-bg {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #eee;
  max-width: 120px;
}

.stock-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #27ae60, #2ecc71);
  transition: width 0.3s;
}

.stock-bar-fill.low {
  background: linear-gradient(90deg, #e74c3c, #f39c12);
}

.stock-text {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
}

.stock-text .num {
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

.soldout-overlay {
  position: absolute;
  top: 12px;
  right: -28px;
  background: #999;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 32px;
  transform: rotate(45deg);
}

@media (max-width: 500px) {
  .goods-card {
    flex-direction: column;
    text-align: center;
  }
  .stock-section {
    justify-content: center;
  }
  .action {
    width: 100%;
  }
  .btn {
    width: 100%;
  }
}
</style>
