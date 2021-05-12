package com.bt;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface BTConnectionObserver {
    @NonNull
    @CheckResult
    BTConnection.BTConnectionState getState();

    @CheckResult
    boolean isConnected();

    @CheckResult
    boolean isSetUp();

    @CheckResult
    boolean isValidForIO();

    @CheckResult
    boolean isInitialized();

    @NonNull
    @CheckResult
    BluetoothDevice getDevice();

    @NonNull
    @CheckResult
    UUID getUuid();

//    void awaitStateChange() throws InterruptedException;
//
//    void awaitStateChange(
//            @IntRange(from = 1) long timeout,
//            @NonNull TimeUnit unit)
//            throws InterruptedException;
//
//    void awaitStateChange(
//            @IntRange(from = 1) long timeout)
//            throws InterruptedException;
//
//    void awaitState(@NonNull BTConnection.BTConnectionState state) throws InterruptedException;
//
//    void awaitState(@NonNull BTConnection.BTConnectionState state,
//                    @IntRange(from = 1) long timeout,
//                    @NonNull TimeUnit unit) throws InterruptedException;
//
//    void awaitState(@NonNull BTConnection.BTConnectionState state,
//                    @IntRange(from = 1) long timeout) throws InterruptedException;
}
