import type { ApiResponse, AuthData, Goods, OrderInfo } from './types'

class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

function getToken(): string | null {
  return localStorage.getItem('token')
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options?.headers as Record<string, string>),
  }

  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const resp = await fetch(url, { ...options, headers })

  if (resp.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    window.location.hash = '#/login'
    throw new ApiError(401, '未登录')
  }

  if (resp.status === 429) {
    throw new ApiError(429, '请求太频繁')
  }

  const body: ApiResponse<T> = await resp.json()

  if (body.code === 1) {
    return body.data
  }

  throw new ApiError(body.code || 0, body.message || '请求失败')
}

// ==================== Auth ====================

export async function registerOptions(username: string): Promise<any> {
  return request('/auth/register/options', {
    method: 'POST',
    body: JSON.stringify({ username }),
  })
}

export async function registerVerify(username: string, registrationResponse: any): Promise<AuthData> {
  return request('/auth/register/verify', {
    method: 'POST',
    body: JSON.stringify({ username, registrationResponse }),
  })
}

export async function loginOptions(username: string): Promise<any> {
  return request('/auth/login/options', {
    method: 'POST',
    body: JSON.stringify({ username }),
  })
}

export async function loginVerify(username: string, authenticationResponse: any): Promise<AuthData> {
  return request('/auth/login/verify', {
    method: 'POST',
    body: JSON.stringify({ username, authenticationResponse }),
  })
}

export async function discoverOptions(): Promise<any> {
  return request('/auth/discover', { method: 'POST' })
}

export async function discoverVerify(authenticationResponse: any): Promise<AuthData> {
  return request('/auth/discover/verify', {
    method: 'POST',
    body: JSON.stringify({ authenticationResponse }),
  })
}

export async function fetchCurrentUser(): Promise<{ userId: string }> {
  return request('/auth/me')
}

export async function logout(): Promise<void> {
  await request('/auth/logout', { method: 'POST' })
}

export async function getTestToken(userId: string): Promise<{ token: string; userId: string }> {
  return request(`/auth/test-token?userId=${encodeURIComponent(userId)}`, { method: 'POST' })
}

// ==================== Goods & Seckill ====================

export function fetchGoods(): Promise<Goods[]> {
  return request<Goods[]>('/goods/list')
}

export function doSeckill(goodsId: number): Promise<void> {
  return request<void>(`/seckill/do/${goodsId}`, { method: 'POST' })
}

export function fetchStock(goodsId: number): Promise<number> {
  return request<number>(`/seckill/stock/${goodsId}`)
}

export function fetchResult(goodsId: number): Promise<OrderInfo> {
  return request<OrderInfo>(`/seckill/result/${goodsId}`)
}

// For stress testing: use per-user token
export async function doSeckillWithToken(goodsId: number, token: string): Promise<void> {
  const resp = await fetch(`/seckill/do/${goodsId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
  })
  if (resp.status === 429) throw new ApiError(429, '请求太频繁')
  const body = await resp.json()
  if (body.code !== 1) throw new ApiError(body.code || 0, body.message || '请求失败')
}

export { ApiError }
