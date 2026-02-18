package com.aless.bicitrack

class TrainingManager(private val tutteLeFasi: List<FaseAllenamento>) {

    private var startTimeMillis: Long = 0
    private val fasiAttive = tutteLeFasi.filter { it.durataMinuti > 0 }

    fun start() {
        startTimeMillis = System.currentTimeMillis()
    }

    private fun getElapsedMinutes(): Int {
        if (startTimeMillis == 0L) return 0
        return ((System.currentTimeMillis() - startTimeMillis) / 60000).toInt()
    }

    fun calcolaFaseAttuale(): FaseAllenamento? {
        if (fasiAttive.isEmpty()) return null
        val minutiTrascorsi = getElapsedMinutes()

        // Se l'ultima fase della lista (Defaticamento) ha durata 0, attiviamo il loop
        val deveCiclar = tutteLeFasi.last().durataMinuti == 0

        if (!deveCiclar) {
            var tempoAccumulato = 0
            for (fase in fasiAttive) {
                tempoAccumulato += fase.durataMinuti
                if (minutiTrascorsi < tempoAccumulato) return fase
            }
            return fasiAttive.last()
        } else {
            val riscaldamento = fasiAttive.first()
            if (minutiTrascorsi < riscaldamento.durataMinuti) return riscaldamento

            val tempoDopoWarmup = minutiTrascorsi - riscaldamento.durataMinuti
            val fasiLoop = fasiAttive.drop(1)

            if (fasiLoop.isEmpty()) return riscaldamento

            val durataCiclo = fasiLoop.sumOf { it.durataMinuti }
            val tempoNelCiclo = tempoDopoWarmup % durataCiclo

            var tempoAccumulatoCiclo = 0
            for (fase in fasiLoop) {
                tempoAccumulatoCiclo += fase.durataMinuti
                if (tempoNelCiclo < tempoAccumulatoCiclo) return fase
            }
            return fasiLoop.last()
        }
    }
}