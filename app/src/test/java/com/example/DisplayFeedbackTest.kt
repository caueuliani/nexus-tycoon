package com.example

import com.example.ads.RewardConfirmationGate
import com.example.ui.compactNumber
import org.junit.Assert.*
import org.junit.Test

class DisplayFeedbackTest {
    @Test fun compactNumbersCoverScalesAndBoundaries() {
        val cases = mapOf(999.0 to "999", 1000.0 to "1K", 100000.0 to "100K", 1250000.0 to "1,25M", 1e9 to "1B", 1e12 to "1T", 1e15 to "1aa", 1e18 to "1ab", 1e21 to "1ac", 1e90 to "1az", 1e93 to "1ba", 999999.0 to "1M", -1250.0 to "-1,25K")
        cases.forEach { (value, expected) -> assertEquals(expected, compactNumber(value)) }
        assertTrue(compactNumber(Double.MAX_VALUE).length < 10)
    }
    @Test fun confirmationWaitsForPaymentAndDismissalInEitherOrder() {
        val paymentFirst = RewardConfirmationGate()
        assertFalse(paymentFirst.credited())
        assertTrue(paymentFirst.closed())
        assertFalse(paymentFirst.closed())
        assertFalse(paymentFirst.credited())
        val closedFirst = RewardConfirmationGate()
        assertFalse(closedFirst.closed())
        assertTrue(closedFirst.credited())
        assertFalse(closedFirst.credited())
        val unpaid = RewardConfirmationGate()
        assertFalse(unpaid.closed())
    }
}
