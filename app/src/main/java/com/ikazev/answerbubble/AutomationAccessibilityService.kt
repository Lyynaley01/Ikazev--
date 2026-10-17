package com.ikazev.answerbubble

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject

/**
 * Automation is intentionally restricted to one user-configured package.
 * The service does nothing until the user explicitly arms automation in the app.
 */
class AutomationAccessibilityService : AccessibilityService() {
    private var lastActionAt = 0L

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 250
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean("automation_armed", false)) return
        val allowedPackage = prefs.getString("target_package", "").orEmpty().trim()
        if (allowedPackage.isBlank() || event.packageName?.toString() != allowedPackage) return
        val pending = prefs.getString("pending_answer", null) ?: return
        if (System.currentTimeMillis() - lastActionAt < 1200) return

        val answer = try { JSONObject(pending) } catch (_: Exception) { return }
        val value = answer.optString("answer").trim()
        if (value.isBlank()) return

        val root = rootInActiveWindow ?: return
        val didAct = when (answer.optString("type", "text")) {
            "choice" -> clickMatchingOption(root, value)
            else -> fillAnswer(root, value)
        }
        if (didAct) {
            lastActionAt = System.currentTimeMillis()
            prefs.edit().remove("pending_answer").putLong("last_action_at", lastActionAt)
                .putInt("answered_count", prefs.getInt("answered_count", 0) + 1).apply()
        }
    }

    private fun clickMatchingOption(root: AccessibilityNodeInfo, answer: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText(answer).orEmpty()
        for (node in nodes) {
            var current: AccessibilityNodeInfo? = node
            repeat(5) {
                if (current?.isClickable == true && current?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
                current = current?.parent
            }
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        return false
    }

    private fun fillAnswer(root: AccessibilityNodeInfo, answer: String): Boolean {
        val editable = mutableListOf<AccessibilityNodeInfo>()
        collectEditable(root, editable)
        val field = editable.firstOrNull() ?: return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, answer) }
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun collectEditable(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isEditable && node.isEnabled) result.add(node)
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectEditable(it, result) }
    }

    override fun onInterrupt() = Unit

    companion object { const val PREFS = "ikazev_settings" }
}
