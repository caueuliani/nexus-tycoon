package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ResearcherCard
import com.example.ui.theme.*

// Shared visual language for collection, industry, combat, ascension and rewards.
internal object NexusVisuals {
    val credits = SolarGold
    val gems = Color(0xFF55B8FF)
    val cores = Color(0xFFC179FF)
    val guild = NeonCyan
    val success = SuccessGreen
    val locked = TextMuted

    fun frame(color: Color) = Brush.linearGradient(listOf(color, color.copy(alpha = 0.25f), color))
    fun surface(color: Color) = Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), CosmicCard))
}

internal object ResearcherStyle {
    fun color(rarity: String): Color = when (rarity.uppercase()) {
        "COMMON" -> Color(0xFF9CB4C5)
        "RARE" -> Color(0xFF55B8FF)
        "EPIC" -> Color(0xFFC179FF)
        "SUPREME" -> SolarGold
        else -> NeonCyan
    }

    fun label(rarity: String): String = when (rarity.uppercase()) {
        "COMMON" -> "COMUM"
        "RARE" -> "RARO"
        "EPIC" -> "ÉPICO"
        "SUPREME" -> "SUPREMO"
        else -> rarity
    }

    fun artwork(id: Int): Int = when (id) {
        1 -> R.drawable.img_sci_elon
        2 -> R.drawable.img_sci_curie
        3 -> R.drawable.img_sci_ada
        4 -> R.drawable.img_sci_albert
        5 -> R.drawable.img_sci_karl
        6 -> R.drawable.img_sci_nebula
        else -> R.drawable.img_scientist_card
    }

    fun artwork(researcher: ResearcherCard): Int = when {
        researcher.id <= 6 -> artwork(researcher.id)
        researcher.boostType == "COST_REDUCTION" -> R.drawable.img_sci_elon
        researcher.boostType == "SPEED" -> R.drawable.img_sci_ada
        researcher.targetBuildingId % 2 == 0 -> R.drawable.img_sci_curie
        else -> R.drawable.img_sci_albert
    }

    fun frame(color: Color) = NexusVisuals.frame(color)
}

@Composable
internal fun NexusPanel(
    accent: Color,
    modifier: Modifier = Modifier,
    unlocked: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val visibleAccent = if (unlocked) accent else NexusVisuals.locked
    Card(
        modifier = modifier.border(1.5.dp, NexusVisuals.frame(visibleAccent), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicCard)
    ) {
        Column(Modifier.background(NexusVisuals.surface(visibleAccent)).padding(16.dp), content = content)
    }
}

@Composable
internal fun NexusMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(accent.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).padding(10.dp)) {
        Text(label.uppercase(), color = accent, fontSize = 10.sp,
            fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        Text(value, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Black,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun NexusRewardRow(credits: String? = null, gems: String? = null,
    guild: String? = null, cores: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (credits != null) NexusRewardLine("CRÉDITOS", credits, NexusVisuals.credits, Icons.Filled.AccountBalanceWallet)
        if (gems != null) NexusRewardLine("GEMAS", gems, NexusVisuals.gems, Icons.Filled.Diamond)
        if (guild != null) NexusRewardLine("GUILD TOKENS", guild, NexusVisuals.guild, Icons.Filled.Shield)
        if (cores != null) NexusRewardLine("STAR CORES", cores, NexusVisuals.cores, Icons.Filled.AutoAwesome)
    }
}

@Composable
private fun NexusRewardLine(label: String, value: String, accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().background(accent.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f))
        Text(value, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun ResearcherPortrait(researcher: ResearcherCard, size: androidx.compose.ui.unit.Dp) {
    val tint = ResearcherStyle.color(researcher.rarity)
    SafeImage(
        resId = ResearcherStyle.artwork(researcher),
        contentDescription = "Retrato de ${researcher.name}",
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp))
            .border(2.dp, ResearcherStyle.frame(tint), RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
        fallbackIcon = Icons.Filled.Person,
        fallbackTint = tint
    )
}

private val rewardPattern = Regex("^(.*) \\(x([0-9]+)\\)$")

@Composable
internal fun ResearcherRewardCards(cards: List<String>, researchers: List<ResearcherCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (cards.isEmpty()) {
            Text("Nenhuma cópia de pesquisador nesta abertura. A ciência recebida foi adicionada ao saldo.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        cards.forEach { entry ->
            val match = rewardPattern.matchEntire(entry)
            val name = match?.groupValues?.get(1) ?: entry
            val quantity = match?.groupValues?.get(2) ?: "1"
            val researcher = researchers.firstOrNull { it.name == name }
            val tint = researcher?.let { ResearcherStyle.color(it.rarity) } ?: NeonCyan
            Card(
                modifier = Modifier.fillMaxWidth().border(1.5.dp, ResearcherStyle.frame(tint), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, tint.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.16f), Color.Transparent)))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (researcher != null) ResearcherPortrait(researcher, 72.dp)
                    else Icon(Icons.Filled.AutoAwesome, null, tint = tint, modifier = Modifier.size(72.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(researcher?.let { ResearcherStyle.label(it.rarity) } ?: "PESQUISADOR",
                            color = tint, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(name, color = TextWhite, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Cópias para evolução", color = TextMuted, fontSize = 11.sp)
                    }
                    Text("×$quantity", color = tint, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun ScienceReward(amount: Long) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, SolarGold.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SolarGold.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Science, null, tint = SolarGold)
            Column {
                Text("CIÊNCIA", color = SolarGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("+$amount", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
