import { ref, onBeforeUnmount } from 'vue'

export function usePolling(fn: () => Promise<void>, intervalMs = 2000) {
  const polling = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    if (polling.value) return
    polling.value = true
    fn()
    timer = setInterval(fn, intervalMs)
  }

  function stop() {
    polling.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  onBeforeUnmount(stop)

  return { polling, start, stop }
}
