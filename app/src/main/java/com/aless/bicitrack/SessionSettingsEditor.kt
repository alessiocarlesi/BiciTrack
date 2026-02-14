package com.aless.bicitrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun SessionSettingsEditor(
    fasiIniziali: List<FaseAllenamento>,
    onSave: (List<FaseAllenamento>) -> Unit
) {
    // Usiamo mutableStateListOf per far sì che Compose veda le modifiche ai singoli elementi
    val fasi = remember { fasiIniziali.toMutableStateList() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Modifica Sessione", style = MaterialTheme.typography.headlineMedium)

        // LazyColumn è meglio per gestire liste che potrebbero crescere
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
            itemsIndexed(fasi) { index, fase ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        OutlinedTextField(
                            value = fase.nome,
                            onValueChange = { newName ->
                                fasi[index] = fase.copy(nome = newName)
                            },
                            label = { Text("Nome fase") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = fase.fcMin.toString(),
                                onValueChange = { val intVal = it.toIntOrNull() ?: 0
                                    fasi[index] = fase.copy(fcMin = intVal) },
                                label = { Text("FC Min") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )
                            OutlinedTextField(
                                value = fase.fcMax.toString(),
                                onValueChange = { val intVal = it.toIntOrNull() ?: 0
                                    fasi[index] = fase.copy(fcMax = intVal) },
                                label = { Text("FC Max") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            )
                        }
                        OutlinedTextField(
                            value = fase.durataMinuti.toString(),
                            onValueChange = { val intVal = it.toIntOrNull() ?: 0
                                fasi[index] = fase.copy(durataMinuti = intVal) },
                            label = { Text("Durata (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onSave(fasi.toList()) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Salva e Inizia Allenamento")
        }
    }
}