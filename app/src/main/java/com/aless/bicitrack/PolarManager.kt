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

class PolarManager(
    context: Context,
    private val onHrUpdate: (Int) -> Unit
) {
    private val TAG = "PolarManager"

    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context,
        setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)
    )

    private var hrDisposable: Disposable? = null

    init {
        Log.d(TAG, "PolarManager inizializzato")
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

    fun connect(deviceId: String) {
        Log.d(TAG, "Avvio connessione a $deviceId")
        api.connectToDevice(deviceId)
    }

    private fun startHrStreaming(deviceId: String) {
        hrDisposable?.dispose()
        hrDisposable = api.startHrStreaming(deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data: PolarHrData ->
                    val hrValue = (data.samples.firstOrNull()?.hr ?: 0).toInt()
                    Log.d(TAG, "HR streaming: $hrValue")
                    onHrUpdate(hrValue)
                },
                { error -> Log.e(TAG, "Errore HR streaming", error) }
            )
    }

    fun disconnect(deviceId: String) {
        hrDisposable?.dispose()
        api.disconnectFromDevice(deviceId)
        Log.d(TAG, "Disconnesso da $deviceId")
    }
}
