package com.bt;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.bt.device.message.incoming.StreamParseException;
import com.bt.device.receiver.BufferPartReference;

// TODO: java.lang.Thread.currentThread() предоставляет доступ к текущему потоку выполнения
//  Тогда в течение операций сборки разбитых сообщений можно проверять был ли поток прерван
//  И досрочно возвращаться с InterruptedException

// TODO: 04.06.2019 Можно выделить из этого класса интерфейс. Тогда можно будет реализовать хук,
//  соответствующий этому интерфейсу, который можно будет вводить вместо *настоящего* стрима,
//  и который будет дублировать все прочтенное. Таким образом, можно будет хотя бы отчасти нормально
//  прослушивать входящие данные

/**
 * Врапер для входящего стрима сокета блютуза. Компенсирует разрывы входящих сообщений при наличии
 * оных.
 */
public class BTInputStream {
    public static final String TAG = "BTInputStream";

    protected byte[] bytes;
    protected int bytesRead;
    protected int firstValuableByte = 0;
    protected int cursor = 0;
    protected InputStream input;
    protected StringBuilder object;

    public BTInputStream(
            @NonNull InputStream input,
            @IntRange(from = 1) int bufferSize) {
        bytes = new byte[bufferSize];
        object = new StringBuilder(bufferSize);
        this.input = input;
    }

    //<editor-fold desc="Inner utils">
    @CheckResult
    @IntRange(from = 0)
    public final int bytesLeftToRead() {
        return bytesRead - cursor;
    }

    @CheckResult
    @NonNull
    protected final BufferPartReference getTheRest() {
        BufferPartReference result = new BufferPartReference(bytes, cursor, bytesRead - 1);
        firstValuableByte = cursor = bytesRead;
        return result;
    }

//Replaced on 06/02/2021, place back if everything's broken
//    @CheckResult
//    @NonNull
//    public final BufferPartReference getTheRest() {
//        BufferPartReference result = new BufferPartReference(bytes, cursor, bytesRead - 1);
//        bytesRead = cursor;
//        return result;
//    }

    @CheckResult
    @NonNull
    protected final BufferPartReference getBytes(@IntRange(from = 1) int numBytes) {
        BufferPartReference result = new BufferPartReference(bytes, cursor, cursor + numBytes - 1);
        firstValuableByte = (cursor += numBytes);
        return result;
    }

//Replaced on 06/02/2021, place back if everything's broken
//    @CheckResult
//    @NonNull
//    public final BufferPartReference getBytes(@IntRange(from = 1) int numBytes) {
//        BufferPartReference result = new BufferPartReference(bytes, cursor, cursor + numBytes - 1);
//        cursor += numBytes;
//        return result;
//    }

    protected final void appendReadBytesIfAny() {
        if(cursor > 0){
            appendReadBytes();
        }
    }

    protected final void appendReadBytes(){
        object.append(new BufferPartReference(bytes, firstValuableByte, cursor - 1));
        firstValuableByte = cursor;
    }

    @IntRange(from = 1)
    protected final int appendTheRestAndRead() throws IndexOutOfBoundsException, IOException{
        object.append(new BufferPartReference(bytes, firstValuableByte, bytesRead - 1));
        return read();
    }

    protected final void appendWhatsLeft(){
        if(bytesLeftToRead() > 0){
            object.append(getTheRest());
        }
    }

    @CheckResult
    public final boolean currentByteIsOneOf(@NonNull char[] values) {
        byte b = bytes[cursor];
        for (char c : values) {
            if (b == c) {
                return true;
            }
        }
        return false;
    }
    //</editor-fold>

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //<editor-fold desc="Read methods">

    /**
     * Should only be called when {@code bytesLeftToRead() == 0} because it overwrites {@code bytes}
     * from the beginning.
     *
     * @return number of bytes read
     * @throws IOException
     */
    @IntRange(from = 1)
    protected final int read() throws IOException {
        Log.d(TAG, this.hashCode() + " will now be reading");
        bytesRead = input.read(bytes);
        firstValuableByte = cursor = 0;
        Log.d(TAG, this.hashCode() + " read " + bytesRead + " bytes: " +
                (new String(Arrays.copyOf(bytes, bytesRead))));
        return bytesRead;
    }

    protected final void readIfCurrentDataIsProcessed() throws IOException{
        if (bytesLeftToRead() == 0){
            read();
        }
    }

    @CheckResult
    @NonNull
    public final String readIncluding(char value) throws IOException {
        doReadExcluding(value);
        cursor++;
        appendReadBytes();
        return object.toString();
    }

    @CheckResult
    @NonNull
    public final String readExcluding(char value) throws IOException {
        doReadExcluding(value);
        appendReadBytesIfAny();
        return object.toString();
    }

    protected final void doReadExcluding(char value) throws IOException {
        readIfCurrentDataIsProcessed();
        object.setLength(0);
        while (bytes[cursor] != value) {
            //BytesLeftToRead has no idea we've already read the byte, that's why >1
            if (bytesLeftToRead() > 1) {
                cursor++;
            } else {
                appendTheRestAndRead();
            }
        }
    }

    @CheckResult
    @NonNull
    public final String readIncluding(char[] values) throws IOException {
        doReadExcluding(values);
        cursor++;
        appendReadBytes();
        return object.toString();
    }

    @CheckResult
    @NonNull
    public final String readExcluding(char[] values) throws IOException {
        doReadExcluding(values);
        appendReadBytesIfAny();
        return object.toString();
    }

    protected final void doReadExcluding(char[] values) throws IOException {
        readIfCurrentDataIsProcessed();
        object.setLength(0);
        while (!currentByteIsOneOf(values)) {
            //BytesLeftToRead has no idea we've already read the byte, that's why >1
            if (bytesLeftToRead() > 1) {
                cursor++;
            } else {
                appendTheRestAndRead();
            }
        }
    }

    public final void skipIncluding(char value) throws IOException {
        doSkipExcluding(value);
        if(bytesLeftToRead() > 0) {
            cursor++;
            firstValuableByte = cursor;
        }else {
            read();
        }
    }

    public final void skipExcluding(char value) throws IOException {
        doSkipExcluding(value);
        firstValuableByte = cursor;
    }

    protected final void doSkipExcluding(char value) throws IOException {
        readIfCurrentDataIsProcessed();
        while (bytes[cursor] != value) {
            //BytesLeftToRead has no idea we've already read the byte, that's why >1
            if (bytesLeftToRead() > 1) {
                cursor++;
            } else {
                //Log.d(TAG, this.hashCode() + " skipped " + (cursor + 1) + " bytes");
                read();
            }
        }
    }

    public final void skipIncluding(char[] values) throws IOException {
        doSkipExcluding(values);
        cursor++;
        firstValuableByte = cursor;
    }

    public final void skipExcluding(char[] values) throws IOException {
        doSkipExcluding(values);
        firstValuableByte = cursor;
    }

    protected final void doSkipExcluding(char[] values) throws IOException {
        readIfCurrentDataIsProcessed();
        while (!currentByteIsOneOf(values)) {
            //BytesLeftToRead has no idea we've already read the byte, that's why >1
            if (bytesLeftToRead() > 1) {
                cursor++;
            } else {
                //Log.d(TAG, this.hashCode() + " skipped " + (cursor + 1) + " bytes");
                read();
            }
        }
    }

    @CheckResult
    @NonNull
    public final String read(@IntRange(from = 1) int numBytes) throws IOException {
        object.setLength(0);
        if (bytesLeftToRead() < numBytes) {
            numBytes -= bytesLeftToRead();
            appendWhatsLeft();
            while (read() < numBytes) {
                numBytes -= bytesRead;
                object.append(getTheRest());
            }
        }
        object.append(getBytes(numBytes));
        return object.toString();
    }

    public final void skip(@IntRange(from = 1) int numBytes) throws IOException {
        if(numBytes > bytesLeftToRead()){
            numBytes -= bytesLeftToRead();
            while (read() < numBytes) {
                numBytes -= bytesRead;
            }
        }
        firstValuableByte = (cursor += numBytes);
    }
    //</editor-fold>
    //<editor-fold desc="Utils">

    /**
     * Places cursor on the footer.
     * @param footer
     * @return
     * @throws IOException
     * @throws StreamParseException
     */
    public int readInt(char footer) throws IOException, StreamParseException, NumberFormatException{
        String buf = "";
        int b;

        try {
            buf = readExcluding(footer);
            b = Integer.parseInt(buf);
        } catch (NumberFormatException e) {
            throw new StreamParseException("Expected an integer, got '" + buf + "instead", buf, 0);
        }
        return b;
    }

    /**
     * Places cursor on the footer.
     * @param footers
     * @return
     * @throws IOException
     * @throws StreamParseException
     */
    public int readInt(char[] footers) throws IOException, StreamParseException, NumberFormatException{
        String buf = "";
        int b;

        try {
            buf = readExcluding(footers);
            b = Integer.parseInt(buf);
        } catch (NumberFormatException e) {
            throw new StreamParseException("Expected an integer, got '" + buf + "instead", buf, 0);
        }
        return b;
    }
    //</editor-fold desc="Utils">

//Replaced on 06/02/2021, place back if everything's broken
//    @CheckResult
//    @NonNull
//    public final String readIncluding(char value) throws IOException {
//        object.setLength(0);
//        while (bytes[cursor] != value) {
//            if (bytesLeftToRead() > 1) {
//                cursor++;
//            } else {
//                cursor++;
//                object.append(getReadBytes());
//                read();
//            }
//        }
//        cursor++;
//        object.append(getReadBytes());
//        firstValuableByte = cursor;
//        return object.toString();
//    }
//
//    @CheckResult
//    @NonNull
//    public final String readExcluding(char value) throws IOException {
//        object.setLength(0);
//        while (bytes[cursor] != value) {
//            if (bytesLeftToRead() > 0) {
//                cursor++;
//            } else {
//                object.append(getTheRest());
//                read();
//            }
//        }
//        //cursor++;
//        object.append(getReadBytes());
//        firstValuableByte = cursor;
//        return object.toString();
//    }
//
//    @CheckResult
//    @NonNull
//    public final String readIncluding(char[] values) throws IOException {
//        object.setLength(0);
//        while (!currentByteIsOneOf(values)) {
//            if (bytesLeftToRead() > 0) {
//                cursor++;
//            } else {
//                object.append(getTheRest());
//                read();
//            }
//        }
//        cursor++;
//        object.append(getReadBytes());
//        firstValuableByte = cursor;
//        return object.toString();
//    }
//
//    @CheckResult
//    @NonNull
//    public final String readExcluding(char[] values) throws IOException {
//        object.setLength(0);
//        while (!currentByteIsOneOf(values)) {
//            if (bytesLeftToRead() > 0) {
//                cursor++;
//            } else {
//                object.append(getTheRest());
//                read();
//            }
//        }
//        //cursor++;
//        object.append(getReadBytes());
//        firstValuableByte = cursor;
//        return object.toString();
//    }
//
//    public final void skipIncluding(char value) throws IOException {
//        skipExcluding(value);
//        cursor++;
//    }
//
//    public final void skipExcluding(char value) throws IOException {
//        while (bytes[cursor] != value) {
//            if (bytesLeftToRead() > 0) {
//                cursor++;
//            } else {
//                //Log.d(TAG, this.hashCode() + " skipped " + (cursor + 1) + " bytes");
//                read();
//            }
//        }
//        firstValuableByte = cursor;
//    }
//
//    public final void skipIncluding(char[] values) throws IOException {
//        skipExcluding(values);
//        cursor++;
//    }
//
//    public final void skipExcluding(char[] values) throws IOException {
//        object.setLength(0);
//        while (!currentByteIsOneOf(values)) {
//            if (bytesLeftToRead() > 0) {
//                cursor++;
//            } else {
//                read();
//            }
//        }
//        firstValuableByte = cursor;
//    }
//
//    @NonNull
//    public final String read(@IntRange(from = 1) int numBytes) throws IOException {
//        object.setLength(0);
//        if (bytesLeftToRead() >= numBytes) {
//            object.append(getBytes(numBytes));
//        } else {
//            numBytes -= bytesLeftToRead();
//            object.append(getTheRest());
//            while (read() < numBytes) {
//                numBytes -= bytesRead;
//                object.append(getTheRest());
//            }
//            object.append(getBytes(numBytes));
//        }
//        firstValuableByte = cursor;
//        return object.toString();
//    }
//
//    public final void skip(@IntRange(from = 1) int numBytes) throws IOException {
//        if (bytesLeftToRead() >= numBytes) {
//            cursor += numBytes;
//        } else {
//            numBytes -= bytesLeftToRead();
//            while (read() < numBytes) {
//                numBytes -= bytesRead;
//            }
//            cursor += numBytes;
//            firstValuableByte = cursor;
//        }
//    }
}
