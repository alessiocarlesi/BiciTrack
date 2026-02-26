package com.aless.bicitrack

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.aless.bicitrack.data.db.AppDatabase
import com.aless.bicitrack.data.db.SessioneEntity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AllenamentoService : Service(), TextToSpeech.OnInitListener {

    private val binder = LocalBinder()
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "BiciTrackChannel"

    var heartRate by mutableStateOf(0)
    var faseCorrente by mutableStateOf<FaseAllenamento?>(null)
    var soloBPM by mutableStateOf(false)

    private var ultimaIndicazione = ""
    private var ultimoNomeFase = ""
    private var lastFeedbackTime = 0L
    private val FEEDBACK_INTERVAL = 60000L
    private var ignoreFeedbackUntil = 0L

    private lateinit var polarManager: PolarManager
    private lateinit var trainingManager: TrainingManager
    private lateinit var database: AppDatabase
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    inner class LocalBinder : Binder() {
        fun getService(): AllenamentoService = this@AllenamentoService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "bicitrack-db")
            .fallbackToDestructiveMigration().build()
        tts = TextToSpeech(this, this)

        soloBPM = SessionSettings.soloBPM

        polarManager = PolarManager(this) { hr ->
            heartRate = hr
            val currentTime = System.currentTimeMillis()
            val nuovaFase = trainingManager.calcolaFaseAttuale()

            if (nuovaFase != null && nuovaFase.nome != ultimoNomeFase) {
                ultimoNomeFase = nuovaFase.nome
                faseCorrente = nuovaFase
                ultimaIndicazione = ""
                speak("Inizio fase ${nuovaFase.nome}. Target tra ${nuovaFase.fcMin} e ${nuovaFase.fcMax}", true)
                ignoreFeedbackUntil = currentTime + 10000L
            }

            if (currentTime > ignoreFeedbackUntil) {
                checkAudioFeedback(hr)
            }
            updateNotification("Battito: $hr BPM - ${faseCorrente?.nome ?: ""}")
        }
        trainingManager = TrainingManager(SessionSettings.sessioneBase)
    }

    private fun checkAudioFeedback(hr: Int) {
        val fase = faseCorrente ?: return
        val currentTime = System.currentTimeMillis()

        val indicazioneRitmo = when {
            hr < fase.fcMin && hr > 40 -> "Aumenta ritmo"
            hr > fase.fcMax -> "Riduci ritmo"
            else -> ""
        }

        val messaggioCompleto = if (soloBPM || indicazioneRitmo.isEmpty()) "$hr" else "$hr, $indicazioneRitmo"

        val deveParlare = if (soloBPM) {
            (currentTime - lastFeedbackTime > FEEDBACK_INTERVAL)
        } else {
            indicazioneRitmo != ultimaIndicazione || (currentTime - lastFeedbackTime > FEEDBACK_INTERVAL)
        }

        if (deveParlare) {
            ultimaIndicazione = indicazioneRitmo
            lastFeedbackTime = currentTime
            speak(messaggioCompleto, false)
        }
    }

    /**
     * MODIFICATO: Chiusura aggressiva del servizio e rimozione notifica
     */
    fun salvaEChiudiSessione() {
        // 1. Fermiamo subito il monitoraggio e l'audio
        polarManager.disconnect()
        tts?.stop()
        SessionSettings.soloBPM = soloBPM

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessioneJson = Gson().toJson(SessionSettings.sessioneBase)
                val dataOra = java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(java.util.Date())
                val entity = SessioneEntity(nome = "Sessione $dataOra", jsonFasi = sessioneJson, soloFC = soloBPM)
                database.sessioneDao().saveSessione(entity)

                launch(Dispatchers.Main) {
                    // 2. Rimuoviamo la notifica Foreground prima di chiudere
                    stopForeground(true)
                    // 3. Fermiamo definitivamente il servizio
                    stopSelf()
                }
            } catch (e: Exception) {
                android.util.Log.e("BiciTrack", "Errore salvataggio: ${e.message}")
                launch(Dispatchers.Main) {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
    }

    private fun speak(text: String, isFase: Boolean) {
        if (ttsReady && tts != null) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (requestAudioFocus(audioManager) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                val mode = if (isFase) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
                tts?.speak(text, mode, null, "BiciTrackMsg")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        polarManager.connect("0FE04C3A")
        trainingManager.start()
        startForeground(NOTIFICATION_ID, createNotification("Allenamento in corso..."))
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ITALIAN
            ttsReady = true
        }
    }

    private fun requestAudioFocus(am: AudioManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).build())
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BiciTrack").setContentText(content).setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent).setOngoing(true).build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Allenamento", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * MODIFICATO: Pulizia finale garantita
     */
    override fun onDestroy() {
        polarManager.disconnect()
        tts?.stop()
        tts?.shutdown()
        // Assicuriamoci che la notifica venga rimossa se il sistema uccide il servizio
        stopForeground(true)
        super.onDestroy()
    }
}