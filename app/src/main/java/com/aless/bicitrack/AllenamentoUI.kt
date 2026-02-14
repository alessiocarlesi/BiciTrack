package com.aless.bicitrack

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AllenamentoDashboard(
    heartRate: Int,
    faseCorrente: FaseAllenamento?,
    onStop: () -> Unit
) {
    val indicazione = when {
        faseCorrente == null -> "--"
        heartRate < faseCorrente.fcMin -> "Aumenta ritmo"
        heartRate > faseCorrente.fcMax -> "Riduci ritmo"
        else -> "Mantieni ritmo"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "BiciTrack",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Fase: ${faseCorrente?.nome ?: "--"}",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "$heartRate BPM",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = indicazione,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onStop) {
            Text("Stop sessione")
        }
    }
}
