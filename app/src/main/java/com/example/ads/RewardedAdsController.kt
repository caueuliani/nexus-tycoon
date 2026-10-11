package com.example.ads

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.BuildConfig
import com.example.data.GameRepository
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.rewarded.*
import com.google.android.ump.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.resume

data class RewardedAdsState(
    val busy: Boolean = false,
    val message: String = "Assista a um anúncio para ganhar 5 gemas.",
    val ageBand: String? = null,
    val privacyRequired: Boolean = false,
    val rewardConfirmation: Boolean = false
)

class RewardedAdsController(private val activity: Activity) {
    private val scope = (activity as LifecycleOwner).lifecycleScope
    private val repository = GameRepository.getInstance(activity.applicationContext)
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private val preferences = activity.getSharedPreferences("ad_privacy", android.content.Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(RewardedAdsState(ageBand = preferences.getString("age_band", null)))
    val state = mutableState.asStateFlow()
    private var attempt = 0
    private var shownAttempt = -1
    private var timeout: Job? = null

    private fun privacyRequired() = consent.privacyOptionsRequirementStatus ==
        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private fun finish(message: String) {
        timeout?.cancel()
        mutableState.value = state.value.copy(busy = false, message = message, privacyRequired = privacyRequired())
    }

    fun dismissRewardConfirmation() {
        mutableState.value = state.value.copy(rewardConfirmation = false)
    }

    fun cancelPending() {
        if (state.value.busy && shownAttempt != attempt) {
            attempt++
            finish("Carregamento cancelado.")
        }
    }

    fun setAgeBand(age: String?) {
        if (state.value.busy) return
        require(age == null || age in setOf("under13", "teen", "adult"))
        preferences.edit().putString("age_band", age).apply()
        mutableState.value = state.value.copy(ageBand = age)
    }

    fun watch() {
        if (state.value.busy || activity.isFinishing || activity.isDestroyed) return
        if (BuildConfig.REWARDED_AD_UNIT_ID.isBlank()) {
            finish("Anúncios indisponíveis nesta versão.")
            return
        }
        if (state.value.ageBand == null || state.value.ageBand == "under13") {
            finish("Anúncios disponíveis apenas para jogadores a partir de 13 anos.")
            return
        }
        val request = ++attempt
        mutableState.value = state.value.copy(busy = true, message = "Verificando privacidade…")
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(state.value.ageBand != "adult").build(), {
            if (request != attempt || activity.isDestroyed) return@requestConsentInfoUpdate
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                if (request == attempt) afterConsent(request)
            }
        }, { _ -> if (request == attempt) afterConsent(request) })
    }

    private fun afterConsent(request: Int) {
        if (!consent.canRequestAds()) {
            finish("Não foi possível liberar anúncios. Confira sua conexão ou tente novamente mais tarde.")
            return
        }
        mutableState.value = state.value.copy(message = "Carregando anúncio…", privacyRequired = privacyRequired())
        timeout = scope.launch {
            delay(45000)
            if (request == attempt && state.value.busy) {
                attempt++
                finish("O anúncio demorou para carregar. Tente novamente.")
            }
        }
        scope.launch {
            try {
                initializeSdk(activity.applicationContext, state.value.ageBand != "adult")
                if (request != attempt || activity.isDestroyed || !consent.canRequestAds()) return@launch
                RewardedAd.load(AdRequest.Builder(BuildConfig.REWARDED_AD_UNIT_ID).build(),
                    object : AdLoadCallback<RewardedAd> {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            scope.launch { if (request == attempt) finish("Nenhum anúncio disponível agora. Tente mais tarde." +
                                if (BuildConfig.DEBUG) " (${adError.code}: ${adError.message})" else "") }
                        }
                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardScope.launch {
                                if (request != attempt || activity.isDestroyed || activity.isFinishing || !consent.canRequestAds()) {
                                    ad.destroy()
                                    return@launch
                                }
                                if (!(activity as LifecycleOwner).lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                                    ad.destroy()
                                    finish("Volte ao jogo e tente carregar o anúncio novamente.")
                                    return@launch
                                }
                                show(ad, request)
                            }
                        }
                    })
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                android.util.Log.w("RewardedAds", "Ad setup failed", e)
                if (request == attempt) finish("Não foi possível carregar o anúncio." +
                    if (BuildConfig.DEBUG) " (${e.javaClass.simpleName}: ${e.message})" else "")
            }
        }
    }

    private fun show(ad: RewardedAd, request: Int) {
        if (shownAttempt == request) return
        shownAttempt = request
        timeout?.cancel()
        val token = UUID.randomUUID().toString()
        var earned = false
        val confirmation = RewardConfirmationGate()
        fun confirmIfReady(ready: Boolean) {
            if (ready && request == attempt) mutableState.value = state.value.copy(rewardConfirmation = true)
        }
        mutableState.value = state.value.copy(message = "Anúncio em exibição…")
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                rewardScope.launch {
                    ad.destroy()
                    confirmIfReady(confirmation.closed())
                    if (request == attempt) finish(if (earned) "Recompensa recebida: 5 gemas." else "Anúncio fechado. Recompensa não recebida.")
                }
            }
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                rewardScope.launch {
                    ad.destroy()
                    if (request == attempt) finish("Não foi possível exibir o anúncio. Tente novamente.")
                }
            }
        }
        try {
            ad.show(activity) { _ ->
                // Application-lived scope lets the local transaction finish during Activity recreation.
                rewardScope.launch {
                    try {
                        val credited = repository.grantAdReward(token)
                        earned = true
                        confirmIfReady(confirmation.credited())
                        if (credited && request == attempt) {
                            mutableState.value = state.value.copy(message = "Recompensa recebida: 5 gemas.")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RewardedAds", "Reward persistence failed", e)
                        if (request == attempt) mutableState.value = state.value.copy(message = "Não foi possível salvar a recompensa. Tente reabrir o jogo.")
                    }
                }
            }
        } catch (e: Exception) {
            ad.destroy()
            finish("Não foi possível exibir o anúncio.")
        }
    }

    fun showPrivacyOptions() {
        if (state.value.busy) return
        mutableState.value = state.value.copy(busy = true)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            finish(if (error == null) "Preferências de privacidade atualizadas." else "Não foi possível abrir as preferências agora.")
        }
    }

    companion object {
        private val sdkMutex = Mutex()
        private var initialization: Deferred<Unit>? = null
        private val rewardScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private suspend fun initializeSdk(context: android.content.Context, underAge: Boolean) {
          val config = RequestConfiguration.Builder()
                .setTagForUnderAgeOfConsent(if (underAge)
                    RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                    else RequestConfiguration.TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_PG)
                .build()
          val pending = sdkMutex.withLock {
            initialization ?: rewardScope.async(Dispatchers.IO) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    MobileAds.initialize(context, InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).setRequestConfiguration(config).build()) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }.also { initialization = it }
          }
          try {
              pending.await()
              // Next-Gen SDK rejects MobileAds configuration calls before initialization.
              // Initial privacy flags are already supplied in InitializationConfig above.
              MobileAds.setRequestConfiguration(config)
          }
          catch (e: CancellationException) { throw e }
          catch (e: Exception) {
              sdkMutex.withLock { if (initialization === pending) initialization = null }
              throw e
          }
        }
    }
}
