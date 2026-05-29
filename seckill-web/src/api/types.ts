export interface Goods {
  id: number
  name: string
  price: number
  stock: number
}

export interface OrderInfo {
  id: number
  goodsId: number
  goodsName?: string
  userId: string
  createTime: string
  status: number // 0=pending, 1=paid, 2=cancelled
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// WebAuthn types
export interface AuthData {
  token: string
  username: string
  userId?: string
}

// Product icon & color mapping
export const PRODUCT_META: Record<number, { emoji: string; color: string }> = {
  1:  { emoji: '📱', color: '#e8f5e9' },
  2:  { emoji: '💻', color: '#e3f2fd' },
  3:  { emoji: '🎧', color: '#fce4ec' },
  4:  { emoji: '⌚', color: '#f3e5f5' },
  5:  { emoji: '📋', color: '#e0f2f1' },
  6:  { emoji: '🎵', color: '#fff3e0' },
  7:  { emoji: '🎮', color: '#e8eaf6' },
  8:  { emoji: '🧹', color: '#efebe9' },
}
