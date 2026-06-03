<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import StatCard from '../components/StatCard.vue'
import ProgressBar from '../components/ProgressBar.vue'
import LogConsole from '../components/LogConsole.vue'
import { fetchStock, doSeckillWithToken, getTestToken } from '../api'

const POLL_MS = 150

const threads = ref(300)
const duration = ref(5)
const stock = ref<number | string>('--')
const sold = ref<number | string>('--')
const reqCount = ref(0)
const qps = ref(0)
const soldPct = ref(0)
const remainPct = ref(100)
const qpsRatio = ref(0)
const statusText = ref('待命中')
const statusState = ref<'idle' | 'running'>('idle')
const logLines = ref<string[]>(['等待启动魔法压测...'])
const startDisabled = ref(false)

let testRunning = false
let monitorTimer: ReturnType<typeof setInterval> | null = null
let initialStock = 0
let lastReqCount = 0
let lastQpsTime = 0
let startTime = 0

fetchStock(1).then(v => {
  stock.value = v
  initialStock = v
})

function addLog(msg: string) {
  const time = new Date().toLocaleTimeString()
  logLines.value.push(`[${time}] ${msg}`)
  if (logLines.value.length > 100) logLines.value.shift()
}

function monitor() {
  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1)

  fetchStock(1).then(v => {
    stock.value = v
    const s = initialStock - v
    sold.value = s
    reqCount.value = reqCount.value
    soldPct.value = Math.min(100, Math.round((s / initialStock) * 100))
    remainPct.value = Math.max(0, Math.round((v / initialStock) * 100))

    const now = Date.now()
    const elapsedQps = (now - lastQpsTime) / 1000
    if (elapsedQps >= 0.5) {
      const currentQps = Math.round((reqCount.value - lastReqCount) / elapsedQps)
      qps.value = currentQps
      qpsRatio.value = Math.min(100, Math.round((currentQps / 5000) * 100))
      lastReqCount = reqCount.value
      lastQpsTime = now
    }

    if (v <= 0 && s >= initialStock) {
      addLog(`[魔药售罄] 耗时 ${elapsed}s | 咒语 ${reqCount.value} | 存货 ${v}`)
    }
  })
}

async function startTest() {
  if (testRunning) return
  testRunning = true
  startDisabled.value = true
  statusText.value = '咒语施放中...'
  statusState.value = 'running'

  reqCount.value = 0
  lastReqCount = 0
  lastQpsTime = Date.now()
  logLines.value = []
  addLog(`[施法开始] 巫师=${threads.value} 时长=${duration.value}s`)

  startTime = Date.now()
  const currentStock = stock.value
  initialStock = currentStock === '--' ? 100 : Number(currentStock)
  sold.value = '0'
  reqCount.value = 0
  qps.value = 0

  monitorTimer = setInterval(monitor, POLL_MS)

  const endTime = Date.now() + duration.value * 1000
  const promises: Promise<void>[] = []
  for (let i = 0; i < threads.value; i++) {
    promises.push(runUser(i, endTime))
  }

  await Promise.allSettled(promises)

  if (monitorTimer) clearInterval(monitorTimer)
  monitorTimer = null
  testRunning = false
  startDisabled.value = false

  const finalStock = await fetchStock(1)
  stock.value = finalStock

  addLog(`[咒语完成] 总咒语数=${reqCount.value} 最终存货=${finalStock}`)
}

async function runUser(userIdx: number, endTime: number) {
  const userId = 'wizard_' + userIdx + '_' + Date.now()
  const { token } = await getTestToken(userId)
  while (Date.now() < endTime) {
    try {
      await doSeckillWithToken(1, token)
      reqCount.value++
    } catch {
      // ignore
    }
    await new Promise(r => setTimeout(r, Math.random() * 50))
  }
}

function resetView() {
  if (monitorTimer) {
    clearInterval(monitorTimer)
    monitorTimer = null
  }
  testRunning = false
  startDisabled.value = false
  statusText.value = '待命中'
  statusState.value = 'idle'
  reqCount.value = 0
  sold.value = '--'
  qps.value = 0
  soldPct.value = 0
  remainPct.value = 100
  qpsRatio.value = 0
  logLines.value = ['等待启动魔法压测...']

  fetchStock(1).then(v => {
    stock.value = v
    initialStock = v
  })
}

onUnmounted(() => {
  if (monitorTimer) clearInterval(monitorTimer)
})
</script>

<template>
  <div class="ministry-page">
    <header class="ministry-header">
      <h1>魔法部 · 监控室</h1>
      <div class="ministry-sub">Department of Mysteries · 实时监控</div>
    </header>

    <div class="controls">
      <div class="control-group">
        <label>🧙 巫师数</label>
        <input v-model.number="threads" type="number" min="10" max="5000">
      </div>
      <div class="control-group">
        <label>⏳ 咒语时长(s)</label>
        <input v-model.number="duration" type="number" min="1" max="60">
      </div>
      <button
        class="btn-cast"
        :disabled="startDisabled"
        @click="startTest"
      >
        <span class="btn-spark">⚡</span> 施放咒语
      </button>
      <button class="btn-reset" @click="resetView">🔄 重置</button>
      <span class="status-badge" :class="statusState">{{ statusText }}</span>
    </div>

    <div class="stat-grid">
      <StatCard label="存货量" :value="stock" color="green" />
      <StatCard label="已售出" :value="sold" color="red" />
      <StatCard label="咒语总数" :value="reqCount" color="blue" />
      <StatCard label="每秒咒语" :value="qps" color="yellow" />
    </div>

    <div class="chart-section">
      <h3 class="chart-title">🔮 存货消耗进度</h3>
      <ProgressBar
        label="已售出"
        :current="soldPct"
        suffix="%"
        :max="100"
        color-class="sold"
      />
      <ProgressBar
        label="存货剩余"
        :current="remainPct"
        suffix="%"
        :max="100"
        color-class="remain"
      />
    </div>

    <div class="chart-section">
      <h3 class="chart-title">⚡ 魔咒强度 (QPS / 5000)</h3>
      <ProgressBar
        label="当前魔咒强度"
        :current="qpsRatio"
        suffix="%"
        :max="100"
        color-class="rate"
      />
    </div>

    <LogConsole :lines="logLines" />
  </div>
</template>

<style scoped>
.ministry-page {
  min-height: 100vh;
  padding-bottom: 40px;
}

.ministry-header {
  background: linear-gradient(180deg, #0d1b3e, #122550);
  border-bottom: 2px solid #2c1050;
  padding: 28px 20px;
  text-align: center;
}

.ministry-header h1 {
  font-family: 'Pirata One', serif;
  font-size: 32px;
  color: #5dade2;
  letter-spacing: 0.1em;
  text-shadow: 0 0 20px rgba(93, 173, 226, 0.2);
  margin: 0;
}

.ministry-sub {
  font-family: 'Cormorant Garamond', serif;
  font-size: 13px;
  color: #6b8faa;
  margin-top: 6px;
  font-style: italic;
}

.controls {
  max-width: 900px;
  margin: 20px auto;
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 0 16px;
  flex-wrap: wrap;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.control-group label {
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  color: #8b8b6b;
  font-style: italic;
}

.control-group input {
  width: 72px;
  padding: 8px 10px;
  background: #1a1008;
  border: 1px solid #3d2820;
  border-radius: 2px;
  color: #e0d5c1;
  font-family: 'Cormorant Garamond', serif;
  font-size: 15px;
  text-align: center;
}

.btn-cast {
  padding: 10px 24px;
  border: 1px solid #8b0000;
  border-radius: 2px;
  font-family: 'Pirata One', serif;
  font-size: 16px;
  letter-spacing: 0.06em;
  cursor: pointer;
  color: #D4AF37;
  background: linear-gradient(180deg, #740001, #5c0000);
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s;
}

.btn-cast:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(116, 0, 1, 0.5);
}

.btn-cast:disabled {
  background: #3d2820;
  border-color: #3d2820;
  color: #665540;
  cursor: not-allowed;
}

.btn-spark { font-size: 14px; }

.btn-reset {
  padding: 10px 20px;
  border: 1px solid #3d2820;
  border-radius: 2px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  cursor: pointer;
  color: #8b7355;
  background: #1e1610;
  transition: all 0.15s;
}

.btn-reset:hover {
  border-color: #D4AF37;
  color: #D4AF37;
}

.status-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 2px;
  font-family: 'Pirata One', serif;
  font-size: 13px;
  letter-spacing: 0.06em;
}

.status-badge.idle {
  background: #1e1610;
  border: 1px solid #3d2820;
  color: #665540;
}

.status-badge.running {
  background: rgba(116, 0, 1, 0.2);
  border: 1px solid #8b0000;
  color: #e74c3c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  max-width: 900px;
  margin: 0 auto 24px;
  padding: 0 16px;
}

.chart-section {
  max-width: 900px;
  margin: 0 auto 20px;
  padding: 20px 24px;
  background: linear-gradient(180deg, #1e1610, #1a1008);
  border: 1px solid #3d2820;
  border-radius: 4px;
}

.chart-title {
  font-family: 'Pirata One', serif;
  font-size: 15px;
  color: #b8a080;
  letter-spacing: 0.08em;
  margin-bottom: 16px;
}
</style>
