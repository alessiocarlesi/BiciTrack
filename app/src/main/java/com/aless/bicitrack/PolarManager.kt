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
    private var connectedDeviceId: String? = null

    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context,
        setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)
    )

    private var hrDisposable: Disposable? = null

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                connectedDeviceId = polarDeviceInfo.deviceId
                Log.d(TAG, "CONNESSO: $connectedDeviceId")
                startHrStreaming(polarDeviceInfo.deviceId)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                connectedDeviceId = null
                Log.d(TAG, "DISCONNESSO")
            }
        })
    }

    fun connect(deviceId: String) = api.connectToDevice(deviceId)

    private fun startHrStreaming(deviceId: String) {
        hrDisposable?.dispose()
        hrDisposable = api.startHrStreaming(deviceId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data: PolarHrData ->
                    val hrValue = data.samples.firstOrNull()?.hr ?: 0
                    onHrUpdate(hrValue)
                },
                { error -> Log.e(TAG, "Errore streaming", error) }
            )
    }

    fun disconnect() {
        hrDisposable?.dispose()
        connectedDeviceId?.let { api.disconnectFromDevice(it) }
    }

    fun isConnected(): Boolean = connectedDeviceId != null
}