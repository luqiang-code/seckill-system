import { ref, onUnmounted, type Ref } from 'vue'

export function usePolling<T>(
  fetcher: () => Promise<T>,
  interval: number,
  immediate = true
) {
  const data: Ref<T | undefined> = ref()
  const error: Ref<Error | null> = ref(null)
  const isActive = ref(false)

  let timer: ReturnType<typeof setInterval>

  async function execute() {
    try {
      data.value = await fetcher()
      error.value = null
    } catch (e) {
      error.value = e as Error
    }
  }

  function start() {
    if (isActive.value) return
    isActive.value = true
    if (immediate) execute()
    timer = setInterval(execute, interval)
  }

  function stop() {
    isActive.value = false
    clearInterval(timer)
  }

  // auto-start
  start()

  onUnmounted(stop)

  return { data, error, isActive, start, stop }
}
