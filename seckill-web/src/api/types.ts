export interface Goods {
  id: number
  name: string
  price: number
  stock: number
}

export interface OrderInfo {
  id: number
  goodsId: number
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
