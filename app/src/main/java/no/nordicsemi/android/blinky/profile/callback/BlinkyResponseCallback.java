package no.nordicsemi.android.blinky.profile.callback;

public interface BlinkyResponseCallback {
    void writeCharacteristicForConnect();
    void onLongConnectSuccess();
    void onUnlockedResult(boolean isSuccess);
}
