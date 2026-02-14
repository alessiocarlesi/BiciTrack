package com.aless.bicitrack

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aless.bicitrack.data.db.AppDatabase
import com.aless.bicitrack.ui.theme.BiciTrackTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Permessi necessari per sensore e notifiche", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Caricamento impostazioni salvate dal Database all'avvio
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "bicitrack-db"
        ).build()

        lifecycleScope.launch {
            val ultimaSessione = db.sessioneDao().getSessione()
            ultimaSessione?.let { entity ->
                try {
                    val listType = object : TypeToken<List<FaseAllenamento>>() {}.type
                    val fasiRecuperate: List<FaseAllenamento> = Gson().fromJson(entity.jsonFasi, listType)
                    SessionSettings.sessioneBase = fasiRecuperate
                } catch (e: Exception) {
                    android.util.Log.e("BiciTrack", "Errore nel caricamento sessione: ${e.message}")
                }
            }
        }

        // Gestione Permessi
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BODY_SENSORS
                )
            }
            else -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        requestPermissionLauncher.launch(permissions)

        setContent {
            BiciTrackTheme {
                // Stato per gestire la navigazione tra Editor e Dashboard
                var isEditing by remember { mutableStateOf(true) }

                if (isEditing) {
                    SessionSettingsEditor(
                        fasiIniziali = SessionSettings.sessioneBase,
                        onSave = { nuoveFasi ->
                            SessionSettings.sessioneBase = nuoveFasi
                            isEditing = false
                        }
                    )
                } else {
                    AllenamentoScreen(onBackToEditor = { isEditing = true })
                }
            }
        }
    }
}

// ... (resto dei pacchetti e classi invariato)

@Composable
fun AllenamentoScreen(onBackToEditor: () -> Unit) {
    val context = LocalContext.current
    var service by remember { mutableStateOf<AllenamentoService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as AllenamentoService.LocalBinder
                service = localBinder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isBound = false
                service = null
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, AllenamentoService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            if (isBound) {
                context.unbindService(connection)
                isBound = false
            }
        }
    }

    if (isBound && service != null) {
        AllenamentoDashboard(
            heartRate = service!!.heartRate,
            faseCorrente = service!!.faseCorrente,
            onStop = {
                // 1. Scolleghiamo il servizio prima di fermarlo
                if (isBound) {
                    context.unbindService(connection)
                    isBound = false
                    service = null
                }

                // 2. Fermiamo il servizio (esegue il salvataggio in onDestroy)
                val intent = Intent(context, AllenamentoService::class.java)
                context.stopService(intent)

                // 3. CHIUSURA TOTALE E RIMOZIONE DALLE APP RECENTI
                (context as? android.app.Activity)?.let { activity ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        activity.finishAndRemoveTask()
                    } else {
                        activity.finishAffinity()
                    }
                }
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}