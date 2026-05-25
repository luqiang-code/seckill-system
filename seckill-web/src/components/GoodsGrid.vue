<script setup lang="ts">
import GoodsCard from './GoodsCard.vue'
import type { Goods } from '../api/types'

defineProps<{
  goodsList: Goods[]
  disabled: boolean
  userId: string
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
      :disabled="disabled"
      :user-id="userId"
      @sold="emit('sold')"
    />
  </div>
</template>

<style scoped>
.goods-grid {
  max-width: 720px;
  margin: 20px auto;
  padding: 0 16px;
  display: grid;
  gap: 16px;
}
</style>
