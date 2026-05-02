import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { usePolling } from '@/composables/usePolling'

describe('usePolling', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should start polling and call function immediately', () => {
    const fn = vi.fn().mockResolvedValue(undefined)
    const { polling, start } = usePolling(fn, 2000)

    start()

    expect(polling.value).toBe(true)
    // Called immediately on start
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('should call function at intervals', async () => {
    const fn = vi.fn().mockResolvedValue(undefined)
    const { start } = usePolling(fn, 2000)

    start()
    vi.advanceTimersByTime(6000) // 3 intervals + immediate

    expect(fn).toHaveBeenCalledTimes(4) // immediate + 3 intervals
  })

  it('should not start twice', () => {
    const fn = vi.fn().mockResolvedValue(undefined)
    const { start } = usePolling(fn, 2000)

    start()
    start() // second call should be ignored

    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('should stop polling', () => {
    const fn = vi.fn().mockResolvedValue(undefined)
    const { polling, start, stop } = usePolling(fn, 2000)

    start()
    stop()

    expect(polling.value).toBe(false)
    const callCount = fn.mock.calls.length

    vi.advanceTimersByTime(10000)
    // No more calls after stop
    expect(fn).toHaveBeenCalledTimes(callCount)
  })

  it('should restart after stop', () => {
    const fn = vi.fn().mockResolvedValue(undefined)
    const { start, stop } = usePolling(fn, 2000)

    start()
    stop()
    start()

    // Called immediately on restart
    expect(fn).toHaveBeenCalledTimes(2) // original start + restart start
  })
})
