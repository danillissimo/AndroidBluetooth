package com.bt.device.receiver;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: 06.05.2019 Можно создать версию на основе ConcurrentLinkedDeque с аннотацией @TargetApi
// TODO: 06.05.2019 Кстати не оч ясно следует ли использовать конкурентность

//Can use ConcurrentLinkedDeque as parent
//But requires higher Java version and Android API
//And, probably, not that useful

/**
 *
 * @param <messageType>
 */
public final class MessageQueue<messageType> extends ConcurrentLinkedQueue<messageType> {
    @CheckResult
    @NonNull
    public messageType waitForMessage() throws InterruptedException {
        messageType result;
        for (; ; ) {
            result = this.poll();
            if (result == null) {
                synchronized (this) {
                    this.wait();
                }
                continue;
            }
            break;
        }
        return result;
    }

    @CheckResult
    @Nullable
    public messageType waitForMessage(long millis) throws InterruptedException {
        messageType result;
        result = this.poll();
        if (result == null) {
            synchronized (this) {
                this.wait(millis);
            }
            result = this.poll();
        }
        return result;
    }

    @CheckResult
    @Nullable
    public messageType waitForMessage(long millis, int nanos) throws InterruptedException {
        messageType result;
        result = this.poll();
        if (result == null) {
            synchronized (this) {
                this.wait(millis, nanos);
            }
            result = this.poll();
        }
        return result;
    }

    @CheckResult
    @Nullable
    public messageType getMessage() {
        return this.poll();
    }

    public void pushMessage(@NonNull messageType message) {
        this.add(message);
        synchronized (this) {
            this.notify();
        }
    }
}

// TODO:  Можно сделать более сложный и совершенный вариант, ориентированный на пулы потоков
//Не будить спящий поток, а прерывать, причем с уведомлением присылать данные
//Это гарантирует, что поток не проснется просто так
//Сложность в том, что нельзя просто так узнать, кто сейчас ждет
//Нативно это реализуемо только через https://docs.oracle.com/javase/7/docs/api/java/lang/management/ThreadMXBean.html
//Но там нелья просто так найти нужный поток, там придется копаться в вообще всех потоках
//Совершенно не камильфо
//Если же делать это самому, то надо заводить потокобезопасный стэк потоков
//Кадый поток, прежде чем улечься спать, должен будет отметиться в стэке
//А так же реализовывать интерфейс, позволяющий передать ему задание напрямую
//Подкидывать сюда thread pool, или что-то такое, нелогично
//Т.к. это просто очередь сообщений, которая может просто резво и горячо раздавать сообщения
//Т.е. какие-то другие реализации скорее всего будут просто тратой ресурсов
//Локи сюда не подойдут, потому что лок не отпустит ожидающего, пока его текущий держатель его самого не отпустит
//То есть совсем не та модель
//На момент написания нужды в этом нет, поэтому только заметка