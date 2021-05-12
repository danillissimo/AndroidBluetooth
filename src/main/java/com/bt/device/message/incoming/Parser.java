package com.bt.device.message.incoming;

import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
// TODO: 12.07.2019 для каждого метода должен быть еще и try метод
//  такие методы возвращают статус, а выходной контейнер принимают как аргумент
// TODO: 12.07.2019 Здесь внизу прорву всего надо еще дописать

/**
 * Вспомогательный класс для разбора сообщений. На данный момент далек от полноты.
 * <p>
 * Методы, названния которых начинаются не с try, предполагают что смогут корректно выполниться,
 * поэтому выбрасывают исключения в случае ошибки. Если источник данных ненадежен, и ошибка ожидаема,
 * то следует использовать одноименные методы с приставкой try(пока что не реализовааны)
 */
public class Parser {
    public static final byte[] bitMasks = {
            0b1,
            0b11,
            0b111,
            0b1111,
            0b11111,
            0b111111,
            0b1111111,
    };
    private static final String TAG = "Parser";
    //www.cpp.sh
    //#include <iostream>
    //
    //using namespace std;
    //
    //int main()
    //{
    //    for(int i = 0; i < 8; i++){
    //        cout << "0b";
    //        for(int b = 0; b <= i; b++){
    //            cout<<1;
    //        }
    //        cout <<"L,"<<endl;
    //    }
    //}
    protected String data;
    protected int cursor;

    public Parser(@NonNull String data) {
        wrap(data);
    }

    //TODO статические методы должны также поддерживать стримы

    //<editor-fold desc="Static methods">

    /**
     * Parses appendix based on given message type.
     *
     * @param data      message to parse
     * @param type      should be msgType.class
     * @param <msgType> class type of message instance to build
     * @return instance of msgType if succeeds, otherwise throws an exception
     * @throws ParseException         if appendix doesn't satisfy the intended format
     * @throws IllegalAccessException if required class is not accessible
     * @throws InstantiationException if no-arguments constructor is not accessible
     */
//    @CheckResult
//    @NonNull
//    public static <msgType extends AbstractIncomingMessage> msgType parse(@NonNull String data, Class<msgType> type)
//            throws ParseException, IllegalAccessException, InstantiationException {
//        msgType result = type.newInstance();
//        result.parse(data);
//        return result;
//    }

    /**
     * Same as {@link Parser#parse(String, Class)}, but suppresses
     * {@link IllegalAccessException} and {@link InstantiationException} which
     * normally should not occur unless you're building something complex.
     *
     * @param data      message to parse
     * @param type      should be msgType.class
     * @param <msgType> class type of message instance to build
     * @return instance of msgType if succeeds, otherwise throws an exception
     * @throws ParseException if appendix doesn't satisfy the intended format
     */
//    @CheckResult
//    @NonNull
//    public static <msgType extends AbstractIncomingMessage> msgType parseSilently(@NonNull String data, Class<msgType> type)
//            throws ParseException {
//        msgType result = null;
//        try {
//            result = type.newInstance();
//            result.parse(data);
//        } catch (IllegalAccessException e) {
//        } catch (InstantiationException e) {
//        }
//        return result;
//    }

    /**
     * <mark><strong><em><u>
     * For testing and development. Use in production code is a symptom of coding errors.
     * </u></em></strong></mark>
     * <p>
     * Attempts to parse appendix based on given message type.
     * </p>
     *
     * @param data      message to parse
     * @param type      should be msgType.class
     * @param <msgType> class type of message instance to build
     * @return instance of msgType if succeeds, otherwise null
     * @throws IllegalAccessException if required class is not accessible
     * @throws InstantiationException if no-arguments constructor is not accessible
     */
//    @CheckResult
//    @Nullable
//    public static <msgType extends IncomingMessage> msgType tryParse(@NonNull String data, Class<msgType> type)
//            throws IllegalAccessException, InstantiationException {
//        msgType result = type.newInstance();
//        try {
//            result.parse(data);
//        } catch (Throwable t) {
//            return null;
//        }
//        return result;
//    }

    /**
     * <mark><strong><em><u>
     * For testing and development. Use in production code is a symptom of coding errors.
     * </u></em></strong></mark>
     * <p>
     * Same as {@link Parser#tryParse(String, Class)}, but suppresses
     * {@link IllegalAccessException} and {@link InstantiationException} which
     * normally should not occur unless you're building something complex.
     * </p>
     *
     * @param data      message to parse
     * @param type      should be msgType.class
     * @param <msgType> class type of message instance to build
     * @return instance of msgType if succeeds, otherwise null
     */
//    @CheckResult
//    @Nullable
//    public static <msgType extends IncomingMessage> msgType tryParseSilently(@NonNull String data, Class<msgType> type) {
//        msgType result = null;
//        try {
//            result = type.newInstance();
//            result.parse(data);
//        } catch (IllegalAccessException e) {
//        } catch (InstantiationException e) {
//        } catch (Throwable t) {
//            return null;
//        }
//        return result;
//    }
    //</editor-fold>

    @NonNull
    @CheckResult
    public String getBytes() { //TODO это че вообще такое?
        String result = data.substring(cursor);
        cursor = data.length();
        return result;
    }

    @NonNull
    @CheckResult
    public String getBytes(@IntRange(from = 1) int numBytes) throws IndexOutOfBoundsException {
        String result = data.substring(cursor, cursor + numBytes);
        cursor += numBytes;
        return result;
    }

    /**
     * Moves the internal cursor forward by the specified number. Used for general purpose, and with
     * bit functions that doesn't affect the cursor due to unpredictable appendix structure.
     *
     * @param numBytes number of bytes to skip
     * @see Parser#getFlag(int)
     * @see Parser#getBits(int, int)
     */
    public Parser skip(@IntRange(from = 1) int numBytes) {
        cursor += numBytes;
        return this;
    }

    public Parser rewind(@IntRange(from = 1) int numBytes) {
        cursor -= numBytes;
        return this;
    }

    public void wrap(@NonNull String data) {
        this.data = data;
        cursor = 0;
    }

    //<editor-fold desc="Int methods">

    /**
     * Extracts a number from the appendix, surrounded by the first pair of {@code header} and
     * {@code footer} found from the current position, and sets the cursor to the position after the
     * {@code footer}.<br>
     *
     * @param header symbol before valuable appendix
     * @param footer symbol after valuable appendix
     * @return number if everything goes smooth. Also sets cursor to ({@code footer} position + 1)
     * @throws ParseException
     * @see Parser#parseInt(char)
     * @see Parser#findInt(char)
     * @see Parser#findFloat(char, char)
     */
    @CheckResult
    public int findInt(
            char header,
            char footer) throws ParseException {
        int headerPos = data.indexOf(header, cursor);
        if (headerPos == -1) {
            throw new ParseException("Couldn't find header '" + header + "': " + data, cursor);
        }
        int footerPos = data.indexOf(footer, headerPos);
        if (footerPos == -1) {
            throw new ParseException("Couldn't find footer '" + footer + "': " + data, headerPos);
        }
        int result;
        try {
            result = Integer.parseInt(data.substring(headerPos + 1, footerPos));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, headerPos);
        }
        cursor = footerPos + 1;
        return result;
    }

    /**
     * Finds the first pair of {@code header} and {@code footer} from current position. If succeeds,
     * sets cursor to position after {@code footer}, otherwise cursor remains unchanged; then tries
     * to extract a number from between them. Failure on any step will cause {@code defaultValue}
     * to be returned.
     *
     * @param header       symbol before valuable appendix
     * @param footer       symbol after valuable appendix
     * @param defaultValue returned in case of failure
     * @return number if everything goes smooth, {@code defaultValue} otherwise.
     * Sets cursor to ({@code footer} position + 1) if both header and footer are found,
     * no matter whether parsing succeeds. Cursor remains unchanged if only header or nothing at all
     * is found.
     */
    public int findInt(
            char header,
            char footer,
            int defaultValue
    ) {
        int headerPos = data.indexOf(header, cursor);
        if (headerPos == -1) {
            return defaultValue;
        }
        int footerPos = data.indexOf(footer, headerPos);
        if (footerPos == -1) {
            return defaultValue;
        }
        cursor = footerPos + 1;
        int result;
        try {
            result = Integer.parseInt(data.substring(headerPos + 1, footerPos));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
        return result;
    }

    /**
     * Extracts a number from all appendix following the first {@code header} found from the current
     * position, and sets the cursor to a position after the end of the appendix, causing any further
     * operations with this parser to fail. Used to process appendix at the end of a message.
     *
     * @param header symbol before valuable appendix
     * @return number if everything goes smooth. Also leaves parser invalid.
     * @throws ParseException
     * @see Parser#parseInt()
     * @see Parser#findInt(char, char)
     * @see Parser#findFloat(char)
     */
    @CheckResult
    public int findInt(char header) throws ParseException {
        int headerPos = data.indexOf(header, cursor);
        if (headerPos == -1) {
            throw new ParseException("Couldn't find header '" + header + "': " + data, cursor);
        }

        int result;
        try {
            result = Integer.parseInt(data.substring(headerPos + 1));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, headerPos);
        }
        cursor = data.length();
        return result;
    }

    // TODO: 13.08.2019 Функции конвертации целого должны принимать систему исчисления

    /**
     * Extracts a number from the appendix from the current position and to the first {@code footer}
     * found from the current position, and sets the cursor to the position after the {@code footer}.
     *
     * @param footer symbol after valuable appendix
     * @return number if everything goes smooth. Also sets cursor to ({@code footer} position + 1)
     * @throws ParseException
     * @see Parser#findInt(char, char)
     * @see Parser#parseInt()
     * @see Parser#parseFloat(char)
     */
    public int parseInt(char footer)
            throws ParseException {
        int footerPos = data.indexOf(footer, cursor);
        if (footerPos == -1) {
            throw new ParseException("Couldn't find footer '" + footer + "': " + data, cursor);
        }
        int result;
        try {
            result = Integer.parseInt(data.substring(cursor, footerPos));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, cursor);
        }
        cursor = footerPos + 1;
        return result;
    }

    @Nullable
    @CheckResult
    public Integer parseInt(char[] possibleFooters) {// TODO: 22.06.2019 Скетч функции, на ревью
        for (int i = 0; i < possibleFooters.length; i++) {
            int footerPos = data.indexOf(possibleFooters[i], cursor);
            if (footerPos != -1) {
                int result;
                try {
                    result = Integer.parseInt((data.substring(cursor, footerPos)));
                } catch (NumberFormatException e) {
                    Log.v(TAG, "parseInt(possibleFooters): footer '"
                            + possibleFooters[i] + "' didn't suit");
                    continue;
                }
                cursor = footerPos + 1;
                return result;
            }
        }
        Log.d(TAG, "parseInt(possibleFooters): neither footer suited");
        return null;
    }

    /**
     * Extracts a number from all appendix from the current position, and sets the cursor to a position
     * after the end of the appendix, causing any further operations with this parser to fail. Used to
     * process appendix at the end of a message.
     *
     * @return number if everything goes smooth. Also leaves parser invalid.
     * @see Parser#findInt(char)
     * @see Parser#parseInt(char)
     * @see Parser#parseFloat()
     */
    public int parseInt() throws ParseException {
        int result;
        try {
            result = Integer.parseInt(data.substring(cursor, data.length() - 1));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, cursor);
        }
        cursor = data.length();
        return result;
    }
    //</editor-fold>

    //<editor-fold desc="Float methods">

    /**
     * Extracts a number from the appendix, surrounded by the first pair of {@code header} and
     * {@code footer} found from the current position, and sets the cursor to the position after the
     * {@code footer}.<br>
     *
     * @param header symbol before valuable appendix
     * @param footer symbol after valuable appendix
     * @return number if everything goes smooth. Also sets cursor to ({@code footer} position + 1)
     * @throws ParseException
     * @see Parser#parseFloat(char)
     * @see Parser#findFloat(char)
     * @see Parser#findInt(char, char)
     */
    @CheckResult
    public float findFloat(
            char header,
            char footer) throws ParseException {
        int headerPos = data.indexOf(header, cursor);
        if (headerPos == -1) {
            throw new ParseException("Couldn't find header '" + header + "': " + data, cursor);
        }
        int footerPos = data.indexOf(' ', headerPos);
        if (footerPos == -1) {
            throw new ParseException("Couldn't find footer '" + footer + "': " + data, headerPos);
        }
        float result;
        try {
            result = Float.parseFloat(data.substring(headerPos + 1, footerPos));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, headerPos);
        }
        cursor = footerPos + 1;
        return result;
    }

    /**
     * Finds the first pair of {@code header} and {@code footer} from current position. If succeeds,
     * sets cursor to position after {@code footer}, otherwise cursor remains unchanged; then tries
     * to extract a number from between them. Failure on any step will cause {@code defaultValue}
     * to be returned.
     *
     * @param header       symbol before valuable appendix
     * @param footer       symbol after valuable appendix
     * @param defaultValue returned in case of failure
     * @return number if everything goes smooth, {@code defaultValue} otherwise.
     * Sets cursor to ({@code footer} position + 1) if both header and footer are found,
     * no matter whether parsing succeeds. Cursor remains unchanged if only header or nothing at all
     * is found.
     */
    public float findFloat(
            char header,
            char footer,
            float defaultValue
    ) {
        int headerPos = data.indexOf(header, cursor);
        if (headerPos == -1) {
            return defaultValue;
        }
        int footerPos = data.indexOf(footer, headerPos);
        if (footerPos == -1) {
            return defaultValue;
        }
        cursor = footerPos + 1;
        int result;
        try {
            result = Integer.parseInt(data.substring(headerPos + 1, footerPos));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
        return result;
    }

    /**
     * Extracts a number from all appendix following the first {@code header} found from the current
     * position, and sets the cursor to a position after the end of the appendix, causing any further
     * operations with this parser to fail. Used to process appendix at the end of a message.
     *
     * @param header symbol before valuable appendix
     * @return number if everything goes smooth. Also leaves parser invalid.
     * @throws ParseException
     * @see Parser#parseFloat()
     * @see Parser#findFloat(char, char)
     * @see Parser#findInt(char)
     */
    @CheckResult
    public float findFloat(char header) throws ParseException {
        int headerPos = data.indexOf(header, cursor);
        if (headerPos == -1) {
            throw new ParseException("Couldn't find header '" + header + "': " + data, cursor);
        }

        float result;
        try {
            result = Float.parseFloat(data.substring(headerPos + 1));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, headerPos);
        }
        cursor = data.length();
        return result;
    }

    /**
     * Extracts a number from the appendix from the current position and to the first {@code footer}
     * found from the current position, and sets the cursor to the position after the {@code footer}.
     *
     * @param footer symbol after valuable appendix
     * @return number if everything goes smooth. Also sets cursor to ({@code footer} position + 1)
     * @throws ParseException
     * @see Parser#findFloat(char, char)
     * @see Parser#parseFloat()
     * @see Parser#parseInt(char)
     */
    public float parseFloat(char footer)
            throws ParseException {
        int footerPos = data.indexOf(footer, cursor);
        if (footerPos == -1) {
            throw new ParseException("Couldn't find footer '" + footer + "': " + data, cursor);
        }
        float result;
        try {
            result = Float.parseFloat(data.substring(cursor, footer));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, cursor);
        }
        cursor = footer + 1;
        return result;
    }

    /**
     * Extracts a number from all appendix from the current position, and sets the cursor to a position
     * after the end of the appendix, causing any further operations with this parser to fail. Used to
     * process appendix at the end of a message.
     *
     * @return number if everything goes smooth. Also leaves parser invalid.
     * @see Parser#findFloat(char)
     * @see Parser#parseFloat(char)
     * @see Parser#parseInt()
     */
    public float parseFloat() throws ParseException {
        float result;
        try {
            result = Float.parseFloat(data.substring(cursor, data.length() - 1));
        } catch (NumberFormatException e) {
            throw new ParseException("Couldn't parse number: " + data, cursor);
        }
        cursor = data.length();
        return result;
    }
    //</editor-fold>

    //<editor-fold desc="Binary methods">

    /**
     * Retrieves a flag from a bit register
     *
     * @param index index of flag to extract
     * @return value of the flag
     * @see Parser#skip(int)
     */
    @CheckResult
    public boolean getFlag(int index) {
        byte mask = (byte) (1 << index);
        return (byte) (data.charAt(cursor) & mask) == mask;
    }

    /**
     * Extracts bits from range [firstBit; firstBit + numBits] from the current character, pointed
     * by inner cursor, and shifts them right so, that firstBit becomes the zero bit
     *
     * @param firstBit
     * @param numBits
     * @return value in range 0-255
     * @see Parser#skip(int)
     */
    @CheckResult
    @IntRange(from = 0, to = 255)
    public int getBits(@IntRange(from = 0, to = 7) int firstBit,
                       @IntRange(from = 1, to = 8) int numBits) {
        return (data.charAt(cursor) & (bitMasks[numBits - 1] << firstBit)) >> firstBit;
    }
    //</editor-fold>

    //<editor-fold desc="String methods">
    @Nullable
    @CheckResult
    public String getStringIncludingEnding(@NonNull String ending){
        String result = null;
        int endingPos = data.indexOf(ending,cursor);
        if(endingPos != -1){
            int newCursorPos = endingPos + ending.length();
            result =  data.substring(cursor,newCursorPos);
            cursor = newCursorPos;
        }
        return result;
    }

    @Nullable
    @CheckResult
    public String getStringIncludingEnding(char ending){
        String result = null;
        int endingPos = data.indexOf(ending,cursor);
        if(endingPos != -1){
            int newCursorPos = endingPos + 1;
            result =  data.substring(cursor,newCursorPos);
            cursor = newCursorPos;
        }
        return result;
    }

    @Nullable
    @CheckResult
    public String getStringExcludingEnding(@NonNull String ending){
        String result = null;
        int endingPos = data.indexOf(ending,cursor);
        if(endingPos != -1){
            int newCursorPos = endingPos + ending.length();
            result =  data.substring(cursor,endingPos);
            cursor = newCursorPos;
        }
        return result;
    }

    @Nullable
    @CheckResult
    public String getStringExcludingEnding(char ending){
        String result = null;
        int endingPos = data.indexOf(ending,cursor);
        if(endingPos != -1){
            int newCursorPos = endingPos + 1;
            result =  data.substring(cursor,endingPos);
            cursor = newCursorPos;
        }
        return result;
    }
    //</editor-fold>
}
