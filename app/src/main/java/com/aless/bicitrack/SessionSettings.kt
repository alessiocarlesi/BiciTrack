package com.aless.bicitrack

object SessionSettings {
    val sessioneBase: List<FaseAllenamento> = listOf(
        FaseAllenamento("Riscaldamento", 50, 60, 5),
        FaseAllenamento("Aerobica leggera", 60, 70, 10),
        FaseAllenamento("Aerobica moderata", 70, 75, 10),
        FaseAllenamento("Defaticamento", 50, 60, 5)
    )

    val sessioneIntermedia: List<FaseAllenamento> = listOf(
        FaseAllenamento("Riscaldamento", 50, 60, 5),
        FaseAllenamento("Aerobica leggera", 60, 70, 7),
        FaseAllenamento("Aerobica moderata", 70, 80, 8),
        FaseAllenamento("Interval training breve", 80, 85, 5),
        FaseAllenamento("Defaticamento", 50, 60, 5)
    )
}
