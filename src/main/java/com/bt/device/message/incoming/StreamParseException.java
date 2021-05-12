package com.bt.device.message.incoming;

import androidx.annotation.NonNull;

import java.text.ParseException;

/**
 * Используется в методах, разбирающих стрим вместо строки. Помимо ошибки несет в себе данные,
 * которые иначе могли бы быть утеряны насовсем.
 *
 * Even the most well-established system must be prepared for such errors due the unreliability of a
 * Bluetooth channel, that's why {@link RuntimeException} is not used as a parent
 * class here.
 */
public class StreamParseException extends Exception {
    protected String data;
    protected int offset;

    public int getErrorOffset() {
        return offset;
    }

    public String getData() {
        return data;
    }

    public StreamParseException(String detailMessage, String data, int offset){
        super(detailMessage);
        this.data = data;
        this.offset = offset;
    }

    public StreamParseException(@NonNull ParseException e, String data){
        super(e.getMessage());
        this.data = data;
        this.offset = e.getErrorOffset();
    }

    @Override
    public String toString() {
        return this.getMessage() + " at " + Integer.toString(offset);
    }
}
