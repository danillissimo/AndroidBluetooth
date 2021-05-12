package com.bt.device.message;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import com.bt.device.message.outgoing.AbstractOutgoingMessage;

public abstract class Header implements AbstractHeader {
    @Override
    @NonNull
    @CheckResult
    public String wrap(@NonNull AbstractOutgoingMessage message){
        return wrap(message.toStream());
    }
}
