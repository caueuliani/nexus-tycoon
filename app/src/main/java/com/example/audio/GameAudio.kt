package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.R
import java.util.ArrayDeque
import java.util.Collections

enum class SoundEvent {
    NAVIGATE, CONFIRM, UNLOCK, MILESTONE, MISSION, PROMOTION,
    PACK_OPEN, REVEAL_COMMON, REVEAL_RARE, REVEAL_EPIC, REVEAL_SUPREME,
    ASCEND, ATTACK, SKILL, VICTORY, DEFEAT
}

private fun SoundEvent.isReveal(): Boolean = when (this) {
    SoundEvent.REVEAL_COMMON, SoundEvent.REVEAL_RARE,
    SoundEvent.REVEAL_EPIC, SoundEvent.REVEAL_SUPREME -> true
    else -> false
}

/** Pure routing gate; gameplay events must be emitted only after their operation succeeds. */
class SoundGate {
    private val last = mutableMapOf<SoundEvent, Long>()
    private var lastRevealAt = Long.MIN_VALUE / 2
    fun permits(event: SoundEvent, nowMs: Long, enabled: Boolean, foreground: Boolean): Boolean {
        if (!enabled || !foreground) return false
        val reveal = event.isReveal()
        if (reveal && nowMs - lastRevealAt < 250L) return false
        val interval = when (event) {
            SoundEvent.NAVIGATE -> 90L
            SoundEvent.ATTACK, SoundEvent.SKILL -> 180L
            SoundEvent.PACK_OPEN -> 350L
            SoundEvent.ASCEND, SoundEvent.VICTORY, SoundEvent.DEFEAT -> 700L
            else -> 120L
        }
        if (nowMs - (last[event] ?: Long.MIN_VALUE / 2) < interval) return false
        last[event] = nowMs
        if (reveal) lastRevealAt = nowMs
        return true
    }
}

class SoundPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("nexus_audio", Context.MODE_PRIVATE)
    fun enabled(): Boolean = prefs.getBoolean("effects_enabled", true)
    fun volume(): Int = prefs.getInt("effects_volume", 35).coerceIn(0, 100)
    fun setEnabled(value: Boolean) { prefs.edit().putBoolean("effects_enabled", value).apply() }
    fun setVolume(value: Int) { prefs.edit().putInt("effects_volume", value.coerceIn(0, 100)).apply() }
    fun voiceEnabled(): Boolean = prefs.getBoolean("voice_enabled", true)
    fun voiceVolume(): Int = prefs.getInt("voice_volume", 65).coerceIn(0, 100)
    fun setVoiceEnabled(value: Boolean) { prefs.edit().putBoolean("voice_enabled", value).apply() }
    fun setVoiceVolume(value: Int) { prefs.edit().putInt("voice_volume", value.coerceIn(0, 100)).apply() }
}

object SoundEventRouter {
    fun confirmed(success: Boolean, event: SoundEvent, emit: (SoundEvent) -> Unit) {
        if (success) emit(event)
    }
}

/** One pool for the Activity lifetime. Missing/unloaded samples never block gameplay. */
class GameAudio(context: Context) {
    private val appContext = context.applicationContext
    private val settings = SoundPreferences(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val gate = SoundGate()
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val loaded = Collections.synchronizedSet(mutableSetOf<Int>())
    private val streams = ArrayDeque<Int>()
    private var revealStream = 0
    private val samples: Map<SoundEvent, Int>
    private var foreground = false
    private var closed = false
    private var lastPreviewAt = 0L
    private var voicePlayer: MediaPlayer? = null
    var voiceEnabled by mutableStateOf(settings.voiceEnabled())
        private set
    var voiceVolume by mutableIntStateOf(settings.voiceVolume())
        private set

    var enabled by mutableStateOf(settings.enabled())
        private set
    var volume by mutableIntStateOf(settings.volume())
        private set

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (!closed && status == 0) loaded.add(sampleId)
            else if (status != 0) Log.w("GameAudio", "Sample load failed: $sampleId")
        }
        samples = mapOf(
            SoundEvent.NAVIGATE to R.raw.sfx_ui,
            SoundEvent.CONFIRM to R.raw.sfx_confirm,
            SoundEvent.UNLOCK to R.raw.sfx_unlock,
            SoundEvent.MILESTONE to R.raw.sfx_milestone,
            SoundEvent.MISSION to R.raw.sfx_confirm,
            SoundEvent.PROMOTION to R.raw.sfx_milestone,
            SoundEvent.PACK_OPEN to R.raw.sfx_pack,
            SoundEvent.REVEAL_COMMON to R.raw.sfx_reveal_common,
            SoundEvent.REVEAL_RARE to R.raw.sfx_reveal_rare,
            SoundEvent.REVEAL_EPIC to R.raw.sfx_reveal_epic,
            SoundEvent.REVEAL_SUPREME to R.raw.sfx_reveal_supreme,
            SoundEvent.ASCEND to R.raw.sfx_ascend,
            SoundEvent.ATTACK to R.raw.sfx_attack,
            SoundEvent.SKILL to R.raw.sfx_skill,
            SoundEvent.VICTORY to R.raw.sfx_victory,
            SoundEvent.DEFEAT to R.raw.sfx_defeat
        ).mapValues { (_, resource) -> pool.load(context, resource, 1) }
    }

    fun setForeground(active: Boolean) {
        foreground = active
        if (!active) { stopPlaying(); stopVoice() }
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        settings.setEnabled(value)
        if (!value) stopPlaying()
    }

    fun updateVolume(value: Int) {
        volume = value.coerceIn(0, 100)
        settings.setVolume(volume)
    }

    fun updateVoiceEnabled(value: Boolean) {
        voiceEnabled = value
        settings.setVoiceEnabled(value)
        if (!value) stopVoice()
    }

    fun updateVoiceVolume(value: Int) {
        voiceVolume = value.coerceIn(0, 100)
        settings.setVoiceVolume(voiceVolume)
        voicePlayer?.setVolume(voiceVolume / 100f, voiceVolume / 100f)
    }

    /** Future approved narration; a missing resource leaves the on-screen caption untouched. */
    fun playVoice(resource: Int?): Boolean {
        if (resource == null || closed || !foreground || !voiceEnabled || voiceVolume == 0 ||
            audioManager.mode != AudioManager.MODE_NORMAL) return false
        stopVoice()
        return try {
            val player = MediaPlayer.create(appContext, resource) ?: return false
            voicePlayer = player
            player.setVolume(voiceVolume / 100f, voiceVolume / 100f)
            player.setOnCompletionListener { stopVoice() }
            player.setOnErrorListener { _, _, _ -> stopVoice(); true }
            player.start()
            true
        } catch (e: Exception) {
            Log.w("GameAudio", "Voice playback failed", e)
            stopVoice()
            false
        }
    }

    fun stopVoice() {
        voicePlayer?.let { player ->
            voicePlayer = null
            player.release()
        }
    }

    fun preview() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPreviewAt < 450L) return
        lastPreviewAt = now
        play(SoundEvent.CONFIRM)
    }

    fun play(event: SoundEvent) {
        if (closed || volume == 0 || audioManager.mode != AudioManager.MODE_NORMAL) return
        if (!gate.permits(event, SystemClock.elapsedRealtime(), enabled, foreground)) return
        val sample = samples[event] ?: return
        if (sample !in loaded) return
        val gain = volume / 100f * 0.65f
        if (event.isReveal() && revealStream != 0) pool.stop(revealStream)
        val stream = pool.play(sample, gain, gain, 1, 0, 1f)
        if (event.isReveal()) revealStream = stream
        if (stream != 0) {
            streams.addLast(stream)
            if (streams.size > 6) streams.removeFirst()
        }
    }

    private fun stopPlaying() {
        streams.forEach(pool::stop)
        streams.clear()
        revealStream = 0
    }

    fun close() {
        if (closed) return
        closed = true
        foreground = false
        stopPlaying()
        stopVoice()
        pool.setOnLoadCompleteListener(null)
        pool.release()
        loaded.clear()
    }
}

val LocalGameAudio = staticCompositionLocalOf<GameAudio> {
    error("GameAudio provider is missing")
}
