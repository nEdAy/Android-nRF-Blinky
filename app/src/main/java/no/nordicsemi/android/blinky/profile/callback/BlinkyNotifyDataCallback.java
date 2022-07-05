package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.blinky.utils.HexString;

public abstract class BlinkyNotifyDataCallback implements ProfileDataCallback, BlinkyResponseCallback {
    private static final String PREPARE_FOR_CONNECT = "FCCF";
    private static final String LONG_CONNECT_SUCCESS = "A90D";
    private static final String UNLOCKED_SUCCESS = "A70D";
    private static final String UNLOCKED_FAIL = "A700";

    @Override
    public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        if (data.size() != 2 || data.getValue() == null) {
            onInvalidDataReceived(device, data);
            return;
        }
        String hexString = HexString.bytesToHex(data.getValue());
        switch (hexString) {
            case PREPARE_FOR_CONNECT:
                writeCharacteristicForConnect();
                break;
            case LONG_CONNECT_SUCCESS:
                onLongConnectSuccess();
                break;
            case UNLOCKED_SUCCESS:
                onUnlockedResult(true);
                break;
            case UNLOCKED_FAIL:
                onUnlockedResult(false);
                break;
            default:
                onInvalidDataReceived(device, data);
                break;
        }
    }
}
