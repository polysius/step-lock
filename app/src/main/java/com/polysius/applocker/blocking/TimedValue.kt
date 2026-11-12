package com.polysius.applocker.blocking

/**
 * Computes a value and reuses it for [ttlMillis]. Kept free of Android classes so the expiry logic
 * can be unit tested. The first call always computes, whatever the clock reads.
 */
class TimedValue<T>(
    private val ttlMillis: Long,
    private val now: () -> Long,
    private val compute: () -> T,
) {
    private var value: T? = null
    private var computedAt: Long? = null

    fun get(): T {
        val at = computedAt
        val current = value
        if (at != null && current != null && now() - at < ttlMillis) return current

        val fresh = compute()
        value = fresh
        computedAt = now()
        return fresh
    }
}
