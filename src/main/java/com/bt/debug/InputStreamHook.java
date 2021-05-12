package com.bt.debug;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Дублирует все считываемые данные, и отправляет в {@link StreamHookListener}
 */
public class InputStreamHook extends InputStream {
    protected InputStream input;
    public StreamHookListener listener;

    public InputStreamHook(InputStream input, StreamHookListener listener) {
        this.input = input;
        this.listener = listener;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int numBytes = input.read(b);
        if(numBytes > 0) {
            listener.onMessage(Arrays.copyOfRange(b, 0, numBytes));
        }
        return numBytes;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int numBytes = input.read(b,off,len);
        if(numBytes>0){
            listener.onMessage(Arrays.copyOfRange(b,off,off+len));
        }
        return numBytes;
    }

    @Override
    public int read() throws IOException {
        int result = input.read();
        if(result != -1){
            byte[] wrapper = new byte[1];
            wrapper[0] = (byte)result;
            listener.onMessage(wrapper);
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        return input.skip(n);
    }

    @Override
    public int available() throws IOException {
        return input.available();
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        input.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        input.reset();
    }

    @Override
    public boolean markSupported() {
        return input.markSupported();
    }
}
