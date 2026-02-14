package com.aless.bicitrack

object SessionSettings {

    // Sessione tipo 1: Allenamento base salute
    var sessioneBase: List<FaseAllenamento> = listOf(
        FaseAllenamento("Riscaldamento", 80, 100, 5),      // 5 min
        FaseAllenamento("Aerobica leggera", 100, 120, 10), // 10 min
        FaseAllenamento("Aerobica moderata", 120, 135, 10),// 10 min
        FaseAllenamento("Defaticamento", 80, 100, 5)       // 5 min
    )

    // Sessione tipo 2: Allenamento leggermente più intenso
    var sessioneIntermedia: List<FaseAllenamento> = listOf(
        FaseAllenamento("Riscaldamento", 80, 100, 5),
        FaseAllenamento("Aerobica leggera", 100, 120, 7),
        FaseAllenamento("Aerobica moderata", 120, 140, 8),
        FaseAllenamento("Interval training breve", 140, 155, 5),
        FaseAllenamento("Defaticamento", 80, 100, 5)
    )
}
