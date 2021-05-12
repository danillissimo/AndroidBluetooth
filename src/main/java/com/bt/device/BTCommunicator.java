package com.bt.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bt.BTConnection.EventListener;

import java.util.UUID;

/**
 * Класс устройства, поддерживающего двустороннее общение
 */
public abstract class BTCommunicator extends BTController {

    public BTCommunicator(@NonNull BluetoothDevice device, @NonNull UUID uuid, @NonNull Context context, @NonNull EventListener listener) {
        super(device, uuid, context, listener);
    }

    public BTCommunicator(@NonNull BluetoothDevice device, @NonNull Context context, @NonNull EventListener listener) {
        super(device, context, listener);
    }

    //No callbacks here, corresponding events are supposed to be triggered by receiver in its listener

    public abstract void startReceiver();

    public abstract void stopReceiver();

    public abstract void terminateReceiver();
}
