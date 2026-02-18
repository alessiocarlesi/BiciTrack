package com.aless.bicitrack

object SessionSettings {
    var sessioneBase: List<FaseAllenamento> = listOf(
        FaseAllenamento("Riscaldamento", 80, 95, 5),
        // Ciclo 1
        FaseAllenamento("Aerobica leggera", 95, 110, 0),
        FaseAllenamento("Aerobica moderata", 110, 125, 0),
        FaseAllenamento("Aerobica intensa", 125, 140, 0),
        // Ciclo 2
        FaseAllenamento("Aerobica leggera", 95, 110, 0),
        FaseAllenamento("Aerobica moderata", 110, 125, 0),
        FaseAllenamento("Aerobica intensa", 125, 140, 0),
        // Ciclo 3
        FaseAllenamento("Aerobica leggera", 95, 110, 0),
        FaseAllenamento("Aerobica moderata", 110, 125, 0),
        FaseAllenamento("Aerobica intensa", 125, 140, 0),
        // Finale
        FaseAllenamento("Defaticamento", 80, 95, 0)
    )
}