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

    private val api: PolarBleApi =
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)
        )

    private var hrDisposable: Disposable? = null

    init {
        api.setApiCallback(object : PolarBleApiCallback() {

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("Polar", "Connesso a: ${polarDeviceInfo.deviceId}")
                startHrStreaming(polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("Polar", "Disconnesso da: ${polarDeviceInfo.deviceId}")
            }
        })
    }

    fun connect(deviceId: String) {
        api.connectToDevice(deviceId)
    }

    private fun startHrStreaming(deviceId: String) {
        hrDisposable?.dispose()

        hrDisposable = api.startHrStreaming(deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data: PolarHrData ->
                    // Prende il primo campione di HR se disponibile
                    val hrValue = data.samples.firstOrNull()?.hr ?: 0
                    onHrUpdate(hrValue)
                },
                { error ->
                    Log.e("Polar", "Errore HR streaming", error)
                }
            )
    }

    fun disconnect(deviceId: String) {
        hrDisposable?.dispose()
        api.disconnectFromDevice(deviceId)
    }
}
