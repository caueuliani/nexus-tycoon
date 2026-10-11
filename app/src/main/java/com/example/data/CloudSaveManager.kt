package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
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
    internal var beforeRestore: () -> Unit = {}
    var lastRestoreSummary: String = ""
        private set

    val isAvailable: Boolean get() = firebaseInitialized

    init {
        if (com.example.BuildConfig.FIREBASE_CONFIGURED) {
            try {
                val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
                check(app.options.projectId == com.example.BuildConfig.FIREBASE_PROJECT_ID)
                auth = FirebaseAuth.getInstance(app)
                db = FirebaseFirestore.getInstance(app)
                firebaseInitialized = true
                _userEmail.value = auth?.currentUser?.email
            } catch (e: Exception) {
                Log.w("CloudSaveManager", "Firebase unavailable", e)
            }
        }
        _isSandboxMode.value = false
    }
    fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _syncLogs.value = (_syncLogs.value + "[$timestamp] $msg").takeLast(5)
    }

    fun setSandboxMode(enabled: Boolean) {
        check(com.example.BuildConfig.DEBUG) { "Sandbox is restricted to debug builds" }
        _userEmail.value = null
        _isSandboxMode.value = enabled
    }

    // Explicit test fixture only; never used by the Google login UI.
    suspend fun handleGoogleSignInSuccess(email: String) {
        check(com.example.BuildConfig.DEBUG && _isSandboxMode.value)
        _userEmail.value = email
    }

    suspend fun signIn(activityContext: Context) {
        check(isAvailable) { "Login indisponível nesta versão." }
        _syncing.value = true
        try {
            val option = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(com.example.R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build()
            val result = androidx.credentials.CredentialManager.create(activityContext).getCredential(
                activityContext,
                androidx.credentials.GetCredentialRequest.Builder().addCredentialOption(option).build()
            )
            val credential = result.credential
            check(credential is androidx.credentials.CustomCredential &&
                credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            val token = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data).idToken
            val user = auth!!.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user
            check(user != null && user.email != null)
            _userEmail.value = user.email
        } finally {
            _syncing.value = false
        }
    }

    suspend fun logout() {
        if (!isSandboxMode.value && firebaseInitialized) {
            auth?.signOut()
        }
        _userEmail.value = null
        try {
            androidx.credentials.CredentialManager.create(context).clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CloudSaveManager", "Credential provider state could not be cleared", e)
        }
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
        if (_syncing.value) { onError("Sincronização já em andamento."); return }
        val email = _userEmail.value
        val sessionUid = auth?.currentUser?.uid
        if (email == null) {
            onError("Por favor, faça login com a sua conta Google primeiro.")
            return
        }

        _syncing.value = true
        addLog("Preparando dados das indústrias e robôs...")

        try {
            val snapshot = repository.exportBackup()
            val payload = createBackupPayload(snapshot)

            if (_isSandboxMode.value) {
                // SANDBOX MODE: Simulate cloud writing
                kotlinx.coroutines.delay(1800)
                // Write locally to device storage so Sandbox Mode is fully functional across app relaunches!
                saveDataToLocalCache(payload, email)
                _syncing.value = false
                addLog("Backup salvo com sucesso na Nuvem Simulada Firestore!")
                onSuccess()
            } else {
                // REAL MODE: Fire up real Firestore write
                val dbRef = db ?: throw Exception("Firestore não inicializado.")
                dbRef.collection("users").document(requireNotNull(sessionUid) { "Sessão expirada." })
                    .set(payload)
                    .await()
                _syncing.value = false
                addLog("Backup de produção transmitido com sucesso ao Firebase!")
                onSuccess()
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) { _syncing.value = false; throw e }
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
        if (_syncing.value) { onError("Sincronização já em andamento."); return }
        val email = _userEmail.value
        val sessionUid = auth?.currentUser?.uid
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
                val payload = loadDataFromLocalCache(email)
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
                val doc = dbRef.collection("users").document(requireNotNull(sessionUid) { "Sessão expirada." })
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()

                if (!doc.exists()) {
                    _syncing.value = false
                    addLog("Nenhum backup de Firebase encontrado.")
                    onError("Nenhum backup de produção encontrado.")
                    return
                }

                check(auth?.currentUser?.uid == sessionUid) { "A conta foi alterada durante a restauração." }
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
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) { _syncing.value = false; throw e }
            _syncing.value = false
            addLog("Falha ao carregar: ${e.message}")
            onError(e.message ?: "Erro ao obter backup")
        }
    }

    internal fun createBackupPayload(snapshot: BackupSnapshot): Map<String, Any> {
            val state = snapshot.state
            val resourcesList = snapshot.resources
            val buildingsList = snapshot.buildings
            val combatList = snapshot.combatUnits

            // Map data structure
            return mapOf(
                "backupVersion" to 4,
                "adReceipts" to snapshot.adReceipts.map { mapOf("token" to it.token, "rewardedAt" to it.rewardedAt) },
                "companyName" to state.companyName,
                "cash" to state.cash,
                "starGems" to state.starGems,
                "nebulaCores" to state.nebulaCores,
                "availableCores" to state.availableCores,
                "industrialEngineeringLevel" to state.industrialEngineeringLevel,
                "tacticalLogisticsLevel" to state.tacticalLogisticsLevel,
                "offlineReserveLevel" to state.offlineReserveLevel,
                "claimedCoreEvents" to state.claimedCoreEvents,
                "guildTokens" to state.guildTokens,
                "guildName" to state.guildName,
                "selectedSkinId" to state.selectedSkinId,
                "selectedAvatarId" to state.selectedAvatarId,
                "isSubscribed" to state.isSubscribed,
                "highestCombatStage" to state.highestCombatStage,
                "hasAutoSellLicense" to state.hasAutoSellLicense,
                "stage1CompletedCount" to state.stage1CompletedCount,
                "stage2CompletedCount" to state.stage2CompletedCount,
                "stage3CompletedCount" to state.stage3CompletedCount,
                "stage4CompletedCount" to state.stage4CompletedCount,
                "hasCompletedTutorial" to state.hasCompletedTutorial,
                "comrades" to state.comrades,
                "comradesPerSec" to state.comradesPerSec,
                "playerRank" to state.playerRank,
                "completedRankMissions" to state.completedRankMissions,
                "science" to state.science,
                "scienceProdLevel" to state.scienceProdLevel,
                "scienceCreditsLevel" to state.scienceCreditsLevel,
                "scienceComradeLevel" to state.scienceComradeLevel,
                "scienceCombatLevel" to state.scienceCombatLevel,
                "lastSavedTime" to System.currentTimeMillis(),

                "resources" to resourcesList.map { r -> mapOf("name" to r.name, "quantity" to r.quantity) },
                "buildings" to buildingsList.map { b -> mapOf(
                    "id" to b.id, "name" to b.name, "level" to b.level, "isAutomated" to b.isAutomated,
                    "baseCost" to b.baseCost, "costMultiplier" to b.costMultiplier,
                    "productionRatePerLevel" to b.productionRatePerLevel, "resourceProduced" to b.resourceProduced,
                    "inputResource" to b.inputResource, "inputAmountPerSec" to b.inputAmountPerSec,
                    "isAutoSelling" to b.isAutoSelling
                ) },
                "researchers" to snapshot.researchers.map { r -> mapOf(
                    "id" to r.id, "name" to r.name, "description" to r.description,
                    "level" to r.level, "cardsCollected" to r.cardsCollected,
                    "cardsNeededToUpgrade" to r.cardsNeededToUpgrade,
                    "targetBuildingId" to r.targetBuildingId, "boostType" to r.boostType,
                    "rarity" to r.rarity
                ) },
                "missions" to snapshot.missions.map { m -> mapOf(
                    "id" to m.id, "description" to m.description, "progress" to m.progress,
                    "target" to m.target, "isCompleted" to m.isCompleted, "isClaimed" to m.isClaimed,
                    "missionType" to m.missionType, "targetId" to m.targetId,
                    "targetString" to m.targetString, "rewardGems" to m.rewardGems,
                    "rewardScience" to m.rewardScience, "rewardCapsuleType" to m.rewardCapsuleType
                ) },
                "combatUnits" to combatList.map { c -> mapOf(
                    "id" to c.id, "name" to c.name, "type" to c.type, "level" to c.level,
                    "health" to c.health, "attack" to c.attack, "upgradeCost" to c.upgradeCost,
                    "costMultiplier" to c.costMultiplier
                ) }
            )

    }

    @Suppress("UNCHECKED_CAST")
    suspend fun restoreFromPayload(repository: GameRepository, payload: Map<String, Any>) {
        BackupValidation.payload(payload)
        val version = (payload["backupVersion"] as? Number)?.toInt()
        require(version == 2 || version == 3 || version == 4) {
            "Backup antigo ou incompatível. Gere um novo backup no dispositivo de origem; o progresso local foi preservado."
        }
        require(listOf("resources", "buildings", "combatUnits", "researchers", "missions").all {
            payload[it] is List<*>
        }) { "Backup incompleto; o progresso local foi preservado." }
        val scienceFields = listOf("scienceProdLevel", "scienceCreditsLevel", "scienceComradeLevel", "scienceCombatLevel")
        require(scienceFields.all { payload[it] is Number && (payload[it] as Number).toInt() >= 0 }) {
            "Pesquisas ausentes ou inválidas no backup."
        }
        if (version == 3 || version == 4) {
            require(listOf("availableCores", "industrialEngineeringLevel", "tacticalLogisticsLevel", "offlineReserveLevel").all { payload[it] is Number }) { "Laboratório ausente no backup." }
            require(payload["claimedCoreEvents"] is String) { "Resgates de eventos ausentes." }
        }
        val state = GameState(
            availableCores = if (version == 2) (payload["nebulaCores"] as? Number)?.toLong() ?: 0L else (payload["availableCores"] as Number).toLong(),
            industrialEngineeringLevel = if (version == 2) 0 else (payload["industrialEngineeringLevel"] as Number).toInt(),
            tacticalLogisticsLevel = if (version == 2) 0 else (payload["tacticalLogisticsLevel"] as Number).toInt(),
            offlineReserveLevel = if (version == 2) 0 else (payload["offlineReserveLevel"] as Number).toInt(),
            claimedCoreEvents = if (version == 2) "" else payload["claimedCoreEvents"] as String,
            id = 1,
            companyName = payload["companyName"] as? String ?: "Nova Core Inc",
            cash = (payload["cash"] as? Number)?.toDouble() ?: 5000.0,
            starGems = (payload["starGems"] as? Number)?.toLong() ?: 0L,
            nebulaCores = (payload["nebulaCores"] as? Number)?.toLong() ?: 0L,
            guildTokens = (payload["guildTokens"] as? Number)?.toLong() ?: 0L,
            guildName = payload["guildName"] as? String ?: "No Guild",
            selectedSkinId = (payload["selectedSkinId"] as? Number)?.toInt() ?: 0,
            selectedAvatarId = (payload["selectedAvatarId"] as? Number)?.toInt() ?: 0,
            isSubscribed = payload["isSubscribed"] as? Boolean ?: false,
            highestCombatStage = (payload["highestCombatStage"] as? Number)?.toInt() ?: 1,
            hasAutoSellLicense = payload["hasAutoSellLicense"] as? Boolean ?: false,
            stage1CompletedCount = (payload["stage1CompletedCount"] as? Number)?.toInt() ?: 0,
            stage2CompletedCount = (payload["stage2CompletedCount"] as? Number)?.toInt() ?: 0,
            stage3CompletedCount = (payload["stage3CompletedCount"] as? Number)?.toInt() ?: 0,
            stage4CompletedCount = (payload["stage4CompletedCount"] as? Number)?.toInt() ?: 0,
            hasCompletedTutorial = payload["hasCompletedTutorial"] as? Boolean ?: false,
            comrades = (payload["comrades"] as? Number)?.toDouble() ?: 0.0,
            comradesPerSec = (payload["comradesPerSec"] as? Number)?.toDouble() ?: 1.0,
            playerRank = (payload["playerRank"] as? Number)?.toInt() ?: 1,
            completedRankMissions = (payload["completedRankMissions"] as? Number)?.toInt() ?: 0,
            science = (payload["science"] as? Number)?.toLong() ?: 0L,
            scienceProdLevel = (payload["scienceProdLevel"] as Number).toInt(),
            scienceCreditsLevel = (payload["scienceCreditsLevel"] as Number).toInt(),
            scienceComradeLevel = (payload["scienceComradeLevel"] as Number).toInt(),
            scienceCombatLevel = (payload["scienceCombatLevel"] as Number).toInt(),
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

        val researchers = (payload["researchers"] as List<Map<String, Any>>).map {
            ResearcherCard(
                id = (it["id"] as Number).toInt(), name = it["name"] as String,
                description = it["description"] as String, level = (it["level"] as Number).toInt(),
                cardsCollected = (it["cardsCollected"] as Number).toInt(),
                cardsNeededToUpgrade = (it["cardsNeededToUpgrade"] as Number).toInt(),
                targetBuildingId = (it["targetBuildingId"] as Number).toInt(),
                boostType = it["boostType"] as String, rarity = it["rarity"] as String
            )
        }
        val missions = (payload["missions"] as List<Map<String, Any>>).map {
            ActiveMission(
                id = (it["id"] as Number).toInt(), description = it["description"] as String,
                progress = (it["progress"] as Number).toDouble(), target = (it["target"] as Number).toDouble(),
                isCompleted = it["isCompleted"] as Boolean, isClaimed = it["isClaimed"] as Boolean,
                missionType = it["missionType"] as String, targetId = (it["targetId"] as Number).toInt(),
                targetString = it["targetString"] as String, rewardGems = (it["rewardGems"] as Number).toLong(),
                rewardScience = (it["rewardScience"] as Number).toLong(), rewardCapsuleType = it["rewardCapsuleType"] as String
            )
        }
        BackupValidation.snapshot(state, resources, buildings, combatUnits, researchers, missions)
        val receipts = if (version == 4) (payload["adReceipts"] as List<Map<String, Any>>).map {
            AdRewardReceipt(it["token"] as String, (it["rewardedAt"] as Number).toLong())
        } else emptyList()
        require(receipts.map { it.token }.distinct().size == receipts.size && receipts.all { it.token.isNotBlank() })
        beforeRestore()
        repository.restoreBackup(state, resources, buildings, combatUnits, researchers, missions, receipts)
        lastRestoreSummary = "Níveis das fábricas (1–8): " +
            buildings.sortedBy { it.id }.joinToString(", ") { it.level.toString() } +
            "\nEstoques com recursos: ${resources.count { it.quantity > 0 }} de ${resources.size}."
    }

    // Helper functions to cache sandbox backups to local disk storage
    // so saves survive app restarts even when real Firebase is offline!
    private fun sandboxBackupFile(email: String): File {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(email.trim().lowercase(java.util.Locale.ROOT).toByteArray(Charsets.UTF_8))
        val accountId = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File(context.filesDir, "sandbox_backup_$accountId.json")
    }

    private fun saveDataToLocalCache(payload: Map<String, Any>, email: String) {
        try {
            val file = sandboxBackupFile(email)
            val jsonObject = org.json.JSONObject(payload)
            val atomicFile = android.util.AtomicFile(file)
            val stream = atomicFile.startWrite()
            try {
                stream.write(jsonObject.toString().toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(stream)
            } catch (error: Throwable) {
                atomicFile.failWrite(stream)
                throw error
            }
        } catch (e: Exception) {
            Log.e("CloudSaveManager", "Error saving cached mock cloud data", e)
            throw e
        }
    }

    private fun loadDataFromLocalCache(email: String): Map<String, Any>? {
        return try {
            val file = sandboxBackupFile(email)
            if (!file.exists() && !File(file.path + ".bak").exists()) return null
            val content = android.util.AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
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
