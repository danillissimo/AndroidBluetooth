package com.bt.device.message;

import androidx.annotation.NonNull;

import com.bt.BTInputStream;

import java.io.IOException;

public abstract class SimpleHeader extends Header {
    /**
     * Skip to next valid header and parse it, assuming that all invalid headers are actually a
     * similarly looking data
     * @param input
     * @throws IOException
     */
    @NonNull
    public abstract void findNext(@NonNull BTInputStream input) throws IOException;
}
