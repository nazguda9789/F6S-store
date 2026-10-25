import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '../lib/api'
export const usePos = defineStore('pos', () => {
  const drafts = ref([]),
    activeId = ref(null),
    busy = ref(false),
    error = ref(''),
    quote = ref(null),
    receipt = ref(null)
  const active = computed(() => drafts.value.find((d) => d.id === activeId.value))
  let createKey = '',
    pendingScan = null
  function update(d) {
    const index = drafts.value.findIndex((x) => x.id === d.id)
    if (index < 0) drafts.value.push(d)
    else drafts.value[index] = d
    quote.value = null
    return d
  }
  async function execute(work) {
    if (busy.value) return
    busy.value = true
    error.value = ''
    try {
      return await work()
    } catch (e) {
      error.value = e.message
      throw e
    } finally {
      busy.value = false
    }
  }
  async function load() {
    return execute(async () => {
      drafts.value = await api('/admin/pos/drafts')
      if (!drafts.value.some((d) => d.id === activeId.value))
        activeId.value = drafts.value[0]?.id ?? null
      quote.value = null
    })
  }
  async function create() {
    return execute(async () => {
      if (!createKey) createKey = crypto.randomUUID()
      const d = await api('/admin/pos/drafts', {
        method: 'POST',
        body: JSON.stringify({ requestKey: createKey }),
      })
      createKey = ''
      update(d)
      activeId.value = d.id
    })
  }
  async function scan(code, quantity = 1) {
    return execute(async () => {
      if (!active.value) throw new Error('Hãy tạo hoặc chọn hóa đơn tạm')
      const id = activeId.value
      if (
        !pendingScan ||
        pendingScan.id !== id ||
        pendingScan.code !== code ||
        pendingScan.quantity !== quantity
      )
        pendingScan = { id, code, quantity, eventKey: crypto.randomUUID() }
      const { eventKey } = pendingScan
      const d = await api(`/admin/pos/drafts/${id}/scan`, {
        method: 'POST',
        body: JSON.stringify({ code, quantity, eventKey }),
      })
      pendingScan = null
      update(d)
    })
  }
  async function quantity(variantId, value) {
    return execute(async () =>
      update(
        await api(`/admin/pos/drafts/${activeId.value}/lines/${variantId}`, {
          method: 'PUT',
          body: JSON.stringify({ quantity: value }),
        }),
      ),
    )
  }
  async function remove(variantId) {
    return execute(async () =>
      update(
        await api(`/admin/pos/drafts/${activeId.value}/lines/${variantId}`, { method: 'DELETE' }),
      ),
    )
  }
  async function cancel() {
    return execute(async () => {
      const id = activeId.value
      await api(`/admin/pos/drafts/${id}`, { method: 'DELETE' })
      drafts.value = drafts.value.filter((d) => d.id !== id)
      activeId.value = drafts.value[0]?.id ?? null
      quote.value = null
    })
  }
  async function price(promotionCode) {
    return execute(async () => {
      quote.value = await api(`/admin/pos/drafts/${activeId.value}/quote`, {
        method: 'POST',
        body: JSON.stringify({ promotionCode }),
      })
    })
  }
  async function pay(cashReceived, promotionCode) {
    return execute(async () => {
      const id = activeId.value
      receipt.value = await api(`/admin/pos/drafts/${id}/checkout`, {
        method: 'POST',
        body: JSON.stringify({
          cashReceived,
          promotionCode,
          expectedTotal: quote.value?.total ?? active.value.subtotal,
        }),
      })
      drafts.value = drafts.value.filter((d) => d.id !== id)
      activeId.value = drafts.value[0]?.id ?? null
      quote.value = null
    })
  }
  function select(id) {
    activeId.value = id
    quote.value = null
  }
  async function customer(customerId) {
    return execute(async () =>
      update(
        await api(`/admin/pos/drafts/${activeId.value}/customer`, {
          method: 'PUT',
          body: JSON.stringify({ customerId }),
        }),
      ),
    )
  }
  window.addEventListener('f6s-session-cleared', () => {
    drafts.value = []
    activeId.value = null
    quote.value = null
    receipt.value = null
    pendingScan = null
    createKey = ''
    error.value = ''
  })
  return {
    drafts,
    activeId,
    active,
    busy,
    error,
    quote,
    receipt,
    load,
    create,
    scan,
    quantity,
    remove,
    cancel,
    price,
    pay,
    select,
    customer,
  }
})
