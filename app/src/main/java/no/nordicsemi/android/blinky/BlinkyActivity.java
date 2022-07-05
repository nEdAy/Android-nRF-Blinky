package no.nordicsemi.android.blinky;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.ble.observer.ConnectionObserver;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.databinding.ActivityBlinkyBinding;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;

public class BlinkyActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";

    private BlinkyViewModel viewModel;
    private ActivityBlinkyBinding binding;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlinkyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Intent intent = getIntent();
        final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

        final MaterialToolbar toolbar = binding.toolbar;
        toolbar.setTitle(deviceName != null ? deviceName : getString(R.string.unknown_device));
        toolbar.setSubtitle(deviceAddress);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configure the view model.
        viewModel = new ViewModelProvider(this).get(BlinkyViewModel.class);
        viewModel.connect(device);

        // Set up views.
        binding.buttonLock.setOnClickListener(view -> viewModel.tryUnlock());
        binding.infoNotSupported.actionRetry.setOnClickListener(v -> viewModel.reconnect());
        binding.infoTimeout.actionRetry.setOnClickListener(v -> viewModel.reconnect());

        viewModel.getConnectionState().observe(this, state -> {
            switch (state.getState()) {
                case CONNECTING:
                    binding.progressContainer.setVisibility(View.VISIBLE);
                    binding.infoNotSupported.container.setVisibility(View.GONE);
                    binding.infoTimeout.container.setVisibility(View.GONE);
                    binding.connectionState.setText(R.string.state_connecting);
                    break;
                case INITIALIZING:
                    binding.connectionState.setText(R.string.state_initializing);
                    break;
                case READY:
                    binding.progressContainer.setVisibility(View.GONE);
                    binding.deviceContainer.setVisibility(View.VISIBLE);
                    onConnectionStateChanged(true);
                    break;
                case DISCONNECTED:
                    if (state instanceof ConnectionState.Disconnected) {
                        binding.deviceContainer.setVisibility(View.GONE);
                        binding.progressContainer.setVisibility(View.GONE);
                        final ConnectionState.Disconnected stateWithReason = (ConnectionState.Disconnected) state;
                        if (stateWithReason.getReason() == ConnectionObserver.REASON_NOT_SUPPORTED) {
                            binding.infoNotSupported.container.setVisibility(View.VISIBLE);
                        } else {
                            binding.infoTimeout.container.setVisibility(View.VISIBLE);
                        }
                    }
                    // fallthrough
                case DISCONNECTING:
                    onConnectionStateChanged(false);
                    break;
            }
        });
        viewModel.getConnectState().observe(this,
                isLongConnect -> {
                    binding.textState.setText(isLongConnect ?
                            R.string.button_isLongConnect : R.string.button_disconnect);
                    binding.buttonLock.setEnabled(isLongConnect);
                });
        viewModel.getUnlockSuccess().observe(this,
                isUnlockSuccess -> Toast.makeText(this, isUnlockSuccess ? "开锁成功" : "开锁失败", Toast.LENGTH_SHORT).show());
    }

    private void onConnectionStateChanged(final boolean connected) {
        if (!connected) {
            binding.buttonLock.setEnabled(false);
            binding.textState.setText(R.string.button_unknown);
        }
    }
}
