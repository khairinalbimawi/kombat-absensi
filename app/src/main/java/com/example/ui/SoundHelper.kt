package com.example.ui

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SoundHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID")) // Indonesian language
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default
                tts?.setLanguage(Locale.US)
            }
            isTtsReady = true
        } else {
            Log.e("SoundHelper", "TTS Initialization failed!")
        }
    }

    fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "KombatSpeech")
        } else {
            Log.w("SoundHelper", "TTS is not ready yet. Cannot speak: $text")
        }
    }

    /**
     * Synthesizes custom square/sine wave frequency sounds mimicking oscillator
     */
    fun playOscillatorSound(isSuccess: Boolean) {
        Thread {
            try {
                if (isSuccess) {
                    // Success beep: Single pure high tone (1200 Hz, 180ms)
                    playTone(1200, 180)
                } else {
                    // Error tone: Flat, descending buzzer-like double beep (350 Hz, 120ms each)
                    playTone(350, 150)
                    Thread.sleep(80)
                    playTone(300, 180)
                }
            } catch (e: Exception) {
                // Secondary fallback to standard system tones
                Log.e("SoundHelper", "Oscillator error, playing fallback", e)
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                if (isSuccess) {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                } else {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                }
            }
        }.start()
    }

    private fun playTone(freqOfTone: Int, durationMs: Int) {
        val sampleRate = 8000
        val numSamples = durationMs * sampleRate / 1000
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)

        // Fill buffer with sine wave
        for (i in 0 until numSamples) {
            sample[i] = Math.sin(2.0 * Math.PI * i / (sampleRate / freqOfTone))
        }

        // Convert to 16-bit PCM (pulse code modulation)
        var idx = 0
        for (doubleVal in sample) {
            val val16 = (doubleVal * 32767).toInt()
            generatedSnd[idx++] = (val16 and 0x00ff).toByte()
            generatedSnd[idx++] = ((val16 and 0xff00) ushr 8).toByte()
        }

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            generatedSnd.size,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
        
        // Let it play out before releasing
        Thread.sleep((durationMs + 50).toLong())
        audioTrack.release()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("SoundHelper", "Shutdown failed", e)
        }
    }
}
