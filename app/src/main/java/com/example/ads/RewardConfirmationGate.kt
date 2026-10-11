package com.example.ads

// Show confirmation once, only after both dismissal and committed payment.
internal class RewardConfirmationGate {
    private var dismissed = false
    private var paid = false
    private var delivered = false
    fun closed(): Boolean { dismissed = true; return ready() }
    fun credited(): Boolean { paid = true; return ready() }
    private fun ready(): Boolean {
        if (!dismissed || !paid || delivered) return false
        delivered = true
        return true
    }
}
