package com.bt.device.message;

import androidx.annotation.NonNull;

import com.bt.device.message.outgoing.AbstractOutgoingMessage;

// TODO: 28.01.2021 Это что за клон Header-а? Он вообще нужен?

public abstract class CommandHeader implements AbstractCommandHeader {
    @NonNull
    @Override
    public String wrap(@NonNull AbstractOutgoingMessage message) {
        return wrap(message.toStream());
    }
}
