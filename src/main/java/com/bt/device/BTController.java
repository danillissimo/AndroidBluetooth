package com.bt.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import com.bt.BTConnection;
import com.bt.BTConnection.BTConnectionState;
import com.bt.BTConnectionController;
import com.bt.device.message.AbstractHeader;
import com.bt.device.message.outgoing.AbstractOutgoingMessage;

import java.io.IOException;
import java.util.UUID;

public abstract class BTController {
    private final static String TAG = "BTController";
    protected final BTConnection connection;

    //<editor-fold desc="Lifecycle">
    public BTController(@NonNull BluetoothDevice device,
                        @NonNull UUID uuid,
                        @NonNull Context context,
                        @NonNull BTConnection.EventListener listener) {
        connection = new BTConnection(device,uuid,context,listener);
    }

    public BTController( @NonNull BluetoothDevice device,
                         @NonNull Context context,
                         @NonNull BTConnection.EventListener listener) {
        connection = new BTConnection(device, context, listener);
    }

    @Override
    protected void finalize() throws IOException {
        connection.close();
    }

    //</editor-fold desc="Lifecycle">

    //==============================================================================================

    protected synchronized void sendMessage(@NonNull byte[] message) throws IOException {
        Log.v(TAG, this.hashCode() + " will now send to " + connection.getDevice().getName() + " '" + new String(message) + "'");
        connection.getOutputStream().write(message);
    }

    protected void sendMessage(@NonNull String message) throws IOException {
        sendMessage(message.getBytes());
    }

    protected void sendMessage(
            @NonNull AbstractHeader header,
            @NonNull AbstractOutgoingMessage message)
            throws IOException {
        sendMessage(header.wrap(message));
    }

    protected void sendMessage(@NonNull AbstractHeader header) throws IOException {
        sendMessage(header.wrap());
    }

    //==============================================================================================

    public BTConnectionController getConnection(){
        return connection;
    }
}