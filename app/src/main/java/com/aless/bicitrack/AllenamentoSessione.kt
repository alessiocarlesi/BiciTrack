package com.aless.bicitrack

import android.os.CountDownTimer

data class FaseAllenamento(
    val nome: String,
    val fcMin: Int,
    val fcMax: Int,
    val durataMinuti: Int
)

class AllenamentoSessione(
    private val polarManager: PolarManager,
    private val onFaseChange: (FaseAllenamento) -> Unit,
    private val onHRUpdate: (Int) -> Unit,
    private val onSessionEnd: () -> Unit
) {
    private val fasi: MutableList<FaseAllenamento> = mutableListOf()
    private var faseCorrenteIndex = 0
    private var timer: CountDownTimer? = null

    fun setFasi(listaFasi: List<FaseAllenamento>) {
        fasi.clear()
        fasi.addAll(listaFasi)
    }

    fun start() {
        faseCorrenteIndex = 0
        if (fasi.isNotEmpty()) startFase(fasi[0])
    }

    private fun startFase(fase: FaseAllenamento) {
        onFaseChange(fase)
        timer?.cancel()
        timer = object : CountDownTimer(fase.durataMinuti * 60 * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                faseCorrenteIndex++
                if (faseCorrenteIndex < fasi.size) startFase(fasi[faseCorrenteIndex])
                else onSessionEnd()
            }
        }.start()
    }

    fun checkHR(hr: Int) {
        val fase = fasi.getOrNull(faseCorrenteIndex) ?: return
        when {
            hr < fase.fcMin -> speakAudio("Aumenta ritmo")
            hr > fase.fcMax -> speakAudio("Riduci ritmo")
        }
    }

    private fun speakAudio(text: String) {
        // TODO: TextToSpeech o cuffie BT
    }

    fun stop() {
        timer?.cancel()
        polarManager.disconnect("0FE04C3A")
    }
}
