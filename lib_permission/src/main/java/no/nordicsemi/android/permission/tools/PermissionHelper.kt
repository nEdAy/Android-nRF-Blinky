package no.nordicsemi.android.permission.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionHelper(private val context: Context) {

//    fun isBluetoothScanPermissionGranted(): Boolean {
//        return if (!dataProvider.isSorAbove) true else ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.BLUETOOTH_SCAN
//        ) == PackageManager.PERMISSION_GRANTED
//    }

    fun isRequiredPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
