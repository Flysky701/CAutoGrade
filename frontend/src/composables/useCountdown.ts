import { ref, computed, onBeforeUnmount } from 'vue'

export function useCountdown(deadline: string | Date) {
  const now = ref(Date.now())
  let timer: ReturnType<typeof setInterval> | null = null

  timer = setInterval(() => { now.value = Date.now() }, 1000)
  onBeforeUnmount(() => { if (timer) clearInterval(timer) })

  const remaining = computed(() => {
    const diff = new Date(deadline).getTime() - now.value
    if (diff <= 0) return { expired: true, days: 0, hours: 0, minutes: 0, seconds: 0 }
    return {
      expired: false,
      days: Math.floor(diff / 86400000),
      hours: Math.floor((diff % 86400000) / 3600000),
      minutes: Math.floor((diff % 3600000) / 60000),
      seconds: Math.floor((diff % 60000) / 1000),
    }
  })

  const display = computed(() => {
    const r = remaining.value
    if (r.expired) return '已截止'
    if (r.days > 0) return `${r.days}天${r.hours}时`
    return `${r.hours}时${r.minutes}分${r.seconds}秒`
  })

  return { remaining, display }
}
