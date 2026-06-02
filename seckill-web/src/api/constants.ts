// Seckill window timing
export const WINDOW_DURATION = 10
export const INTERVAL_DURATION = 25

// Payment deadline
export const PAYMENT_DEADLINE_MS = 15 * 60 * 1000

// UX debounce
export const DEBOUNCE_MS = 1000

export function formatCountdown(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m + '分' + s + '秒'
}
