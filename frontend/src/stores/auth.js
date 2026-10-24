import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api, setAdminAuthorization } from '../lib/api'
export const useAuth = defineStore('auth', () => {
  const username = ref(''),
    authenticated = computed(() => !!username.value)
  function logout() {
    username.value = ''
    setAdminAuthorization('')
    window.dispatchEvent(new Event('f6s-session-cleared'))
  }
  async function login(user, password) {
    const bytes = new TextEncoder().encode(user + ':' + password)
    setAdminAuthorization('Basic ' + btoa(String.fromCharCode(...bytes)))
    try {
      const session = await api('/admin/session')
      username.value = session.username
    } catch (e) {
      logout()
      throw e
    }
  }
  window.addEventListener('f6s-auth-expired', logout)
  return { username, authenticated, login, logout }
})
