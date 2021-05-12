package com.bt.debug;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Дублирует все вводимые данные, и отправляет в {@link StreamHookListener}
 */
public class OutputStreamHook extends OutputStream {
    public OutputStream output;
    public StreamHookListener listener;

    public OutputStreamHook() {
    }

    public OutputStreamHook(OutputStream output, StreamHookListener listener) {
        this.output = output;
        this.listener = listener;
    }

    @Override
    public void write(byte[] b) throws IOException {
        listener.onMessage(b);
        output.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(len == 0){
            return;
        }
        listener.onMessage(Arrays.copyOfRange(b,off,off+len));
        output.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        byte[] wrapper = new byte[1];
        wrapper[0]=(byte)b;
        listener.onMessage(wrapper);
        output.write(b);
    }

    //==============================================================================================

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
