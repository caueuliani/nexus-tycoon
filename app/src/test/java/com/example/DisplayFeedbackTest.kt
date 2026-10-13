package com.example

import com.example.ads.RewardConfirmationGate
import com.example.ui.compactNumber
import org.junit.Assert.*
import org.junit.Test

class DisplayFeedbackTest {
    @Test fun compactNumbersCoverScalesAndBoundaries() {
        val cases = mapOf(
            999.0 to "999", 999.994 to "999,99", 999.995 to "1K",
            1000.0 to "1K", 999_999.0 to "1M", 1_000_000.0 to "1M",
            999_999_999.0 to "1B", 1e9 to "1B", 999_999_999_999.0 to "1T",
            1e12 to "1T", 1e15 to "1aa", 1e18 to "1ab", 1e21 to "1ac",
            1e90 to "1az", 1e93 to "1ba", 100_000.0 to "100K",
            1_250_000.0 to "1,25M", -1_250.0 to "-1,25K"
        )
        cases.forEach { (value, expected) -> assertEquals(expected, compactNumber(value)) }
        assertTrue(compactNumber(Double.MAX_VALUE).length < 10)
        assertEquals("—", compactNumber(Double.NaN))
        assertEquals("∞", compactNumber(Double.POSITIVE_INFINITY))
        assertEquals("−∞", compactNumber(Double.NEGATIVE_INFINITY))
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
