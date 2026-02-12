package com.aless.bicitrack

import android.Manifest
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
import androidx.compose.ui.unit.dp
import com.aless.bicitrack.ui.theme.BiciTrackTheme

class MainActivity : ComponentActivity() {

    // Launcher per i permessi Bluetooth necessari su Android 12+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Permessi necessari per connettere il Polar", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chiediamo i permessi all'avvio
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        setContent {
            BiciTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiciTrackDashboard()
                }
            }
        }
    }
}

@Composable
fun BiciTrackDashboard() {
    var heartRate by remember { mutableStateOf("--") }
    var isConnected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "BiciTrack", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Frequenza Cardiaca", style = MaterialTheme.typography.labelLarge)
        Text(text = "$heartRate BPM", style = MaterialTheme.typography.displayLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { /* Qui attiveremo la scansione Polar */ }) {
            Text(if (isConnected) "Disconnetti" else "Connetti Verity Sense")
        }
    }
}