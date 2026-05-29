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
const logLines = ref<string[]>(['等待启动压测...'])
const startDisabled = ref(false)

let testRunning = false
let monitorTimer: ReturnType<typeof setInterval> | null = null
let initialStock = 0
let lastReqCount = 0
let lastQpsTime = 0
let startTime = 0

// Initial stock fetch
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
      addLog(`[售罄] 耗时 ${elapsed}s | 请求 ${reqCount.value} | 库存 ${v}`)
    }
  })
}

async function startTest() {
  if (testRunning) return
  testRunning = true
  startDisabled.value = true
  statusText.value = '压测中...'
  statusState.value = 'running'

  reqCount.value = 0
  lastReqCount = 0
  lastQpsTime = Date.now()
  logLines.value = []
  addLog(`[启动] 并发=${threads.value} 时长=${duration.value}s`)

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

  addLog(`[完成] 总请求=${reqCount.value} 最终库存=${finalStock}`)
}

async function runUser(userIdx: number, endTime: number) {
  const userId = 'stress_' + userIdx + '_' + Date.now()
  const { token } = await getTestToken(userId)
  while (Date.now() < endTime) {
    try {
      await doSeckillWithToken(1, token)
      reqCount.value++
    } catch {
      // ignore errors during stress test
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
  logLines.value = ['等待启动压测...']

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
  <div class="page">
    <h1>秒杀压测监控面板</h1>
    <div class="subtitle">实时监控 Redis 库存 & 请求状态</div>

    <div class="controls">
      <label>并发数</label>
      <input v-model.number="threads" type="number" min="10" max="5000">
      <label>压测时长(s)</label>
      <input v-model.number="duration" type="number" min="1" max="60">
      <button
        class="btn btn-start"
        :disabled="startDisabled"
        @click="startTest"
      >启动压测</button>
      <button class="btn btn-reset" @click="resetView">重置</button>
      <span class="status-badge" :class="statusState">{{ statusText }}</span>
    </div>

    <div class="grid">
      <StatCard label="剩余库存" :value="stock" color="green" />
      <StatCard label="已售出" :value="sold" color="red" />
      <StatCard label="累计请求" :value="reqCount" color="blue" />
      <StatCard label="QPS" :value="qps" color="yellow" />
    </div>

    <div class="chart-container">
      <h3>库存消耗进度</h3>
      <ProgressBar
        label="已售出"
        :current="soldPct"
        suffix="%"
        :max="100"
        color-class="sold"
      />
      <ProgressBar
        label="剩余库存"
        :current="remainPct"
        suffix="%"
        :max="100"
        color-class="remain"
      />
    </div>

    <div class="chart-container">
      <h3>QPS 实时曲线</h3>
      <ProgressBar
        label="当前 QPS / 限流阈值 5000"
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
.page {
  background: #1a1a2e;
  color: #eee;
  min-height: 100vh;
  padding: 30px;
}

h1 {
  text-align: center;
  margin-bottom: 8px;
  font-size: 24px;
}

.subtitle {
  text-align: center;
  color: #888;
  font-size: 13px;
  margin-bottom: 30px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  max-width: 900px;
  margin: 0 auto 30px;
}

.chart-container {
  max-width: 900px;
  margin: 0 auto 30px;
  background: #16213e;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #0f3460;
}

.chart-container h3 {
  margin-bottom: 16px;
  font-size: 14px;
  color: #888;
}

.controls {
  max-width: 900px;
  margin: 0 auto 20px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.controls input {
  width: 80px;
  padding: 8px 12px;
  border: 1px solid #0f3460;
  border-radius: 6px;
  background: #16213e;
  color: #eee;
  font-size: 14px;
  text-align: center;
}

.controls label {
  font-size: 13px;
  color: #888;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-start {
  background: #e74c3c;
  color: #fff;
}

.btn-start:disabled {
  background: #555;
  cursor: not-allowed;
}

.btn-reset {
  background: #2c3e50;
  color: #ccc;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.idle {
  background: #2c3e50;
  color: #888;
}

.status-badge.running {
  background: #e74c3c22;
  color: #e74c3c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
