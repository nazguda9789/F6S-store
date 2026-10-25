import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api, setCustomerToken } from '../lib/api'
export const useCustomer = defineStore('customer', () => {
  const profile = ref(null)
  function clear() {
    profile.value = null
    setCustomerToken('')
  }
  async function login(challengeId, code) {
    const result = await api('/auth/verify', {
      method: 'POST',
      body: JSON.stringify({ challengeId, code }),
    })
    setCustomerToken(result.token)
    profile.value = result.profile
  }
  async function save(data) {
    profile.value = await api('/me/profile', { method: 'PUT', body: JSON.stringify(data) })
  }
  async function logout() {
    try {
      await api('/me/logout', { method: 'POST' })
    } finally {
      clear()
    }
  }
  window.addEventListener('f6s-customer-expired', clear)
  return { profile, login, save, logout }
})
