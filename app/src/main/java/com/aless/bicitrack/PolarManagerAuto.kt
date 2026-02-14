package com.aless.bicitrack

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

class PolarManagerAuto(
    context: Context,
    private val onHrUpdate: (Int) -> Unit
) {

    private val TAG = "PolarManagerAuto"

    // Inizializza API Polar BLE con feature HR
    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context,
        setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)
    )

    private var hrDisposable: Disposable? = null
    private var connectedDeviceId: String? = null

    init {
        api.setApiCallback(object : PolarBleApiCallback() {

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                connectedDeviceId = polarDeviceInfo.deviceId
                Log.d(TAG, "DEVICE CONNESSO: ${polarDeviceInfo.deviceId}")
                startHrStreaming(polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DEVICE DISCONNESSO: ${polarDeviceInfo.deviceId}")
                connectedDeviceId = null
            }

            // Non serve più `deviceDiscovered` nella SDK 5.1
        })
    }

    // Avvia scansione e connessione automatica al primo dispositivo disponibile con HR
    fun startScan() {
        Log.d(TAG, "Avvio ricerca dispositivi Polar")
//        api.startDeviceDiscovery(setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR), 0)
        // La connessione avviene automaticamente quando un dispositivo HR risponde
        // grazie al callback deviceConnected
    }

    private fun startHrStreaming(deviceId: String) {
        hrDisposable?.dispose()
        hrDisposable = api.startHrStreaming(deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data: PolarHrData ->
                    val hrValue = data.samples.firstOrNull()?.hr ?: 0
                    Log.d(TAG, "HR streaming: $hrValue")
                    onHrUpdate(hrValue)
                },
                { error ->
                    Log.e(TAG, "Errore HR streaming", error)
                }
            )
    }

    fun disconnect() {
        hrDisposable?.dispose()
        connectedDeviceId?.let {
            api.disconnectFromDevice(it)
            connectedDeviceId = null
        }
    }
}
