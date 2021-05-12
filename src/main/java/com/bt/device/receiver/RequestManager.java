package com.bt.device.receiver;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Supposed to be embedded in a message handler.
 *
 * Для устройств с поддержкой аснихронных запросов. Класс представляет собой
 * карту(идентификатор, ответ), идентификатор уникален. Класс предоставляет ресиверу метод отправки
 * ответа, а клиенту методы синхронного и аснихронного ожидания ответа.Возможно нужна
 * доработка\проверка, на момент консервации уверенности нет.
 */
public class RequestManager {
    protected ConcurrentHashMap<Object, Response> requestMap;

    @NonNull
    public Response addRequest(@NonNull Object key) {
        Response result = new Response();
        requestMap.put(key, result);
        return result;
    }

    /**
     *
     * @param key
     * @param response
     * @return true if someone was waiting for response
     */
    @CheckResult
    public boolean pushResponse(@NonNull Object key, @NonNull Object response) {
        Response future = requestMap.get(key);
        if (future == null) {
            return false;
        }
        future.response = response;
        synchronized (future) {
            future.notifyAll();
        }
        return true;
    }

    @CheckResult
    @Nullable
    public Response peekResponse(@NonNull Object key) {
        return requestMap.get(key);
    }

    @CheckResult
    @Nullable
    public Response getResponse(@NonNull Object key){
        Response response = requestMap.get(key);
        if(response == null){
            return null;
        }
        requestMap.remove(key);
        return response;
    }

    public void releaseResponse(@NonNull Object key){
        requestMap.remove(key);
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    // TODO: 15.06.2019 Должно быть protected, но чет не сходится
    public static class Response implements Future<Object> {
        protected Object response = null;
        //Этот параметр может использоваться разработчиком интерфейса устройства по желанию
        protected boolean cancelled = false;


        public boolean cancel(){
            return cancel(false);
        }

        /**
         *
         * @param mayInterruptIfRunning not used, because not applicable
         * @return whether succeeded or not
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (cancelled) {
                return true;
            }
            if (response == null) {
                cancelled = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return response == null || cancelled;
        }

        @Override
        @CheckResult
        @NonNull
        public Object get() throws InterruptedException {
            if(response == null) {
                synchronized (this) {
                    this.wait();// TODO: 06.05.2019 ЗАменить на BTUtil.wait()
                }
            }
            return response;
        }

        @Override
        @CheckResult
        @Nullable
        public Object get(long timeout, TimeUnit unit) throws InterruptedException {
            if(response == null){
                synchronized (this) {
                    unit.timedWait(this, timeout);// TODO: 06.05.2019 Заменить на BTUtil.wait()
                }
            }
            return response;
        }
    }
}
