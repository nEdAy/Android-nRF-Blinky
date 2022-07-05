package no.nordicsemi.android.blinky.profile.data;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.data.Data;

public final class BlinkyLED {
    private static final byte STATE_OFF = 0x00;
    private static final byte STATE_ON = 0x01;

    @NonNull
    public static Data turn(final boolean on) {
        return on ? turnOn() : turnOff();
    }

    @NonNull
    public static Data turnOn() {
        return Data.opCode(STATE_ON);
    }

    @NonNull
    public static Data turnOff() {
        return Data.opCode(STATE_OFF);
    }
}
