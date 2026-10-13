package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

class MainActivity : ComponentActivity() {
    val rewardedAds by lazy { com.example.ads.RewardedAdsController(this) }
    val playBilling by lazy { com.example.billing.PlayBillingController(this) }
    override fun onResume() {
        super.onResume()
        playBilling.onResume()
    }
    override fun onDestroy() {
        playBilling.close()
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val gameViewModel: GameViewModel = viewModel()
            val state by gameViewModel.gameState.collectAsStateWithLifecycle()
            val selectedSkinId = state?.selectedSkinId ?: 0

            MyApplicationTheme(selectedSkinId = selectedSkinId) {
                CosmicUniverseBackground {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        MainAppContent(viewModel = gameViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun CosmicUniverseBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "StarTwinkle")
    
    // Twinkling pulses for background stars
    val twinkleFactor1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle1"
    )
    val twinkleFactor2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3100, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle2"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height
                
                // Deep space galactic backdrop
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030611), // Cosmic almost black
                            Color(0xFF070B16), // Dark space navy
                            Color(0xFF020409)
                        )
                    )
                )
                
                // Draw majestic purple cosmic nebula gaseous glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1F512DA8), // Cosmic Deep Purple (slightly transparent)
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.25f),
                        radius = width * 0.9f
                    ),
                    center = Offset(width * 0.15f, height * 0.25f),
                    radius = width * 0.9f
                )
                
                // Draw deep teal cosmic nebula gaseous glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x15006064), // Cosmic Dark Teal (slightly transparent)
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.75f),
                        radius = width * 0.8f
                    ),
                    center = Offset(width * 0.85f, height * 0.75f),
                    radius = width * 0.8f
                )

                // Twinkling star particle coordinates
                val starPositions = listOf(
                    Offset(0.12f, 0.15f) to 3.5f,
                    Offset(0.88f, 0.08f) to 2.8f,
                    Offset(0.06f, 0.48f) to 4.2f,
                    Offset(0.94f, 0.38f) to 3.0f,
                    Offset(0.32f, 0.68f) to 2.7f,
                    Offset(0.76f, 0.86f) to 4.8f,
                    Offset(0.25f, 0.82f) to 3.5f,
                    Offset(0.55f, 0.22f) to 3.0f,
                    Offset(0.68f, 0.48f) to 4.0f,
                    Offset(0.48f, 0.92f) to 2.5f
                )

                starPositions.forEachIndexed { idx, (ratio, baseRadius) ->
                    val factor = if (idx % 2 == 0) twinkleFactor1 else twinkleFactor2
                    drawCircle(
                        color = Color.White.copy(alpha = factor),
                        radius = baseRadius,
                        center = Offset(width * ratio.x, height * ratio.y)
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun MainAppContent(viewModel: GameViewModel) {
    val stateNullable by viewModel.gameState.collectAsStateWithLifecycle()
    val state = stateNullable
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val buildings by viewModel.buildings.collectAsStateWithLifecycle()
    val combatUnits by viewModel.combatUnits.collectAsStateWithLifecycle()
    val offlineReport by viewModel.offlineEarnings.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0: Industry, 1: Combat, 2: Market, 3: Guild, 4: Profile, 5: Ascension, 6: Shop
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val storageError by viewModel.storageError.collectAsStateWithLifecycle()
    if (storageError != null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(storageError!!, color = AlertRed)
        }
        return
    }
    if (state == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Offline income dialog
    if (offlineReport != null) {
        OfflineEarningsDialog(
            report = offlineReport!!,
            isSubscribed = state.isSubscribed,
            offlineHours = com.example.data.StellarLab.offlineHours(state),
            onDismiss = { viewModel.dismissOfflineEarnings() }
        )
    }

    // Guided Introduction Tutorial Dialog
    if (!state.hasCompletedTutorial) {
        GameTutorialDialog(
            onDismiss = { viewModel.completeTutorial() }
        )
    }

    // Responsive Root UI
    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar Navigation Rail for Tablets / PC
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .testTag("pc_nav_rail"),
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CorporateFare,
                            contentDescription = "Logotipo da sede",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                Spacer(modifier = Modifier.weight(1f))
                NavigationRailItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.Business, contentDescription = "Indústria") },
                    label = { Text("Indústria", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_industry_pc")
                )
                NavigationRailItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.Security, contentDescription = "Defesa") },
                    label = { Text("Defesa", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_combat_pc")
                )
                NavigationRailItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.TrendingUp, contentDescription = "Mercado") },
                    label = { Text("Mercado", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_market_pc")
                )
                NavigationRailItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Filled.Group, contentDescription = "Aliança") },
                    label = { Text("Aliança", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_guild_pc")
                )
                NavigationRailItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil") },
                    label = { Text("Perfil", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_profile_pc")
                )
                NavigationRailItem(
                    selected = activeTab == 5,
                    onClick = { activeTab = 5 },
                    icon = { Icon(Icons.Filled.Autorenew, contentDescription = "Ascensão") },
                    label = { Text("Ascensão", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_ascension_pc")
                )
                NavigationRailItem(
                    selected = activeTab == 6,
                    onClick = { activeTab = 6 },
                    icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = "Premium") },
                    label = { Text("Premium", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_premium_pc")
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Main View Area for PC
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                TabContent(
                    tab = activeTab,
                    viewModel = viewModel,
                    state = state,
                    resources = resources,
                    buildings = buildings,
                    combatUnits = combatUnits
                )
            }
        }
    } else {
        // Mobile UI layout
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("mobile_bottom_bar"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Filled.Business, contentDescription = null) },
                        label = { Text("Indústria", fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_industry")
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Filled.Security, contentDescription = null) },
                        label = { Text("Defesa", fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_combat")
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                        label = { Text("Mercado", fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_market")
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                        label = { Text("Aliança", fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_guild")
                    )
                    NavigationBarItem(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                        label = { Text("Perfil", fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_profile")
                    )
                    NavigationBarItem(
                        selected = activeTab == 5,
                        onClick = { activeTab = 5 },
                        icon = { Icon(Icons.Filled.Autorenew, contentDescription = null) },
                        label = { Text("Ascensão", fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_ascension")
                    )
                    NavigationBarItem(
                        selected = activeTab == 6,
                        onClick = { activeTab = 6 },
                        icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = null) },
                        label = { Text("Premium", fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_premium")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabContent(
                    tab = activeTab,
                    viewModel = viewModel,
                    state = state,
                    resources = resources,
                    buildings = buildings,
                    combatUnits = combatUnits
                )
            }
        }
    }
}

@Composable
fun TabContent(
    tab: Int,
    viewModel: GameViewModel,
    state: GameState,
    resources: List<ResourceInventory>,
    buildings: List<BusinessBuilding>,
    combatUnits: List<CombatUnit>
) {
    // Destinations can contain several live animations; composing both screens during
    // a crossfade causes a visible pause on lower-end phones.
    when (tab) {
        0 -> IndustryTab(viewModel, state, resources, buildings)
        1 -> CombatTab(viewModel, state, combatUnits)
        2 -> MarketTab(viewModel, state, resources)
        3 -> GuildTab(viewModel, state)
        4 -> ProfileTab(viewModel, state)
        5 -> AscensionTab(viewModel, state)
        6 -> ShopAndEventTab(viewModel, state)
    }
}

// ---------------- TAB 1: INDUSTRY TAB ----------------
@Composable
fun IndustryTab(
    viewModel: GameViewModel,
    state: GameState,
    resources: List<ResourceInventory>,
    buildings: List<BusinessBuilding>
) {
    var showAutoSellDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    var showResearchersDialog by remember { mutableStateOf(false) }

    val activeMissions by viewModel.activeMissions.collectAsStateWithLifecycle()
    val scientists by viewModel.researchers.collectAsStateWithLifecycle()
    val prices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val currentMultiplier by viewModel.upgradeMultiplier.collectAsStateWithLifecycle()

    if (showProfileDialog) {
        ProfileDialog(viewModel = viewModel, onDismiss = { showProfileDialog = false })
    }

    if (showAutoSellDialog) {
        AlertDialog(
            onDismissRequest = { showAutoSellDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Stars, contentDescription = null, tint = SolarGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Módulo Comercial Premium 🛰️", fontWeight = FontWeight.Bold, color = SolarGold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "A funcionalidade de Auto-venda automática é um recurso premium exclusivo!",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite
                    )
                    Text(
                        "Com a Licença Comercial Ativa, todas as suas fábricas, mineradoras e montadoras vendem recursos e geram C$ passivamente em segundo tempo real sem que você precise acessar a aba Mercado manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Benefícios inclusos:\n• Conversão automática direta 1:1 com base no preço atual de mercado\n• Geração de créditos idle em segundo plano e progresso offline\n• Demonstração gratuita no debug; pagamento real ainda indisponível",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextWhite
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.buyAutoSellLicense()
                        showAutoSellDialog = false
                    },
                    enabled = BuildConfig.DEBUG,
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black)
                ) {
                    Text(if (BuildConfig.DEBUG) "Ativar demo gratuita" else "Compras indisponíveis", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoSellDialog = false }) {
                    Text("Voltar", color = TextMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderDashboardCard(
                state = state,
                onOpenProfile = { showProfileDialog = true }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Quartel de Comando de Patente 🏆",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SolarGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showMissionsDialog = true },
                            modifier = Modifier.weight(1f).testTag("btn_show_missions"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Missões (${activeMissions.count { !it.isClaimed }})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Button(
                            onClick = { showResearchersDialog = true },
                            modifier = Modifier.weight(1f).testTag("btn_show_researchers"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cientistas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Recursos & Commodities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Horizontal inventories tracker
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(resources) { res ->
                    ResourceInventoryCard(resource = res)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Módulos de Produção",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.isSubscribed) {
                    Badge(
                        containerColor = SolarGold.copy(alpha = 0.2f),
                        contentColor = SolarGold
                    ) {
                        Text("Membro VIP Ativo: +50% Vel", modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Evolução em lote:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("x1", "x5", "x10", "MAX").forEach { mult ->
                        val isSelected = currentMultiplier == mult
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else Color.Black.copy(alpha = 0.4f))
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Comprar ${if (mult == "MAX") "máximo possível" else mult} níveis" }
                                .clickable { viewModel.setUpgradeMultiplier(mult) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mult,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextWhite
                            )
                        }
                    }
                }
            }
        }

        if (buildings.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Inicializando geradores...", color = TextMuted)
                }
            }
        } else {
            items(buildings) { building ->
                val currentPrice = prices[building.resourceProduced] ?: 1.0
                BuildingProductionCard(
                    building = building,
                    inventory = resources,
                    currentCash = state.cash,
                    currentGems = state.starGems,
                    marketPrice = currentPrice,
                    hasAutoSellLicense = state.hasAutoSellLicense,
                    upgradeMultiplier = currentMultiplier,
                    scientists = scientists,
                    gameState = state,
                    onUpgrade = { viewModel.upgradeBuilding(building.id) },
                    onAutomate = { viewModel.automateBuildingWithGems(building.id) },
                    onProduce = { viewModel.produceManual(building.id) },
                    onToggleAutoSell = { active ->
                        if (!state.hasAutoSellLicense) {
                            showAutoSellDialog = true
                        } else {
                            viewModel.toggleAutoSelling(building.id, active)
                        }
                    }
                )
            }
        }
    }

    if (showMissionsDialog) {
        MissionsDialog(
            viewModel = viewModel,
            state = state,
            missions = activeMissions,
            onDismiss = { showMissionsDialog = false }
        )
    }

    if (showResearchersDialog) {
        ResearchersLabDialog(
            viewModel = viewModel,
            state = state,
            scientists = scientists,
            onDismiss = { showResearchersDialog = false }
        )
    }
}

@Composable
fun MissionsDialog(
    viewModel: GameViewModel,
    state: GameState,
    missions: List<ActiveMission>,
    onDismiss: () -> Unit
) {
    val researchers by viewModel.researchers.collectAsStateWithLifecycle()
    var rewardScience by remember { mutableStateOf<Long?>(null) }
    var rewardGems by remember { mutableStateOf<Long?>(null) }
    var rewardCapType by remember { mutableStateOf<String?>(null) }
    var rewardCards by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCapsuleAlert by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = CosmicDark,
            border = BorderStroke(1.dp, CosmicBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = SolarGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Operações de Patente: Rank ${state.playerRank} 🎖️", fontWeight = FontWeight.Bold, color = SolarGold, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Progresso da Patente", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                                Text("${state.completedRankMissions}/3 Missões", fontWeight = FontWeight.ExtraBold, color = SolarGold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = (state.completedRankMissions / 3f).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = SolarGold,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                            
                            if (state.completedRankMissions >= 3) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.rankUpPlayer(
                                            onSuccess = { nextRank, science, cards ->
                                                rewardScience = science
                                                rewardGems = 50L + nextRank * 10L
                                                rewardCapType = "REPROMOÇÃO SUPREMA"
                                                rewardCards = cards
                                                showCapsuleAlert = true
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("btn_rank_up"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black)
                                ) {
                                    Text("SUBIR DE PATENTE! 🎖️", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Complete as 3 missões para desbloquear sua promoção de patente e receber uma Super Cápsula de Cientistas!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Text("Lista de Missões Ativas:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)

                    if (missions.isEmpty()) {
                        Text("Nenhuma missão ativa carregada.", color = TextMuted)
                    }

                    missions.forEach { m ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, if (m.isCompleted && !m.isClaimed) SolarGold else Color.Transparent),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(m.description, fontWeight = FontWeight.Bold, color = TextWhite)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Recompensas: +${m.rewardGems} 💎 | +${m.rewardScience} 🧪 | Cápsula ${m.rewardCapsuleType}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SolarGold
                                        )
                                    }
                                    if (m.isClaimed) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Completo", tint = Color.Green, modifier = Modifier.size(24.dp))
                                    } else if (m.isCompleted) {
                                        Button(
                                            onClick = {
                                                viewModel.claimMissionReward(m.id, onSuccess = { gems, science, capType, cards ->
                                                    rewardGems = gems
                                                    rewardScience = science
                                                    rewardCapType = capType
                                                    rewardCards = cards
                                                    showCapsuleAlert = true
                                                })
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("REIVINDICAR", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        }
                                    } else {
                                        Icon(Icons.Filled.HourglassEmpty, contentDescription = "Andamento", tint = TextMuted, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val ratio = (m.progress / m.target).toFloat().coerceIn(0f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = ratio,
                                        modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                        color = if (m.isCompleted) Color.Green else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${formatMissionProgress(m.progress)} / ${formatMissionProgress(m.target)}",
                                        fontSize = 11.sp,
                                        color = if (m.isCompleted) Color.Green else TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }

    if (showCapsuleAlert) {
        AlertDialog(
            onDismissRequest = { showCapsuleAlert = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = SolarGold,
                        modifier = Modifier.size(36.dp))
                    Text("CONQUISTA DE PATENTE", fontWeight = FontWeight.Black, color = SolarGold, fontSize = 20.sp)
                    Text("Tipo: Cápsula $rewardCapType", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Você coletou suprimentos incríveis!", fontWeight = FontWeight.Bold, color = TextWhite)
                    ScienceReward(rewardScience ?: 0)
                    NexusRewardRow(gems = "+${rewardGems ?: 0}")
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("CARTAS CIENTÍFICAS OBTIDAS:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ResearcherRewardCards(rewardCards, researchers)
                }
            },
            confirmButton = {
                Button(onClick = { showCapsuleAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

fun formatMissionProgress(value: Double): String = com.example.ui.compactNumber(value)

@Composable
fun ResearchersLabDialog(
    viewModel: GameViewModel,
    state: GameState,
    scientists: List<ResearcherCard>,
    onDismiss: () -> Unit
) {
    var shopScienceGain by remember { mutableStateOf<Long?>(null) }
    var shopCardsObtained by remember { mutableStateOf<List<String>>(emptyList()) }
    var shopCapType by remember { mutableStateOf("") }
    var showShopAlert by remember { mutableStateOf(false) }
    var evolvedResearcherId by remember { mutableStateOf<Int?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = CosmicDark,
            border = BorderStroke(1.dp, CosmicBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = SolarGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lab de Pesquisadores 🧪",
                            fontWeight = FontWeight.Black,
                            color = SolarGold,
                            fontSize = 20.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar laboratório", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Accumulated Science Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CIÊNCIA DISPONÍVEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tokens para recrutamento e melhoria",
                            fontSize = 11.sp,
                            color = TextWhite.copy(alpha = 0.7f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.science} 🧪",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = SolarGold
                        )
                    }
                }

                // Section: Capsules Store
                Text(
                    text = "LOJA DE CÁPSULAS DE SUPRIMENTOS",
                    fontWeight = FontWeight.Black,
                    color = NeonCyan,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CAPSULE 1: COMUM
                    CapsuleShopItem(
                        title = "Comum 🎁",
                        cost = 50,
                        imageResId = R.drawable.img_capsule_comum,
                        glowColor = NeonCyan,
                        onBuy = {
                            viewModel.buyAndOpenCapsuleWithGems("COMUM", onSuccess = { sci, cards ->
                                shopScienceGain = sci
                                shopCardsObtained = cards
                                shopCapType = "COMUM"
                                showShopAlert = true
                            }, onError = { err ->
                                errorMsg = err
                            })
                        }
                    )

                    // CAPSULE 2: ÉPICA
                    CapsuleShopItem(
                        title = "Épica ⭐",
                        cost = 150,
                        imageResId = R.drawable.img_capsule_epica,
                        glowColor = NeonMagenta,
                        onBuy = {
                            viewModel.buyAndOpenCapsuleWithGems("EPICA", onSuccess = { sci, cards ->
                                shopScienceGain = sci
                                shopCardsObtained = cards
                                shopCapType = "ÉPICA"
                                showShopAlert = true
                            }, onError = { err ->
                                errorMsg = err
                            })
                        }
                    )

                    // CAPSULE 3: SUPREMA
                    CapsuleShopItem(
                        title = "Suprema 👑",
                        cost = 400,
                        imageResId = R.drawable.img_capsule_suprema,
                        glowColor = SolarGold,
                        onBuy = {
                            viewModel.buyAndOpenCapsuleWithGems("SUPREMA", onSuccess = { sci, cards ->
                                shopScienceGain = sci
                                shopCardsObtained = cards
                                shopCapType = "SUPREMA"
                                showShopAlert = true
                            }, onError = { err ->
                                errorMsg = err
                            })
                        }
                    )
                }

                // Section: Science Upgrades
                Text(
                    text = "TECNOLOGIAS E MELHORIAS LAB (PASSIVO)",
                    fontWeight = FontWeight.Black,
                    color = SolarGold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // UPGRADE 1: PROD
                    val prodLevel = state.scienceProdLevel
                    val prodCost = (100 * 1.5.pow(prodLevel.toDouble())).toLong()
                    val canBuyProd = state.science >= prodCost
                    ScienceUpgradeItem(
                        title = "Otimização Quântica",
                        description = "Velocidade/Produção global",
                        boostText = "+${prodLevel * 15}%",
                        nextBoostText = "+15%",
                        cost = prodCost,
                        level = prodLevel,
                        themeColor = NeonCyan,
                        canBuy = canBuyProd,
                        onUpgrade = {
                            viewModel.purchaseScienceUpgrade("PROD", onSuccess = {}, onError = { errorMsg = it })
                        }
                    )

                    // UPGRADE 2: CREDITS
                    val creditsLevel = state.scienceCreditsLevel
                    val creditsCost = (150 * 1.6.pow(creditsLevel.toDouble())).toLong()
                    val canBuyCredits = state.science >= creditsCost
                    ScienceUpgradeItem(
                        title = "Reatores de Fusão",
                        description = "Mais Créditos (C$) ganhos",
                        boostText = "+${creditsLevel * 10}%",
                        nextBoostText = "+10%",
                        cost = creditsCost,
                        level = creditsLevel,
                        themeColor = SolarGold,
                        canBuy = canBuyCredits,
                        onUpgrade = {
                            viewModel.purchaseScienceUpgrade("CREDITS", onSuccess = {}, onError = { errorMsg = it })
                        }
                    )

                    // UPGRADE 3: COMRADE
                    val comradeLevel = state.scienceComradeLevel
                    val comradeCost = (80 * 1.5.pow(comradeLevel.toDouble())).toLong()
                    val canBuyComrade = state.science >= comradeCost
                    ScienceUpgradeItem(
                        title = "Sensores Interestelares",
                        description = "Comrades por segundo",
                        boostText = "+${comradeLevel * 20}%",
                        nextBoostText = "+20%",
                        cost = comradeCost,
                        level = comradeLevel,
                        themeColor = NeonMagenta,
                        canBuy = canBuyComrade,
                        onUpgrade = {
                            viewModel.purchaseScienceUpgrade("COMRADE", onSuccess = {}, onError = { errorMsg = it })
                        }
                    )

                    // UPGRADE 4: COMBAT
                    val combatLevel = state.scienceCombatLevel
                    val combatCost = (200 * 1.7.pow(combatLevel.toDouble())).toLong()
                    val canBuyCombat = state.science >= combatCost
                    ScienceUpgradeItem(
                        title = "Liga de Titânio",
                        description = "Dano/HP extra de Combate",
                        boostText = "+${combatLevel * 15}%",
                        nextBoostText = "+15%",
                        cost = combatCost,
                        level = combatLevel,
                        themeColor = SuccessGreen,
                        canBuy = canBuyCombat,
                        onUpgrade = {
                            viewModel.purchaseScienceUpgrade("COMBAT", onSuccess = {}, onError = { errorMsg = it })
                        }
                    )
                }

                // Section: Assigned Scientists
                Text(
                    text = "PESQUISADORES RECRUTADOS",
                    fontWeight = FontWeight.Black,
                    color = NeonCyan,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )

                if (scientists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(CosmicCard, RoundedCornerShape(12.dp))
                            .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Help,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum pesquisador recrutado no momento.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Abra cápsulas acima para extrair recursos!",
                                color = TextMuted.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    scientists.forEach { r ->
                        ScientistDossierCard(
                            r = r,
                            currentScience = state.science,
                            onUpgrade = {
                                viewModel.upgradeResearcherCard(r.id,
                                    onSuccess = { evolvedResearcherId = r.id },
                                    onError = { err -> errorMsg = err })
                            }
                        )
                    }
                } // Closes else block
            } // Closes Inner scrollable Column

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("FECHAR", fontWeight = FontWeight.Black)
            }
        } // Closes Main Column
    } // Closes Surface
} // Closes Dialog

    if (showShopAlert) {
        AlertDialog(
            onDismissRequest = { showShopAlert = false },
            title = {
                Text(
                    text = "CÁPSULA REVELADA",
                    fontWeight = FontWeight.Black,
                    color = SolarGold,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val shopAsset = when (shopCapType.uppercase()) {
                        "COMUM" -> R.drawable.img_capsule_comum
                        "EPICA", "ÉPICA" -> R.drawable.img_capsule_epica
                        "SUPREMA" -> R.drawable.img_capsule_suprema
                        else -> R.drawable.img_capsule_comum
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(SolarGold.copy(alpha = 0.08f), CircleShape)
                            .border(1.5.dp, SolarGold.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SafeImage(
                            resId = shopAsset,
                            contentDescription = "Cápsula",
                            modifier = Modifier.size(85.dp).clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            fallbackIcon = Icons.Filled.CardGiftcard,
                            fallbackTint = SolarGold
                        )
                    }

                    Text(
                        text = "Suprimentos extraídos com sucesso para sua operação espacial:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp
                    )

                    ScienceReward(shopScienceGain ?: 0)

                    Text(
                        text = "CARTAS DE PESQUISADORES:",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )

                    ResearcherRewardCards(shopCardsObtained, scientists)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showShopAlert = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "ÓTIMO, RECOLHER! 🪐",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            },
            containerColor = CosmicDark,
            tonalElevation = 8.dp
        )
    }

    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Aviso de Sistema", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text(errorMsg ?: "", color = TextWhite) },
            confirmButton = {
                Button(onClick = { errorMsg = null }) {
                    Text("Entendido")
                }
            }
        )
    }
    evolvedResearcherId?.let { id ->
        val researcher = scientists.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { evolvedResearcherId = null },
            title = { Text("PESQUISADOR EVOLUÍDO", color = SolarGold, fontWeight = FontWeight.Black) },
            text = {
                if (researcher != null) NexusPanel(
                    accent = ResearcherStyle.color(researcher.rarity), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResearcherPortrait(researcher, 80.dp)
                        Column {
                            Text(researcher.name, fontWeight = FontWeight.Black)
                            Text("Nível ${researcher.level}", color = ResearcherStyle.color(researcher.rarity))
                            Text("${ResearcherStyle.label(researcher.rarity)} • ${researcher.description}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { evolvedResearcherId = null }) { Text("CONTINUAR") } },
            containerColor = CosmicDark
        )
    }
}

@Composable
fun SafeImage(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackIcon: ImageVector = Icons.Filled.CardGiftcard,
    fallbackTint: Color = Color.Gray
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasValidResource = remember(resId) {
        try {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
            drawable != null
        } catch (e: Throwable) {
            false
        }
    }

    if (hasValidResource) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Icon(
            imageVector = fallbackIcon,
            contentDescription = contentDescription,
            tint = fallbackTint,
            modifier = modifier
        )
    }
}

@Composable
fun CapsuleShopItem(
    title: String,
    cost: Int,
    imageResId: Int,
    glowColor: Color,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .border(
                border = BorderStroke(1.5.dp, NexusVisuals.frame(glowColor)),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.substringBefore(' '),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = TextWhite,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                    .border(1.5.dp, glowColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SafeImage(
                    resId = imageResId,
                    contentDescription = title,
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    fallbackIcon = Icons.Filled.CardGiftcard,
                    fallbackTint = glowColor
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Custo: ",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "$cost gemas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarGold
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = glowColor,
                    contentColor = if (glowColor == SolarGold) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text(
                    text = "ABRIR",
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun ScienceUpgradeItem(
    title: String,
    description: String,
    boostText: String,
    nextBoostText: String,
    cost: Long,
    level: Int,
    themeColor: Color,
    canBuy: Boolean,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(185.dp)
            .height(160.dp)
            .border(
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp)
            ).testTag("science_upgrade_${title.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicCard)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = themeColor,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = description,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 2,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Atual: $boostText",
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Level $level",
                color = themeColor.copy(alpha = 0.82f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onUpgrade,
                enabled = canBuy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = if (themeColor == SolarGold) Color.Black else Color.White,
                    disabledContainerColor = themeColor.copy(alpha = 0.15f),
                    disabledContentColor = TextMuted.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text(
                    text = "$cost 🧪",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScientistDossierCard(
    r: ResearcherCard,
    currentScience: Long,
    onUpgrade: () -> Unit
) {
    val rColor = ResearcherStyle.color(r.rarity)
    val rarityName = ResearcherStyle.label(r.rarity)

    val glowBrush = ResearcherStyle.frame(rColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(2.dp, glowBrush),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CosmicCard)
    ) {
        Column(modifier = Modifier.background(Brush.verticalGradient(listOf(rColor.copy(alpha = 0.16f), CosmicCard))).padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scientist Artwork (Left)
                ResearcherPortrait(r, 96.dp)

                Spacer(modifier = Modifier.width(12.dp))

                // Mid Text info: Name, Rarity, Level
                Column(modifier = Modifier.weight(1f)) {
                    Text(rarityName, fontSize = 10.sp, fontWeight = FontWeight.Black,
                        color = rColor, letterSpacing = 1.sp)
                    Text(r.name, fontWeight = FontWeight.Black, color = TextWhite,
                        fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = if (r.level == 0) "NÃO RECRUTADO" else "ATIVO • nível ${r.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (r.level > 0) SuccessGreen else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Description of effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    val role = when (r.boostType) {
                        "PRODUCTION" -> "PRODUÇÃO"
                        "SPEED" -> "VELOCIDADE"
                        "COST_REDUCTION" -> "EFICIÊNCIA"
                        "COMRADE_GEN" -> "EQUIPE"
                        else -> "PESQUISA"
                    }
                    Text(role, color = rColor, fontSize = 10.sp, fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp)
                    Text(r.description, style = MaterialTheme.typography.bodySmall,
                        color = TextWhite.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Cards collected Progress row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val barRatio = if (r.cardsNeededToUpgrade > 0) {
                    (r.cardsCollected.toFloat() / r.cardsNeededToUpgrade.toFloat()).coerceAtMost(1f)
                } else 1f
                
                LinearProgressIndicator(
                    progress = barRatio,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = rColor,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                
                Text(
                    text = "${r.cardsCollected} / ${r.cardsNeededToUpgrade}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (r.cardsCollected >= r.cardsNeededToUpgrade) SuccessGreen else TextWhite
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            val canUpgrade = r.cardsCollected >= r.cardsNeededToUpgrade && currentScience >= r.getUpgradeScienceCost()
            Button(
                onClick = onUpgrade,
                enabled = canUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = rColor,
                    contentColor = if (r.rarity == "SUPREME") Color.Black else CosmicDark),
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("${if (r.level == 0) "RECRUTAR" else "EVOLUIR"} • ${r.getUpgradeScienceCost()} ciência",
                    fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun getAvatarDetails(id: Int): Pair<String, androidx.compose.ui.graphics.vector.ImageVector> {
    return when (id) {
        0 -> "Engenheiro" to Icons.Filled.Engineering
        1 -> "Comandante" to Icons.Filled.MilitaryTech
        2 -> "Ciborgue" to Icons.Filled.Psychology
        3 -> "Cientista" to Icons.Filled.Science
        4 -> "Hacker" to Icons.Filled.Memory
        5 -> "Magnata" to Icons.Filled.Business
        6 -> "Operador" to Icons.Filled.Build
        7 -> "Almirante" to Icons.Filled.Stars
        else -> "Administrador" to Icons.Filled.AccountCircle
    }
}

@Composable
fun HeaderDashboardCard(
    state: GameState,
    onOpenProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (state.isSubscribed) SolarGold.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(145.dp)) {
            Image(
                painter = painterResource(id = R.drawable.img_space_banner),
                contentDescription = "HQ Estelar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Soft fading galactic gradient overlying the image to display text readable and pristine
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
            
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarPair = getAvatarDetails(state.selectedAvatarId)
                        Icon(
                            imageVector = avatarPair.second,
                            contentDescription = "Avatar do Administrador",
                            tint = if (state.isSubscribed) SolarGold else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.companyName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Patente ${state.playerRank} 🎖️",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SolarGold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "|  Setor: ${state.guildName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    val avatarPair = getAvatarDetails(state.selectedAvatarId)
                    Icon(
                        imageVector = avatarPair.second,
                        contentDescription = "Perfil e Avatar",
                        tint = SolarGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Capital Disponível",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Breath-pulsing continuous scales on capital counter to denote live activity
                val infiniteTransition = rememberInfiniteTransition(label = "CreditsPulse")
                val textPulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Text(
                    text = "${formatCredits(state.cash)} C\$",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.isSubscribed) SolarGold else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = textPulseScale
                            scaleY = textPulseScale
                        }
                        .padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Other in-game currency indicators - Grid representation
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CurrencyBadge(icon = Icons.Filled.Stars, label = "${state.starGems} 💎", text = "Gemas")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CurrencyBadge(icon = Icons.Filled.Science, label = "${state.science} 🧪", text = "Ciência")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        CurrencyBadge(
                            icon = Icons.Filled.Group,
                            label = "${formatComrades(state.comrades)} 👥 (+${String.format("%.1f", state.comradesPerSec)}/s)",
                            text = "Operários"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CurrencyBadge(icon = Icons.Filled.ChangeCircle, label = "${state.nebulaCores} Núcleos", text = "Núcleos de Nebulosa")
                    }
                }
            }
        }
    }
}

fun formatComrades(amount: Double): String = com.example.ui.compactNumber(amount)

@Composable
fun CurrencyBadge(icon: ImageVector, label: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = text, tint = SolarGold, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextWhite)
    }
}

data class ResourceVisuals(val color: Color, val icon: ImageVector)

@Composable
fun getResourceVisuals(name: String): ResourceVisuals {
    return when (name) {
        "Energy Cell" -> ResourceVisuals(SolarGold, Icons.Filled.Bolt)
        "Iron Ore" -> ResourceVisuals(Color(0xFFB0BEC5), Icons.Filled.Build)
        "Hyperalloy" -> ResourceVisuals(NeonCyan, Icons.Filled.Layers)
        "Quantum Chip" -> ResourceVisuals(NeonMagenta, Icons.Filled.Memory)
        "Organic Feedstock" -> ResourceVisuals(Color(0xFF4CAF50), Icons.Filled.Eco)
        "Neural Implant" -> ResourceVisuals(Color(0xFF00E676), Icons.Filled.Psychology)
        "Antimatter Containment" -> ResourceVisuals(Color(0xFFFF9100), Icons.Filled.LocalFireDepartment)
        "Warp Drive" -> ResourceVisuals(Color(0xFF2979FF), Icons.Filled.RocketLaunch)
        else -> ResourceVisuals(NeonCyan, Icons.Filled.Star)
    }
}

@Composable
fun ResourceInventoryCard(resource: ResourceInventory) {
    val visuals = getResourceVisuals(resource.name)

    Card(
        modifier = Modifier
            .width(135.dp)
            .border(1.dp, visuals.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = translateText(resource.name),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = visuals.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(visuals.icon, contentDescription = null, tint = visuals.color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAmount(resource.quantity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TextWhite
            )
            Text(
                text = "Em estoque",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun BuildingProductionCard(
    building: BusinessBuilding,
    inventory: List<ResourceInventory>,
    currentCash: Double,
    currentGems: Long,
    marketPrice: Double,
    hasAutoSellLicense: Boolean,
    upgradeMultiplier: String,
    scientists: List<ResearcherCard>,
    gameState: GameState? = null,
    onUpgrade: () -> Unit,
    onAutomate: () -> Unit,
    onProduce: () -> Unit,
    onToggleAutoSell: (Boolean) -> Unit
) {
    val discountedBaseCost = com.example.data.EconomyCosts.buildingBase(building, scientists, gameState ?: GameState())
    val (levelsGained, cost) = calculateMultiUpgrade(discountedBaseCost, building.costMultiplier, building.level, currentCash, upgradeMultiplier)

    // --- Scientist/Researcher multipliers ---
    // a) Global supreme boost (ID 6)
    val globalSupremeCard = scientists.find { it.id == 6 }
    val globalSupremeMulti = 1.0 + (if (globalSupremeCard != null && globalSupremeCard.level > 0) globalSupremeCard.getBoostValue() else 0.0)
    
    // b) Local production boost (target ID matches building)
    val localProdCard = scientists.find { it.targetBuildingId == building.id && it.boostType == "PRODUCTION" }
    val localProdMulti = 1.0 + (if (localProdCard != null && localProdCard.level > 0) localProdCard.getBoostValue() else 0.0)
    
    // c) Local speed boost (speeding up turns = more output)
    val localSpeedCard = scientists.find { it.targetBuildingId == building.id && it.boostType == "SPEED" }
    val localSpeedMulti = 1.0 + (if (localSpeedCard != null && localSpeedCard.level > 0) localSpeedCard.getBoostValue() else 0.0)

    val scienceProdMulti = 1.0 + ((gameState?.scienceProdLevel ?: 0) * 0.15)
    val prestigeBonus = 1.0 + ((gameState?.nebulaCores ?: 0L) * 0.10)
    val energyFactor = (if (gameState?.isSubscribed == true) 1.5 else 1.0) * prestigeBonus
    
    val totalMultiplier = energyFactor * scienceProdMulti * globalSupremeMulti * localProdMulti * localSpeedMulti
    val runsRate = building.getActualProductionRate() *
        (if (building.isAutomated) totalMultiplier else energyFactor)
    val inputRequired = building.inputAmountPerSec * building.level
    val inputStock = inventory.firstOrNull { it.name == building.inputResource }?.quantity ?: 0.0
    val hasFullInput = building.inputResource == null || inputStock >= inputRequired
    val productionUnit = if (building.isAutomated) "/s" else "/toque"
    val isLocked = building.level == 0

    // High-fidelity animation loop for spinning/pulsating building gears
    val infiniteRotation = rememberInfiniteTransition(label = "BuildingIconRotation")
    val rotationAngle by infiniteRotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (building.isAutomated) 3200 else 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val buildingIcon = when (building.resourceProduced) {
        "Energy Cell" -> Icons.Filled.WbSunny       // Glowing rotating Solar array
        "Iron Ore" -> Icons.Filled.Settings         // Hard steel mining drill gear
        "Hyperalloy" -> Icons.Filled.Layers         // Melting core layers
        "Quantum Chip" -> Icons.Filled.Cyclone      // Quantum orbital vortex particle
        "Organic Feedstock" -> Icons.Filled.Eco      // Biosphere eco logo
        "Neural Implant" -> Icons.Filled.Psychology // Cyber brain neural implant
        "Antimatter Containment" -> Icons.Filled.LocalFireDepartment // Reactor hazard / force field
        "Warp Drive" -> Icons.Filled.RocketLaunch     // Interstellar warp travel assembly
        else -> Icons.Filled.OfflineBolt
    }

    val themeColor = when (building.resourceProduced) {
        "Energy Cell" -> SolarGold
        "Iron Ore" -> Color(0xFFB0BEC5)
        "Hyperalloy" -> NeonCyan
        "Quantum Chip" -> NeonMagenta
        "Organic Feedstock" -> Color(0xFF4CAF50)
        "Neural Implant" -> Color(0xFF00E676)
        "Antimatter Containment" -> Color(0xFFFF9100)
        "Warp Drive" -> Color(0xFF2979FF)
        else -> MaterialTheme.colorScheme.primary
    }

    NexusPanel(
        accent = themeColor,
        unlocked = !isLocked,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("building_card_${building.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(38.dp)
                        .background(
                            if (isLocked) TextMuted.copy(alpha = 0.1f) else themeColor.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = buildingIcon,
                        contentDescription = null,
                        tint = if (isLocked) TextMuted else themeColor,
                        modifier = Modifier.size(20.dp).rotate(if (!isLocked) rotationAngle else 0f)
                    )
                }
                Text(
                    text = translateText(building.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) TextMuted else TextWhite,
                    maxLines = 2,
                    modifier = Modifier.weight(1f).testTag("building_name_${building.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (isLocked) {
                Button(
                    onClick = onUpgrade,
                    enabled = currentCash >= cost,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Iniciar${if (levelsGained > 1) " +$levelsGained" else ""} • ${formatCredits(cost)} C\$",
                        fontWeight = FontWeight.Black)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Badge(containerColor = themeColor.copy(alpha = 0.2f), contentColor = themeColor) {
                        Text("Nív ${building.level}", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                    val currentMult = building.getMilestoneMultiplier()
                    if (currentMult > 1.0) {
                        Badge(containerColor = NeonCyan.copy(alpha = 0.15f), contentColor = NeonCyan) {
                            Text("x${formatAmount(currentMult)} Multi", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Badge(containerColor = if (building.isAutomated) SuccessGreen.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        contentColor = if (building.isAutomated) SuccessGreen else TextMuted) {
                        Text(if (building.isAutomated) "Automação IA" else "Manual",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                text = when {
                    isLocked -> "Sistema desconectado"
                    !building.isAutomated -> "Manual: toque em Produzir"
                    else -> "Produz: ${translateText(building.resourceProduced)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2
            )

            if (!isLocked) {
                Spacer(modifier = Modifier.height(12.dp))
                NexusMetric(
                    label = "Produção atual",
                    value = if (building.isAutoSelling && hasAutoSellLicense)
                        "+${formatCredits(runsRate * marketPrice)} C\$$productionUnit"
                    else "+${formatAmount(runsRate)} $productionUnit",
                    accent = if (building.isAutoSelling && hasAutoSellLicense) SuccessGreen else themeColor,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Live production progress display
                val transition = rememberInfiniteTransition(label = "productionLoader")
                val animatedProgress by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "loader"
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (building.isAutoSelling && hasAutoSellLicense) {
                                "Capacidade: +${formatCredits(runsRate * marketPrice)} C\$$productionUnit (Auto-venda)"
                            } else {
                                "Capacidade: +${formatAmount(runsRate)}$productionUnit de ${translateText(building.resourceProduced)}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (building.isAutoSelling && hasAutoSellLicense) SuccessGreen else themeColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (building.inputResource != null) {
                        Text(
                            text = "Consumo: ${formatAmount(inputRequired)}$productionUnit de ${translateText(building.inputResource)}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "Estoque: ${formatAmount(inputStock)}" +
                                if (hasFullInput) " • insumo disponível" else " • insuficiente para um ciclo completo",
                            fontSize = 11.sp,
                            color = if (hasFullInput) SuccessGreen else AlertRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Production progression gauge (Linear indicator styled)
                    LinearProgressIndicator(
                        progress = if (building.isAutomated) animatedProgress else 1f,
                        color = themeColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val nextMilestone = building.getNextMilestone()
                    val prevMilestone = when (nextMilestone) {
                        10 -> 0
                        25 -> 10
                        50 -> 25
                        100 -> 50
                        250 -> 100
                        500 -> 250
                        1000 -> 500
                        else -> nextMilestone - 500
                    }
                    val progressRatio = ((building.level - prevMilestone).toFloat() / (nextMilestone - prevMilestone).toFloat()).coerceIn(0f, 1f)
                    val nextBoost = building.getNextMilestoneMultiplierBoost()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Meta de Marco: Nív $nextMilestone (Bônus x$nextBoost)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${building.level}/$nextMilestone",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progressRatio,
                        color = NeonCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }

                // If not automated, render manual actions in a dedicated full-width split-row
                if (!building.isAutomated) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onProduce,
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Produzir", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = onAutomate,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gerente: 35 💎", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                val isHighLevelResource = building.resourceProduced != "Energy Cell" && building.resourceProduced != "Iron Ore"
                if (isHighLevelResource) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = if (building.isAutoSelling && hasAutoSellLicense) SuccessGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasAutoSellLicense) "Auto-venda (Conversão direta em C\$)" else "Auto-venda (★ Premium VIP)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (building.isAutoSelling && hasAutoSellLicense) SuccessGreen else if (hasAutoSellLicense) TextWhite else SolarGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = building.isAutoSelling && hasAutoSellLicense,
                            onCheckedChange = onToggleAutoSell,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SuccessGreen,
                                checkedTrackColor = SuccessGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Matéria-prima reservada para refino industrial",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val targetLevel = building.level + levelsGained
                    val nextRate = building.copy(level = targetLevel).getActualProductionRate() *
                        (if (building.isAutomated) totalMultiplier else energyFactor)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("PRÓXIMO • NÍV $targetLevel", color = themeColor,
                            fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(if (building.isAutoSelling && hasAutoSellLicense)
                            "+${formatCredits(nextRate * marketPrice)} C\$$productionUnit"
                            else "+${formatAmount(nextRate)} $productionUnit", color = TextWhite,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onUpgrade,
                        enabled = levelsGained > 0 && currentCash >= cost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (levelsGained > 1) "Melhorar +$levelsGained • ${formatCredits(cost)} C\$" else "Melhorar • ${formatCredits(cost)} C\$",
                            maxLines = 2,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (currentCash < cost || levelsGained == 0) {
                        Text("Créditos insuficientes para a melhoria selecionada", color = TextMuted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}


// ---------------- TAB 2: COMBAT TAB ----------------
@Composable
fun CombatTab(
    viewModel: GameViewModel,
    state: GameState,
    combatUnits: List<CombatUnit>
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) viewModel.closeBattle()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.closeBattle()
        }
    }
    var replayToShow by remember { mutableStateOf<CombatReplay?>(null) }
    replayToShow?.let { replay ->
        AlertDialog(
            onDismissRequest = { replayToShow = null },
            title = { Text("${replay.stageName} ? ${if (replay.cancelled) "Cancelado" else if (replay.victory) "Vit?ria" else "Derrota"}") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text("${replay.roundsCount} rodadas")
                    replay.logsSnapshot.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { TextButton(onClick = { replayToShow = null }) { Text("Fechar") } }
        )
    }
    val isCombatActive by viewModel.combatActive.collectAsStateWithLifecycle()
    androidx.activity.compose.BackHandler(enabled = isCombatActive) { viewModel.closeBattle() }
    val combatStatus by viewModel.combatStatus.collectAsStateWithLifecycle()
    val battleState by viewModel.liveCombatStats.collectAsStateWithLifecycle()
    val logs by viewModel.combatLogs.collectAsStateWithLifecycle()
    val lastReplay by viewModel.lastCombatReplay.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isCombatActive) {
            // Out-of-battle screen: Manage squads, view maps
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(145.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_combat_banner),
                        contentDescription = "Patrulhas de Combate",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Operações de Patrulha Estelar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            "Adote decisões em combate tático para proteger os pipelines de extração.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SolarGold
                        )
                    }
                }
            }

            Text("Sua Unidade Tática de Combate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Layout combat units upgrades
            combatUnits.forEach { unit ->
                CombatUnitItemCard(
                    state = state,
                    unit = unit,
                    currentCash = state.cash,
                    onUpgrade = { viewModel.upgradeCombatUnit(unit.id) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Setores com Mineração hostil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // List of stages
            viewModel.combatStages.forEach { stage ->
                val isUnlocked = state.highestCombatStage >= stage.id
                val completedCount = when (stage.id) {
                    1 -> state.stage1CompletedCount
                    2 -> state.stage2CompletedCount
                    3 -> state.stage3CompletedCount
                    4 -> state.stage4CompletedCount
                    else -> 0
                }
                
                // Scale enemy stats
                val scaleHp = 1.0 + 0.15 * completedCount
                val scaleDmg = 1.0 + 0.12 * completedCount
                val enemyHp = (stage.enemyHp * scaleHp).toInt()
                val enemyDmg = (stage.enemyDmg * scaleDmg).toInt()

                val reward = com.example.data.CombatRewards.forLevel(stage.id, completedCount + 1)
                val lootCash = reward.credits
                val lootGems = reward.diamonds
                val lootTokens = reward.guildTokens

                val sectorAccent = when (stage.id) {
                    1 -> NeonCyan
                    2 -> Color(0xFF55B8FF)
                    3 -> NeonMagenta
                    else -> SolarGold
                }
                NexusPanel(
                    accent = sectorAccent,
                    unlocked = isUnlocked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stage.name, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isUnlocked) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "Ameaça nível ${completedCount + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Icon(Icons.Filled.Lock, contentDescription = "Setor bloqueado", tint = TextMuted, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NexusMetric("HP INIMIGO", "$enemyHp", sectorAccent, Modifier.weight(1f))
                            NexusMetric("DANO INIMIGO", "$enemyDmg", AlertRed, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("RECOMPENSA DA VITÓRIA", color = sectorAccent,
                            fontSize = 10.sp, fontWeight = FontWeight.Black)
                        NexusRewardRow(credits = "+${formatCredits(lootCash)} C$",
                            gems = "+$lootGems", guild = "+$lootTokens")
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { viewModel.startCombatBattle(stage.id) }, enabled = isUnlocked,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = sectorAccent),
                            shape = RoundedCornerShape(8.dp)) {
                            Text(if (isUnlocked) "ATACAR SETOR" else "SETOR BLOQUEADO",
                                fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // last combat replay trigger
            if (lastReplay != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Registro do último combate", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${lastReplay!!.stageName} - ${if (lastReplay!!.victory) "Vitória" else "Retirada"} (${lastReplay!!.roundsCount} Turnos)",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                replayToShow = lastReplay
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver registro da batalha", fontSize = 11.sp)
                        }
                    }
                }
            }

        } else {
            // Live combat arena!
            LiveCombatArenaScreen(
                viewModel = viewModel,
                battleState = battleState,
                logs = logs,
                status = combatStatus,
                onClose = { viewModel.closeBattle() }
            )
        }
    }
}

@Composable
fun CombatUnitItemCard(
    unit: CombatUnit,
    state: GameState,
    currentCash: Double,
    onUpgrade: () -> Unit
) {
    val upgradeCost = com.example.data.EconomyCosts.combat(unit, state)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("combat_unit_card_${unit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (unit.id) {
                        1 -> Icons.Filled.MilitaryTech
                        2 -> Icons.Filled.Shield
                        3 -> Icons.Filled.Whatshot
                        else -> Icons.Filled.Cyclone
                    }
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = translateText(unit.name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text("Nív ${unit.level}", modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp), fontSize = 10.sp)
                        }
                    }
                    Text(
                        text = "Classe: ${translateText(unit.type)} • Vida: ${unit.health} • Ataque: ${unit.attack}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 2
                    )
                }
            }

            Button(
                onClick = onUpgrade,
                enabled = currentCash >= upgradeCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Evoluir • ${formatAmount(upgradeCost)} C\$", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (currentCash < upgradeCost) {
                Text("Créditos insuficientes para evoluir", color = TextMuted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun LiveCombatArenaScreen(
    viewModel: GameViewModel,
    battleState: ActiveBattleState?,
    logs: List<String>,
    status: CombatStatus,
    onClose: () -> Unit
) {
    // Tactics skill state trackers hoisted to drive battle aesthetics and overlays
    val cdShield by viewModel.shieldCooldown.collectAsStateWithLifecycle()
    val cdBeam by viewModel.beamCooldown.collectAsStateWithLifecycle()
    val cdHeal by viewModel.overchargeCooldown.collectAsStateWithLifecycle()

    // Smooth sliding spring transitions on taking damage or recovery repairs
    val animatedAllyHp by animateFloatAsState(
        targetValue = if (battleState != null && battleState.allyMaxHp > 0) {
            battleState.allyHp.toFloat() / battleState.allyMaxHp.toFloat()
        } else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "AllyHPAnim"
    )

    val animatedEnemyHp by animateFloatAsState(
        targetValue = if (battleState != null && battleState.enemyMaxHp > 0) {
            battleState.enemyHp.toFloat() / battleState.enemyMaxHp.toFloat()
        } else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "EnemyHPAnim"
    )

    // Breathing neon-alpha values for corresponding tactical energy overlays
    val shieldPulse = rememberInfiniteTransition(label = "ShieldEnergyPulse")
    val shieldAlpha by shieldPulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldAlpha"
    )

    val healPulse = rememberInfiniteTransition(label = "HealNanitesPulse")
    val healAlpha by healPulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "healAlpha"
    )

    val beamPulse = rememberInfiniteTransition(label = "IonLaserChargePulse")
    val beamAlpha by beamPulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beamAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SISTEMA DE COMBATE TÁTICO",
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                if (status is CombatStatus.Running) {
                    Badge(containerColor = AlertRed, contentColor = Color.White) {
                        Text("RODADA ${battleState?.round ?: 1}", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (battleState != null) {
                // SQUAD ALLY HP Gauge - Dynamically glows and pulses based on defensive / repair active actions
                val isShieldActive = cdShield > 0
                val isHealActive = cdHeal > 0
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isShieldActive || isHealActive) 2.dp else 1.dp,
                            color = when {
                                isShieldActive -> NeonCyan.copy(alpha = shieldAlpha)
                                isHealActive -> SuccessGreen.copy(alpha = healAlpha)
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = when {
                                isShieldActive -> NeonCyan.copy(alpha = 0.08f)
                                isHealActive -> SuccessGreen.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isShieldActive) Icons.Filled.Shield else Icons.Filled.MilitaryTech,
                                contentDescription = null,
                                tint = if (isShieldActive) NeonCyan else if (isHealActive) SuccessGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Esquadrão de Segurança da Aliança", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp,
                                color = if (isShieldActive) NeonCyan else if (isHealActive) SuccessGreen else TextWhite
                            )
                        }
                        Text("${battleState.allyHp}/${battleState.allyMaxHp} HP", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = animatedAllyHp,
                        color = if (isShieldActive) NeonCyan else if (isHealActive) SuccessGreen else NeonCyan,
                        trackColor = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "V.S.",
                    fontWeight = FontWeight.Black,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                // RAIDER BOSS HP Gauge - Flashes when under laser overcharge lock-on impact
                val isBeamActive = cdBeam > 0
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isBeamActive) 2.dp else 1.dp,
                            color = if (isBeamActive) NeonMagenta.copy(alpha = beamAlpha) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = if (isBeamActive) NeonMagenta.copy(alpha = 0.08f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBeamActive) Icons.Filled.Whatshot else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (isBeamActive) NeonMagenta else AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Líder Corsário do Setor", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp,
                                color = if (isBeamActive) NeonMagenta else TextWhite
                            )
                        }
                        Text("${battleState.enemyHp}/${battleState.enemyMaxHp} HP", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = animatedEnemyHp,
                        color = NeonMagenta,
                        trackColor = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Centro de Comando Tático", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Tome decisões estratégicas para anular os ataques inimigos.", fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Shield Trigger
                Button(
                    onClick = { viewModel.triggerShieldSkill() },
                    enabled = cdShield == 0 && status is CombatStatus.Running,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (cdShield > 0) "Escudo (${cdShield}s)" else "Escudo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Laser Beam Trigger
                Button(
                    onClick = { viewModel.triggerLaserStrikeSkill() },
                    enabled = cdBeam == 0 && status is CombatStatus.Running,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (cdBeam > 0) "Laser (${cdBeam}s)" else "Canhão Íon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Auto-Heal Nanites Trigger
                Button(
                    onClick = { viewModel.triggerOverchargeHealSkill() },
                    enabled = cdHeal == 0 && status is CombatStatus.Running,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (cdHeal > 0) "Reparar (${cdHeal}s)" else "Reparar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Histórico de Turno", fontWeight = FontWeight.Bold, fontSize = 12.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(logs.reversed()) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                log.contains("Action") -> SolarGold
                                log.contains("neutralized") || log.contains("🏆") -> SuccessGreen
                                log.contains("withdrew") || log.contains("damaged") -> AlertRed
                                else -> TextWhite
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (status) {
                is CombatStatus.Victory -> {
                    NexusPanel(
                        accent = SuccessGreen,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column {
                            Text("VITÓRIA SETORIAL", fontWeight = FontWeight.Black, color = SuccessGreen, fontSize = 19.sp)
                            Text("Ameaça neutralizada • recompensas coletadas", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            NexusRewardRow(credits = "+${formatCredits(status.credits)} C$",
                                gems = "+${status.gems}", guild = "+${status.guildTokens}")
                        }
                    }
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Text("Retornar ao HQ")
                    }
                }
                is CombatStatus.Defeated -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌ RECUO ESTRATÉGICO!", fontWeight = FontWeight.Black, color = AlertRed, fontSize = 16.sp)
                            Text("O inimigo era muito forte. Evolua os HP e ataques do batalhão de defesa e tente novamente.", fontSize = 12.sp, color = TextWhite)
                        }
                    }
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Text("Retornar e Fortalecer")
                    }
                }
                else -> {
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(8.dp)) {
                        Text("Forçar Retirada")
                    }
                }
            }
        }
    }
}


// ---------------- TAB 3: MARKET TAB ----------------
@Composable
fun MarketTab(
    viewModel: GameViewModel,
    state: GameState,
    resources: List<ResourceInventory>
) {
    val prices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val trends by viewModel.marketTrends.collectAsStateWithLifecycle()
    val standings by viewModel.globalTradeStandings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Mercado Comercial Galático (Flutuação em Tempo Real)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Os preços das commodities variam com base na oferta e procura das alianças. Venda nos picos!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        Text("Cotação Atual de Commodities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Commodity list cards
        resources.forEach { inventory ->
            val price = prices[inventory.name] ?: 1.0
            val trend = trends[inventory.name] ?: "ESTÁVEL"
            val detailColor = when {
                trend.contains("BOOMING") || trend.contains("RISING") -> SuccessGreen
                trend.contains("DUMPING") || trend.contains("FALLING") -> AlertRed
                else -> SolarGold
            }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("market_card_${inventory.name}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = translateText(inventory.name),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = detailColor.copy(alpha = 0.15f), contentColor = detailColor) {
                                Text(translateTrend(trend), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Preço: ", fontSize = 12.sp, color = TextMuted)
                            Text("${formatCredits(price)} C\$", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = detailColor)
                        }
                        Text("Estoque: ${formatAmount(inventory.quantity)} un", fontSize = 12.sp, color = TextMuted)
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sellResourceToMarket(inventory.name, 10.0) },
                            enabled = inventory.quantity >= 10.0,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.width(105.dp).height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Vender 10", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = { viewModel.sellResourceToMarket(inventory.name, inventory.quantity) },
                            enabled = inventory.quantity > 0.0,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.width(105.dp).height(28.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Vender Tudo", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Competição Comercial de Guilda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Leaderboard of corps
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ranking simulado local (sem multijogador online)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                standings.forEachIndexed { idx, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (entry.isPlayer) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayName = if (entry.isPlayer) "${state.companyName} (Você)" else translateText(entry.companyName)
                        Text(
                            text = "${idx + 1}. $displayName",
                            fontWeight = if (entry.isPlayer) FontWeight.Bold else FontWeight.Normal,
                            color = if (entry.isPlayer) MaterialTheme.colorScheme.primary else TextWhite
                        )
                        Text(
                            text = "${formatCredits(entry.score)} C\$",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// ---------------- TAB 4: GUILD (ALIANÇA) ----------------
@Composable
fun GuildTab(
    viewModel: GameViewModel,
    state: GameState
) {
    val missions by viewModel.guildMissions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Aliança & Corporações Cooperativas 🤝",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta
                )
                Text(
                    "Demonstração local de guildas. Não há outros jogadores conectados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Faction selection info
        if (state.guildName == "No Guild Joined") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ainda não ingressou em uma Guilda!", fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("Escolha uma facção abaixo para iniciar:", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.joinGuild("Alpha Vanguard") }, 
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                        ) {
                            Text("Alpha Vanguard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.joinGuild("Cyber Syndicate") }, 
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta, contentColor = Color.White)
                        ) {
                            Text("Cyber Syndicate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sua Guilda ativa:", fontSize = 11.sp, color = TextMuted)
                            Text(state.guildName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = NeonCyan)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Missões simuladas; sem bônus cooperativo real.", fontSize = 12.sp, color = SuccessGreen)
                        }
                        Button(
                            onClick = { viewModel.joinGuild("No Guild Joined") },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Sair", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Missões Ativas da Aliança (Progresso Coop Real-Time)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)

            // Missions list
            missions.forEach { mission ->
                Card(
                     modifier = Modifier.fillMaxWidth(),
                     shape = RoundedCornerShape(12.dp),
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                     border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mission.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhite)
                            Badge(containerColor = NeonMagenta.copy(alpha = 0.15f), contentColor = NeonMagenta) {
                                Text("+${mission.rewardContribution} Contrib", modifier = Modifier.padding(2.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(mission.titleDescription, fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = (mission.progress / mission.target).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${formatAmount(mission.progress)}/${formatAmount(mission.target)}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 5: PROFILE O CORE PERSONALIZATION ----------------
@Composable
fun ProfileTab(
    viewModel: GameViewModel,
    state: GameState
) {
    var newName by remember(state.companyName) { mutableStateOf(state.companyName) }
    var selectedAvatar by remember(state.selectedAvatarId) { mutableStateOf(state.selectedAvatarId) }
    var selectedSkin by remember(state.selectedSkinId) { mutableStateOf(state.selectedSkinId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Perfil da Corporação & Aparência 🧑‍🚀",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    "Altere o tema visual e customize seu avatar para se destacar no placar estelar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Customize Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Identificação Corporativa", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextWhite)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nome da Corporação") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Código Visual (Avatar)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val itemsRow1 = listOf(0, 1, 2, 3)
                    val itemsRow2 = listOf(4, 5, 6, 7)

                    listOf(itemsRow1, itemsRow2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { id ->
                                val (label, icon) = getAvatarDetails(id)
                                val isSel = selectedAvatar == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.2f)
                                        )
                                        .clickable { selectedAvatar = id }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSel) MaterialTheme.colorScheme.primary else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Tema de Cores da Base Estelar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val skinsList = listOf("Industrial Blue", "Solar Gold", "Cyber Neon Purple")
                    skinsList.forEachIndexed { index, name ->
                        val isSel = selectedSkin == index
                        val colorAccent = when (index) {
                            1 -> SolarGold
                            2 -> NeonMagenta
                            else -> NeonCyan
                        }
                        val skinIcon = when (index) {
                            1 -> Icons.Filled.WbSunny
                            2 -> Icons.Filled.Cyclone
                            else -> Icons.Filled.Business
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) colorAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(
                                    color = if (isSel) colorAccent.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.2f)
                                )
                                .clickable { selectedSkin = index }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = skinIcon,
                                contentDescription = null,
                                tint = if (isSel) colorAccent else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) colorAccent else TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selecionado",
                                    tint = colorAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.customizeProfile(selectedAvatar, selectedSkin, newName) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Salvar Alterações de Perfil", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.resetTutorial() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reexibir Guia de Aprendizado (Tutorial)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------- TAB 6: PREMIUM ASCENSÃO (PRESTIGIO) ----------------
@Composable
fun AscensionTab(
    viewModel: GameViewModel,
    state: GameState
) {
    val prestigeBonusPct = (state.nebulaCores * 10).toInt()
    val currentCash = state.cash
    val minCashRequired = viewModel.getPrestigeMinCash(state.nebulaCores)
    val showPrestigeButton = currentCash >= minCashRequired
    val earnedCoresNow = if (showPrestigeButton) {
        kotlin.math.floor(kotlin.math.sqrt(currentCash / minCashRequired)).toLong()
    } else 0L

    var prestigeError by remember { mutableStateOf("") }
    var prestigeSuccess by remember { mutableStateOf("") }
    var showPrestigeConfirmation by remember { mutableStateOf(false) }
    var prestigePending by remember { mutableStateOf(false) }

    if (showPrestigeConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!prestigePending) showPrestigeConfirmation = false },
            title = { Text("Confirmar Ascensão Estelar") },
            text = {
                Text("Uma nova run começará na patente inicial, com as missões iniciais. " +
                    "Créditos, estoques e fábricas serão reiniciados. " +
                    "Gemas, ciência, pesquisadores e suas cartas, Núcleos de Nebulosa, " +
                    "melhorias permanentes e recompensas já resgatadas permanecem.")
            },
            confirmButton = {
                Button(
                    enabled = !prestigePending,
                    onClick = {
                        prestigePending = true
                        viewModel.performPrestigeAction(
                            onSuccess = {
                                prestigePending = false
                                showPrestigeConfirmation = false
                                prestigeSuccess = "Ascensão concluída! Patente e missões reiniciadas; seus ganhos permanentes foram mantidos."
                                prestigeError = ""
                            },
                            onError = {
                                prestigePending = false
                                showPrestigeConfirmation = false
                                prestigeError = it
                                prestigeSuccess = ""
                            }
                        )
                    }
                ) { Text(if (prestigePending) "Ascendendo…" else "Confirmar Ascensão") }
            },
            dismissButton = {
                TextButton(onClick = { showPrestigeConfirmation = false }, enabled = !prestigePending) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.example.ui.StellarLaboratory(state, viewModel)
        NexusPanel(
            accent = SolarGold,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ASCENSÃO ESTELAR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SolarGold
                    )
                    Badge(containerColor = SolarGold, contentColor = Color.Black) {
                        Text("x${1.0 + state.nebulaCores * 0.1} Ativo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A Ascensão Estelar redefine suas indústrias, moedas e estoques em troca de Núcleos de Nebulosa. " +
                            "Cada Star Core adiciona +10% de bônus permanente à velocidade global de todas as fábricas! " +
                            "O requisito de C$ escalona em 1.6x a cada ascensão realizada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bônus Passivo Atual:", fontSize = 11.sp, color = TextMuted)
                        Text("+$prestigeBonusPct% de Velocidade Global", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Núcleos conquistados:", fontSize = 11.sp, color = TextMuted)
                        Text("${state.nebulaCores}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SolarGold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cada core aumenta a produção em 10%. Cores e pesquisas são mantidos na ascensão.",
                    fontSize = 11.sp,
                    color = SolarGold.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        NexusPanel(
            accent = if (showPrestigeButton) SolarGold else NexusVisuals.locked,
            modifier = Modifier.fillMaxWidth(),
            unlocked = showPrestigeButton
        ) {
            Column {
                Text("Processar Fenda de Ascensão", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhite)
                Spacer(modifier = Modifier.height(8.dp))
                NexusMetric("Créditos para ascender",
                    "${formatCredits(currentCash)} / ${formatCredits(minCashRequired)} C$",
                    if (showPrestigeButton) SolarGold else TextMuted, Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                if (showPrestigeButton) {
                    NexusRewardRow(cores = "+$earnedCoresNow")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sua corporação alcançou os requisitos! A Ascensão reinicia créditos, estoques, fábricas, patente e missões da run. Você recebe +$earnedCoresNow Núcleos de Nebulosa; gemas, ciência, pesquisadores e melhorias permanentes permanecem.",
                        fontSize = 12.sp,
                        color = SolarGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showPrestigeConfirmation = true },
                        enabled = !prestigePending,
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ascender e coletar +$earnedCoresNow núcleos", fontWeight = FontWeight.Black)
                    }
                } else {
                    val remaining = minCashRequired - currentCash
                    Text(
                        "Ascensão Bloqueada: É necessário acumular pelo menos ${formatCredits(minCashRequired)} C$ para canalizar uma fenda de prestígio nesta etapa.",
                        fontSize = 12.sp,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Falta acumular: ${formatCredits(remaining)} C$. Otimize suas fábricas para atingir o valor exigido!",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (prestigeSuccess.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, SuccessGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(prestigeSuccess, color = SuccessGreen, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                    }
                }

                if (prestigeError.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(prestigeError, color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}


// ---------------- TAB 5: SHOP AND EVENTS ----------------
@Composable
fun ShopAndEventTab(
    viewModel: GameViewModel,
    state: GameState
) {
    val event by viewModel.seasonalChampionship.collectAsStateWithLifecycle()
    val billing = (androidx.activity.compose.LocalActivity.current as? MainActivity)?.playBilling
    val purchaseStates = billing?.state?.collectAsStateWithLifecycle()?.value.orEmpty()
    val vipPurchase = purchaseStates[com.example.billing.PlayProduct.VIP]
    val licensePurchase = purchaseStates[com.example.billing.PlayProduct.AUTO_SELL]
    var gemLicenseBusy by remember { mutableStateOf(false) }
    var gemLicenseFeedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RewardedAdsCard()
        // Seasonal Event Section
        NexusPanel(
            accent = SolarGold,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVENTO LOCAL: ${event.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SolarGold
                    )
                    Badge(containerColor = SolarGold) {
                        Text("Objetivos locais", modifier = Modifier.padding(2.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Complete objetivos sazonais para ganhar diamantes e Núcleos de Nebulosa!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                event.tasks.forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.description, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                LinearProgressIndicator(
                                    progress = (task.progress / task.target).toFloat().coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = SolarGold
                                )
                                Text("Progresso: ${formatAmount(task.progress)} / ${formatAmount(task.target)}", fontSize = 10.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            if (task.progress >= task.target) {
                                if (task.isClaimed) {
                                    Text("Resgatado", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                } else {
                                    Button(
                                        onClick = { viewModel.claimEventReward(task.taskId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SolarGold),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Resgatar", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            } else {
                                Icon(Icons.Filled.Lock, contentDescription = "Bloqueado", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Text("Loja Estelar & Assinaturas Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Compras em dinheiro real exigem produtos na Play Console e verificação no servidor. Trocas por créditos e gemas são do jogo.",
            fontSize = 12.sp, color = TextMuted)

        // VIP Subscription purchase
        NexusPanel(
            accent = SolarGold,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Navegador Premium VIP (Assinatura)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (state.isSubscribed) {
                        Badge(containerColor = SuccessGreen) { Text("Ativado", modifier = Modifier.padding(4.dp)) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• +50% Velocidade de toda a produção idle\n• Multiplicador VIP em lucros off-line\n• 1.000 Gemas iniciais somente após compra verificada",
                    fontSize = 12.sp,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (!state.isSubscribed) {
                    Button(
                        onClick = {
                            if (BuildConfig.DEBUG) viewModel.buySubscription()
                            else billing?.buy(com.example.billing.PlayProduct.VIP)
                        },
                        enabled = BuildConfig.DEBUG || vipPurchase?.phase == com.example.billing.PurchasePhase.AVAILABLE,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (BuildConfig.DEBUG) "Ativar VIP de demonstração"
                            else if (vipPurchase?.phase == com.example.billing.PurchasePhase.AVAILABLE)
                                "Assinar VIP • ${vipPurchase.price ?: "Google Play"}"
                            else "Assinatura indisponível", fontWeight = FontWeight.Black)
                    }
                    Text(vipPurchase?.message ?: "Consultando Google Play…", color = TextMuted,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "Benefícios locais ativos. Esta tela não comprova uma assinatura paga.",
                        color = SolarGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Auto-venda license purchase
        NexusPanel(
            accent = NeonCyan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Módulos: Auto-venda Comercial (Licença)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (state.hasAutoSellLicense) {
                        Badge(containerColor = SuccessGreen) { Text("Adquirido", modifier = Modifier.padding(4.dp)) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Desbloqueia o interruptor de Auto-venda nas indústrias\n• Converte produção diretamente em Créditos (C$) com base na cotação\n• Funciona off-line em segundo plano e em tempo real",
                    fontSize = 12.sp,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (!state.hasAutoSellLicense) {
                    Button(
                        onClick = { viewModel.buyAutoSellLicense() },
                        enabled = BuildConfig.DEBUG,
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (BuildConfig.DEBUG) "Ativar licença de demonstração" else "Compras indisponíveis", fontWeight = FontWeight.Black)
                    }
                    if (!BuildConfig.DEBUG) {
                        Button(onClick = { billing?.buy(com.example.billing.PlayProduct.AUTO_SELL) },
                            enabled = licensePurchase?.phase == com.example.billing.PurchasePhase.AVAILABLE,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (licensePurchase?.phase == com.example.billing.PurchasePhase.AVAILABLE)
                                "Comprar licença • ${licensePurchase.price ?: "Google Play"}"
                                else "Compra real indisponível")
                        }
                        Text(licensePurchase?.message ?: "Consultando Google Play…", color = TextMuted,
                            style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            gemLicenseBusy = true
                            gemLicenseFeedback = ""
                            viewModel.buyAutoSellLicenseWithGems(
                                onSuccess = {
                                    gemLicenseBusy = false
                                    gemLicenseFeedback = "Licença de auto-venda adquirida."
                                },
                                onError = {
                                    gemLicenseBusy = false
                                    gemLicenseFeedback = it
                                }
                            )
                        },
                        enabled = !gemLicenseBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Stars, contentDescription = null, modifier = Modifier.size(16.dp), tint = SolarGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (gemLicenseBusy) "Processando…" else "Desbloquear com 300 diamantes", fontWeight = FontWeight.Bold)
                    }
                    if (gemLicenseFeedback.isNotEmpty()) {
                        Text(gemLicenseFeedback, color = TextWhite,
                            style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(
                        "Licença comercial ativa! Suas fábricas e mineradoras agora podem vender e lucrar em C$ automaticamente.",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Star gem packages
        Text("Comprar Gemas com Moedas do Jogo", fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                GemsShopItem(
                    rewardGems = 100,
                    costCoins = 3000.0,
                    packName = "Pilha de Gemas",
                    state = state,
                    onClick = { viewModel.buyPremiumPack(3000.0, 100L, "Pilha de Gemas") }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                GemsShopItem(
                    rewardGems = 500,
                    costCoins = 12000.0,
                    packName = "Cofre de Gemas",
                    state = state,
                    onClick = { viewModel.buyPremiumPack(12000.0, 500L, "Cofre de Gemas") }
                )
            }
        }

        // Star Gems spending options
        Text("Utilidades e Gastos de Gemas Estelares 💎", fontWeight = FontWeight.SemiBold)

        var purchaseStatusSuccess by remember { mutableStateOf("") }
        var purchaseStatusError by remember { mutableStateOf("") }

        if (purchaseStatusSuccess.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(purchaseStatusSuccess, color = SuccessGreen, fontSize = 11.sp, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
            }
        }
        if (purchaseStatusError.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(purchaseStatusError, color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Time Warp 1h
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Autorenew, contentDescription = null, tint = SolarGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dobra Temporal 1h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Acelera prod/créditos de 1h", fontSize = 9.sp, color = TextMuted, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.purchaseTimeWarpWithGems(1, 50, 
                                onSuccess = {
                                    purchaseStatusSuccess = "Dobra de 1 hora efetuada com sucesso! Recursos e moedas creditados."
                                    purchaseStatusError = ""
                                },
                                onError = {
                                    purchaseStatusError = it
                                    purchaseStatusSuccess = ""
                                }
                            )
                        },
                        enabled = state.starGems >= 50,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text("50 💎", fontSize = 10.sp)
                    }
                }
            }

            // Time Warp 4h
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FlashOn, contentDescription = null, tint = SolarGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dobra Temporal 4h", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Acelera prod/créditos de 4h", fontSize = 9.sp, color = TextMuted, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.purchaseTimeWarpWithGems(4, 150, 
                                onSuccess = {
                                    purchaseStatusSuccess = "Dobra de 4 horas efetuada com sucesso! Sua prosperidade disparou."
                                    purchaseStatusError = ""
                                },
                                onError = {
                                    purchaseStatusError = it
                                    purchaseStatusSuccess = ""
                                }
                            )
                        },
                        enabled = state.starGems >= 150,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text("150 💎", fontSize = 10.sp)
                    }
                }
            }
        }

        // Supply crates
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = SolarGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cofre de Suprimentos Estelar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Contém: +25.0k C$, +500 Cel. de Energia, +300 Ferro, +100 Hiperliga, +25 Chips Quânticos", fontSize = 11.sp, color = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.purchaseSupplyCrateWithGems(
                            onSuccess = {
                                purchaseStatusSuccess = "Suprimentos Estelares creditados com sucesso!"
                                purchaseStatusError = ""
                            },
                            onError = {
                                purchaseStatusError = it
                                purchaseStatusSuccess = ""
                            }
                        )
                    },
                    enabled = state.starGems >= 100,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Desbloquear Carga Estelar por 100 Gemas 💎", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GemsShopItem(
    rewardGems: Int,
    costCoins: Double,
    packName: String,
    state: GameState,
    onClick: () -> Unit
) {
    NexusPanel(
        accent = NexusVisuals.gems,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Diamond, contentDescription = null, tint = NexusVisuals.gems, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(packName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("+$rewardGems GEMAS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NexusVisuals.gems)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                enabled = state.cash >= costCoins,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NexusVisuals.gems)
            ) {
                Text("${formatAmount(costCoins)} C\$", fontSize = 10.sp)
            }
        }
    }
}


// ---------------- HELPER COMPONENTS & FORMATS ----------------
fun translateText(term: String): String {
    val normalized = term.trim()
    return when (normalized) {
        // Resources
        "Energy Cell" -> "Célula de Energia"
        "Iron Ore" -> "Minério de Ferro"
        "Hyperalloy" -> "Hiperliga"
        "Quantum Chip" -> "Chip Quântico"
        "Organic Feedstock" -> "Massa Biológica Orgânica"
        "Neural Implant" -> "Implante Neural"
        "Antimatter Containment" -> "Célula de Antimatéria"
        "Warp Drive" -> "Motor de Dobra Espacial"
        // Buildings
        "Sol-Power Array" -> "Matriz de Energia Solar"
        "Deep-Core Drill" -> "Sonda de Perfuração Profunda"
        "Alloy Smelter" -> "Fundição de Metal"
        "Quantum Assembler" -> "Montador Quântico"
        "Biosphere Dome" -> "Domo de Biosfera"
        "Cybernetics Lab" -> "Laboratório Cibernético"
        "Antimatter Reactor" -> "Reator de Antimatéria"
        "Galactic Shipyard" -> "Estaleiro Estelar"
        // Combat Units
        "Titan Defender" -> "Defensor Titã"
        "Aegis Vanguard" -> "Vanguarda Égide"
        "Plasma Striker" -> "Atacante de Plasma"
        "EMP Interceptor" -> "Defensor de Pulso Eletromagnético (Interceptador PEM)"
        // Combat Unit types / classes
        "Heavy Mech" -> "Mech Pesado"
        "Shield Troop" -> "Tropa com Escudo"
        "Ranged DPS" -> "DPS de Longo Alcance"
        "Disrupter" -> "Disruptor"
        // Factions leaderboard
        "Apex Industries" -> "Indústrias Ápice"
        "Sol Star Sagas" -> "Sagas Industriais do Sol"
        "Hydra Conglomerate" -> "Conglomerado Hidra"
        else -> term
    }
}

fun translateTrend(trend: String): String {
    val normalized = trend.trim()
    return when {
        normalized.contains("BOOMING") -> "Crescimento Rápido 📈"
        normalized.contains("DUMPING") -> "Queda Livre 📉"
        normalized.contains("RISING") -> "Subindo ▲"
        normalized.contains("FALLING") -> "Caindo ▼"
        normalized.contains("STEADY") -> "Estável ●"
        else -> trend
    }
}

fun formatSecondsToDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val sb = StringBuilder()
    if (h > 0) sb.append("$h h ")
    if (m > 0) sb.append("$m min ")
    if (s > 0 || (h == 0L && m == 0L)) sb.append("$s s")
    return sb.toString().trim()
}

@Composable
fun OfflineEarningsDialog(
    report: OfflineEarnings,
    offlineHours: Int,
    isSubscribed: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CloudSync,
                    contentDescription = null,
                    tint = SolarGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Rendimentos Offline 🛰️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SolarGold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Suas fábricas automatizadas continuaram ativas durante o seu período de ausência estelar!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )

                // Info card about offline limit
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSubscribed) SolarGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, if (isSubscribed) SolarGold.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Tempo Total Registrado",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Badge(containerColor = if (isSubscribed) SolarGold else MaterialTheme.colorScheme.secondary) {
                                Text(
                                    if (isSubscribed) "Premium VIP (Máx: ${offlineHours}h)" else "Jogador Comum (Máx: ${offlineHours}h)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubscribed) Color.Black else TextWhite,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatSecondsToDuration(report.offlineSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isSubscribed) SolarGold else TextWhite
                        )
                        if (!isSubscribed && report.offlineSeconds >= offlineHours * 3600L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 Dica: Seu tempo offline atingiu o limite de $offlineHours horas. Vire VIP na Loja para expandir seu limite para ${offlineHours + 4} horas!",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarGold,
                                fontSize = 10.sp
                            )
                        } else if (isSubscribed && report.offlineSeconds >= offlineHours * 3600L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⭐ Seu limite Premium de $offlineHours horas de lucros offline foi aproveitado ao máximo!",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Text("Recursos extraídos:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextWhite)
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var producedAny = false
                    report.resourcesEarned.forEach { (name, qty) ->
                        if (qty > 0.0) {
                            producedAny = true
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(translateText(name), color = TextMuted)
                                Text("+${formatAmount(qty)} un", fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }
                    }
                    if (report.cashEarned > 0.0) {
                        producedAny = true
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Moedas Convertidas (Auto-venda)", color = SuccessGreen)
                            Text("+${formatAmount(report.cashEarned)} C\$", fontWeight = FontWeight.Black, color = SuccessGreen)
                        }
                    }
                    if (!producedAny) {
                        Text("Nenhum recurso gerado (suas fábricas podem estar sem energia ou desativadas).", color = TextMuted, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black)
            ) {
                Text("Coletar Lucros", fontWeight = FontWeight.Black)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = TextWhite,
        textContentColor = TextWhite
    )
}

data class TutorialSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun GameTutorialDialog(
    onDismiss: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }

    val slides = listOf(
        TutorialSlide(
            title = "Bem-vindo, Comandante! 🧑‍🚀",
            description = "Sua missão é gerenciar uma rede corporativa de extração, refino e processamento de recursos intersiderais avançados. Prepare-se para maximizar faturamentos galácticos, coordenar frotas táticas e obter ascensão estelar!",
            icon = Icons.Filled.RocketLaunch,
            tint = SolarGold
        ),
        TutorialSlide(
            title = "Indústria & Fábricas 🏭",
            description = "Ative fábricas e melhore seus níveis com créditos. A produção automática depende da automação; fábricas manuais precisam de toques. Observe o estoque e o consumo de insumos antes de expandir. Marcos de nível aumentam a eficiência.",
            icon = Icons.Filled.Business,
            tint = NeonCyan
        ),
        TutorialSlide(
            title = "Cientistas & Operários 👥",
            description = "Abra cápsulas para obter cartas de pesquisadores. Recrute e evolua pesquisadores com cartas e ciência; seus efeitos ajudam a indústria. Camaradas são gerados ao longo do tempo e representam sua força de trabalho.",
            icon = Icons.Filled.Group,
            tint = NeonCyan
        ),
        TutorialSlide(
            title = "Segurança & Patrulhas 🛡️",
            description = "Melhore suas unidades na Defesa e enfrente quatro setores de mineração hostil. Cada vitória aumenta a ameaça apenas naquele setor. Vitórias concedem créditos, diamantes e tokens; derrotas não reduzem a ameaça.",
            icon = Icons.Filled.Security,
            tint = NeonMagenta
        ),
        TutorialSlide(
            title = "Mercado Flutuante 📈",
            description = "Os preços de venda de cada elemento no Mercado mudam instantaneamente! Use os multiplicadores flutuantes a seu favor. Escolha o momento perfeito de alta galáctica para descarregar seu inventário excedente e realizar lucros altíssimos em créditos.",
            icon = Icons.Filled.TrendingUp,
            tint = SolarGold
        ),
        TutorialSlide(
            title = "Ascensão Dimensional 🌀",
            description = "Ao cumprir os requisitos, ascenda para conquistar Núcleos de Nebulosa. O total histórico mantém o bônus passivo de +10% de produção por núcleo, mesmo após gastar o saldo disponível. Use esse saldo no Laboratório Estelar para comprar melhorias permanentes de custos e limite offline.",
            icon = Icons.Filled.Autorenew,
            tint = NeonCyan
        ),
        TutorialSlide(
            title = "Salvamento e Backup 🛰️",
            description = "O progresso é salvo neste aparelho. Na aba Perfil, conecte sua Conta Google e toque em Salvar na Nuvem para criar um backup real. Para recuperar, toque em Carregar Nuvem. Confirme o backup antes de desinstalar o jogo; a demonstração local não substitui o Firebase.",
            icon = Icons.Filled.CloudSync,
            tint = NeonMagenta
        )
    )
    val totalSlides = slides.size

    val current = slides[currentSlide]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tutorial de Integração Mecânica",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${currentSlide + 1} / $totalSlides",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (currentSlide + 1) / totalSlides.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = current.tint,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.55f).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(current.tint.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, current.tint.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = current.icon,
                        contentDescription = null,
                        tint = current.tint,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = current.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp
                    ),
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentSlide < totalSlides - 1) {
                        currentSlide++
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = current.tint, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (currentSlide == totalSlides - 1) {
                    Text("Concluir! 🚀", fontWeight = FontWeight.Bold)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Próximo", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentSlide > 0) {
                    TextButton(
                        onClick = { currentSlide-- },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextWhite)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voltar")
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)
                ) {
                    Text("Pular")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = TextWhite,
        textContentColor = TextWhite
    )
}

fun calculateMultiUpgrade(baseCost: Double, costMultiplier: Double, currentLevel: Int, currentCash: Double, multiplier: String): Pair<Int, Double> =
    com.example.data.EconomyCosts.calculateMultiUpgrade(baseCost, costMultiplier, currentLevel, currentCash, multiplier)

// Format double values cleanly as strings
fun formatCredits(amount: Double): String = com.example.ui.compactNumber(amount)

fun formatAmount(amount: Double): String = com.example.ui.compactNumber(amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val activeState = state ?: return

    val userEmail by viewModel.cloudSaveManager.userEmail.collectAsStateWithLifecycle()
    val demoBackup by viewModel.cloudSaveManager.isSandboxMode.collectAsStateWithLifecycle()
    val syncing by viewModel.cloudSaveManager.syncing.collectAsStateWithLifecycle()

    var textNameInput by remember(activeState.companyName) { mutableStateOf(activeState.companyName) }
    var selectedAvatar by remember(activeState.selectedAvatarId) { mutableStateOf(activeState.selectedAvatarId) }

    val loginContext = androidx.compose.ui.platform.LocalContext.current

    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackSuccess by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!syncing) onDismiss() },
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = SolarGold, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Perfil do Administrador 🧑‍🚀", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 20.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = textNameInput,
                    onValueChange = { textNameInput = it },
                    label = { Text("Nome da Corporação") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolarGold,
                        focusedLabelColor = SolarGold,
                        cursorColor = SolarGold
                    )
                )

                Text("Selecione seu Código Visual (Avatar):", style = MaterialTheme.typography.titleSmall, color = SolarGold, fontWeight = FontWeight.Bold)

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val itemsRow1 = listOf(0, 1, 2, 3)
                    val itemsRow2 = listOf(4, 5, 6, 7)

                    listOf(itemsRow1, itemsRow2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { id ->
                                val (label, icon) = getAvatarDetails(id)
                                val isSelected = selectedAvatar == id
                                Button(
                                    onClick = {
                                        selectedAvatar = id
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) SolarGold else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSelected) Color.Black else TextWhite
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Section: Google Sync
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, tint = SolarGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sincronização em Nuvem", style = MaterialTheme.typography.titleSmall, color = SolarGold, fontWeight = FontWeight.Bold)
                }

                if (userEmail == null) {
                    // Not connected
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (viewModel.cloudSaveManager.isAvailable) "Conecte sua conta Google para salvar e restaurar seu progresso." else "O backup em nuvem ainda não está disponível nesta versão.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        viewModel.cloudSaveManager.signIn(loginContext)
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        feedbackSuccess = false
                                        feedbackMessage = "Não foi possível conectar sua conta Google. Tente novamente."
                                    }
                                }
                            },
                            enabled = viewModel.cloudSaveManager.isAvailable && !syncing,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vincular com o Google", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Connected
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(if (demoBackup) "DEMONSTRAÇÃO LOCAL: não salva no Firebase" else "Conta Google Vinculada", fontSize = 9.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                        Text(userEmail!!, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextWhite)
                                    }
                                }
                                TextButton(
                                    onClick = { viewModel.logoutFromCloud() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                                ) {
                                    Text("Sair", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.uploadToCloud(
                                        onSuccess = {
                                            feedbackMessage = if (demoBackup) "Demonstração salva apenas neste aparelho." else "Backup efetuado com sucesso no Firebase!"
                                            feedbackSuccess = true
                                        },
                                        onError = {
                                            feedbackMessage = "Falha ao sincronizar: $it"
                                            feedbackSuccess = false
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !syncing,
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (syncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                                } else {
                                    Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salvar na Nuvem", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.downloadFromCloud(
                                        onSuccess = {
                                            feedbackMessage = "Progresso restaurado com sucesso da nuvem!\n" + viewModel.cloudSaveManager.lastRestoreSummary
                                            feedbackSuccess = true
                                        },
                                        onError = {
                                            feedbackMessage = "Falha ao restaurar: $it"
                                            feedbackSuccess = false
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !syncing,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (syncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                } else {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (demoBackup) "Carregar demo" else "Carregar Nuvem", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Feedback section
                if (feedbackMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (feedbackSuccess) SuccessGreen.copy(alpha = 0.12f) else Color.Red.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, if (feedbackSuccess) SuccessGreen else Color.Red)
                    ) {
                        Text(
                            text = feedbackMessage,
                            modifier = Modifier.padding(10.dp),
                            color = if (feedbackSuccess) SuccessGreen else Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.customizeProfile(
                        avatarId = selectedAvatar,
                        skinId = activeState.selectedSkinId,
                        newName = textNameInput
                    )
                    onDismiss()
                },
                enabled = !syncing,
                colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !syncing
            ) {
                Text("Fechar", color = TextMuted)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = TextWhite,
        textContentColor = TextWhite
    )
}
