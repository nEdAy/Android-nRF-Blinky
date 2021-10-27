package no.nordicsemi.android.scanner.viewmodel

import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import no.nordicsemi.android.scanner.data.ScanDevicesData
import no.nordicsemi.android.scanner.view.OnCancelButtonClick
import no.nordicsemi.android.scanner.view.OnDeviceSelected
import no.nordicsemi.android.scanner.view.ScanDevicesViewEvent
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import no.nordicsemi.android.theme.viewmodel.CloseableViewModel
import no.nordicsemi.android.utils.SelectedBluetoothDeviceHolder
import no.nordicsemi.android.utils.exhaustive

class ScanDevicesViewModel(
    private val deviceHolder: SelectedBluetoothDeviceHolder
) : CloseableViewModel() {

    val data = MutableStateFlow(ScanDevicesData())

    private val scanner = BluetoothLeScannerCompat.getScanner()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            data.tryEmit(data.value.copyWithNewDevice(result.device))
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            val devices = results.map { it.device }
            data.tryEmit(data.value.copyWithNewDevices(devices))
        }

        override fun onScanFailed(errorCode: Int) {
            //todo
        }
    }

    fun onEvent(event: ScanDevicesViewEvent) {
        when (event) {
            OnCancelButtonClick -> finish()
            is OnDeviceSelected -> onDeviceSelected(event)
        }.exhaustive
    }

    private fun onDeviceSelected(event: OnDeviceSelected) {
        deviceHolder.attachDevice(event.device)
        finish()
    }

    fun startScan(serviceId: String) {
        val scanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
        val settings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(5000)
            .setUseHardwareBatchingIfSupported(true)
            .build()

        val filters: MutableList<ScanFilter> = ArrayList()
        val uuid = ParcelUuid.fromString(serviceId)
        filters.add(ScanFilter.Builder().setServiceUuid(uuid).build())

        scanner.startScan(filters, settings, scanCallback)
    }

    fun stopScanner() {
        scanner.stopScan(scanCallback)
    }
}
