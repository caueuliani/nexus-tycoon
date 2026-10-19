package com.example

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.NovaPilot
import com.example.ui.theme.NeonCyan

/** Text always remains available; approved pt-BR voice resources can be supplied later. */
internal data class NovaLine(val event: Int, val text: String, @param:RawRes val voiceResource: Int? = null)

internal object NovaScript {
    fun forEvent(event: Int): NovaLine = when (event) {
        NovaPilot.FIRST_INDUSTRY -> NovaLine(event,
            "Uma indústria funcionando, Comandante. Excelente! Faltam só algumas centenas para dominarmos a galáxia.")
        NovaPilot.FIRST_RESEARCHER -> NovaLine(event,
            "Um novo pesquisador! Tomara que este saiba diferenciar um laboratório de uma cafeteria.")
        NovaPilot.FIRST_ASCENSION -> NovaLine(event,
            "Parabéns, Comandante. Você acabou de destruir seu próprio império para construir um melhor. Estranhamente, faz sentido.")
        else -> error("Evento narrativo desconhecido: $event")
    }
}

@Composable
internal fun NovaCompanionPanel(line: NovaLine, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val audio = com.example.audio.LocalGameAudio.current
    NexusPanel(accent = NeonCyan, modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.img_nova_hologram),
                contentDescription = "Retrato holográfico da N.O.V.A.",
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text("N.O.V.A.", color = NeonCyan, fontWeight = FontWeight.Black)
                Text(line.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(modifier = Modifier.align(Alignment.End)) {
            if (line.voiceResource != null) {
                TextButton(onClick = { audio.playVoice(line.voiceResource) },
                    enabled = audio.voiceEnabled && audio.voiceVolume > 0) { Text("Ouvir") }
            }
            TextButton(onClick = { audio.stopVoice(); onDismiss() }) { Text("Continuar") }
        }
    }
}
