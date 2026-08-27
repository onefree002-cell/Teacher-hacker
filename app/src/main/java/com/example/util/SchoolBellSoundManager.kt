package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class AlertSoundOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String
)

object SchoolBellSoundManager {

    const val SOUND_SCHOOL_BELL = "school_bell"
    const val SOUND_SCHOOL_CHIME = "school_chime"
    const val SOUND_CLASS_ALARM = "class_alarm"
    const val SOUND_START_FANFARE = "start_fanfare"
    const val SOUND_SYSTEM_DEFAULT = "system_default"
    const val SOUND_VIBRATE_ONLY = "vibrate_only"

    val soundOptions = listOf(
        AlertSoundOption(
            id = SOUND_SCHOOL_BELL,
            title = "جرس المدرسة الكلاسيكي 🔔",
            subtitle = "صوت جرس المدرسة الكهربائي التقليدي (موصى به)",
            icon = "🔔"
        ),
        AlertSoundOption(
            id = SOUND_SCHOOL_CHIME,
            title = "جرس الحصص الموسيقي 🛎️",
            subtitle = "نغمات موسيقية راقية (Westminster) لبدء الحصة",
            icon = "🛎️"
        ),
        AlertSoundOption(
            id = SOUND_CLASS_ALARM,
            title = "منبه الحصص الإلكتروني ⏰",
            subtitle = "تنبيه إلكتروني مزدوج وسريع ومنبه",
            icon = "⏰"
        ),
        AlertSoundOption(
            id = SOUND_START_FANFARE,
            title = "بوق بداية الحصة 🎺",
            subtitle = "نغمة ترحيبية حماسية لبدء الدرس",
            icon = "🎺"
        ),
        AlertSoundOption(
            id = SOUND_SYSTEM_DEFAULT,
            title = "نغمة إشعارات الهاتف 📱",
            subtitle = "صوت نغمة الإشعارات الافتراضية للنظام",
            icon = "📱"
        ),
        AlertSoundOption(
            id = SOUND_VIBRATE_ONLY,
            title = "اهتزاز فقط بدون صوت 🔕",
            subtitle = "تنبيه صامت مع اهتزاز الجهاز",
            icon = "🔕"
        )
    )

    private var currentAudioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentlyPlayingSoundId = MutableStateFlow<String?>(null)
    val currentlyPlayingSoundId: StateFlow<String?> = _currentlyPlayingSoundId.asStateFlow()

    fun getSoundOption(id: String): AlertSoundOption {
        return soundOptions.firstOrNull { it.id == id } ?: soundOptions.first()
    }

    /**
     * Plays the selected alert sound and vibrates if requested.
     */
    fun playAlertSound(context: Context, soundId: String, vibrate: Boolean = true) {
        stopSound()

        if (vibrate) {
            triggerVibration(context)
        }

        if (soundId == SOUND_VIBRATE_ONLY) {
            return
        }

        if (soundId == SOUND_SYSTEM_DEFAULT) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
                _currentlyPlayingSoundId.value = soundId
                scope.launch {
                    delay(2000)
                    if (_currentlyPlayingSoundId.value == soundId) {
                        _currentlyPlayingSoundId.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("SchoolBellSoundManager", "Error playing default ringtone", e)
            }
            return
        }

        // Synthesize and play sound via AudioTrack
        playbackJob = scope.launch {
            _currentlyPlayingSoundId.value = soundId
            try {
                val pcmData = generatePcmForSound(soundId)
                playPcmData(pcmData)
            } catch (e: Exception) {
                Log.e("SchoolBellSoundManager", "Error synthesizing sound: $soundId", e)
            } finally {
                withContext(Dispatchers.Main) {
                    if (_currentlyPlayingSoundId.value == soundId) {
                        _currentlyPlayingSoundId.value = null
                    }
                }
            }
        }
    }

    fun stopSound() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            currentAudioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("SchoolBellSoundManager", "Error stopping audio track", e)
        } finally {
            currentAudioTrack = null
            _currentlyPlayingSoundId.value = null
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 300, 150, 300, 150, 500)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 300, 150, 500), -1)
            }
        } catch (e: Exception) {
            Log.e("SchoolBellSoundManager", "Error triggering vibration", e)
        }
    }

    private fun playPcmData(pcmData: ShortArray) {
        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBufferSize, pcmData.size * 2)

        val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_ALARM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STATIC
            )
        }

        currentAudioTrack = audioTrack
        audioTrack.write(pcmData, 0, pcmData.size)
        audioTrack.play()

        // Wait for playback to finish
        val durationMs = (pcmData.size * 1000L) / sampleRate
        Thread.sleep(durationMs + 100)

        try {
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            // ignore
        }
        currentAudioTrack = null
    }

    /**
     * Synthesizes rich 16-bit PCM audio waveforms for each sound type.
     */
    private fun generatePcmForSound(soundId: String): ShortArray {
        val sampleRate = 44100
        return when (soundId) {
            SOUND_SCHOOL_BELL -> generateSchoolBellPcm(sampleRate, durationSeconds = 3.5)
            SOUND_SCHOOL_CHIME -> generateSchoolChimePcm(sampleRate)
            SOUND_CLASS_ALARM -> generateClassAlarmPcm(sampleRate)
            SOUND_START_FANFARE -> generateFanfarePcm(sampleRate)
            else -> generateSchoolBellPcm(sampleRate, durationSeconds = 3.0)
        }
    }

    /**
     * Authentic Electric School Bell Synthesizer (جرس المدرسة الكلاسيكي الكهربائي)
     * Characteristics: Rapid hammer strikes (15 strikes/sec) hitting metallic bowl,
     * producing high metallic resonances (1240 Hz, 2480 Hz, 620 Hz, 3720 Hz) with natural decays and metallic shimmer.
     */
    private fun generateSchoolBellPcm(sampleRate: Int, durationSeconds: Double): ShortArray {
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)
        val strikeFreq = 15.0 // 15 strikes per second
        val strikePeriodSamples = sampleRate / strikeFreq

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val strikeIndex = (i % strikePeriodSamples)
            val strikeTime = strikeIndex.toDouble() / sampleRate

            // Strike envelope: rapid 1.5ms attack, 45ms exponential decay
            val attack = min(1.0, strikeTime / 0.0015)
            val decay = exp(-strikeTime * 42.0)
            val strikeEnvelope = attack * decay

            // Metallic harmonic frequencies of a classic bell gong
            val f1 = sin(2.0 * PI * 1240.0 * t) * 0.45
            val f2 = sin(2.0 * PI * 2480.0 * t) * 0.30
            val f3 = sin(2.0 * PI * 620.0 * t) * 0.25
            val f4 = sin(2.0 * PI * 3720.0 * t + 0.5) * 0.15
            val strikeMetallic = f1 + f2 + f3 + f4

            // Sustained resonant body hum of the bell
            val overallFade = max(0.0, 1.0 - (t / durationSeconds).pow(1.5))
            val bodyHum = (sin(2.0 * PI * 440.0 * t) * 0.15 + sin(2.0 * PI * 880.0 * t) * 0.12) * overallFade

            val sampleVal = (strikeMetallic * strikeEnvelope * 0.85 + bodyHum) * 32000.0
            buffer[i] = sampleVal.coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
        return buffer
    }

    /**
     * School Westminster Chimes Synthesizer (جرس الحصص الموسيقي 4 نغمات)
     * Notes: E4 (329.6 Hz), G#4 (415.3 Hz), F#4 (369.9 Hz), B3 (246.9 Hz)
     */
    private fun generateSchoolChimePcm(sampleRate: Int): ShortArray {
        val notes = listOf(
            329.63 to 0.55, // E4
            415.30 to 0.55, // G#4
            369.99 to 0.55, // F#4
            246.94 to 0.90  // B3
        )
        val totalSeconds = notes.sumOf { it.second } + 0.4
        val totalSamples = (sampleRate * totalSeconds).toInt()
        val buffer = ShortArray(totalSamples)

        var sampleOffset = 0
        for ((freq, duration) in notes) {
            val noteSamples = (sampleRate * duration).toInt()
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 4.2)
                val strike = min(1.0, t / 0.005)

                // Bell rich harmonics
                val h1 = sin(2.0 * PI * freq * t) * 0.60
                val h2 = sin(2.0 * PI * (freq * 2.0) * t) * 0.25
                val h3 = sin(2.0 * PI * (freq * 3.0) * t) * 0.12
                val h4 = sin(2.0 * PI * (freq * 4.2) * t) * 0.08

                val sampleVal = (h1 + h2 + h3 + h4) * strike * decay * 31000.0
                val bufIndex = sampleOffset + i
                if (bufIndex < totalSamples) {
                    val currentVal = buffer[bufIndex].toDouble()
                    buffer[bufIndex] = (currentVal + sampleVal).coerceIn(-32767.0, 32767.0).toInt().toShort()
                }
            }
            sampleOffset += (sampleRate * (duration * 0.85)).toInt()
        }
        return buffer
    }

    /**
     * Electronic Class Alarm (منبه إلكتروني)
     */
    private fun generateClassAlarmPcm(sampleRate: Int): ShortArray {
        val durationSeconds = 2.4
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val cycleTime = t % 0.6
            val isBeep1 = cycleTime in 0.0..0.12
            val isBeep2 = cycleTime in 0.18..0.30

            val sampleVal = if (isBeep1 || isBeep2) {
                val freq = if (isBeep1) 1200.0 else 1800.0
                (sin(2.0 * PI * freq * t) * 0.8 + sin(2.0 * PI * (freq * 2) * t) * 0.2) * 28000.0
            } else {
                0.0
            }
            buffer[i] = sampleVal.coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
        return buffer
    }

    /**
     * Class Start Fanfare (بوق بداية الحصة)
     */
    private fun generateFanfarePcm(sampleRate: Int): ShortArray {
        val notes = listOf(
            523.25 to 0.20, // C5
            659.25 to 0.20, // E5
            783.99 to 0.20, // G5
            1046.50 to 0.70 // C6
        )
        val totalSeconds = notes.sumOf { it.second } + 0.3
        val totalSamples = (sampleRate * totalSeconds).toInt()
        val buffer = ShortArray(totalSamples)

        var sampleOffset = 0
        for ((freq, duration) in notes) {
            val noteSamples = (sampleRate * duration).toInt()
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val attack = min(1.0, t / 0.02)
                val decay = if (duration > 0.4) max(0.0, 1.0 - (t / duration).pow(2)) else 1.0

                // Brass timbre
                val b1 = sin(2.0 * PI * freq * t) * 0.55
                val b2 = sin(2.0 * PI * freq * 2 * t) * 0.30
                val b3 = sin(2.0 * PI * freq * 3 * t) * 0.15

                val sampleVal = (b1 + b2 + b3) * attack * decay * 29000.0
                val bufIndex = sampleOffset + i
                if (bufIndex < totalSamples) {
                    buffer[bufIndex] = sampleVal.coerceIn(-32767.0, 32767.0).toInt().toShort()
                }
            }
            sampleOffset += (sampleRate * (duration * 0.9)).toInt()
        }
        return buffer
    }
}
