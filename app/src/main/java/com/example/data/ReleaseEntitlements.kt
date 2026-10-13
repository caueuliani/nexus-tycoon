package com.example.data

/** A local snapshot is not proof of a paid entitlement. Play rights restored from
 * a backup must be reconciled with Play and the verifier before use. */
internal fun withoutUnverifiedReleaseEntitlements(
    state: GameState,
    restoredFromBackup: Boolean = false
): GameState {
    val keepLicense = state.hasAutoSellLicense && when (state.autoSellLicenseSource) {
        "GEMS" -> true
        "PLAY" -> !restoredFromBackup
        else -> false
    }
    return state.copy(
        isSubscribed = false,
        hasAutoSellLicense = keepLicense,
        autoSellLicenseSource = if (keepLicense) state.autoSellLicenseSource else "NONE"
    )
}
