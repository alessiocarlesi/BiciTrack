package com.aless.bicitrack

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Assicurati che FaseAllenamento sia visibile, import dal file corretto
import com.aless.bicitrack.FaseAllenamento

@Composable
fun AllenamentoDashboard(
    heartRate: Int,
    faseCorrente: FaseAllenamento?,
    onStop: () -> Unit
) {
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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Fase: ${faseCorrente?.nome ?: "--"}",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "$heartRate BPM",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onStop) {
            Text("Stop sessione")
        }
    }
}
