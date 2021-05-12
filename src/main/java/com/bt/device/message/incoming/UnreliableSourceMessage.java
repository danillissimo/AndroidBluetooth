package com.bt.device.message.incoming;

import androidx.annotation.NonNull;

/**
 * Закладка на будущее
 */
public abstract class UnreliableSourceMessage extends IncomingMessage {
    /**
     * Should try to parse message without throwing any exceptions
     * @param data message to parse
     * @return state state
     */
    abstract boolean tryParse(@NonNull String data);

    //No tryParse(BTInputStream) version, because one should explicitly select data to be parsed
}
