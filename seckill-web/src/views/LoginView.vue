<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePasskey } from '../composables/usePasskey'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const route = useRoute()
const { registerPasskey, authenticateWithPasskey, authenticateDiscoverable } = usePasskey()
const { login } = useAuth()

const username = ref('')
const message = ref('')
const loading = ref(false)

const notice = computed(() => {
  if (route.query.reason === 'logout') return '你已离开古灵阁金库'
  if (route.query.reason === 'auth') return '请出示你的金库密钥'
  return ''
})

function setError(msg: string) {
  message.value = msg
}

function mapError(err: unknown): string {
  if (err instanceof Error) {
    if (err.name === 'NotAllowedError' || err.message.includes('cancel')) return '咒语已取消'
    if (err.message.includes('用户名已被注册')) return '该名字已在古灵阁注册'
    if (err.message.includes('未注册 Passkey')) return '该巫师未注册金库密钥，请先创建'
    if (err.message.includes('超时')) return '咒语超时，请重试'
    return err.message
  }
  return '魔法失败，请重试'
}

async function handleRegister() {
  if (!username.value.trim()) {
    setError('请输入你的巫师名字')
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
    setError('请输入你的巫师名字')
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
  <div class="vault-page">
    <div class="brick-wall"></div>

    <div class="vault-card">
      <div class="vault-arch">
        <div class="arch-top">✦ ✦ ✦</div>
        <h1 class="vault-title">古 灵 阁</h1>
        <p class="vault-subtitle">Gringotts Wizarding Bank</p>
        <div class="arch-bottom">✦ ✦ ✦</div>
      </div>

      <p class="vault-desc">金库密钥 · 通行无阻</p>

      <p v-if="notice" class="notice-text">{{ notice }}</p>

      <div class="input-group">
        <label class="input-label">巫师名号</label>
        <input
          v-model="username"
          type="text"
          placeholder="输入你的巫师名字..."
          :disabled="loading"
          @keydown="handleEnter"
        />
      </div>

      <div class="button-group">
        <button class="btn-vault primary" :disabled="loading" @click="handleRegister">
          🔑 创建新密钥
        </button>
        <button class="btn-vault primary" :disabled="loading" @click="handleUsernameLogin">
          🏛️ 密钥登录
        </button>
        <button class="btn-vault secondary" :disabled="loading" @click="handleDiscoverLogin">
          ✨ 无痕显形
        </button>
      </div>

      <p v-if="loading" class="loading-text">✦ 正在验证魔法身份...</p>
      <p v-if="message" class="error-text">{{ message }}</p>

      <p class="hint-text">
        支持古灵阁金钥匙、巫师生日印记、Windows 魔杖与 Android 生物魔法
      </p>
    </div>

    <div class="vault-footer">
      <span>⚡</span> 对角巷 · 魔法限时抢购 <span>⚡</span>
    </div>
  </div>
</template>

<style scoped>
.vault-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #1a1410;
  background-image:
    radial-gradient(ellipse at 30% 20%, rgba(139, 105, 20, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 80%, rgba(116, 0, 1, 0.08) 0%, transparent 50%);
  position: relative;
  overflow: hidden;
}

.brick-wall {
  position: absolute;
  inset: 0;
  background-image:
    repeating-linear-gradient(0deg, transparent, transparent 31px, rgba(0,0,0,0.15) 31px, rgba(0,0,0,0.15) 32px),
    repeating-linear-gradient(90deg, transparent, transparent 99px, rgba(0,0,0,0.1) 99px, rgba(0,0,0,0.1) 100px);
  opacity: 0.3;
  pointer-events: none;
}

.vault-card {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: 90vw;
  background: linear-gradient(180deg, #2c1f14 0%, #1e1610 100%);
  border: 2px solid #5c0000;
  border-radius: 4px;
  padding: 44px 36px;
  box-shadow:
    0 0 60px rgba(116, 0, 1, 0.15),
    0 20px 60px rgba(0, 0, 0, 0.5);
  text-align: center;
}

.vault-card::before {
  content: '';
  position: absolute;
  top: 8px; left: 8px; right: 8px; bottom: 8px;
  border: 1px solid rgba(212, 175, 55, 0.15);
  border-radius: 2px;
  pointer-events: none;
}

/* arch */
.vault-arch {
  margin-bottom: 20px;
}

.arch-top, .arch-bottom {
  color: #D4AF37;
  font-size: 12px;
  letter-spacing: 8px;
  opacity: 0.6;
}

.arch-top { margin-bottom: 12px; }
.arch-bottom { margin-top: 4px; }

.vault-title {
  font-family: 'Pirata One', serif;
  font-size: 42px;
  letter-spacing: 0.2em;
  color: #D4AF37;
  text-shadow: 0 0 30px rgba(212, 175, 55, 0.3), 0 2px 4px rgba(0,0,0,0.5);
  margin: 0;
  line-height: 1.2;
}

.vault-subtitle {
  font-family: 'Pirata One', serif;
  font-size: 14px;
  color: #8b7355;
  letter-spacing: 0.15em;
  margin-top: 4px;
}

.vault-desc {
  font-family: 'Cormorant Garamond', serif;
  color: #b8a080;
  font-size: 15px;
  font-style: italic;
  margin-bottom: 24px;
}

.notice-text {
  background: rgba(212, 175, 55, 0.1);
  color: #D4AF37;
  border: 1px solid rgba(212, 175, 55, 0.2);
  padding: 10px 14px;
  border-radius: 2px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  margin-bottom: 16px;
  font-style: italic;
}

/* input */
.input-group { margin-bottom: 20px; text-align: left; }

.input-label {
  display: block;
  font-family: 'Pirata One', serif;
  font-size: 14px;
  color: #b8a080;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}

input {
  width: 100%;
  padding: 12px 16px;
  background: #1a1008;
  border: 2px solid #3d2820;
  border-radius: 2px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 17px;
  color: #e0d5c1;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

input:focus {
  border-color: #D4AF37;
  box-shadow: 0 0 12px rgba(212, 175, 55, 0.15);
}

input::placeholder {
  color: #665540;
  font-style: italic;
}

/* buttons */
.button-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-vault {
  padding: 13px;
  border: none;
  border-radius: 2px;
  font-family: 'Pirata One', serif;
  font-size: 18px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-vault:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-vault.primary {
  color: #D4AF37;
  background: linear-gradient(180deg, #740001, #5c0000);
  border: 1px solid #8b0000;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}

.btn-vault.primary:hover:not(:disabled) {
  background: linear-gradient(180deg, #8b0000, #740001);
  box-shadow: 0 0 20px rgba(116, 0, 1, 0.4);
}

.btn-vault.secondary {
  color: #b8a080;
  background: linear-gradient(180deg, #2c1f14, #1e1610);
  border: 1px solid #3d2820;
}

.btn-vault.secondary:hover:not(:disabled) {
  border-color: #D4AF37;
  color: #D4AF37;
}

.loading-text {
  font-family: 'Cormorant Garamond', serif;
  color: #D4AF37;
  margin-top: 16px;
  font-style: italic;
  animation: shimmer 1.5s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.error-text {
  font-family: 'Cormorant Garamond', serif;
  color: #e74c3c;
  margin-top: 16px;
  font-size: 15px;
  word-break: break-all;
}

.hint-text {
  margin-top: 28px;
  font-family: 'Cormorant Garamond', serif;
  font-size: 12px;
  color: #665540;
  font-style: italic;
  line-height: 1.6;
}

.vault-footer {
  position: relative;
  z-index: 1;
  margin-top: 32px;
  font-family: 'Pirata One', serif;
  font-size: 14px;
  color: #665540;
  letter-spacing: 0.1em;
}

.vault-footer span {
  color: #D4AF37;
  opacity: 0.5;
}
</style>
