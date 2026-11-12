package com.polysius.applocker.ui

import androidx.annotation.StringRes
import com.polysius.applocker.R
import com.polysius.applocker.permissions.GrantState
import com.polysius.applocker.permissions.PermissionKind

@get:StringRes
val PermissionKind.titleRes: Int
    get() = when (this) {
        PermissionKind.ACCESSIBILITY -> R.string.accessibility_title
        PermissionKind.OVERLAY -> R.string.overlay_title
        PermissionKind.USAGE_ACCESS -> R.string.usage_access_title
        PermissionKind.HEALTH_CONNECT -> R.string.health_connect_title
        PermissionKind.BATTERY -> R.string.battery_title
        PermissionKind.NOTIFICATIONS -> R.string.notifications_title
    }

@get:StringRes
val PermissionKind.whyRes: Int
    get() = when (this) {
        PermissionKind.ACCESSIBILITY -> R.string.accessibility_why
        PermissionKind.OVERLAY -> R.string.overlay_why
        PermissionKind.USAGE_ACCESS -> R.string.usage_access_why
        PermissionKind.HEALTH_CONNECT -> R.string.health_connect_why
        PermissionKind.BATTERY -> R.string.battery_why
        PermissionKind.NOTIFICATIONS -> R.string.notifications_why
    }

@get:StringRes
val GrantState.labelRes: Int
    get() = when (this) {
        GrantState.GRANTED -> R.string.state_granted
        GrantState.MISSING -> R.string.state_missing
        GrantState.NOT_NEEDED -> R.string.state_not_needed
        GrantState.UNAVAILABLE -> R.string.state_unavailable
    }

/** Android withholds these two grants from sideloaded apps until restricted settings are allowed. */
val PermissionKind.mayBeRestricted: Boolean
    get() = this == PermissionKind.ACCESSIBILITY || this == PermissionKind.USAGE_ACCESS
