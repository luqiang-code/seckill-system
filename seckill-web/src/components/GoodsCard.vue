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

const meta = computed(() => PRODUCT_META[props.goods.id] ?? { emoji: '📦', color: '#3d2820', name: props.goods.name })
const displayName = computed(() => meta.value.name || props.goods.name)
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
  <div class="shop-card" :class="{ 'sold-out': isSoldOut }">
    <div class="shop-sign">
      <div class="sign-hanger"></div>
      <div class="sign-plate">
        <div class="sign-emoji" :style="{ background: meta.color }" @click="goDetail">
          {{ meta.emoji }}
        </div>
        <div class="sign-info" @click="goDetail">
          <div class="sign-name">{{ displayName }}</div>
          <div class="sign-hint">✦ 轻敲查看详情 ✦</div>
        </div>
      </div>
    </div>

    <div class="shop-body">
      <div class="price-tag">
        <span class="galleon-icon">🪙</span>
        <span class="price-num">{{ goods.price.toLocaleString() }}</span>
        <span class="price-label">加隆</span>
      </div>

      <div class="stock-area">
        <div class="stock-cauldron">
          <div class="cauldron-track">
            <div class="cauldron-fill" :style="{ width: stockPct + '%' }" :class="{ low: stockPct < 20 }">
              <div class="cauldron-sparkle"></div>
            </div>
          </div>
        </div>
        <div class="stock-text">
          存货 <span class="stock-num">{{ goods.stock }}</span> 件
        </div>
      </div>

      <div class="spell-area">
        <button
          class="spell-btn"
          :disabled="disabled || isSoldOut"
          @click="onSeckill"
        >
          <span class="spell-icon">⚡</span>
          {{ isSoldOut ? '魔药售罄' : disabled ? '等待开门' : '念咒抢购' }}
        </button>
        <div v-if="resultType" class="spell-result" :class="resultType">{{ resultMsg }}</div>
        <div v-if="orderInfo && paymentLeft > 0" class="payment-bar">
          <div class="payment-note">
            请在 <span class="countdown-num">{{ formatCountdown(paymentLeft) }}</span> 内献上金币
          </div>
          <button class="pay-btn" @click="handlePay">🪙 金币支付</button>
        </div>
      </div>
    </div>

    <div v-if="isSoldOut" class="soldout-seal">
      <span>售罄</span>
    </div>
  </div>
</template>

<style scoped>
.shop-card {
  position: relative;
  background: linear-gradient(180deg, #2c1f14 0%, #1e1610 100%);
  border: 2px solid #5c0000;
  border-radius: 2px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.shop-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(116, 0, 1, 0.3), 0 0 0 1px rgba(212, 175, 55, 0.2);
}

.shop-card.sold-out {
  opacity: 0.6;
}

/* ---- shop sign ---- */
.shop-sign {
  padding: 20px 20px 0;
  text-align: center;
}

.sign-hanger {
  width: 40px; height: 6px;
  background: #5c0000;
  border-radius: 3px;
  margin: 0 auto 8px;
}

.sign-plate {
  background: linear-gradient(180deg, #f5e6cc 0%, #ebd5b3 100%);
  border: 2px solid #8b6914;
  border-radius: 2px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.3);
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.sign-plate:hover {
  box-shadow: 0 4px 16px rgba(212, 175, 55, 0.3), inset 0 1px 0 rgba(255,255,255,0.3);
}

.sign-emoji {
  width: 56px; height: 56px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  border: 2px solid #8b6914;
  margin-bottom: 8px;
}

.sign-name {
  font-family: 'Pirata One', serif;
  font-size: 20px;
  color: #3d1c00;
  letter-spacing: 0.05em;
}

.sign-hint {
  font-family: 'Cormorant Garamond', serif;
  font-size: 12px;
  color: #8b7355;
  margin-top: 4px;
  font-style: italic;
}

/* ---- body ---- */
.shop-body {
  padding: 16px 20px 20px;
}

.price-tag {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 14px;
}

.galleon-icon { font-size: 18px; }

.price-num {
  font-family: 'Pirata One', serif;
  font-size: 26px;
  color: #D4AF37;
  text-shadow: 0 0 8px rgba(212, 175, 55, 0.3);
}

.price-label {
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #b8a080;
  font-style: italic;
}

/* ---- stock ---- */
.stock-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.stock-cauldron { flex: 1; }

.cauldron-track {
  height: 8px;
  background: #1a1008;
  border: 1px solid #3d2820;
  border-radius: 4px;
  overflow: hidden;
}

.cauldron-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #1a5632, #2ecc71);
  transition: width 0.4s;
  position: relative;
}

.cauldron-fill.low {
  background: linear-gradient(90deg, #740001, #c0392b);
}

.cauldron-sparkle {
  position: absolute;
  top: 0; right: 0; bottom: 0;
  width: 20px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2));
}

.stock-text {
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #8b7355;
  white-space: nowrap;
  font-style: italic;
}

.stock-num {
  color: #D4AF37;
  font-weight: 700;
  font-style: normal;
}

/* ---- spell button ---- */
.spell-area {
  text-align: center;
}

.spell-btn {
  width: 100%;
  padding: 14px 0;
  border: none;
  border-radius: 2px;
  font-family: 'Pirata One', serif;
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0.06em;
  cursor: pointer;
  color: #D4AF37;
  background: linear-gradient(180deg, #740001 0%, #5c0000 100%);
  border: 1px solid #8b0000;
  transition: all 0.15s;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spell-icon { font-size: 18px; }

.spell-btn:hover:not(:disabled) {
  background: linear-gradient(180deg, #8b0000 0%, #740001 100%);
  box-shadow: 0 0 20px rgba(116, 0, 1, 0.5);
}

.spell-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.spell-btn:disabled {
  background: #3d2820;
  color: #665540;
  border-color: #3d2820;
  cursor: not-allowed;
  text-shadow: none;
}

/* ---- result ---- */
.spell-result {
  font-family: 'Pirata One', serif;
  font-size: 15px;
  margin-top: 10px;
  letter-spacing: 0.04em;
}

.spell-result.success { color: #2ecc71; }
.spell-result.fail    { color: #e74c3c; }
.spell-result.info    { color: #5dade2; }

/* ---- payment ---- */
.payment-bar {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(180deg, #2c1f14, #1e1610);
  border: 1px solid #D4AF37;
  border-radius: 2px;
}

.payment-note {
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #c8b898;
  margin-bottom: 10px;
  font-style: italic;
}

.countdown-num {
  font-family: 'Pirata One', serif;
  color: #D4AF37;
  font-weight: 400;
  font-style: normal;
}

.pay-btn {
  padding: 8px 28px;
  border: 1px solid #8b6914;
  border-radius: 2px;
  font-family: 'Pirata One', serif;
  font-size: 16px;
  letter-spacing: 0.05em;
  cursor: pointer;
  color: #3d1c00;
  background: linear-gradient(180deg, #D4AF37, #b8941a);
  transition: all 0.15s;
}

.pay-btn:hover {
  box-shadow: 0 0 12px rgba(212, 175, 55, 0.4);
}

.pay-btn:active {
  transform: scale(0.96);
}

/* ---- sold out seal ---- */
.soldout-seal {
  position: absolute;
  top: 16px; right: -32px;
  background: #740001;
  color: #D4AF37;
  font-family: 'Pirata One', serif;
  font-size: 14px;
  letter-spacing: 0.1em;
  padding: 4px 36px;
  transform: rotate(45deg);
  border: 1px solid #D4AF37;
}

@media (max-width: 500px) {
  .shop-body { text-align: center; }
  .stock-area { justify-content: center; }
}
</style>
