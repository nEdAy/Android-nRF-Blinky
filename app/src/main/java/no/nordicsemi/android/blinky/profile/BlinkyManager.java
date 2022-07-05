package no.nordicsemi.android.blinky.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.ObservableBleManager;
import no.nordicsemi.android.blinky.BuildConfig;
import no.nordicsemi.android.blinky.profile.callback.BlinkyNotifyDataCallback;
import no.nordicsemi.android.blinky.profile.callback.BlinkyWriteDataCallback;
import no.nordicsemi.android.blinky.utils.HexString;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class BlinkyManager extends ObservableBleManager {
    /**
     * Nordic Blinky Service UUID.
     */
    public final static UUID LBS_UUID_SERVICE = UUID.fromString("0000FFB0-0000-1000-8000-00805F9B34FB");
    /**
     * BUTTON characteristic UUID.
     */
    private final static UUID LBS_UUID_BUTTON_CHAR = UUID.fromString("0000FFB2-0000-1000-8000-00805F9B34FB");
    /**
     * LED characteristic UUID.
     */
    private final static UUID LBS_UUID_LED_CHAR = UUID.fromString("0000FFB1-0000-1000-8000-00805F9B34FB");


    private final static String WRITE_FOR_CONNECT_CHAR = "A902FCCF";
    private final static String WRITE_FOR_UNLOCK_CHAR = "A70701020304050603";

    private final MutableLiveData<Boolean> connectState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> unlockSuccess = new MutableLiveData<>();

    private BluetoothGattCharacteristic notifyCharacteristic, writeCharacteristic;
    private LogSession logSession;
    private boolean supported;

    public BlinkyManager(@NonNull final Context context) {
        super(context);
    }

    public final LiveData<Boolean> getConnectState() {
        return connectState;
    }

    public LiveData<Boolean> getUnlockSuccess() {
        return unlockSuccess;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new BlinkyBleManagerGattCallback();
    }

    /**
     * Sets the log session to be used for low level logging.
     *
     * @param session the session, or null, if nRF Logger is not installed.
     */
    public void setLogger(@Nullable final LogSession session) {
        logSession = session;
    }

    @Override
    public void log(final int priority, @NonNull final String message) {
        if (BuildConfig.DEBUG) {
            Log.println(priority, "BlinkyManager", message);
        }
        // The priority is a Log.X constant, while the Logger accepts it's log levels.
        Logger.log(logSession, LogContract.Log.Level.fromPriority(priority), message);
    }

    @Override
    protected boolean shouldClearCacheWhenDisconnected() {
        return !supported;
    }

    /**
     * The Button callback will be notified when a notification from Button characteristic
     * has been received, or its data was read.
     * <p>
     * If the data received are valid the {@link BlinkyNotifyDataCallback#writeCharacteristicForConnect}
     * or {@link BlinkyNotifyDataCallback#onLongConnectSuccess}  will be called.
     * Otherwise, the {@link BlinkyNotifyDataCallback#onInvalidDataReceived(BluetoothDevice, Data)}
     * will be called with the data received.
     */
    private final BlinkyNotifyDataCallback notifyCallback = new BlinkyNotifyDataCallback() {

        @Override
        public void writeCharacteristicForConnect() {
            // Are we connected?
            if (writeCharacteristic == null)
                return;
            writeCharacteristic(
                    writeCharacteristic,
                    HexString.hexToBytes(WRITE_FOR_CONNECT_CHAR),
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ).with(ledCallback).enqueue();
        }

        @Override
        public void onLongConnectSuccess() {
            log(Log.WARN, "Long Connect Success!");
            connectState.setValue(true);
        }

        @Override
        public void onUnlockedResult(boolean isSuccess) {
            log(Log.WARN, "Unlocked " + (isSuccess ? "Success" : "Fail"));
            unlockSuccess.setValue(isSuccess);
        }

        @Override
        public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                          @NonNull final Data data) {
            log(Log.WARN, "Invalid data received: " + data);
        }
    };

    /**
     * The LED callback will be notified when the LED state was read or sent to the target device.
     * <p>
     * This callback implements both {@link no.nordicsemi.android.ble.callback.DataReceivedCallback}
     * and {@link no.nordicsemi.android.ble.callback.DataSentCallback} and calls the same
     * method on success.
     * <p>
     * If the data received were invalid, the
     * {@link BlinkyWriteDataCallback#onInvalidDataReceived(BluetoothDevice, Data)} will be
     * called.
     */
    private final BlinkyWriteDataCallback ledCallback = new BlinkyWriteDataCallback() {
        @Override
        public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                          @NonNull final Data data) {
            // Data can only invalid if we read them. We assume the app always sends correct data.
            log(Log.WARN, "Invalid data received: " + data);
        }
    };

    /**
     * BluetoothGatt callbacks object.
     */
    private class BlinkyBleManagerGattCallback extends BleManagerGattCallback {
        @Override
        protected void initialize() {
            setNotificationCallback(notifyCharacteristic).with(notifyCallback);
            readCharacteristic(writeCharacteristic).with(ledCallback).enqueue();
            readCharacteristic(notifyCharacteristic).with(notifyCallback).enqueue();
            enableNotifications(notifyCharacteristic).enqueue();
        }

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(LBS_UUID_SERVICE);
            if (service != null) {
                notifyCharacteristic = service.getCharacteristic(LBS_UUID_BUTTON_CHAR);
                writeCharacteristic = service.getCharacteristic(LBS_UUID_LED_CHAR);
            }
            supported = notifyCharacteristic != null && writeCharacteristic != null;
            return supported;
        }

        @Override
        protected void onServicesInvalidated() {
            notifyCharacteristic = null;
            writeCharacteristic = null;
        }
    }

    /**
     * Sends a request to the device to unlock
     */
    public void tryUnlock() {
        // Are we connected?
        if (writeCharacteristic == null)
            return;

        log(Log.VERBOSE, "Unlocking ...");
        writeCharacteristic(
                writeCharacteristic,
                HexString.hexToBytes(WRITE_FOR_UNLOCK_CHAR),
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        ).with(ledCallback).enqueue();
    }
}
