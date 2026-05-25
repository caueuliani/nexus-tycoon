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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
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
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val buildings by viewModel.buildings.collectAsStateWithLifecycle()
    val combatUnits by viewModel.combatUnits.collectAsStateWithLifecycle()
    val offlineReport by viewModel.offlineEarnings.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Industry, 1: Combat, 2: Market, 3: Guild/Base, 4: Shop
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

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
            isSubscribed = state!!.isSubscribed,
            onDismiss = { viewModel.dismissOfflineEarnings() }
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
                            contentDescription = "HQ Logo",
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
                    label = { Text("Segurança", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                    state = state!!,
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
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("mobile_bottom_bar"),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Filled.Business, contentDescription = null) },
                        label = { Text("Indústria", fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.testTag("nav_industry")
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Filled.Security, contentDescription = null) },
                        label = { Text("Defesa", fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.testTag("nav_combat")
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Filled.TrendingUp, contentDescription = null) },
                        label = { Text("Mercado", fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.testTag("nav_market")
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                        label = { Text("Aliança", fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.testTag("nav_guild")
                    )
                    NavigationBarItem(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = null) },
                        label = { Text("Premium", fontSize = 10.sp, maxLines = 1) },
                        modifier = Modifier.testTag("nav_premium")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                TabContent(
                    tab = activeTab,
                    viewModel = viewModel,
                    state = state!!,
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
    AnimatedContent(
        targetState = tab,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
        },
        label = "TabTransition"
    ) { currentTab ->
        when (currentTab) {
            0 -> IndustryTab(viewModel, state, resources, buildings)
            1 -> CombatTab(viewModel, state, combatUnits)
            2 -> MarketTab(viewModel, state, resources)
            3 -> GuildAndSkinsTab(viewModel, state)
            4 -> ShopAndEventTab(viewModel, state)
        }
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
                        "Benefícios inclusos:\n• Conversão automática direta 1:1 com base no preço atual de mercado\n• Geração de créditos idle em segundo plano e progresso offline\n• Pagamento único (R$ 14,90)",
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
                    colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black)
                ) {
                    Text("Comprar por R$ 14,90", fontWeight = FontWeight.Black)
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

        if (buildings.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Inicializando geradores...", color = TextMuted)
                }
            }
        } else {
            items(buildings) { building ->
                val prices by viewModel.marketPrices.collectAsStateWithLifecycle()
                val currentPrice = prices[building.resourceProduced] ?: 1.0
                BuildingProductionCard(
                    building = building,
                    currentCash = state.cash,
                    currentGems = state.starGems,
                    marketPrice = currentPrice,
                    hasAutoSellLicense = state.hasAutoSellLicense,
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
                    Text(
                        text = "Setor Estelar: ${state.guildName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        fontSize = (32 * textPulseScale).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.isSubscribed) SolarGold else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Other in-game currency indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CurrencyBadge(icon = Icons.Filled.Stars, label = "${state.starGems}", text = "Gemas")
                CurrencyBadge(icon = Icons.Filled.ChangeCircle, label = "${state.nebulaCores}", text = "Star Cores")
                CurrencyBadge(icon = Icons.Filled.Group, label = "${state.guildTokens}", text = "Aliança")
            }
        }
    }
}

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
    currentCash: Double,
    currentGems: Long,
    marketPrice: Double,
    hasAutoSellLicense: Boolean,
    onUpgrade: () -> Unit,
    onAutomate: () -> Unit,
    onProduce: () -> Unit,
    onToggleAutoSell: (Boolean) -> Unit
) {
    val cost = building.baseCost * building.costMultiplier.pow(building.level.toDouble())
    val runsRate = building.productionRatePerLevel * building.level
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
        else -> Icons.Filled.OfflineBolt
    }

    val themeColor = when (building.resourceProduced) {
        "Energy Cell" -> SolarGold
        "Iron Ore" -> Color(0xFFB0BEC5)
        "Hyperalloy" -> NeonCyan
        "Quantum Chip" -> NeonMagenta
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .testTag("building_card_${building.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    // Animating spinning/rotating icon gear representing live industrial factory outputs
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
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (!isLocked) rotationAngle else 0f)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = translateText(building.name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocked) TextMuted else TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (!isLocked) {
                                Badge(containerColor = themeColor.copy(alpha = 0.2f), contentColor = themeColor) {
                                    Text("Nív ${building.level}", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(
                            text = if (isLocked) "Sistemas Desconectados" else "Produzindo: ${translateText(building.resourceProduced)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isLocked) {
                    Button(
                        onClick = onUpgrade,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Iniciar (${formatCredits(building.baseCost)} C\$)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                } else if (!building.isAutomated) {
                    Badge(containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), contentColor = TextMuted) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(10.dp), tint = TextMuted)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Manual", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Badge(containerColor = SuccessGreen.copy(alpha = 0.15f), contentColor = SuccessGreen) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(10.dp), tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Automação IA", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (!isLocked) {
                Spacer(modifier = Modifier.height(12.dp))
                
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
                                "Rendimento: +${formatCredits(runsRate * marketPrice)} C\$/s (Auto-vendido)"
                            } else {
                                "Rendimento: +${formatAmount(runsRate)}/s de ${translateText(building.resourceProduced)}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (building.isAutoSelling && hasAutoSellLicense) SuccessGreen else themeColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (building.inputResource != null) {
                            Text(
                                text = "Consome: ${formatAmount(building.inputAmountPerSec * building.level)}/s de ${translateText(building.inputResource)}",
                                fontSize = 11.sp,
                                color = AlertRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                            Text("Coletar Rendimento", fontSize = 11.sp, fontWeight = FontWeight.Black)
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

                if (building.resourceProduced == "Hyperalloy" || building.resourceProduced == "Quantum Chip") {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evoluir para Nív ${building.level + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Button(
                        onClick = onUpgrade,
                        enabled = currentCash >= cost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Upgrade: ${formatCredits(cost)} C\$", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    val isCombatActive by viewModel.combatActive.collectAsStateWithLifecycle()
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Operações de Patrulha Estelar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Adote decisões em combate tático para proteger os pipelines de extração.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            Text("Sua Unidade Tática de Combate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Layout combat units upgrades
            combatUnits.forEach { unit ->
                CombatUnitItemCard(
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    if (!isUnlocked) {
                                        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = TextMuted, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(
                                    text = "Inimigo: HP ${stage.enemyHp} | Dano ${stage.enemyDmg}", 
                                    fontSize = 12.sp, 
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { viewModel.startCombatBattle(stage.id) },
                                enabled = isUnlocked,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Deploi", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
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
                        Text("Sala de Replay (Último Combate)", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${lastReplay!!.stageName} - ${if (lastReplay!!.victory) "Vitória" else "Retirada"} (${lastReplay!!.roundsCount} Turnos)",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                // Simulate replay trigger by posting logs
                                viewModel.startCombatBattle(viewModel.selectedStage.value)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reassistir Clipe de Replay", fontSize = 11.sp)
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
    currentCash: Double,
    onUpgrade: () -> Unit
) {
    val upgradeCost = unit.upgradeCost * unit.costMultiplier.pow((unit.level - 1).toDouble())
    Card(
        modifier = Modifier.fillMaxWidth().testTag("combat_unit_card_${unit.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
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
                        text = "Classe: ${translateText(unit.type)} | HP: ${unit.health} | ATK: ${unit.attack}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onUpgrade,
                enabled = currentCash >= upgradeCost,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Evoluir: ${formatAmount(upgradeCost)} C\$", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛡️ VITÓRIA SETORIAL!", fontWeight = FontWeight.Black, color = SuccessGreen, fontSize = 16.sp)
                            Text("Ameaça raider totalmente neutralizada. Recompensas resgatadas:", fontSize = 12.sp, color = TextWhite)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("+${status.credits} Créditos", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("+${status.gems} Gemas", color = SolarGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("+${status.guildTokens} Alianças", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
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
                    text = "Multijogador Competitivo (Valor de Capital Corporativo)",
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


// ---------------- TAB 4: GUILD AND CUSTOMIZATION ----------------
@Composable
fun GuildAndSkinsTab(
    viewModel: GameViewModel,
    state: GameState
) {
    val missions by viewModel.guildMissions.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf(state.companyName) }
    var selectedAvatar by remember { mutableStateOf(state.selectedAvatarId) }
    var selectedSkin by remember { mutableStateOf(state.selectedSkinId) }

    val coroutines = rememberCoroutineScope()

    // --- PRESTIGIO / ASCENSÃO ESTELAR ---
    val prestigeBonusPct = (state.nebulaCores * 10).toInt()
    val currentCash = state.cash
    val minCashRequired = 100000.0
    val showPrestigeButton = currentCash >= minCashRequired
    val earnedCoresNow = if (showPrestigeButton) {
        kotlin.math.floor(kotlin.math.sqrt(currentCash / minCashRequired)).toLong()
    } else 0L

    var prestigeError by remember { mutableStateOf("") }
    var prestigeSuccess by remember { mutableStateOf("") }

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.5.dp, SolarGold)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ascensão Estelar (Prestígio) 💥",
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
                    text = "A Ascensão Estelar redefine suas fábricas (nível 1), moedas e estoques em troca de Star Cores super-raros. " +
                            "Cada Star Core adiciona +10% de bônus cumulativo à sua velocidade e eficiência global de produção!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bônus Passivo Atual:", fontSize = 11.sp, color = TextMuted)
                        Text("+$prestigeBonusPct% de Produção Global", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Star Cores Atuais:", fontSize = 11.sp, color = TextMuted)
                        Text("${state.nebulaCores} ✨", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SolarGold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                if (showPrestigeButton) {
                    Text(
                        "Pronto para Ascender! Sua corporação gerou riquezas suficientes. " +
                                "Ao reiniciar agora, você coletará +$earnedCoresNow Star Cores ✨ imediatamente!",
                        fontSize = 12.sp,
                        color = SolarGold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.performPrestigeAction(
                                onSuccess = {
                                    prestigeSuccess = "Ascensão Estelar efetuada! Sua produção global agora é x${1.0 + (state.nebulaCores + earnedCoresNow) * 0.1} mais rápida!"
                                    prestigeError = ""
                                },
                                onError = {
                                    prestigeError = it
                                    prestigeSuccess = ""
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reiniciar e Coletar +$earnedCoresNow Star Cores", fontWeight = FontWeight.Black)
                    }
                } else {
                    val remaining = minCashRequired - currentCash
                    Text(
                        "Ascensão Bloqueada: É necessário acumular pelo menos 100.00K C$ para canalizar uma fenda de prestígio. " +
                                "Falta ${formatAmount(remaining)} C$. Continue otimizando suas indústrias!",
                        fontSize = 11.sp,
                        color = TextMuted
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Skins da Base & Customização",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Altere o tema visual e seu perfil para se destacar no placar estelar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Customize Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Perfil da Corporação", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nome da Corporação") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Selecione seu Avatar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                Text("Tema Visual da Base (Personalização de Cores)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                                    contentDescription = "Selected",
                                    tint = colorAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.customizeProfile(selectedAvatar, selectedSkin, newName) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Salvar Configurações de Aparência", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Projetos Cooperativos da Guilda (Alianças)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Faction selection info
        if (state.guildName == "No Guild Joined") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ainda não ingressou em uma Guilda!", fontWeight = FontWeight.Bold)
                    Text("Participe de guildas para colaborar na produção e ganhar tokens exclusivos.", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.joinGuild("Alpha Vanguard") }, modifier = Modifier.weight(1f)) {
                            Text("Alpha Vanguard", fontSize = 11.sp)
                        }
                        Button(onClick = { viewModel.joinGuild("Cyber Syndicate") }, modifier = Modifier.weight(1f)) {
                            Text("Cyber Syndicate", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sua Guilda: ${state.guildName}", fontWeight = FontWeight.Bold)
                            Text("Bônus cooperativo ativo: +20% extra em missões de guilda.", fontSize = 12.sp, color = TextMuted)
                        }
                        Button(
                            onClick = { viewModel.joinGuild("No Guild Joined") },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                        ) {
                            Text("Sair", fontSize = 11.sp)
                        }
                    }
                }
            }

            Text("Missões Ativas da Aliança (Progresso Coop Real-Time)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Missions list
            missions.forEach { mission ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mission.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Badge(containerColor = NeonCyan.copy(alpha = 0.15f), contentColor = NeonCyan) {
                                Text("+${mission.rewardContribution} Contrib", modifier = Modifier.padding(2.dp))
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
                                fontWeight = FontWeight.Bold
                            )
                        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Seasonal Event Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 EVENTO SAZONAL: ${event.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SolarGold
                    )
                    Badge(containerColor = SolarGold) {
                        Text("${event.activeDaysRemaining}d restantes", modifier = Modifier.padding(2.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Complete objetivos sazonais para ganhar pacotes de Gemas e Star Cores raros exclusivíssimos!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                event.tasks.forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
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
                                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Text("Loja Estelar & Assinaturas Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // VIP Subscription purchase
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                    text = "• +50% Velocidade de toda a produção idle\n• Multiplicador VIP em lucros off-line\n• Resgata 1,000 Gemas Estelares instantâneas",
                    fontSize = 12.sp,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (!state.isSubscribed) {
                    Button(
                        onClick = { viewModel.buySubscription() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ativar Assinatura VIP (FREE Promo)", fontWeight = FontWeight.Black)
                    }
                } else {
                    Text(
                        "Obrigado por apoiar nosso desenvolvimento! Benefícios VIP aplicados.",
                        color = SolarGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Auto-venda license purchase
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, SolarGold)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Adquirir Licença Comercial (R$ 14,90)", fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.buyAutoSellLicenseWithGems(
                                onSuccess = { },
                                onError = { }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Stars, contentDescription = null, modifier = Modifier.size(16.dp), tint = SolarGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desbloquear com 300 Gemas 💎", fontWeight = FontWeight.Bold)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Stars, contentDescription = null, tint = SolarGold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(packName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("+$rewardGems 💎", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SolarGold)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                enabled = state.cash >= costCoins,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().height(32.dp)
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
        // Buildings
        "Sol-Power Array" -> "Matriz de Energia Solar"
        "Deep-Core Drill" -> "Sonda de Perfuração Profunda"
        "Alloy Smelter" -> "Fundição de Metal"
        "Quantum Assembler" -> "Montador Quântico"
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
                                    if (isSubscribed) "Premium VIP (Máx: 12h)" else "Jogador Comum (Máx: 8h)",
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
                        if (!isSubscribed && report.offlineSeconds >= 28800) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 Dica: Seu tempo offline atingiu o limite de 8 horas. Vire VIP na Loja para expandir seu limite para 12 horas!",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarGold,
                                fontSize = 10.sp
                            )
                        } else if (isSubscribed && report.offlineSeconds >= 43200) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⭐ Seu limite Premium de 12 horas de lucros offline foi aproveitado ao máximo!",
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

// Format double values cleanly as strings
fun formatCredits(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(amount)
}

fun formatAmount(amount: Double): String {
    if (amount >= 1_000_000) {
        return String.format("%.2fM", amount / 1_000_000.0)
    }
    if (amount >= 1_000) {
        return String.format("%.1fK", amount / 1_000.0)
    }
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 1
    return formatter.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val activeState = state ?: return

    val userEmail by viewModel.cloudSaveManager.userEmail.collectAsStateWithLifecycle()
    val syncing by viewModel.cloudSaveManager.syncing.collectAsStateWithLifecycle()

    var textNameInput by remember(activeState.companyName) { mutableStateOf(activeState.companyName) }
    var selectedAvatar by remember(activeState.selectedAvatarId) { mutableStateOf(activeState.selectedAvatarId) }

    var customEmailInput by remember { mutableStateOf("caueuliani@gmail.com") }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackSuccess by remember { mutableStateOf(true) }

    var manualBackupContent by remember { mutableStateOf("") }
    var showManualSyncSection by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!syncing) onDismiss() },
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Vincule sua conta do Google para realizar backups automáticos e acessar o seu progresso em múltiplos dispositivos.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        OutlinedTextField(
                            value = customEmailInput,
                            onValueChange = { customEmailInput = it },
                            label = { Text("E-mail do Google para Vínculo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = SolarGold, modifier = Modifier.size(18.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SolarGold,
                                focusedLabelColor = SolarGold,
                                cursorColor = SolarGold
                            )
                        )
                        Button(
                            onClick = {
                                if (customEmailInput.isNotBlank()) {
                                    viewModel.googleSignIn(customEmailInput.trim())
                                }
                            },
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
                                        Text("Conta Google Vinculada", fontSize = 9.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
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
                                            feedbackMessage = "Backup efetuado com sucesso na nuvem!"
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
                                            feedbackMessage = "Progresso restaurado com sucesso da nuvem!"
                                            feedbackSuccess = true
                                        },
                                        onError = {
                                            feedbackMessage = "Backup não encontrado."
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
                                    Text("Carregar Nuvem", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Collapsible manual system backup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualSyncSection = !showManualSyncSection }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = SolarGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recuperação por Código de Texto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = SolarGold)
                    }
                    Icon(
                        if (showManualSyncSection) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null, tint = SolarGold, modifier = Modifier.size(16.dp)
                    )
                }

                if (showManualSyncSection) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Export de segurança / Import manual de saves em codificação Base64.",
                            fontSize = 11.sp, color = TextMuted
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val sVal = viewModel.gameState.value ?: GameState()
                                            val resList = viewModel.resources.value
                                            val bList = viewModel.buildings.value
                                            val cList = viewModel.combatUnits.value

                                            val root = org.json.JSONObject()
                                            root.put("companyName", sVal.companyName)
                                            root.put("cash", sVal.cash)
                                            root.put("starGems", sVal.starGems)
                                            root.put("nebulaCores", sVal.nebulaCores)
                                            root.put("guildTokens", sVal.guildTokens)
                                            root.put("guildName", sVal.guildName)
                                            root.put("selectedSkinId", sVal.selectedSkinId)
                                            root.put("selectedAvatarId", selectedAvatar)
                                            root.put("isSubscribed", sVal.isSubscribed)
                                            root.put("highestCombatStage", sVal.highestCombatStage)
                                            root.put("hasAutoSellLicense", sVal.hasAutoSellLicense)

                                            val resArr = org.json.JSONArray()
                                            resList.forEach { r ->
                                                val o = org.json.JSONObject().put("name", r.name).put("quantity", r.quantity)
                                                resArr.put(o)
                                            }
                                            root.put("resources", resArr)

                                            val bArr = org.json.JSONArray()
                                            bList.forEach { b ->
                                                val o = org.json.JSONObject()
                                                    .put("id", b.id).put("name", b.name).put("level", b.level)
                                                    .put("isAutomated", b.isAutomated).put("baseCost", b.baseCost)
                                                    .put("costMultiplier", b.costMultiplier)
                                                    .put("productionRatePerLevel", b.productionRatePerLevel)
                                                    .put("resourceProduced", b.resourceProduced)
                                                    .put("inputResource", b.inputResource ?: org.json.JSONObject.NULL)
                                                    .put("inputAmountPerSec", b.inputAmountPerSec)
                                                    .put("isAutoSelling", b.isAutoSelling)
                                                bArr.put(o)
                                            }
                                            root.put("buildings", bArr)

                                            val cArr = org.json.JSONArray()
                                            cList.forEach { c ->
                                                val o = org.json.JSONObject()
                                                    .put("id", c.id).put("name", c.name).put("type", c.type)
                                                    .put("level", c.level).put("health", c.health).put("attack", c.attack)
                                                    .put("upgradeCost", c.upgradeCost).put("costMultiplier", c.costMultiplier)
                                                cArr.put(o)
                                            }
                                            root.put("combatUnits", cArr)

                                            val rawBytes = root.toString().toByteArray(Charsets.UTF_8)
                                            val b64 = android.util.Base64.encodeToString(rawBytes, android.util.Base64.NO_WRAP)
                                            manualBackupContent = b64
                                            
                                            feedbackMessage = "Código de backup gerado! Copie o texto abaixo."
                                            feedbackSuccess = true
                                        } catch (e: Exception) {
                                            feedbackMessage = "Erro ao exportar: ${e.message}"
                                            feedbackSuccess = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Gerar Código", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            if (manualBackupContent.isBlank()) {
                                                feedbackMessage = "Insira o código de backup antes de carregar."
                                                feedbackSuccess = false
                                                return@launch
                                            }
                                            val decodedBytes = android.util.Base64.decode(manualBackupContent.trim(), android.util.Base64.DEFAULT)
                                            val decodedStr = String(decodedBytes, Charsets.UTF_8)
                                            val jsonObject = org.json.JSONObject(decodedStr)
                                            
                                            val payload = mutableMapOf<String, Any>()
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
                                                            val valObj = obj.get(oKey)
                                                            if (valObj != org.json.JSONObject.NULL) {
                                                                 map[oKey] = valObj
                                                            }
                                                        }
                                                        list.add(map)
                                                    }
                                                    payload[key] = list
                                                } else {
                                                    if (value != org.json.JSONObject.NULL) {
                                                        payload[key] = value
                                                    }
                                                }
                                            }

                                            viewModel.restoreRawPayload(
                                                payload = payload,
                                                onSuccess = {
                                                    feedbackMessage = "Progresso restaurado com sucesso do código offline!"
                                                    feedbackSuccess = true
                                                },
                                                onError = {
                                                    feedbackMessage = "Incompatibilidade de código: $it"
                                                    feedbackSuccess = false
                                                }
                                            )
                                        } catch (e: Exception) {
                                            feedbackMessage = "Código de backup inválido ou corrompido."
                                            feedbackSuccess = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Injetar Código", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = manualBackupContent,
                            onValueChange = { manualBackupContent = it },
                            label = { Text("Código de Backup Base64") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SolarGold,
                                focusedLabelColor = SolarGold,
                                cursorColor = SolarGold
                            )
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
