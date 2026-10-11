package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.MainActivity

@Composable
fun RewardedAdsCard() {
    val activity = LocalActivity.current as? MainActivity ?: return
    val controller = activity.rewardedAds
    val state by controller.state.collectAsStateWithLifecycle()
    DisposableEffect(controller) { onDispose { controller.cancelPending() } }
    if (state.rewardConfirmation) {
        AlertDialog(
            onDismissRequest = controller::dismissRewardConfirmation,
            title = { Text("Recompensa recebida!") },
            text = { Text("5 gemas foram adicionadas ao seu saldo.") },
            confirmButton = {
                TextButton(onClick = controller::dismissRewardConfirmation) { Text("Continuar") }
            }
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ganhe 5 gemas", style = MaterialTheme.typography.titleMedium)
            Text(state.message)
            if (BuildConfig.DEBUG) Text("Anúncio de teste • sem receita real", style = MaterialTheme.typography.bodySmall)
            if (state.ageBand == null) {
                Text("Qual sua faixa etária? Usamos essa informação para configurar os anúncios.")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { controller.setAgeBand("under13") }) { Text("Até 12") }
                    TextButton(onClick = { controller.setAgeBand("teen") }) { Text("13–17") }
                    TextButton(onClick = { controller.setAgeBand("adult") }) { Text("18 ou mais") }
                }
            }
            if (state.ageBand == "under13") Text("Anúncios indisponíveis para essa faixa etária.")
            Button(onClick = controller::watch, enabled = !state.busy && state.ageBand in listOf("teen", "adult"), modifier = Modifier.fillMaxWidth()) {
                Text(if (state.busy) "Aguarde…" else "Assistir anúncio • +5 gemas")
            }
            if (state.ageBand != null) TextButton(onClick = { controller.setAgeBand(null) }, enabled = !state.busy) {
                Text("Alterar faixa etária")
            }
            if (state.privacyRequired) {
                TextButton(onClick = controller::showPrivacyOptions, enabled = !state.busy) {
                    Text("Privacidade dos anúncios")
                }
            }
        }
    }
}
