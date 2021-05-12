package com.bt.device.message;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import com.bt.device.message.outgoing.AbstractOutgoingMessage;

/**
 * Used with {@link com.bt.device.BTController}
 */
public interface AbstractCommandHeader extends AbstractOutgoingMessage {
    @NonNull
    @CheckResult
    String wrap(@NonNull String message);

    @NonNull
    @CheckResult
    String wrap(@NonNull AbstractOutgoingMessage message);

    /**
     * @return a complete message consisting of a header only
     */
    @NonNull
    @CheckResult
    String wrap();
}
