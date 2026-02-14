package com.aless.bicitrack

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import com.aless.bicitrack.ui.theme.BiciTrackTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Permessi necessari per connettere il Polar",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        requestPermissionLauncher.launch(permissions)

        setContent {
            BiciTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AllenamentoScreen()
                }
            }
        }
    }
}

@Composable
fun AllenamentoScreen() {
    val context = LocalContext.current

    var heartRate by remember { mutableStateOf("--") }
    var isConnected by remember { mutableStateOf(false) }
    var faseCorrente by remember { mutableStateOf<FaseAllenamento?>(null) }

    val polarManager = remember {
        PolarManager(context) { hr ->
            heartRate = hr.toString()
            isConnected = true
        }
    }

    // Configurazione della sessione di allenamento
    val sessione = remember {
        AllenamentoSessione(
            polarManager = polarManager,
            onFaseChange = { fase -> faseCorrente = fase },
            onHRUpdate = { hr -> heartRate = hr.toString() },
            onSessionEnd = { faseCorrente = null }
        )
    }

    // Imposta la sessione tipo base
    LaunchedEffect(Unit) {
        polarManager.connect("0FE04C3A")
        sessione.setFasi(SessionSettings.sessioneBase)
        sessione.start()
    }

    AllenamentoDashboard(
        heartRate = if (isConnected) heartRate.toIntOrNull() ?: 0 else 0,
        faseCorrente = faseCorrente,
        onStop = { sessione.stop(); isConnected = false; heartRate = "--" }
    )
}
