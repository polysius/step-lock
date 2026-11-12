package com.polysius.applocker.permissions

/** The grants blocking needs, in the order the first-launch flow asks for them (FR-48). */
enum class PermissionKind {
    ACCESSIBILITY,
    OVERLAY,
    USAGE_ACCESS,
    HEALTH_CONNECT,
    BATTERY,
    NOTIFICATIONS,
}

enum class GrantState {
    GRANTED,
    MISSING,

    /** The enforcement path chosen does not use this permission, so it is never requested. */
    NOT_NEEDED,

    /** The device cannot provide this at all (for example, Health Connect is absent). */
    UNAVAILABLE,
}

/** True when this state means the user still has something to do. */
val GrantState.needsAttention: Boolean
    get() = this == GrantState.MISSING || this == GrantState.UNAVAILABLE
