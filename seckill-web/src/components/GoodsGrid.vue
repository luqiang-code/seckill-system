<script setup lang="ts">
import GoodsCard from './GoodsCard.vue'
import type { Goods } from '../api/types'

defineProps<{
  goodsList: Goods[]
  initialStocks: Record<number, number>
  disabled: boolean
}>()

const emit = defineEmits<{
  sold: []
}>()
</script>

<template>
  <div class="goods-grid">
    <GoodsCard
      v-for="g in goodsList"
      :key="g.id"
      :goods="g"
      :initial-stock="initialStocks[g.id] ?? g.stock"
      :disabled="disabled"
      @sold="emit('sold')"
    />
  </div>
</template>

<style scoped>
.goods-grid {
  max-width: 960px;
  margin: 20px auto;
  padding: 0 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 16px;
}

@media (max-width: 900px) {
  .goods-grid {
    grid-template-columns: 1fr;
  }
}
</style>
