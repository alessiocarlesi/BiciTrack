package com.aless.bicitrack

import androidx.compose.foundation.layout.*
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
    var fasi by remember { mutableStateOf(fasiIniziali.toMutableList()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Modifica Sessione", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Elenco fasi modificabili
        fasi.forEachIndexed { index, fase ->
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedTextField(
                            value = fase.fcMin.toString(),
                            onValueChange = { newFcMin ->
                                val intVal = newFcMin.toIntOrNull() ?: fase.fcMin
                                fasi[index] = fase.copy(fcMin = intVal)
                            },
                            label = { Text("FC Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = fase.fcMax.toString(),
                            onValueChange = { newFcMax ->
                                val intVal = newFcMax.toIntOrNull() ?: fase.fcMax
                                fasi[index] = fase.copy(fcMax = intVal)
                            },
                            label = { Text("FC Max") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                    OutlinedTextField(
                        value = fase.durataMinuti.toString(),
                        onValueChange = { newDurata ->
                            val intVal = newDurata.toIntOrNull() ?: fase.durataMinuti
                            fasi[index] = fase.copy(durataMinuti = intVal)
                        },
                        label = { Text("Durata (minuti)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = { onSave(fasi) }, modifier = Modifier.align(Alignment.End)) {
            Text("Salva sessione")
        }
    }
}
