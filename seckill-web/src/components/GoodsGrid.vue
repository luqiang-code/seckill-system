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
  <div class="diagon-grid">
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
.diagon-grid {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(440px, 1fr));
  gap: 20px;
}

@media (max-width: 920px) {
  .diagon-grid {
    grid-template-columns: 1fr;
  }
}
</style>
