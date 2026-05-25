import { ref, computed, onUnmounted } from 'vue'

export function useCountdown(windowDuration: number, intervalDuration: number) {
  const remaining = ref(windowDuration)
  const isWindowOpen = ref(true)

  let roundStart = Date.now()
  let timer: ReturnType<typeof setInterval>

  function tick() {
    const now = Date.now()
    const elapsed = (now - roundStart) / 1000

    isWindowOpen.value = elapsed < windowDuration

    if (isWindowOpen.value) {
      remaining.value = windowDuration - elapsed
    } else {
      const nextStart = roundStart + intervalDuration * 1000
      const wait = (nextStart - now) / 1000

      if (wait <= 0) {
        roundStart = now
        remaining.value = windowDuration
      } else {
        remaining.value = wait
      }
    }
  }

  timer = setInterval(tick, 200)
  tick() // 立即执行一次，避免等待 200ms

  onUnmounted(() => clearInterval(timer))

  const status = computed<'open' | 'waiting'>(() =>
    isWindowOpen.value ? 'open' : 'waiting'
  )

  return { remaining, status, isWindowOpen }
}
