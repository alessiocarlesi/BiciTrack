package com.aless.bicitrack

object SessionSettings {
    // Deve essere inizializzata qui!
    var soloBPM: Boolean = false

    var sessioneBase: List<FaseAllenamento> = listOf(
        FaseAllenamento("Riscaldamento", 80, 95, 5),
        FaseAllenamento("Aerobica leggera", 95, 110, 0),
        FaseAllenamento("Aerobica moderata", 110, 125, 0),
        FaseAllenamento("Aerobica intensa", 125, 140, 0),
        FaseAllenamento("Aerobica leggera", 95, 110, 0),
        FaseAllenamento("Aerobica moderata", 110, 125, 0),
        FaseAllenamento("Aerobica intensa", 125, 140, 0),
        FaseAllenamento("Aerobica leggera", 95, 110, 0),
        FaseAllenamento("Aerobica moderata", 110, 125, 0),
        FaseAllenamento("Aerobica intensa", 125, 140, 0),
        FaseAllenamento("Defaticamento", 80, 95, 0)
    )
}