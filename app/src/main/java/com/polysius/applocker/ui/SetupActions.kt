package com.polysius.applocker.ui

import com.polysius.applocker.permissions.PermissionKind

/** What the setup screens can ask the Activity to do. The Activity owns the system launchers. */
class SetupActions(
    /** Opens the system screen or dialog where the user grants [PermissionKind]. */
    val onGrant: (PermissionKind) -> Unit,
    val onOpenAppInfo: () -> Unit,
    val onOpenHealthConnectSettings: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
)
