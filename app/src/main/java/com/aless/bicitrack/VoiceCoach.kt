package com.aless.bicitrack

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class VoiceCoach(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var lastSpeechTime: Long = 0
    private val speechInterval = 10000L // Parla ogni 10 secondi se fuori soglia

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.ITALIAN
        }
    }

    // Cambiato 'Fase' in 'FaseAllenamento'
    fun controllaEParla(hr: Int, fase: FaseAllenamento) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSpeechTime < speechInterval) return

        when {
            // Usiamo i nomi corretti: fcMax e fcMin
            hr > fase.fcMax -> {
                speak("Rallenta. Battito troppo alto per la fase ${fase.nome}")
                lastSpeechTime = currentTime
            }
            hr < fase.fcMin && hr > 40 -> {
                speak("Aumenta il ritmo. Battito troppo basso per la fase ${fase.nome}")
                lastSpeechTime = currentTime
            }
        }
    }

    // Cambiato 'Fase' in 'FaseAllenamento'
    fun annunciaCambioFase(fase: FaseAllenamento) {
        speak("Inizio fase ${fase.nome}. Mantieni tra ${fase.fcMin} e ${fase.fcMax} battiti.")
        lastSpeechTime = System.currentTimeMillis()
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}