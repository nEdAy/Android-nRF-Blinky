package no.nordicsemi.android.blinky.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.profile.BlinkyManager;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class BlinkyViewModel extends AndroidViewModel {
	private final BlinkyManager blinkyManager;
	private BluetoothDevice device;
	@Nullable
	private ConnectRequest connectRequest;

	public BlinkyViewModel(@NonNull final Application application) {
		super(application);
		// Initialize the manager.
		blinkyManager = new BlinkyManager(getApplication());
	}

	public LiveData<ConnectionState> getConnectionState() {
		return blinkyManager.state;
	}

	public LiveData<Boolean> getConnectState() {
		return blinkyManager.getConnectState();
	}

	public LiveData<Boolean> getUnlockSuccess() {
		return blinkyManager.getUnlockSuccess();
	}

	/**
	 * Connect to the given peripheral.
	 *
	 * @param target the target device.
	 */
	public void connect(@NonNull final DiscoveredBluetoothDevice target) {
		// Prevent from calling again when called again (screen orientation changed).
		if (device == null) {
			device = target.getDevice();
			final LogSession logSession = Logger
					.newSession(getApplication(), null, target.getAddress(), target.getName());
			blinkyManager.setLogger(logSession);
			reconnect();
		}
	}

	/**
	 * Reconnects to previously connected device.
	 * If this device was not supported, its services were cleared on disconnection, so
	 * reconnection may help.
	 */
	public void reconnect() {
		if (device != null) {
			connectRequest = blinkyManager.connect(device)
					.retry(3, 100)
					.useAutoConnect(false)
					.then(d -> connectRequest = null);
			connectRequest.enqueue();
		}
	}

	/**
	 * Disconnect from peripheral.
	 */
	private void disconnect() {
		device = null;
		if (connectRequest != null) {
			connectRequest.cancelPendingConnection();
		} else if (blinkyManager.isConnected()) {
			blinkyManager.disconnect().enqueue();
		}
	}

	/**
	 * Sends a command to unlock.
	 */
	public void tryUnlock() {
		blinkyManager.tryUnlock();
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		disconnect();
	}
}
