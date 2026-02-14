package com.aless.bicitrack

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.*
import java.util.Locale

class AllenamentoService : Service(), TextToSpeech.OnInitListener {

    private val binder = LocalBinder()
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "BiciTrackChannel"

    // Stato osservabile dalla UI
    var heartRate by mutableStateOf(0)
    var faseCorrente by mutableStateOf<FaseAllenamento?>(null)
    private var ultimaIndicazione = ""

    private lateinit var polarManager: PolarManager
    private lateinit var sessione: AllenamentoSessione
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    inner class LocalBinder : Binder() {
        fun getService(): AllenamentoService = this@AllenamentoService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        tts = TextToSpeech(this, this)

        // Modifica qui: aggiungiamo checkAudioFeedback(hr)
        polarManager = PolarManager(this) { hr ->
            heartRate = hr // Questo aggiorna il numero che vedi a schermo
            sessione.checkHR(hr) // Questo serve alla logica interna della sessione

            // AGGIUNGI QUESTA RIGA: è il "grilletto" per la voce del ritmo
            checkAudioFeedback(hr)

            updateNotification("Battito: $hr BPM - ${faseCorrente?.nome ?: ""}")
        }

        sessione = AllenamentoSessione(
            polarManager = polarManager,
            onFaseChange = { fase ->
                faseCorrente = fase
                speak("Inizio fase ${fase.nome}")
            },
            onHRUpdate = { hr -> heartRate = hr },
            onSessionEnd = {
                speak("Allenamento terminato")
                stopSelf()
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        polarManager.connect("0FE04C3A")
        sessione.setFasi(SessionSettings.sessioneBase)
        sessione.start()

        startForeground(NOTIFICATION_ID, createNotification("Connessione al sensore..."))
        return START_STICKY
    }

    private fun checkAudioFeedback(hr: Int) {
        val fase = faseCorrente ?: return

        val nuovaIndicazione = when {
            hr < fase.fcMin -> "Aumenta ritmo"
            hr > fase.fcMax -> "Riduci ritmo"
            else -> "Mantieni ritmo"
        }

        // Se lo stato cambia (es. passi da "Aumenta" a "Mantieni")
        if (nuovaIndicazione != ultimaIndicazione) {
            ultimaIndicazione = nuovaIndicazione

            // Adesso dirà TUTTE le indicazioni, incluso "Mantieni ritmo"
            speak(nuovaIndicazione)

            android.util.Log.d("BiciTrack", "Audio inviato: $nuovaIndicazione")
        }
    }
    private fun speak(text: String) {
        if (ttsReady) {
            // Usiamo un Bundle per specificare che questo è un messaggio di tipo "segnalazione"
            val params = android.os.Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)

            // QUEUE_FLUSH cancella i messaggi vecchi e dice subito quello nuovo
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "BiciTrackMsg")
            Log.d("BiciTrackAudio", "Comando vocale inviato: $text")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ITALIAN
            ttsReady = true
        }
    }

    // --- FUNZIONI DI NOTIFICA (Quelle che mancavano) ---

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }

    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BiciTrack - Allenamento")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Canale Allenamento",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        sessione.stop()
        super.onDestroy()
    }
}