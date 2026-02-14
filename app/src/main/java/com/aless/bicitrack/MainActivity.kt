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

        // Chiedi i permessi BLE/Location
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        requestPermissionLauncher.launch(permissionsToRequest)

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
    val context = LocalContext.current

    // Stato per battiti e connessione
    var heartRate by remember { mutableStateOf("--") }
    var isConnected by remember { mutableStateOf(false) }

    // Istanza PolarManager
    val polarManager = remember {
        PolarManager(context = context) { hr ->
            heartRate = hr.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "BiciTrack", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Frequenza Cardiaca", style = MaterialTheme.typography.labelLarge)
        Text(text = "$heartRate BPM", style = MaterialTheme.typography.displayLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            if (!isConnected) {
                polarManager.connect("0FE04C3A") // ID corretto del Polar
                isConnected = true
            } else {
                polarManager.disconnect("0FE04C3A")
                isConnected = false
                heartRate = "--"
            }
        }) {
            Text(if (isConnected) "Disconnetti" else "Connetti Verity Sense")
        }
    }
}
