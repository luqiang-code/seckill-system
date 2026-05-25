import type { ApiResponse, Goods, OrderInfo } from './types'

class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const resp = await fetch(url, options)
  const body: ApiResponse<T> = await resp.json()

  if (resp.status === 429) {
    throw new ApiError(429, '请求太频繁')
  }

  if (body.code === 1) {
    return body.data
  }

  throw new ApiError(body.code || 0, body.message || '请求失败')
}

export function fetchGoods(): Promise<Goods[]> {
  return request<Goods[]>('/goods/list')
}

export function doSeckill(goodsId: number, userId: string): Promise<void> {
  return request<void>(`/seckill/do/${goodsId}?userId=${encodeURIComponent(userId)}`, { method: 'POST' })
}

export function fetchStock(goodsId: number): Promise<number> {
  return request<number>(`/seckill/stock/${goodsId}`)
}

export function fetchResult(goodsId: number, userId: string): Promise<OrderInfo> {
  return request<OrderInfo>(`/seckill/result/${goodsId}?userId=${encodeURIComponent(userId)}`)
}

export { ApiError }
