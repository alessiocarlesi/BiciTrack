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
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.*
import androidx.room.Room
import com.aless.bicitrack.data.db.AppDatabase
import com.aless.bicitrack.data.db.SessioneEntity
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
    private var ultimaIndicazione = ""

    private lateinit var polarManager: PolarManager
    private lateinit var sessione: AllenamentoSessione
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

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "bicitrack-db"
        ).build()

        tts = TextToSpeech(this, this)

        polarManager = PolarManager(this) { hr ->
            heartRate = hr
            sessione.checkHR(hr)
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ITALIAN

            // Impostazione attributi audio richiesti per USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .build()

            tts?.setAudioAttributes(audioAttributes)

            // Listener per rilasciare l'audio focus quando finisce di parlare
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { abandonFocus() }
                override fun onError(utteranceId: String?) { abandonFocus() }
            })

            ttsReady = true
        }
    }

    private fun speak(text: String) {
        if (ttsReady && tts != null) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Richiesta Audio Focus per fare il "ducking" (abbassare YouTube)
            val focusResult = requestAudioFocus(audioManager)

            if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                val params = Bundle()
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                // Usiamo un ID univoco per il listener
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "BiciTrackMsg")
            }
        }
    }

    private fun requestAudioFocus(audioManager: AudioManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(null)
    }

    private fun checkAudioFeedback(hr: Int) {
        val fase = faseCorrente ?: return
        val nuovaIndicazione = when {
            hr < fase.fcMin -> "Aumenta ritmo"
            hr > fase.fcMax -> "Riduci ritmo"
            else -> "Mantieni ritmo"
        }

        if (nuovaIndicazione != ultimaIndicazione) {
            ultimaIndicazione = nuovaIndicazione
            speak(nuovaIndicazione)
        }
    }

    // --- Gestione Notifiche e Ciclo di Vita ---

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        polarManager.connect("0FE04C3A")
        sessione.setFasi(SessionSettings.sessioneBase)
        sessione.start()
        startForeground(NOTIFICATION_ID, createNotification("Connessione al sensore..."))
        return START_STICKY
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }

    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
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
            val channel = NotificationChannel(CHANNEL_ID, "Canale Allenamento", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        val sessioneJson = com.google.gson.Gson().toJson(SessionSettings.sessioneBase)
        val entity = SessioneEntity(
            nome = "Sessione ${java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(java.util.Date())}",
            jsonFasi = sessioneJson
        )
        CoroutineScope(Dispatchers.IO).launch { database.sessioneDao().saveSessione(entity) }
        tts?.stop()
        tts?.shutdown()
        sessione.stop()
        super.onDestroy()
    }
}