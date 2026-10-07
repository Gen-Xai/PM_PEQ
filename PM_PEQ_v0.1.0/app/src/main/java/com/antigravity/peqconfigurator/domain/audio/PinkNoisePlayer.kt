package com.antigravity.peqconfigurator.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import kotlin.math.sin

class PinkNoisePlayer {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(0.4f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var isInitialized = false

    private fun applyVolumes() {
        val base = _volume.value
        val bal = _balance.value
        val left = if (bal <= 0) base else base * (1.0f - bal / 10.0f)
        val right = if (bal >= 0) base else base * (1.0f - kotlin.math.abs(bal) / 10.0f)
        try {
            @Suppress("DEPRECATION")
            audioTrack?.setStereoVolume(left, right)
        } catch (_: Exception) {}
    }

    private fun initTrack() {
        if (isInitialized) return
        try {
            val sampleRate = 48000
            val durationSeconds = 3
            val totalFrames = sampleRate * durationSeconds
            val buffer = ShortArray(totalFrames * 2) // Stereo (L, R)

            // Paul Kellet's refined 5-pole filter method for high-accuracy -3dB/octave pink noise
            val random = Random(42) // Constant seed for consistent timbre
            var b0 = 0.0
            var b1 = 0.0
            var b2 = 0.0
            var b3 = 0.0
            var b4 = 0.0
            var b5 = 0.0
            var b6 = 0.0

            // Pre-warm the filter for 4800 samples to reach steady state
            for (i in 0 until 4800) {
                val white = random.nextDouble() * 2.0 - 1.0
                b0 = 0.99886 * b0 + white * 0.0555179
                b1 = 0.99332 * b1 + white * 0.0750759
                b2 = 0.96900 * b2 + white * 0.1538520
                b3 = 0.86650 * b3 + white * 0.3104856
                b4 = 0.55000 * b4 + white * 0.5329522
                b5 = -0.7616 * b5 - white * 0.0168980
                b6 = white * 0.115926
            }

            for (frame in 0 until totalFrames) {
                val white = random.nextDouble() * 2.0 - 1.0
                b0 = 0.99886 * b0 + white * 0.0555179
                b1 = 0.99332 * b1 + white * 0.0750759
                b2 = 0.96900 * b2 + white * 0.1538520
                b3 = 0.86650 * b3 + white * 0.3104856
                b4 = 0.55000 * b4 + white * 0.5329522
                b5 = -0.7616 * b5 - white * 0.0168980
                val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
                b6 = white * 0.115926

                // Scale to comfortable reference level (~ -14 dBFS)
                var sample = (pink * 0.11 * 32767.0).coerceIn(-32767.0, 32767.0)

                // Smooth window crossfade at boundaries (50ms) to ensure seamless looping without clicks
                val fadeFrames = (sampleRate * 0.05).toInt()
                if (frame < fadeFrames) {
                    val progress = frame.toDouble() / fadeFrames
                    val gain = sin(progress * Math.PI / 2.0)
                    sample *= gain
                } else if (frame > totalFrames - fadeFrames) {
                    val progress = (totalFrames - frame).toDouble() / fadeFrames
                    val gain = sin(progress * Math.PI / 2.0)
                    sample *= gain
                }

                val shortSample = sample.toInt().toShort()
                buffer[frame * 2] = shortSample     // Left channel
                buffer[frame * 2 + 1] = shortSample // Right channel
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val track = AudioTrack(
                attributes,
                format,
                buffer.size * 2,
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            track.write(buffer, 0, buffer.size)
            track.setLoopPoints(0, totalFrames, -1)
            audioTrack = track
            isInitialized = true
            applyVolumes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        if (!isInitialized) initTrack()
        audioTrack?.let {
            if (it.state == AudioTrack.STATE_INITIALIZED) {
                it.play()
                _isPlaying.value = true
            }
        }
    }

    fun pause() {
        audioTrack?.let {
            if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                it.pause()
            }
        }
        _isPlaying.value = false
    }

    fun togglePlay() {
        if (_isPlaying.value) pause() else play()
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0.0f, 1.0f)
        applyVolumes()
    }

    fun setBalance(bal: Int) {
        _balance.value = bal.coerceIn(-10, 10)
        applyVolumes()
    }

    fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        isInitialized = false
        _isPlaying.value = false
    }
}
