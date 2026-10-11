package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.*

@Composable
fun StellarLaboratory(state: GameState, viewModel: GameViewModel) {
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Laboratório Estelar", style = MaterialTheme.typography.titleLarge)
            Text("Total conquistado: ${state.nebulaCores} Star Cores")
            Text("Saldo disponível: ${state.availableCores} Star Cores")
            Text("Bônus passivo: +${compactNumber(state.nebulaCores * 10.0)}% de produção")
            Text("Melhorias permanentes: permanecem após ascender. Gastar cores preserva o bônus passivo e complementa as pesquisas de ciência.")
            StellarUpgrade.entries.forEach { upgrade ->
                val level = upgrade.level(state)
                val cost = upgrade.costs.getOrNull(level)
                fun effect(at: Int) = if (upgrade == StellarUpgrade.OFFLINE)
                    "+$at h de reserva (limite: ${(if (state.isSubscribed) 12 else 8) + at} h)"
                    else "−${at * 2}% no custo em créditos"
                HorizontalDivider()
                Text("${upgrade.title} • $level/${upgrade.costs.size}", style = MaterialTheme.typography.titleMedium)
                Text("Atual: ${effect(level)}")
                Text(if (cost == null) "Nível máximo" else "Próximo: ${effect(level + 1)}")
                if (cost != null) Button(
                    enabled = !busy && state.availableCores >= cost,
                    onClick = {
                        busy = true
                        viewModel.buyStellarUpgrade(upgrade, level) { success ->
                            busy = false
                            message = if (success) "Melhoria adquirida!" else "Compra não realizada. Confira o saldo e o nível atualizado."
                        }
                    }
                ) { Text("Melhorar • $cost cores") }
            }
            if (message.isNotEmpty()) Text(message)
        }
    }
}
