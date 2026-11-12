package com.polysius.applocker.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.polysius.applocker.blocking.AppLockerAccessibilityService

/** Reads the current state of every permission in [PermissionKind]. Never changes anything. */
class PermissionChecker(private val context: Context) {

    /** Health Connect permissions the app asks for today. Exercise and calories join in Phase 4. */
    val healthPermissions: Set<String> =
        setOf(HealthPermission.getReadPermission(StepsRecord::class))

    suspend fun checkAll(): Map<PermissionKind, GrantState> =
        PermissionKind.entries.associateWith { check(it) }

    private suspend fun check(kind: PermissionKind): GrantState = when (kind) {
        PermissionKind.ACCESSIBILITY -> grantedIf(isAccessibilityServiceEnabled())
        PermissionKind.OVERLAY -> GrantState.NOT_NEEDED
        PermissionKind.USAGE_ACCESS -> grantedIf(hasUsageAccess())
        PermissionKind.HEALTH_CONNECT -> checkHealthConnect()
        PermissionKind.BATTERY -> grantedIf(isIgnoringBatteryOptimizations())
        PermissionKind.NOTIFICATIONS -> grantedIf(hasNotificationPermission())
    }

    private fun grantedIf(granted: Boolean) =
        if (granted) GrantState.GRANTED else GrantState.MISSING

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(context, AppLockerAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return AccessibilityServiceList.isListed(
            enabledServices,
            listOf(service.flattenToString(), service.flattenToShortString()),
        )
    }

    // The (op, uid, package) overload is flagged as deprecated but is still the documented way to
    // read Usage Access without side effects; revisit when a replacement is documented.
    @Suppress("DEPRECATION")
    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private suspend fun checkHealthConnect(): GrantState {
        val status = HealthConnectClient.getSdkStatus(context)
        if (status != HealthConnectClient.SDK_AVAILABLE) return GrantState.UNAVAILABLE

        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return grantedIf(granted.containsAll(healthPermissions))
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun hasNotificationPermission(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        return result == PackageManager.PERMISSION_GRANTED
    }
}
