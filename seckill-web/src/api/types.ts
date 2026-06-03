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
export const PRODUCT_META: Record<number, { emoji: string; color: string; name: string }> = {
  1:  { emoji: '🔮', color: '#e8e0f5', name: '魔法双面镜' },
  2:  { emoji: '📖', color: '#e3f2fd', name: '有求必应笔记本' },
  3:  { emoji: '👂', color: '#fce4ec', name: '伸缩耳' },
  4:  { emoji: '⏳', color: '#fff8e1', name: '时间转换器' },
  5:  { emoji: '🗺️', color: '#e0f2f1', name: '活点地图' },
  6:  { emoji: '📻', color: '#fff3e0', name: '魔法收音机' },
  7:  { emoji: '♟️', color: '#e8eaf6', name: '巫师棋' },
  8:  { emoji: '🧹', color: '#d7ccc8', name: '光轮2026飞天扫帚' },
}
