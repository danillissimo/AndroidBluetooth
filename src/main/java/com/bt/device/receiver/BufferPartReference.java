package com.bt.device.receiver;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

// TODO: 12.07.2019 внимательно пересмотреть нет ли лучшей реализации\альтренативы BufferPartReference
//  а еще похоже что это надо выносить в утилиты и делать более общим
//  по сути это javax.swing.text.Segment, который здесь почему-то недоступен, вероятно слишком низкая
//  версия явы


/**
 * Смотри где используется, вроде как должно работать быстрее чем то, что предлагает ява
 */
public final class BufferPartReference implements CharSequence {
    protected byte[] buffer;
    protected int firstIndex, lastIndex;

    public BufferPartReference(
            @NonNull byte[] buffer,
            @IntRange(from = 0) int firstIndex,
            @IntRange(from = 0) int lastIndex) {
        if ((firstIndex < 0 || firstIndex >= buffer.length) || (lastIndex < 0 || lastIndex >= buffer.length)) {
            throw new IndexOutOfBoundsException("Indices must belong to [0; buffer.bytes.length) interval");
        }
        if (firstIndex > lastIndex) {
            throw new IllegalArgumentException("FirstIndex can't be greater than LastIndex");
        }

        this.buffer = buffer;
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
    }

    /**
     * Behaviour is undefined if given index is bigger then {@code lastIndex}
     * @param index
     * @return
     */
    @Override
    public char charAt(int index) {
        //if (firstIndex + index > lastIndex) {
        //    throw new IndexOutOfBoundsException("Index must belong to [0; sequence.length()) interval");
        //}
        return (char) buffer[firstIndex + index];
    }

    @Override
    public int length() {
        return lastIndex - firstIndex + 1;
    }

    @Override
    public @NonNull
    CharSequence subSequence(@IntRange(from = 0) int start, @IntRange(from = 0) int end) {
        //TODO BufferPartReference - внутренний механизм узкого назначения, все проверки можно вынести в те места, в которых они реально нужны
        if ((firstIndex + start > lastIndex) || (firstIndex + end > lastIndex) || (start < 0) || (end < 0)) {
            throw new IndexOutOfBoundsException("'start' and 'end' arguments must belong to [0; originalSequence.length()) interval");
        }
        if (start > end) {
            throw new IllegalArgumentException("'end' argument must be greater or equal than 'start' argument");
        }

        return new BufferPartReference(buffer, firstIndex + start, firstIndex + end);
    }
}
