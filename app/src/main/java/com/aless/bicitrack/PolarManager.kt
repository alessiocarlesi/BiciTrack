package com.aless.bicitrack

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

class PolarManager(
    context: Context,
    private val onHrUpdate: (Int) -> Unit
) {

    private val TAG = "PolarManager"

    // Inizializzazione API Polar BLE solo con HR
    private val api: PolarBleApi =
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)
        )

    private var hrDisposable: Disposable? = null

    init {
        Log.d(TAG, "PolarManager inizializzato")

        // Callback minimo necessario
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DEVICE CONNESSO: ${polarDeviceInfo.deviceId}")
                startHrStreaming(polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DEVICE DISCONNESSO: ${polarDeviceInfo.deviceId}")
            }
        })
    }

    // Avvia la connessione al dispositivo Polar
    fun connect(deviceId: String) {
        Log.d(TAG, "Avvio connessione a $deviceId")
        api.connectToDevice(deviceId)
    }

    // Avvio dello streaming dei battiti
    private fun startHrStreaming(deviceId: String) {
        Log.d(TAG, "Avvio streaming HR per $deviceId")
        hrDisposable?.dispose() // Ferma eventuale streaming precedente

        hrDisposable = api.startHrStreaming(deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data ->
                    // Prende il primo campione HR disponibile
                    val hrValue = data.samples.firstOrNull()?.hr ?: 0
                    Log.d(TAG, "HR streaming: $hrValue")
                    onHrUpdate(hrValue)
                },
                { error ->
                    Log.e(TAG, "Errore HR streaming", error)
                }
            )
    }

    // Disconnessione dal dispositivo
    fun disconnect(deviceId: String) {
        Log.d(TAG, "Disconnessione da $deviceId")
        hrDisposable?.dispose()
        api.disconnectFromDevice(deviceId)
    }
}
