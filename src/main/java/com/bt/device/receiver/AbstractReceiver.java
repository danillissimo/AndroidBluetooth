package com.bt.device.receiver;

import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.bt.BTConnection;
import com.bt.BTInputStream;

import java.util.concurrent.TimeUnit;

/**
 * An abstract receiver bound by no certain model.
 * Generally useless and should be removed.
 * Kept just for Maximal Flexibility And Abstraction.
 * ¯\_(ツ)_/¯
 */
public abstract class AbstractReceiver extends Thread {
    private static final String TAG = "AbstractReceiver";

//    protected ExceptionHandler exceptionHandler;
    protected BTInputStream input;

    /*
     * No control methods here. All control is done through owning BTController.
     */

    public AbstractReceiver(
            @NonNull BTConnection connection,
            @IntRange(from = 1) int bufferSize
//            @NonNull ExceptionHandler exceptionHandler
    ) {
        if(!connection.isSetUp()){
            Log.e(TAG, "connection, provided to constructor, is not set up!");
            throw new IllegalStateException("Provided connection is not set up!");
        }
        input = new BTInputStream(connection.getInputStream(), bufferSize);
//        this.exceptionHandler = exceptionHandler;
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public interface ExceptionHandler{
        /**
         * @param t
         * @return Whether receiver should continue working.
         */
        @CheckResult
        boolean onException(Throwable t);
    }
}