package com.aless.bicitrack

class TrainingManager(private val fasi: List<FaseAllenamento>) {

    private var startTimeMillis: Long = 0

    fun start() {
        startTimeMillis = System.currentTimeMillis()
    }

    private fun getElapsedMinutes(): Int {
        if (startTimeMillis == 0L) return 0
        return ((System.currentTimeMillis() - startTimeMillis) / 60000).toInt()
    }

    fun calcolaFaseAttuale(): FaseAllenamento? {
        if (fasi.isEmpty()) return null
        val minutiTrascorsi = getElapsedMinutes()

        val riscaldamento = fasi[0]
        val leggera = fasi.getOrNull(1) ?: riscaldamento
        val moderata = fasi.getOrNull(2) ?: leggera
        val defaticamento = fasi.getOrNull(3) ?: FaseAllenamento("Fine", 0, 0, 0)

        // 1. Riscaldamento
        if (minutiTrascorsi < riscaldamento.durataMinuti) return riscaldamento

        // 2. Loop Infinito (se defaticamento durata == 0)
        if (defaticamento.durataMinuti == 0) {
            val tempoDopoWarmup = minutiTrascorsi - riscaldamento.durataMinuti
            val durataCiclo = leggera.durataMinuti + moderata.durataMinuti
            if (durataCiclo <= 0) return leggera

            val tempoNelCiclo = tempoDopoWarmup % durataCiclo
            return if (tempoNelCiclo < leggera.durataMinuti) leggera else moderata
        }

        // 3. Logica Lineare (se defaticamento > 0)
        val fineLeggera = riscaldamento.durataMinuti + leggera.durataMinuti
        val fineModerata = fineLeggera + moderata.durataMinuti

        return when {
            minutiTrascorsi < fineLeggera -> leggera
            minutiTrascorsi < fineModerata -> moderata
            else -> defaticamento
        }
    }
}