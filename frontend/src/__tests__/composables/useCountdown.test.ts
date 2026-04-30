import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useCountdown } from '@/composables/useCountdown'

describe('useCountdown', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-01T12:00:00').getTime())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should show expired when deadline is in the past', () => {
    const pastDate = new Date('2026-05-01T00:00:00')
    const { display, remaining } = useCountdown(pastDate)

    expect(remaining.value.expired).toBe(true)
    expect(display.value).toBe('已截止')
  })

  it('should show days and hours when deadline is far away', () => {
    const futureDate = new Date('2026-06-04T15:00:00') // ~3 days 3 hours
    const { display, remaining } = useCountdown(futureDate)

    expect(remaining.value.expired).toBe(false)
    expect(remaining.value.days).toBe(3)
    // display format: "3天3时" (approximately)
    expect(display.value).toContain('天')
  })

  it('should show hours and minutes when less than 1 day', () => {
    const futureDate = new Date('2026-06-01T14:30:00') // 2.5 hours ahead
    const { display, remaining } = useCountdown(futureDate)

    expect(remaining.value.expired).toBe(false)
    expect(remaining.value.days).toBe(0)
    expect(display.value).not.toContain('天')
  })

  it('should update after timer ticks', async () => {
    const futureDate = new Date('2026-06-01T12:00:05') // 5 seconds ahead
    const { remaining } = useCountdown(futureDate)

    expect(remaining.value.seconds).toBe(5)

    vi.advanceTimersByTime(3000)
    expect(remaining.value.seconds).toBe(2)

    vi.advanceTimersByTime(3000)
    expect(remaining.value.expired).toBe(true)
  })

  it('should handle Date string input', () => {
    const { remaining } = useCountdown('2026-07-01T00:00:00')

    expect(remaining.value.expired).toBe(false)
    expect(remaining.value.days).toBeGreaterThan(0)
  })
})
