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

        // Richiesta permessi runtime
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
    var editingFasi by remember { mutableStateOf(false) }

    val polarManager = remember {
        PolarManager(context) { hr ->
            heartRate = hr.toString()
            isConnected = true
        }
    }

    val sessione = remember {
        AllenamentoSessione(
            polarManager = polarManager,
            onFaseChange = { fase -> faseCorrente = fase },
            onHRUpdate = { hr -> heartRate = hr.toString() },
            onSessionEnd = { faseCorrente = null }
        )
    }

    // Lista modificabile delle fasi, inizialmente base
    var fasiAttuali by remember { mutableStateOf(SessionSettings.sessioneBase.toMutableList()) }

    if (editingFasi) {
        // Mostra editor fasi
        SessionSettingsEditor(
            fasiIniziali = fasiAttuali,
            onSave = { nuoveFasi ->
                fasiAttuali = nuoveFasi.toMutableList()
                sessione.setFasi(fasiAttuali)
                editingFasi = false
            }
        )
    } else {
        // Mostra dashboard allenamento
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("BiciTrack", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text("Fase: ${faseCorrente?.nome ?: "--"}", style = MaterialTheme.typography.labelLarge)
            Text("$heartRate BPM", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    // Connetti / disconnetti Polar
                    if (isConnected) {
                        polarManager.disconnect("0FE04C3A")
                        isConnected = false
                        heartRate = "--"
                    } else {
                        polarManager.connect("0FE04C3A")
                    }
                }) {
                    Text(if (isConnected) "Disconnetti" else "Connetti Polar")
                }

                Button(onClick = {
                    // Avvia o ferma sessione
                    if (faseCorrente == null) {
                        sessione.setFasi(fasiAttuali)
                        sessione.start()
                    } else {
                        sessione.stop()
                        faseCorrente = null
                    }
                }) {
                    Text(if (faseCorrente == null) "Avvia sessione" else "Ferma sessione")
                }

                Button(onClick = { editingFasi = true }) {
                    Text("Modifica fasi")
                }
            }
        }
    }
}
