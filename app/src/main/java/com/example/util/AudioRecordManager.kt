package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecordManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    fun startRecording(customTitle: String? = null): File? {
        if (_isRecording.value) return currentOutputFile

        try {
            val audioDir = File(context.cacheDir, "audio_notes").apply {
                if (!exists()) mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val safeTitle = (customTitle ?: "note").replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(20)
            val file = File(audioDir, "VOICE_${safeTitle}_$timeStamp.m4a")
            currentOutputFile = file

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            _isRecording.value = true
            _elapsedSeconds.value = 0

            startTimerAndAmplitudeTracker()
            return file
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Failed to start recording", e)
            stopRecording(discard = true)
            return null
        }
    }

    private fun startTimerAndAmplitudeTracker() {
        recordingJob?.cancel()
        recordingJob = coroutineScope.launch {
            var seconds = 0
            while (_isRecording.value && isActive) {
                delay(100)
                try {
                    val maxAmp = recorder?.maxAmplitude ?: 0
                    _amplitude.value = maxAmp
                } catch (_: Exception) {}

                if (System.currentTimeMillis() % 1000 < 100) {
                    seconds++
                    _elapsedSeconds.value = seconds
                }
            }
        }
    }

    fun stopRecording(discard: Boolean = false): File? {
        recordingJob?.cancel()
        recordingJob = null
        val file = currentOutputFile

        try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.e("AudioRecordManager", "Error stopping recorder", e)
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error releasing recorder", e)
        } finally {
            recorder = null
            _isRecording.value = false
            _amplitude.value = 0
        }

        if (discard && file != null && file.exists()) {
            file.delete()
            currentOutputFile = null
            return null
        }

        return file
    }

    fun release() {
        stopRecording(discard = true)
        coroutineScope.cancel()
    }
}
