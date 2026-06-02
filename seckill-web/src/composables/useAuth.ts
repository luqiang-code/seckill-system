import { ref, computed } from 'vue'
import type { AuthData } from '../api'
import { logout as apiLogout } from '../api'

export function getToken(): string | null {
  return localStorage.getItem('token')
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem('token', token)
  else localStorage.removeItem('token')
}

export function getUsername(): string | null {
  return localStorage.getItem('username')
}

export function setUsername(username: string | null) {
  if (username) localStorage.setItem('username', username)
  else localStorage.removeItem('username')
}

const token = ref(getToken())
const username = ref(getUsername())

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  function login(data: AuthData) {
    token.value = data.token
    username.value = data.username
    setToken(data.token)
    setUsername(data.username)
  }

  async function logout() {
    try {
      await apiLogout()
    } catch {
      // ignore
    }
    token.value = null
    username.value = null
    setToken(null)
    setUsername(null)
  }

  return { token, username, isLoggedIn, login, logout }
}
