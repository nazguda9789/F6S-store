export const money = (value) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value)
let adminAuthorization = ''
export function setAdminAuthorization(value) {
  adminAuthorization = value
}
export async function api(path, options = {}) {
  const response = await fetch((import.meta.env.VITE_API_BASE || '/api') + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(path.startsWith('/admin') && adminAuthorization
        ? { Authorization: adminAuthorization }
        : {}),
      ...options.headers,
    },
  })
  const body = await response.json().catch(() => null)
  if (response.status === 401 && path.startsWith('/admin'))
    window.dispatchEvent(new Event('f6s-auth-expired'))
  if (!response.ok)
    throw new Error(body?.message || `Yêu cầu thất bại (${response.status}). Vui lòng thử lại.`)
  return body
}
