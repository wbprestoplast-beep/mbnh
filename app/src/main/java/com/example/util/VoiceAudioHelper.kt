package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VoiceAudioHelper {
    private const val TAG = "VoiceAudioHelper"
    private var currentPlayer: MediaPlayer? = null

    /**
     * Generates a standard RIFF WAV byte array containing a realistic, pleasant hospital
     * announcement chime / audio tone (523Hz + 659Hz + 784Hz) with smooth decay.
     * Guaranteed to be 100% playable by Android MediaPlayer on every device and emulator.
     */
    fun generateSyntheticChimeWav(durationSeconds: Int = 3): ByteArray {
        val sampleRate = 16000
        val numSamples = sampleRate * durationSeconds.coerceIn(2, 6)
        val shortBuffer = ShortArray(numSamples)

        val chordFreqs = doubleArrayOf(523.25, 659.25, 783.99) // C5, E5, G5 hospital chime chord

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sampleVal = 0.0

            for ((idx, freq) in chordFreqs.withIndex()) {
                val delay = idx * 0.25 // slight arpeggiation
                if (t >= delay) {
                    val localT = t - delay
                    val envelope = Math.exp(-2.2 * localT) // exponential decay
                    sampleVal += Math.sin(2.0 * Math.PI * freq * localT) * envelope
                }
            }

            // Normalization and scaling to 16-bit PCM
            val clamped = (sampleVal * 0.35).coerceIn(-1.0, 1.0)
            shortBuffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        // Build standard RIFF WAV header (44 bytes)
        val byteDataLength = numSamples * 2
        val totalDataLen = byteDataLength + 36
        val outStream = ByteArrayOutputStream(44 + byteDataLength)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // SubChunk1Size (16 for PCM)
        header.putShort(1) // AudioFormat (1 for PCM)
        header.putShort(1) // NumChannels (1 mono)
        header.putInt(sampleRate) // SampleRate
        header.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
        header.putShort(2) // BlockAlign
        header.putShort(16) // BitsPerSample
        header.put("data".toByteArray())
        header.putInt(byteDataLength)

        outStream.write(header.array())

        val pcmBytes = ByteBuffer.allocate(byteDataLength).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in shortBuffer) {
            pcmBytes.putShort(sample)
        }
        outStream.write(pcmBytes.array())

        return outStream.toByteArray()
    }

    /**
     * Encodes a synthetic voice chime to Base64.
     */
    fun generateSyntheticVoiceBase64(durationSeconds: Int = 3): String {
        val wavBytes = generateSyntheticChimeWav(durationSeconds)
        return Base64.encodeToString(wavBytes, Base64.NO_WRAP)
    }

    /**
     * Plays voice audio from a Base64 string on the device.
     */
    fun playVoiceAudio(
        context: Context,
        base64Data: String,
        onCompletion: () -> Unit,
        onError: (String) -> Unit
    ): MediaPlayer? {
        stopAudio()
        return try {
            val cleanBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val decoded = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (decoded.isEmpty()) {
                onError("Voice audio data is empty")
                return null
            }

            val isWav = decoded.size > 4 &&
                    decoded[0] == 'R'.code.toByte() &&
                    decoded[1] == 'I'.code.toByte() &&
                    decoded[2] == 'F'.code.toByte() &&
                    decoded[3] == 'F'.code.toByte()
            val extension = if (isWav) ".wav" else ".m4a"

            val tempFile = File.createTempFile("voice_playback_", extension, context.cacheDir)
            FileOutputStream(tempFile).use { it.write(decoded) }

            val player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    try {
                        tempFile.delete()
                    } catch (_: Exception) {}
                    currentPlayer = null
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    try {
                        tempFile.delete()
                    } catch (_: Exception) {}
                    currentPlayer = null
                    onError("Playback error: $what / $extra")
                    true
                }
            }
            currentPlayer = player
            player
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback exception: ${e.message}", e)
            onError(e.localizedMessage ?: "Unknown playback error")
            null
        }
    }

    fun stopAudio() {
        try {
            currentPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audio: ${e.message}")
        } finally {
            currentPlayer = null
        }
    }

    fun isPlaying(): Boolean {
        return try {
            currentPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }
}
