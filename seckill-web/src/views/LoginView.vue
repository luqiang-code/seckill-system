<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePasskey } from '../composables/usePasskey'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { registerPasskey, authenticateWithPasskey, authenticateDiscoverable } = usePasskey()
const { login } = useAuth()

const username = ref('')
const message = ref('')
const loading = ref(false)

function setError(msg: string) {
  message.value = msg
}

function mapError(err: unknown): string {
  if (err instanceof Error) {
    if (err.name === 'NotAllowedError' || err.message.includes('cancel')) return '操作已取消'
    if (err.message.includes('用户名已被注册')) return '用户名已被注册'
    if (err.message.includes('未注册 Passkey')) return '该用户未注册 Passkey，请先注册'
    if (err.message.includes('超时')) return '操作超时，请重试'
    return err.message
  }
  return '操作失败，请重试'
}

async function handleRegister() {
  if (!username.value.trim()) {
    setError('请输入用户名')
    return
  }
  loading.value = true
  setError('')
  try {
    const data = await registerPasskey(username.value.trim())
    login(data)
    router.push('/')
  } catch (err) {
    setError(mapError(err))
  } finally {
    loading.value = false
  }
}

async function handleDiscoverLogin() {
  loading.value = true
  setError('')
  try {
    const data = await authenticateDiscoverable()
    login(data)
    router.push('/')
  } catch (err) {
    setError(mapError(err))
  } finally {
    loading.value = false
  }
}

async function handleUsernameLogin() {
  if (!username.value.trim()) {
    setError('请输入用户名')
    return
  }
  loading.value = true
  setError('')
  try {
    const data = await authenticateWithPasskey(username.value.trim())
    login(data)
    router.push('/')
  } catch (err) {
    setError(mapError(err))
  } finally {
    loading.value = false
  }
}

function handleEnter(e: KeyboardEvent) {
  if (e.key === 'Enter' && username.value.trim()) {
    handleUsernameLogin()
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>秒杀系统</h1>
      <p class="subtitle">Passkey 通行密钥登录</p>

      <div class="input-group">
        <input
          v-model="username"
          type="text"
          placeholder="输入用户名"
          :disabled="loading"
          @keydown="handleEnter"
        />
      </div>

      <div class="button-group">
        <button class="btn-primary" :disabled="loading" @click="handleRegister">
          注册新设备
        </button>
        <button class="btn-primary" :disabled="loading" @click="handleUsernameLogin">
          输入用户名登录
        </button>
        <button class="btn-secondary" :disabled="loading" @click="handleDiscoverLogin">
          通过指纹/面容登录
        </button>
      </div>

      <p v-if="loading" class="loading-text">等待生物识别验证...</p>
      <p v-if="message" class="error-text">{{ message }}</p>

      <p class="hint">
        支持 Touch ID、Face ID、Windows Hello 和 Android 生物识别
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  width: 400px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  text-align: center;
}

h1 {
  margin: 0;
  font-size: 28px;
  color: #1a1a2e;
}

.subtitle {
  color: #666;
  margin: 8px 0 32px;
}

.input-group {
  margin-bottom: 20px;
}

input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

input:focus {
  border-color: #e74c3c;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

button {
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.2s;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #e74c3c;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #c0392b;
}

.btn-secondary {
  background: #3498db;
  color: #fff;
}

.btn-secondary:hover:not(:disabled) {
  background: #2980b9;
}

.loading-text {
  color: #e67e22;
  margin-top: 16px;
}

.error-text {
  color: #e74c3c;
  margin-top: 16px;
  word-break: break-all;
}

.hint {
  margin-top: 24px;
  font-size: 12px;
  color: #999;
}
</style>
