package com.example.ui

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

fun compactNumber(value: Double): String {
    if (value.isNaN()) return "—"
    if (value.isInfinite()) return if (value > 0) "∞" else "−∞"
    var scaled = abs(value)
    var group = 0
    while (scaled >= 1000.0) { scaled /= 1000.0; group++ }
    if (round(scaled * 100.0) / 100.0 >= 1000.0) { scaled /= 1000.0; group++ }
    val suffix = when (group) {
        0 -> ""
        1 -> "K"
        2 -> "M"
        3 -> "B"
        4 -> "T"
        else -> {
            var index = group - 5 + 27
            var letters = ""
            while (index > 0) {
                index--
                letters = ('a'.code + index % 26).toChar() + letters
                index /= 26
            }
            letters
        }
    }
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        isGroupingUsed = false
        maximumFractionDigits = 2
    }
    return (if (value < 0) "-" else "") + formatter.format(scaled) + suffix
}
