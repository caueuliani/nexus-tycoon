package com.example.billing

import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Contract for a trusted server. No URL is bundled in the app, so release checkout is off by default. */
internal class PlayPurchaseVerifier {
    data class Verdict(val active: Boolean, val firstVipGift: Boolean)

    val configured: Boolean get() = BuildConfig.PLAY_PURCHASE_VERIFICATION_URL.isNotBlank()
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()

    suspend fun verify(productId: String, token: String): Verdict {
        check(configured) { "Servidor de verificação de compras não configurado." }
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Entre com sua conta Google antes de comprar.")
        val authToken = suspendCancellableCoroutine<String> { continuation ->
            user.getIdToken(false).addOnSuccessListener { result ->
                val value = result.token
                if (continuation.isActive) {
                    if (value.isNullOrBlank()) continuation.resumeWithException(IllegalStateException("Login expirado."))
                    else continuation.resume(value)
                }
            }.addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
        return withContext(Dispatchers.IO) {
            val body = JSONObject().put("packageName", BuildConfig.APPLICATION_ID)
                .put("productId", productId).put("purchaseToken", token).toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(BuildConfig.PLAY_PURCHASE_VERIFICATION_URL)
                .header("Authorization", "Bearer $authToken").post(body).build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Verificação da compra indisponível (${response.code})." }
                val answer = JSONObject(response.body?.string() ?: "")
                require(answer.getString("productId") == productId)
                Verdict(answer.getBoolean("active"), answer.optBoolean("firstVipGift", false))
            }
        }
    }
}
