package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.NexusMetric
import com.example.NexusPanel
import com.example.NexusVisuals
import com.example.data.*
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted

@Composable
fun StellarLaboratory(state: GameState, viewModel: GameViewModel) {
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    NexusPanel(accent = NexusVisuals.cores, modifier = Modifier.fillMaxWidth()) {
            Text("LABORATÓRIO ESTELAR", color = NexusVisuals.cores,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NexusMetric("Total conquistado", "${state.nebulaCores}", NexusVisuals.cores, Modifier.weight(1f))
                NexusMetric("Disponíveis", "${state.availableCores}", SolarGold, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("Bônus passivo: +${compactNumber(state.nebulaCores * 10.0)}% de produção",
                color = SolarGold, fontWeight = FontWeight.Bold)
            Text("Melhorias permanentes. Gastar cores preserva o bônus passivo.",
                color = TextMuted, style = MaterialTheme.typography.bodySmall)
            StellarUpgrade.entries.forEach { upgrade ->
                val level = upgrade.level(state)
                val cost = upgrade.costs.getOrNull(level)
                fun effect(at: Int) = if (upgrade == StellarUpgrade.OFFLINE)
                    "+$at h de reserva (limite: ${(if (state.isSubscribed) 12 else 8) + at} h)"
                    else "−${at * 2}% no custo em créditos"
                Spacer(Modifier.height(12.dp))
                NexusPanel(accent = if (cost == null) SolarGold else NexusVisuals.cores,
                    modifier = Modifier.fillMaxWidth()) {
                Text(upgrade.title.uppercase(), color = NexusVisuals.cores,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text("Nível $level/${upgrade.costs.size}", color = TextMuted)
                Text("Atual: ${effect(level)}")
                Text(if (cost == null) "Nível máximo" else "Próximo: ${effect(level + 1)}")
                if (cost != null) Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusVisuals.cores),
                    enabled = !busy && state.availableCores >= cost,
                    onClick = {
                        busy = true
                        viewModel.buyStellarUpgrade(upgrade, level) { success ->
                            busy = false
                            message = if (success) "Melhoria adquirida!" else "Compra não realizada. Confira o saldo e o nível atualizado."
                        }
                    }
                ) { Text("MELHORAR • $cost CORES") }
                }
            }
            if (message.isNotEmpty()) Text(message)
    }
}
