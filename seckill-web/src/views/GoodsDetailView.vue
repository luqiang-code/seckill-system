<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCountdown } from '../composables/useCountdown'
import { useAuth } from '../composables/useAuth'
import { useSeckill } from '../composables/useSeckill'
import { fetchGoodsDetail, fetchRecentOrders } from '../api'
import { PRODUCT_META } from '../api/types'
import { WINDOW_DURATION, INTERVAL_DURATION, formatCountdown } from '../api/constants'

const route = useRoute()
const router = useRouter()

const { isWindowOpen } = useCountdown(WINDOW_DURATION, INTERVAL_DURATION)
const { username } = useAuth()

const goodsId = Number(route.params.id)

const {
  resultType, resultMsg, orderInfo, paymentLeft,
  handleSeckill, handlePay,
} = useSeckill(goodsId)

const detail = ref<{ id: number; name: string; price: number; initialStock: number; currentStock: number; sold: number } | null>(null)
const buyerList = ref<any[]>([])
const loading = ref(true)
const errorMsg = ref('')

const soldPct = ref(0)
const stockPct = ref(100)
const meta = computed(() => PRODUCT_META[goodsId] ?? { emoji: '📦', color: '#3d2820', name: detail.value?.name ?? '' })
const displayName = computed(() => meta.value.name || detail.value?.name || '')

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
    errorMsg.value = e instanceof Error ? e.message : '魔法失效，加载失败'
  } finally {
    loading.value = false
  }
}

function refreshOrders() {
  fetchRecentOrders(goodsId, 15).then(orders => {
    buyerList.value = orders
  }).catch(() => {})
}

function onSeckill() {
  handleSeckill(() => {
    loadDetail()
    refreshOrders()
  })
}

onMounted(loadDetail)
</script>

<template>
  <div class="shop-page">
    <header class="shop-header">
      <button class="back-btn" @click="router.push('/')">← 返回对角巷</button>
      <h1 class="shop-name">{{ displayName }}</h1>
      <div class="wizard-tag">🧙 {{ username }}</div>
    </header>

    <div v-if="loading" class="state-box">✦ 显形中...</div>
    <div v-else-if="errorMsg" class="state-box error">{{ errorMsg }}</div>

    <template v-else-if="detail">
      <div class="shop-window">
        <!-- item showcase -->
        <div class="showcase">
          <div class="showcase-frame">
            <div class="showcase-emoji" :style="{ background: meta.color }">
              {{ meta.emoji }}
            </div>
            <div class="showcase-sparkles">
              <span class="sparkle" v-for="n in 5" :key="n">✦</span>
            </div>
          </div>
        </div>

        <!-- info panel -->
        <div class="info-scroll">
          <h2 class="item-name">{{ displayName }}</h2>
          <p class="item-latin">Ollivanders · Makers of Fine Magic Items since 382 B.C.</p>

          <div class="price-row">
            <span class="price-galleon">
              <span class="g-icon">🪙</span>
              <span class="g-num">{{ detail.price.toLocaleString() }}</span>
              <span class="g-label">加隆</span>
            </span>
            <span class="price-sickle">或 {{ (detail.price * 17).toLocaleString() }} 西可</span>
          </div>

          <!-- stock cauldron -->
          <div class="stock-scroll">
            <div class="scroll-header">
              <span>存货状态</span>
              <span class="scroll-num">已售 {{ detail.sold }} / {{ detail.initialStock }}</span>
            </div>
            <div class="cauldron-track">
              <div class="cauldron-fill sold" :style="{ width: soldPct + '%' }"></div>
              <div class="cauldron-fill remain" :style="{ width: stockPct + '%' }"></div>
            </div>
            <div class="scroll-footer">
              <span class="footer-sold">已售 {{ soldPct }}%</span>
              <span class="footer-remain">存货 {{ detail.currentStock }} 件</span>
            </div>
          </div>

          <!-- spell button -->
          <div class="spell-section">
            <button class="spell-btn" :disabled="!isWindowOpen" @click="onSeckill">
              <span class="btn-spark">⚡</span>
              {{ isWindowOpen ? '念咒抢购' : '等待开门' }}
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
      </div>

      <!-- rules -->
      <div class="rules-parchment">
        <h3>📋 魔法交易法则</h3>
        <ul>
          <li>每位巫师限购 <strong>1</strong> 件</li>
          <li>念咒后 <strong>15 分钟</strong> 内献上金币，超时魔咒自动解除</li>
          <li>魔法物品数量有限，先到先得</li>
        </ul>
      </div>

      <!-- buyers -->
      <div class="buyers-parchment">
        <h3>📜 最新魔法交易记录</h3>
        <div v-if="buyerList.length === 0" class="empty-buyers">暂无记录</div>
        <div v-else class="buyer-list">
          <div v-for="(b, idx) in buyerList" :key="idx" class="buyer-row">
            <span class="buyer-idx">{{ idx + 1 }}</span>
            <span class="buyer-wizard">{{ b.userId }}</span>
            <span class="buyer-time">{{ new Date(b.createTime).toLocaleTimeString() }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.shop-page {
  min-height: 100vh;
  padding-bottom: 40px;
}

/* ---- header ---- */
.shop-header {
  background: linear-gradient(180deg, #1e1610, #2c1f14);
  border-bottom: 2px solid #5c0000;
  color: #D4AF37;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shop-name {
  font-family: 'Pirata One', serif;
  font-size: 22px;
  letter-spacing: 0.08em;
  margin: 0;
}

.wizard-tag {
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #b8a080;
  font-style: italic;
}

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

/* ---- state ---- */
.state-box {
  text-align: center;
  padding: 80px 20px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 18px;
  color: #8b7355;
  font-style: italic;
}
.state-box.error { color: #e74c3c; }

/* ---- shop window ---- */
.shop-window {
  max-width: 520px;
  margin: 24px auto 0;
  background: linear-gradient(180deg, #2c1f14, #1e1610);
  border: 2px solid #5c0000;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 4px 30px rgba(0,0,0,0.4);
}

.showcase {
  padding: 36px 0;
  text-align: center;
  background: linear-gradient(180deg, #1e1610, #2c1f14);
  border-bottom: 1px solid #3d2820;
}

.showcase-frame {
  position: relative;
  display: inline-block;
  padding: 20px;
  border: 2px solid #5c0000;
  border-radius: 4px;
  background: #1a1008;
}

.showcase-emoji {
  width: 90px; height: 90px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  border: 2px solid #8b6914;
}

.showcase-sparkles {
  position: absolute;
  inset: -8px;
  pointer-events: none;
}

.sparkle {
  position: absolute;
  color: #D4AF37;
  font-size: 10px;
  animation: twinkle 2s ease-in-out infinite;
}
.sparkle:nth-child(1) { top: -4px; left: 50%; animation-delay: 0s; }
.sparkle:nth-child(2) { top: 25%; right: -8px; animation-delay: 0.4s; }
.sparkle:nth-child(3) { bottom: -4px; left: 50%; animation-delay: 0.8s; }
.sparkle:nth-child(4) { top: 25%; left: -8px; animation-delay: 1.2s; }
.sparkle:nth-child(5) { bottom: 20%; left: -6px; animation-delay: 1.6s; }

@keyframes twinkle {
  0%, 100% { opacity: 0.2; transform: scale(0.8); }
  50% { opacity: 0.8; transform: scale(1.2); }
}

/* info panel */
.info-scroll {
  padding: 24px;
}

.item-name {
  font-family: 'Pirata One', serif;
  font-size: 26px;
  color: #D4AF37;
  letter-spacing: 0.06em;
  margin: 0 0 4px;
}

.item-latin {
  font-family: 'Cormorant Garamond', serif;
  font-size: 12px;
  color: #665540;
  font-style: italic;
  margin-bottom: 20px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #1a1008;
  border: 1px solid #3d2820;
  border-radius: 2px;
}

.price-galleon {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.g-icon { font-size: 20px; }
.g-num {
  font-family: 'Pirata One', serif;
  font-size: 28px;
  color: #D4AF37;
  text-shadow: 0 0 8px rgba(212, 175, 55, 0.2);
}
.g-label {
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #b8a080;
  font-style: italic;
}

.price-sickle {
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #665540;
  font-style: italic;
}

/* stock */
.stock-scroll { margin-bottom: 24px; }

.scroll-header {
  display: flex;
  justify-content: space-between;
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #8b7355;
  margin-bottom: 8px;
  font-style: italic;
}

.scroll-num { color: #c8b898; font-style: normal; }

.cauldron-track {
  height: 10px;
  border-radius: 5px;
  background: #1a1008;
  border: 1px solid #3d2820;
  display: flex;
  overflow: hidden;
}

.cauldron-fill.sold   { background: linear-gradient(90deg, #740001, #c0392b); }
.cauldron-fill.remain { background: linear-gradient(90deg, #1a5632, #2ecc71); }

.scroll-footer {
  display: flex;
  justify-content: space-between;
  font-family: 'Cormorant Garamond', serif;
  font-size: 12px;
  margin-top: 6px;
}

.footer-sold   { color: #e74c3c; font-style: italic; }
.footer-remain { color: #2ecc71; font-style: italic; }

/* spell */
.spell-section { text-align: center; }

.spell-btn {
  width: 100%;
  padding: 16px 0;
  border: none;
  border-radius: 2px;
  font-family: 'Pirata One', serif;
  font-size: 22px;
  letter-spacing: 0.08em;
  cursor: pointer;
  color: #D4AF37;
  background: linear-gradient(180deg, #740001, #5c0000);
  border: 1px solid #8b0000;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  transition: all 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-spark { font-size: 20px; }

.spell-btn:hover:not(:disabled) {
  background: linear-gradient(180deg, #8b0000, #740001);
  box-shadow: 0 0 24px rgba(116, 0, 1, 0.5);
}

.spell-btn:active:not(:disabled) { transform: scale(0.97); }

.spell-btn:disabled {
  background: #3d2820;
  color: #665540;
  border-color: #3d2820;
  cursor: not-allowed;
  text-shadow: none;
}

.spell-result {
  font-family: 'Pirata One', serif;
  font-size: 15px;
  margin-top: 10px;
  letter-spacing: 0.04em;
}

.spell-result.success { color: #2ecc71; }
.spell-result.fail    { color: #e74c3c; }
.spell-result.info    { color: #5dade2; }

/* payment */
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
}

.pay-btn {
  padding: 8px 32px;
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

/* ---- rules ---- */
.rules-parchment, .buyers-parchment {
  max-width: 520px;
  margin: 16px auto 0;
  background: linear-gradient(180deg, #2c1f14, #1e1610);
  border: 1px solid #3d2820;
  border-radius: 4px;
  padding: 20px 24px;
}

.rules-parchment h3, .buyers-parchment h3 {
  font-family: 'Pirata One', serif;
  font-size: 16px;
  color: #D4AF37;
  letter-spacing: 0.08em;
  margin: 0 0 10px;
}

.rules-parchment ul {
  margin: 0;
  padding-left: 18px;
}

.rules-parchment li {
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #b8a080;
  line-height: 2;
  font-style: italic;
}

/* buyer list */
.empty-buyers {
  font-family: 'Cormorant Garamond', serif;
  color: #665540;
  font-style: italic;
  text-align: center;
  padding: 20px 0;
}

.buyer-list {
  max-height: 300px;
  overflow-y: auto;
}

.buyer-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #2c1f14;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
}

.buyer-row:last-child { border-bottom: none; }

.buyer-idx {
  width: 28px;
  font-family: 'Pirata One', serif;
  font-size: 14px;
  color: #D4AF37;
}

.buyer-wizard { flex: 1; color: #c8b898; }
.buyer-time { color: #8b7355; font-size: 13px; font-style: italic; }
</style>
