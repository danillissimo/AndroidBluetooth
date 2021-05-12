package com.bt;

import android.Manifest;
import android.os.Handler;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.bt.debug.InputStreamHook;
import com.bt.debug.StreamHookListener;

import java.io.IOException;
import java.io.InputStream;

public interface BTConnectionController extends BTConnectionObserver {
    @CheckResult
    boolean init(boolean createSecureConnection,
                 @Nullable Handler eventListener,
                 @NonNull BTConnection.BTControlMode btControlMode);

    @CheckResult
    boolean init(
            boolean createSecureConnection,
            @Nullable Handler eventListener);

    @NonNull
    @CheckResult
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    BTConnection.BTConnectionState connect(@NonNull BTConnection.BTControlMode btControlMode);

    BTConnection.AsyncConnectionTask getAsyncConnectionTask();

    void close() throws IOException;

    BTConnection.InputHook getInputHook();

    BTConnection.OutputHook getOutputHook();
}
