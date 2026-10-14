package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ResearcherCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/** Presentation only: the repository has already credited every reward before this is shown. */
@Composable
internal fun RewardPackOpening(
    capsuleType: String,
    cards: List<String>,
    researchers: List<ResearcherCard>,
    science: Long,
    gems: Long = 0,
    onClose: () -> Unit
) {
    var phase by rememberSaveable { mutableIntStateOf(0) } // entrance, ready, opening, revealed
    var cardIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val accent = when (capsuleType.uppercase()) {
        "COMUM" -> NeonCyan
        "EPICA", "ÉPICA" -> NexusVisuals.cores
        else -> SolarGold
    }
    LaunchedEffect(phase) {
        when (phase) {
            0 -> { delay(380); phase = 1 }
            2 -> { delay(460); phase = 3 }
        }
    }
    val scale by animateFloatAsState(
        targetValue = when (phase) { 0 -> 0.72f; 2 -> 1.16f; else -> 1f },
        animationSpec = tween(340, easing = FastOutSlowInEasing), label = "packScale"
    )
    val glow by animateFloatAsState(
        targetValue = if (phase == 2) 0.8f else 0.28f,
        animationSpec = tween(280), label = "packGlow"
    )
    Dialog(
        onDismissRequest = {
            if (phase == 3) {
                if (cards.size > 1 && !showAll && cardIndex < cards.lastIndex) showAll = true
                else onClose()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false,
            dismissOnBackPress = phase == 3)
    ) {
        Box(
            Modifier.fillMaxSize().background(CosmicDark).navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (phase == 3) "PESQUISA REVELADA" else "CÁPSULA $capsuleType",
                    color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                if (phase < 3) {
                    Box(
                        Modifier.size(240.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            .background(accent.copy(alpha = glow * 0.28f), CircleShape)
                            .border(2.dp, accent.copy(alpha = glow), CircleShape)
                            .clickable(enabled = phase == 1) { phase = 2 }
                            .testTag("reward_pack"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            Modifier.width(150.dp).height(184.dp)
                                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.8f), CosmicCard,
                                    accent.copy(alpha = 0.4f))), RoundedCornerShape(38.dp))
                                .border(3.dp, accent, RoundedCornerShape(38.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = TextWhite, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("NEXUS", color = TextWhite, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth().height(2.dp).background(accent))
                            Spacer(Modifier.height(8.dp))
                            Text(capsuleType.uppercase(), color = TextWhite, fontSize = 11.sp,
                                fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(30.dp))
                    Text(if (phase == 1) "TOQUE PARA ABRIR" else "PREPARANDO REVELAÇÃO…",
                        color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center)
                } else {
                    if (showAll) {
                        Text("TODAS AS CARTAS • ${cards.size}", color = accent, fontWeight = FontWeight.Black)
                        ResearcherRewardCards(cards, researchers)
                    } else AnimatedVisibility(visible = true, enter = fadeIn(tween(280)) + scaleIn(tween(280))) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val entry = cards.getOrNull(cardIndex).orEmpty()
                            val match = Regex("^(.*) \\(x([0-9]+)\\)$").matchEntire(entry)
                            val name = match?.groupValues?.get(1) ?: entry
                            val quantity = match?.groupValues?.get(2) ?: "1"
                            val researcher = researchers.firstOrNull { it.name == name }
                            val tint = researcher?.let { ResearcherStyle.color(it.rarity) } ?: accent
                            if (researcher != null) {
                                ResearcherPortrait(researcher, 164.dp)
                                Text(ResearcherStyle.label(researcher.rarity), color = tint,
                                    fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Text(researcher.name, color = TextWhite, fontSize = 25.sp,
                                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                Text(researcherSpecialization(researcher.boostType), color = tint,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(if (researcher.level > 0 || researcher.cardsCollected > quantity.toIntOrNull().orZero())
                                    "CÓPIA ADICIONAL" else "NOVO PESQUISADOR",
                                    color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.AutoAwesome, null, tint = tint, modifier = Modifier.size(100.dp))
                                Text(name, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("×$quantity cópia${if (quantity == "1") "" else "s"} para evolução",
                                color = TextWhite, fontWeight = FontWeight.Bold)
                            if (cards.size > 1) Text("${cardIndex + 1} de ${cards.size}", color = TextMuted)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    if (showAll || cardIndex == cards.lastIndex) {
                        if (science > 0) ScienceReward(science)
                        if (gems > 0) NexusRewardRow(gems = "+$gems")
                    }
                    Spacer(Modifier.height(20.dp))
                    if (!showAll && cards.size > 1 && cardIndex < cards.lastIndex) {
                        TextButton(onClick = { showAll = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("VER TODAS AS CARTAS", color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = {
                        if (!showAll && cardIndex < cards.lastIndex) cardIndex++ else onClose()
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reward_continue"),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                        Text(if (!showAll && cardIndex < cards.lastIndex) "REVELAR PRÓXIMA" else "CONTINUAR",
                            fontWeight = FontWeight.Black)
                    }
                    // Dialog windows on some devices draw behind the three-button navigation area.
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

private fun researcherSpecialization(boostType: String): String = when (boostType) {
    "PRODUCTION" -> "PRODUÇÃO"
    "SPEED" -> "VELOCIDADE"
    "COST_REDUCTION" -> "EFICIÊNCIA"
    "COMRADE_GEN" -> "GESTÃO"
    else -> "PESQUISADOR"
}
