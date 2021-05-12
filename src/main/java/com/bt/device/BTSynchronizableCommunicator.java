package com.bt.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import com.bt.BTConnection;
import com.bt.BTConnection.EventListener;
import com.bt.device.receiver.RequestManager;
import com.bt.device.receiver.RequestManager.Response;
import com.bt.device.message.AbstractHeader;
import com.bt.device.message.outgoing.AbstractOutgoingMessage;

import java.io.IOException;
import java.util.UUID;

/**
 * Класс устройства поддерживающего асинхронные запросы; преполагается использование {@link RequestManager}
 */
public abstract class BTSynchronizableCommunicator extends BTCommunicator {

    public BTSynchronizableCommunicator(@NonNull BluetoothDevice device, @NonNull UUID uuid, @NonNull Context context, @NonNull EventListener listener) {
        super(device, uuid, context, listener);
    }

    public BTSynchronizableCommunicator(@NonNull BluetoothDevice device, @NonNull Context context, @NonNull EventListener listener) {
        super(device, context, listener);
    }

    @NonNull
    @CheckResult
    protected abstract Object request(@NonNull byte[] request) throws IOException;

    @NonNull
    @CheckResult
    protected Object request(
            @NonNull AbstractHeader header,
            @NonNull AbstractOutgoingMessage message
    ) throws IOException {
        return request((header.toStream() + message.toStream()).getBytes());
    }

    @NonNull
    @CheckResult
    protected abstract Response requestAsync(@NonNull byte[] request) throws IOException;

    @NonNull
    @CheckResult
    protected Response requestAsync(
            @NonNull AbstractHeader header,
            @NonNull AbstractOutgoingMessage message) throws IOException {
        return requestAsync((header.toStream() + message.toStream()).getBytes());
    }
}
