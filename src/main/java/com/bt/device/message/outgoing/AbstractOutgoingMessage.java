package com.bt.device.message.outgoing;

import androidx.annotation.NonNull;

public interface AbstractOutgoingMessage {
    @NonNull
    public String toStream();
}
