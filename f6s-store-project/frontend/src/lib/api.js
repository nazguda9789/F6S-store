export const money = (value) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value)
export async function api(path, options = {}) {
  const response = await fetch((import.meta.env.VITE_API_BASE || '/api') + path, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  const body = await response.json().catch(() => null)
  if (!response.ok)
    throw new Error(body?.message || `Yêu cầu thất bại (${response.status}). Vui lòng thử lại.`)
  return body
}
