<script setup lang="ts">
import { ref } from 'vue'
import CountdownBar from '../components/CountdownBar.vue'
import UserBar from '../components/UserBar.vue'
import GoodsGrid from '../components/GoodsGrid.vue'
import { useCountdown } from '../composables/useCountdown'
import { usePolling } from '../composables/usePolling'
import { fetchGoods } from '../api'
import type { Goods } from '../api/types'

const WINDOW_DURATION = 10
const INTERVAL_DURATION = 25

const { remaining, status, isWindowOpen } = useCountdown(WINDOW_DURATION, INTERVAL_DURATION)

const { data: goodsList } = usePolling<Goods[]>(fetchGoods, 5000)

const userId = ref(randomUserId())

function randomUserId(): string {
  return 'user_' + Math.random().toString(36).substring(2, 10)
}

function onSold() {
  fetchGoods().then(list => {
    if (list) goodsList.value = list
  })
}
</script>

<template>
  <div class="page">
    <header class="header">
      <h1>秒杀抢购</h1>
      <div class="subtitle">限时特惠 · 手慢无</div>
    </header>

    <CountdownBar :remaining="remaining" :status="status" />

    <UserBar v-model="userId" />

    <GoodsGrid
      :goods-list="goodsList ?? []"
      :disabled="!isWindowOpen"
      :user-id="userId"
      @sold="onSold"
    />
  </div>
</template>

<style scoped>
.header {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  color: #fff;
  padding: 30px 20px;
  text-align: center;
}

.header h1 {
  font-size: 28px;
  margin-bottom: 4px;
}

.header .subtitle {
  font-size: 13px;
  opacity: 0.8;
}
</style>
