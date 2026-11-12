package com.polysius.applocker.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityServiceListTest {

    private val spellings = listOf(
        "com.polysius.applocker/com.polysius.applocker.blocking.AppLockerAccessibilityService",
        "com.polysius.applocker/.blocking.AppLockerAccessibilityService",
    )

    @Test
    fun `null or empty setting means the service is off`() {
        assertFalse(AccessibilityServiceList.isListed(null, spellings))
        assertFalse(AccessibilityServiceList.isListed("", spellings))
    }

    @Test
    fun `finds the full spelling among other services`() {
        val setting = "other.app/other.app.Service:${spellings[0]}"
        assertTrue(AccessibilityServiceList.isListed(setting, spellings))
    }

    @Test
    fun `finds the short spelling`() {
        assertTrue(AccessibilityServiceList.isListed(spellings[1], spellings))
    }

    @Test
    fun `ignores other services`() {
        val setting = "other.app/other.app.Service:another.app/.Service"
        assertFalse(AccessibilityServiceList.isListed(setting, spellings))
    }
}
