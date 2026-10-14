package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/** Presentation only. The database transaction has completed before [rewardCores] becomes non-null. */
@Composable
fun AscensionTransition(rewardCores: Long?, onContinue: () -> Unit) {
    val coreScale = remember { Animatable(0.3f) }
    val flash = remember { Animatable(0f) }
    var phase by remember { mutableStateOf(0) }

    LaunchedEffect(rewardCores) {
        if (rewardCores == null) return@LaunchedEffect
        phase = 1
        coreScale.snapTo(0.3f)
        coreScale.animateTo(1.15f, tween(1000, easing = FastOutSlowInEasing))
        phase = 2
        coreScale.animateTo(0.32f, tween(1050, easing = FastOutSlowInEasing))
        phase = 3
        coreScale.animateTo(0.05f, tween(700))
        flash.animateTo(0.85f, tween(250))
        phase = 4
        flash.animateTo(0f, tween(650))
        coreScale.animateTo(1f, tween(550))
        delay(300)
        phase = 5
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF030612), Color(0xFF170D31), Color(0xFF030612)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.size(240.dp)
                            .graphicsLayer { scaleX = coreScale.value; scaleY = coreScale.value }
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFFE49D), Color(0xFFDA8DFF), Color(0x554D21A9), Color.Transparent)
                                ), CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier.size(if (phase == 2 || phase == 3) 70.dp else 38.dp)
                            .background(if (phase == 2 || phase == 3) Color(0xFF030612) else Color.White, CircleShape)
                    )
                }
                Spacer(Modifier.height(36.dp))
                if (rewardCores == null) {
                    CircularProgressIndicator(color = Color(0xFFFFCB69))
                    Spacer(Modifier.height(16.dp))
                    Text("Iniciando nova run…", color = Color.White, fontSize = 18.sp)
                } else if (phase < 5) {
                    Text(
                        when (phase) {
                            1 -> "A estrela desperta"
                            2 -> "A energia converge"
                            3 -> "A antiga run se encerra"
                            else -> "Uma nova jornada começa"
                        },
                        color = Color(0xFFFFD98C), fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("ASCENSÃO CONCLUÍDA", color = Color(0xFFFFD98C), fontSize = 23.sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("Nova run iniciada", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("+$rewardCores Núcleos de Nebulosa", color = Color(0xFFFFD98C),
                        fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Suas recompensas permanentes foram preservadas.", color = Color(0xFFCFC5E7),
                        fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC15C))) {
                        Text("Continuar nas Indústrias", color = Color(0xFF171020), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(Modifier.fillMaxSize().alpha(flash.value).background(Color.White))
        }
    }
}
