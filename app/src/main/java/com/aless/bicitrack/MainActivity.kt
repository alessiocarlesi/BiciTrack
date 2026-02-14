package com.aless.bicitrack

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import com.aless.bicitrack.ui.theme.BiciTrackTheme
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Permessi necessari per il corretto funzionamento",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurazione permessi dinamica in base alla versione di Android
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // Android 13+
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // Android 12
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            else -> { // Android 11 e precedenti
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        requestPermissionLauncher.launch(permissions)

        setContent {
            BiciTrackTheme {
                AllenamentoScreen()
            }
        }
    }
}

@Composable
fun AllenamentoScreen() {
    val context = LocalContext.current
    var service by remember { mutableStateOf<AllenamentoService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    // Gestione della connessione al Servizio (Bridge tra UI e Background)
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

    // Ciclo di vita del collegamento al Servizio
    DisposableEffect(Unit) {
        val intent = Intent(context, AllenamentoService::class.java)

        // Avvio compatibile con tutte le versioni di Android
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

    // Logica di visualizzazione
    if (isBound && service != null) {
        AllenamentoDashboard(
            heartRate = service!!.heartRate,
            faseCorrente = service!!.faseCorrente,
            onStop = {
                val intent = Intent(context, AllenamentoService::class.java)
                context.stopService(intent)
            }
        )
    } else {
        // Mostra caricamento mentre il servizio si inizializza o connette al Polar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}