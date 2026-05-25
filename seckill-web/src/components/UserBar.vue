<script setup lang="ts">
const model = defineModel<string>({ required: true })
</script>

<template>
  <div class="user-bar">
    <label>用户 ID</label>
    <input
      v-model="model"
      type="text"
      placeholder="自动生成"
    />
  </div>
</template>

<style scoped>
.user-bar {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 16px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  max-width: 200px;
}

input:focus {
  border-color: #e74c3c;
}
</style>
