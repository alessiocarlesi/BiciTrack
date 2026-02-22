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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        if (!permissions.values.all { it }) {
            Toast.makeText(this, "Permessi necessari", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "bicitrack-db"
        ).fallbackToDestructiveMigration().build()

        lifecycleScope.launch {
            try {
                val ultimaSessione = db.sessioneDao().getSessione()
                ultimaSessione?.let { entity ->
                    val listType = object : TypeToken<List<FaseAllenamento>>() {}.type
                    val recuperate: List<FaseAllenamento> = Gson().fromJson(entity.jsonFasi, listType)

                    // Carichiamo solo se i dati sono validi e coerenti (11 fasi)
                    if (recuperate.size == 11) {
                        SessionSettings.sessioneBase = recuperate
                    }
                    // Recupero sicuro del flag
                    SessionSettings.soloBPM = entity.soloFC
                }
            } catch (e: Exception) {
                android.util.Log.e("BiciTrack", "Errore DB: ${e.message}")
            }
        }

        richiediPermessi()

        setContent {
            BiciTrackTheme {
                var isEditing by remember { mutableStateOf(true) }
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (isEditing) {
                        SessionSettingsEditor(
                            fasiIniziali = SessionSettings.sessioneBase,
                            onSave = { nuoveFasi ->
                                SessionSettings.sessioneBase = nuoveFasi
                                isEditing = false
                            }
                        )
                    } else {
                        AllenamentoScreen(onFinished = { finishAndRemoveTask() })
                    }
                }
            }
        }
    }

    private fun richiediPermessi() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS,
                    Manifest.permission.POST_NOTIFICATIONS)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS)
            }
            else -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        requestPermissionLauncher.launch(permissions)
    }
}

@Composable
fun AllenamentoScreen(onFinished: () -> Unit) {
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
        Box(modifier = Modifier.fillMaxSize()) {
            AllenamentoDashboard(
                heartRate = service!!.heartRate,
                faseCorrente = service!!.faseCorrente,
                onStop = {
                    service?.salvaEChiudiSessione()
                    onFinished()
                }
            )

            // UI del Flag Solo FC
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Solo BPM", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = service!!.soloBPM,
                    onCheckedChange = { service!!.soloBPM = it }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}