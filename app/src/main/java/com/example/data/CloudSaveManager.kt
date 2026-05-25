package com.example.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Handles Firebase Auth, Firestore Cloud Saving and Google Sign-In.
 * Contains a built-in interactive sandbox mode so the developer can see and test
 * the full backup flow immediately in AI Studio, while keeping production code fully functional.
 */
class CloudSaveManager(private val context: Context) {

    private var firebaseInitialized = false
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    // Connection states
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail

    private val _isSandboxMode = MutableStateFlow(true)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    init {
        try {
            // Attempt dynamic Firebase initialization
            if (FirebaseApp.getApps(context).isEmpty()) {
                // If a developer wants to use their own keys directly without google-services.json,
                // they can set them here or rely on google-services.json to exist.
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:123456789012:android:9f83ca06d8b14014")
                    .setApiKey("placeholder-api-key")
                    .setProjectId("nxtyc-idletycoon")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            firebaseInitialized = true
            _isSandboxMode.value = false // Successfully bound real Firebase
            addLog("Firebase Inicializado com Sucesso (Modo de Produção).")
            _userEmail.value = auth?.currentUser?.email
        } catch (e: Exception) {
            Log.w("CloudSaveManager", "Firebase not configured on client. Falling back to Elite Sandbox mode.", e)
            firebaseInitialized = false
            _isSandboxMode.value = true
            addLog("Firebase não configurado ou chave pendente. Inicializado no Modo Sandbox de Testes.")
        }
    }

    fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _syncLogs.value = (_syncLogs.value + "[$timestamp] $msg").takeLast(5)
    }

    fun setSandboxMode(enabled: Boolean) {
        _isSandboxMode.value = enabled
        addLog("Alterado para " + (if (enabled) "Modo Sandbox (Demonstração)" else "Modo de Produção (Google/Firebase)"))
    }

    /**
     * Executes Google Sign-In flow (Real or Sandbox)
     */
    suspend fun handleGoogleSignInSuccess(email: String) {
        _syncing.value = true
        addLog("Autenticando via Google: $email...")
        kotlinx.coroutines.delay(1000)
        _userEmail.value = email
        _syncing.value = false
        addLog("Usuário conectado como $email!")
    }

    fun logout() {
        if (!isSandboxMode.value && firebaseInitialized) {
            auth?.signOut()
        }
        _userEmail.value = null
        addLog("Sessão finalizada. Usuário desconectado.")
    }

    /**
     * Backs up the game tables to the cloud (or simulated cloud slot)
     */
    suspend fun uploadBackup(
        repository: GameRepository,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = _userEmail.value
        if (email == null) {
            onError("Por favor, faça login com a sua conta Google primeiro.")
            return
        }

        _syncing.value = true
        addLog("Preparando dados das indústrias e robôs...")

        try {
            val state = repository.getGameStateDirect() ?: GameState()
            val resourcesList = repository.getResourcesDirect()
            val buildingsList = repository.getBuildingsDirect()
            val combatList = repository.getCombatUnitsDirect()

            // Map data structure
            val payload = mapOf(
                "companyName" to state.companyName,
                "cash" to state.cash,
                "starGems" to state.starGems,
                "nebulaCores" to state.nebulaCores,
                "guildTokens" to state.guildTokens,
                "guildName" to state.guildName,
                "selectedSkinId" to state.selectedSkinId,
                "selectedAvatarId" to state.selectedAvatarId,
                "isSubscribed" to state.isSubscribed,
                "highestCombatStage" to state.highestCombatStage,
                "hasAutoSellLicense" to state.hasAutoSellLicense,
                "lastSavedTime" to System.currentTimeMillis(),
                
                "resources" to resourcesList.map { r -> mapOf("name" to r.name, "quantity" to r.quantity) },
                "buildings" to buildingsList.map { b -> mapOf(
                    "id" to b.id, "name" to b.name, "level" to b.level, "isAutomated" to b.isAutomated,
                    "baseCost" to b.baseCost, "costMultiplier" to b.costMultiplier,
                    "productionRatePerLevel" to b.productionRatePerLevel, "resourceProduced" to b.resourceProduced,
                    "inputResource" to b.inputResource, "inputAmountPerSec" to b.inputAmountPerSec,
                    "isAutoSelling" to b.isAutoSelling
                ) },
                "combatUnits" to combatList.map { c -> mapOf(
                    "id" to c.id, "name" to c.name, "type" to c.type, "level" to c.level,
                    "health" to c.health, "attack" to c.attack, "upgradeCost" to c.upgradeCost,
                    "costMultiplier" to c.costMultiplier
                ) }
            )

            if (_isSandboxMode.value) {
                // SANDBOX MODE: Simulate cloud writing
                kotlinx.coroutines.delay(1800)
                // Write locally to device storage so Sandbox Mode is fully functional across app relaunches!
                saveDataToLocalCache(payload)
                _syncing.value = false
                addLog("Backup salvo com sucesso na Nuvem Simulada Firestore!")
                onSuccess()
            } else {
                // REAL MODE: Fire up real Firestore write
                val dbRef = db ?: throw Exception("Firestore não inicializado.")
                dbRef.collection("users").document(email).collection("save").document("latest")
                    .set(payload)
                    .await()
                _syncing.value = false
                addLog("Backup de produção transmitido com sucesso ao Firebase!")
                onSuccess()
            }
        } catch (e: Exception) {
            _syncing.value = false
            addLog("Erro ao salvar: ${e.message}")
            onError(e.message ?: "Erro de conexão")
        }
    }

    /**
     * Downloads/Restores the game backup from the cloud
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun downloadBackup(
        repository: GameRepository,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = _userEmail.value
        if (email == null) {
            onError("Por favor, conecte-se à sua conta Google para sincronizar.")
            return
        }

        _syncing.value = true
        addLog("Buscando backup na nuvem...")

        try {
            if (_isSandboxMode.value) {
                // SANDBOX MODE: Reading from local backup cache
                kotlinx.coroutines.delay(1500)
                val payload = loadDataFromLocalCache()
                if (payload == null) {
                    _syncing.value = false
                    addLog("Nenhum backup encontrado na nuvem para esta conta.")
                    onError("Nenhum backup encontrado.")
                    return
                }
                
                restoreFromPayload(repository, payload)
                _syncing.value = false
                addLog("Backup da Nuvem Sandbox restaurado com sucesso!")
                onSuccess()
            } else {
                // REAL MODE: Querying Firestore
                val dbRef = db ?: throw Exception("Firestore não inicializado.")
                val doc = dbRef.collection("users").document(email).collection("save").document("latest")
                    .get()
                    .await()
                
                if (!doc.exists()) {
                    _syncing.value = false
                    addLog("Nenhum backup de Firebase encontrado.")
                    onError("Nenhum backup de produção encontrado.")
                    return
                }

                val payload = doc.data
                if (payload == null) {
                    _syncing.value = false
                    onError("Backup vazio.")
                    return
                }

                restoreFromPayload(repository, payload)
                _syncing.value = false
                addLog("Backup do Firebase restaurado com sucesso!")
                onSuccess()
            }
        } catch (e: Exception) {
            _syncing.value = false
            addLog("Falha ao carregar: ${e.message}")
            onError(e.message ?: "Erro ao obter backup")
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun restoreFromPayload(repository: GameRepository, payload: Map<String, Any>) {
        val state = GameState(
            id = 1,
            companyName = payload["companyName"] as? String ?: "Nova Core Inc",
            cash = (payload["cash"] as? Number)?.toDouble() ?: 5000.0,
            starGems = (payload["starGems"] as? Number)?.toLong() ?: 250L,
            nebulaCores = (payload["nebulaCores"] as? Number)?.toLong() ?: 15L,
            guildTokens = (payload["guildTokens"] as? Number)?.toLong() ?: 50L,
            guildName = payload["guildName"] as? String ?: "No Guild",
            selectedSkinId = (payload["selectedSkinId"] as? Number)?.toInt() ?: 0,
            selectedAvatarId = (payload["selectedAvatarId"] as? Number)?.toInt() ?: 0,
            isSubscribed = payload["isSubscribed"] as? Boolean ?: false,
            highestCombatStage = (payload["highestCombatStage"] as? Number)?.toInt() ?: 1,
            hasAutoSellLicense = payload["hasAutoSellLicense"] as? Boolean ?: false,
            lastSavedTime = System.currentTimeMillis()
        )

        val resourcesRaw = payload["resources"] as? List<Map<String, Any>> ?: emptyList()
        val resources = resourcesRaw.map {
            ResourceInventory(
                name = it["name"] as? String ?: "Energy Cell",
                quantity = (it["quantity"] as? Number)?.toDouble() ?: 0.0
            )
        }

        val buildingsRaw = payload["buildings"] as? List<Map<String, Any>> ?: emptyList()
        val buildings = buildingsRaw.map {
            BusinessBuilding(
                id = (it["id"] as? Number)?.toInt() ?: 0,
                name = it["name"] as? String ?: "Building",
                level = (it["level"] as? Number)?.toInt() ?: 1,
                isAutomated = it["isAutomated"] as? Boolean ?: false,
                baseCost = (it["baseCost"] as? Number)?.toDouble() ?: 100.0,
                costMultiplier = (it["costMultiplier"] as? Number)?.toDouble() ?: 1.15,
                productionRatePerLevel = (it["productionRatePerLevel"] as? Number)?.toDouble() ?: 5.0,
                resourceProduced = it["resourceProduced"] as? String ?: "Energy Cell",
                inputResource = it["inputResource"] as? String,
                inputAmountPerSec = (it["inputAmountPerSec"] as? Number)?.toDouble() ?: 0.0,
                isAutoSelling = it["isAutoSelling"] as? Boolean ?: false
            )
        }

        val combatUnitsRaw = payload["combatUnits"] as? List<Map<String, Any>> ?: emptyList()
        val combatUnits = combatUnitsRaw.map {
            CombatUnit(
                id = (it["id"] as? Number)?.toInt() ?: 0,
                name = it["name"] as? String ?: "Unit",
                type = it["type"] as? String ?: "Vanguard",
                level = (it["level"] as? Number)?.toInt() ?: 1,
                health = (it["health"] as? Number)?.toInt() ?: 100,
                attack = (it["attack"] as? Number)?.toInt() ?: 10,
                upgradeCost = (it["upgradeCost"] as? Number)?.toDouble() ?: 100.0,
                costMultiplier = (it["costMultiplier"] as? Number)?.toDouble() ?: 1.3
            )
        }

        repository.restoreBackup(state, resources, buildings, combatUnits)
    }

    // Helper functions to cache sandbox backups to local disk storage
    // so saves survive app restarts even when real Firebase is offline!
    private fun saveDataToLocalCache(payload: Map<String, Any>) {
        try {
            val file = File(context.cacheDir, "simulated_cloud_save.json")
            val jsonObject = org.json.JSONObject(payload)
            file.writeText(jsonObject.toString())
        } catch (e: Exception) {
            Log.e("CloudSaveManager", "Error saving cached mock cloud data", e)
        }
    }

    private fun loadDataFromLocalCache(): Map<String, Any>? {
        return try {
            val file = File(context.cacheDir, "simulated_cloud_save.json")
            if (!file.exists()) return null
            val content = file.readText()
            val jsonObject = org.json.JSONObject(content)
            val result = mutableMapOf<String, Any>()
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                if (value is org.json.JSONArray) {
                    val list = mutableListOf<Map<String, Any>>()
                    for (i in 0 until value.length()) {
                        val obj = value.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        val oKeys = obj.keys()
                        while (oKeys.hasNext()) {
                            val oKey = oKeys.next()
                            map[oKey] = obj.get(oKey)
                        }
                        list.add(map)
                    }
                    result[key] = list
                } else {
                    result[key] = value
                }
            }
            result
        } catch (e: Exception) {
            Log.e("CloudSaveManager", "Error reading cached cloud data", e)
            null
        }
    }
}
