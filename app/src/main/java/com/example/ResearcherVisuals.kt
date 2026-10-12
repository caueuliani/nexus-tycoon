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

// Shared visual language for researcher cards and capsule reveals.
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

    fun frame(color: Color) = Brush.linearGradient(listOf(color, color.copy(alpha = 0.25f), color))
}

@Composable
internal fun ResearcherPortrait(researcher: ResearcherCard, size: androidx.compose.ui.unit.Dp) {
    val tint = ResearcherStyle.color(researcher.rarity)
    SafeImage(
        resId = ResearcherStyle.artwork(researcher.id),
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
