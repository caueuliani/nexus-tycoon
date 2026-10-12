package com.example.billing

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.example.BuildConfig
import com.example.data.GameRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

enum class PurchasePhase { UNAVAILABLE, AVAILABLE, LOADING, PENDING, COMPLETED, CANCELLED, ERROR }
data class PurchaseUiState(val phase: PurchasePhase = PurchasePhase.UNAVAILABLE,
    val message: String = "Compra indisponível.", val price: String? = null)
enum class PlayProduct(val id: String, val type: String) {
    VIP(BuildConfig.PLAY_VIP_SUBSCRIPTION_ID, BillingClient.ProductType.SUBS),
    AUTO_SELL(BuildConfig.PLAY_AUTOSELL_PRODUCT_ID, BillingClient.ProductType.INAPP)
}

/** Billing transport. Entitlements are written only after the configured server verifies a token. */
class PlayBillingController(private val activity: Activity) : PurchasesUpdatedListener {
    private val scope = (activity as LifecycleOwner).lifecycleScope
    private val repository = GameRepository.getInstance(activity.applicationContext)
    private val verifier = PlayPurchaseVerifier()
    private val mutex = Mutex()
    private val details = mutableMapOf<PlayProduct, ProductDetails>()
    private val states = MutableStateFlow(PlayProduct.entries.associateWith { PurchaseUiState() })
    val state = states.asStateFlow()
    private var connecting = false
    private val client = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private fun update(product: PlayProduct, phase: PurchasePhase, message: String,
        price: String? = states.value[product]?.price) {
        states.value = states.value + (product to PurchaseUiState(phase, message, price))
    }

    fun onResume() {
        if (!verifier.configured) {
            PlayProduct.entries.forEach { update(it, PurchasePhase.UNAVAILABLE,
                "Compras reais aguardam servidor de verificação.") }
            if (!BuildConfig.DEBUG) scope.launch {
                repository.revokeVipWhenAbsent()
                repository.revokePlayLicenseWhenAbsent()
            }
            return
        }
        if (connecting) return
        connecting = true
        PlayProduct.entries.forEach { update(it, PurchasePhase.LOADING, "Consultando Google Play…") }
        scope.launch {
            try {
                if (repository.getGameStateDirect() == null) repository.checkAndInitialize()
            }
            catch (_: Exception) {
                connecting = false
                PlayProduct.entries.forEach { update(it, PurchasePhase.ERROR, "Progresso local indisponível.") }
                return@launch
            }
            if (client.isReady) {
                connecting = false
                loadProducts()
                reconcile()
                return@launch
            }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    PlayProduct.entries.forEach { update(it, PurchasePhase.ERROR,
                        "Google Play indisponível (${result.responseCode}).") }
                    return
                }
                scope.launch { loadProducts(); reconcile() }
            }
            override fun onBillingServiceDisconnected() {
                connecting = false
                PlayProduct.entries.forEach { update(it, PurchasePhase.ERROR,
                    "Conexão com Google Play interrompida.") }
            }
        })
        }
    }

    private suspend fun loadProducts() {
        for (product in PlayProduct.entries) {
            if (product.id.isBlank()) {
                update(product, PurchasePhase.UNAVAILABLE, "Produto ainda não cadastrado na Play Console.")
                continue
            }
            val response = CompletableDeferred<Pair<BillingResult, QueryProductDetailsResult>>()
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(product.id)
                    .setProductType(product.type).build()
            )).build()
            client.queryProductDetailsAsync(params) { result, products -> response.complete(result to products) }
            val responsePair = withTimeoutOrNull(15_000) { response.await() }
            if (responsePair == null) {
                update(product, PurchasePhase.ERROR, "Consulta de produto expirou.")
                continue
            }
            val (result, products) = responsePair
            val item = products.productDetailsList.firstOrNull { it.productId == product.id }
            if (result.responseCode != BillingClient.BillingResponseCode.OK || item == null) {
                update(product, PurchasePhase.UNAVAILABLE, "Produto não disponível para esta conta/dispositivo.")
                continue
            }
            details[product] = item
            val price = if (product == PlayProduct.VIP)
                item.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            else item.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            update(product, PurchasePhase.AVAILABLE, "Disponível no Google Play.", price)
        }
    }

    fun buy(product: PlayProduct) {
        if (!verifier.configured || product.id.isBlank()) {
            update(product, PurchasePhase.UNAVAILABLE, "Compra real indisponível nesta versão.")
            return
        }
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            update(product, PurchasePhase.ERROR, "Entre com sua conta Google antes de comprar.")
            return
        }
        if (states.value[product]?.phase != PurchasePhase.AVAILABLE) return
        val item = details[product] ?: return
        val offer = if (product == PlayProduct.VIP)
            item.subscriptionOfferDetails?.firstOrNull()?.offerToken
        else item.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
        val itemParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(item)
        if (offer != null) itemParams.setOfferToken(offer)
        update(product, PurchasePhase.LOADING, "Aguardando Google Play…")
        val result = client.launchBillingFlow(activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(itemParams.build())).build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            update(product, PurchasePhase.ERROR, "Não foi possível iniciar a compra (${result.responseCode}).")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> PlayProduct.entries.forEach {
                if (states.value[it]?.phase == PurchasePhase.LOADING)
                    update(it, PurchasePhase.CANCELLED, "Compra cancelada.")
            }
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { purchase ->
                scope.launch { process(purchase) }
            }
            else -> PlayProduct.entries.forEach {
                if (states.value[it]?.phase == PurchasePhase.LOADING)
                    update(it, PurchasePhase.ERROR, "Falha na compra (${result.responseCode}).")
            }
        }
    }

    private suspend fun reconcile() {
        if (!verifier.configured) return
        val subscriptions = queryOwned(BillingClient.ProductType.SUBS)
        val products = queryOwned(BillingClient.ProductType.INAPP)
        if (subscriptions == null || products == null) {
            repository.revokeVipWhenAbsent()
            repository.revokePlayLicenseWhenAbsent()
            PlayProduct.entries.forEach { update(it, PurchasePhase.ERROR,
                "Não foi possível reconciliar compras com Google Play.") }
            return
        }
        var activeVip = false
        var activeLicense = false
        for (purchase in subscriptions + products) {
            val verified = process(purchase)
            if (verified && PlayProduct.VIP.id in purchase.products) activeVip = true
            if (verified && PlayProduct.AUTO_SELL.id in purchase.products) activeLicense = true
        }
        if (!activeVip) repository.revokeVipWhenAbsent()
        if (!activeLicense) repository.revokePlayLicenseWhenAbsent()
    }

    private suspend fun queryOwned(type: String): List<Purchase>? {
        val response = CompletableDeferred<Pair<BillingResult, List<Purchase>>>()
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) {
            result, purchases -> response.complete(result to purchases)
        }
        val (result, purchases) = withTimeoutOrNull(15_000) { response.await() } ?: return null
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases else null
    }

    private suspend fun process(purchase: Purchase): Boolean = mutex.withLock {
        val product = PlayProduct.entries.firstOrNull { it.id.isNotBlank() && it.id in purchase.products }
            ?: return@withLock false
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            update(product, PurchasePhase.PENDING, "Pagamento pendente; nenhum benefício foi liberado.")
            return@withLock false
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return@withLock false
        try {
            update(product, PurchasePhase.LOADING, "Verificando compra…")
            val verdict = verifier.verify(product.id, purchase.purchaseToken)
            if (!verdict.active) {
                update(product, PurchasePhase.ERROR, "Compra sem direito ativo confirmado.")
                return@withLock false
            }
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(purchase.purchaseToken.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            check(repository.getGameStateDirect() != null)
            repository.applyVerifiedPlayPurchase(hash, product.id,
                vip = product == PlayProduct.VIP, firstVipGift = verdict.firstVipGift)
            if (!purchase.isAcknowledged) {
                val acknowledged = try {
                    val ack = CompletableDeferred<BillingResult>()
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()) { ack.complete(it) }
                    withTimeoutOrNull(15_000) { ack.await() }?.responseCode == BillingClient.BillingResponseCode.OK
                } catch (_: Exception) { false }
                if (!acknowledged) {
                    update(product, PurchasePhase.ERROR,
                        "Benefício salvo, mas a confirmação na Play está pendente; tentaremos novamente.")
                    return@withLock true
                }
            }
            update(product, PurchasePhase.COMPLETED, "Compra verificada e aplicada.")
            true
        } catch (_: Exception) {
            update(product, PurchasePhase.ERROR, "Verificação indisponível; benefício não liberado nesta tentativa.")
            false
        }
    }

    fun close() = client.endConnection()
}
