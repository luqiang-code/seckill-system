import { ref, computed } from 'vue'
import type { AuthData } from '../api'
import { logout as apiLogout } from '../api'

const token = ref(localStorage.getItem('token'))
const username = ref(localStorage.getItem('username'))

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  function login(data: AuthData) {
    token.value = data.token
    username.value = data.username
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
  }

  async function logout() {
    try {
      await apiLogout()
    } catch {
      // ignore
    }
    token.value = null
    username.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  return { token, username, isLoggedIn, login, logout }
}
