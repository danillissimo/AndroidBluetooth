package com.bt.device.receiver;

import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bt.BTConnection;
import com.bt.BTInputStream;
import com.bt.BTUtil;

import java.util.concurrent.TimeUnit;

/**
 * A simple finite-state receiver implementing some basic behaviour.
 * For more complex behaviour, make you own extension of {@link AbstractReceiver}
 */
public abstract class FiniteStateReceiver<ListenerType extends FiniteStateReceiver.Listener> extends AbstractReceiver {
    private static final String TAG = "SimpleReceiver";

    @NonNull
    private ReceiverState state = ReceiverState.UNINITIALIZED;
    @Nullable
    private ReceiverState desiredState = ReceiverState.STOPPED;
    @NonNull
    protected final ListenerType listener;

    public FiniteStateReceiver(
            @NonNull BTConnection connection,
            @IntRange(from = 1) int bufferSize,
            @NonNull ListenerType listener) {
        super(connection, bufferSize);
        this.listener = listener;
    }

    //<editor-fold desc="State machine">
    private void waitForNewDesiredState() {
        BTUtil.WaitPredicate predicate = new BTUtil.WaitPredicate() {
            @Override
            public boolean requiredConditionIsReached() {
                return desiredState != null;
            }
        };

        synchronized (this) {
            while (!predicate.requiredConditionIsReached()) {
                Log.d(TAG, this.hashCode() + " will now wait for a new desired state");
                try {
                    BTUtil.wait(this, predicate);
                    break;
                } catch (InterruptedException e) {
                }
            }
        }
        Log.d(TAG, this.hashCode() + " was given a new desired state " + desiredState.name());
    }

    private boolean successfullyEnteredDesiredState() {
        switch (desiredState) {
            case RUNNING:
                state = ReceiverState.RUNNING;
                Log.d(TAG, this.hashCode() + " will now start serving its connection");
                listener.onStarted();
                try {
                    serveConnection();
                } catch (InterruptedException e) {
                    //Interruption signal is a stop signal
                    //Server can handle it on its own - it can ignore it to keep working, or prepare to be stopped
                    //Or can completely ignore it, and it wan't cause any damage to state machine
                    Log.d(TAG, this.hashCode() + " was interrupted while serving connection"
                            + " and passed exception to SimpleReceiver.");
                }
                Log.d(TAG, this.hashCode() + " stopped serving connection");
                //Since something stopped server, there must be some new desired state already set
                //In case there's no new state, it'll simply reboot the server
                //Though it won't happen until someone directly interrupts the thread
                return true;
            case STOPPED:
                state = ReceiverState.STOPPED;
                listener.onStopped();
                Log.d(TAG, this.hashCode() + " is stopped and will now notify about it");
                synchronized (this) {
                    desiredState = null;
                    notifyAll();
                }
                return true;
            //TERMINATED
            default:
                state = ReceiverState.TERMINATED;
                listener.onTerminated();
                Log.d(TAG, this.hashCode() + "is terminated and will now notify about it");
                synchronized (this) {
                    notifyAll();
                }
                return false;
        }
    }

    @Override
    public void run() {
        try {
            setUp();
            while (successfullyEnteredDesiredState()) {
                waitForNewDesiredState();
            }
        }catch (Throwable t){
            listener.onCrash(t);
        }
    }

    public ReceiverState startReceiver() {
        if (state == ReceiverState.UNINITIALIZED) {
            //Thread isn't running yet, gotta be safe to modify it without synchronization
            desiredState = ReceiverState.RUNNING;
            this.start();
        } else {
            synchronized (this) {
                desiredState = ReceiverState.RUNNING;
                this.notifyAll();
            }
        }
        return ReceiverState.STARTING;
    }

    /**
     * At the moment, a receiver can't be stopped without closing the socket if it's stuck on a read
     * operation. Therefore, this method should stop the receiver as soon as possible, minimizing
     * its presence until then.
     */
    public void stopReceiver() {
        Log.d(TAG, "stopping " + this.hashCode() + " and sending interruption signal to it");
        synchronized (this) {
            desiredState = ReceiverState.STOPPED;
            this.interrupt();
            state = ReceiverState.STOPPING;
        }
    }

    public void terminateReceiver() {
        Log.d(TAG, "terminating " + this.hashCode() + " and sending interruption signal to it");
        synchronized (this) {
            desiredState = ReceiverState.TERMINATED;
            this.interrupt();
            state = ReceiverState.TERMINATING;
        }
    }

    protected void innerStop() {
        Log.d(TAG, this.hashCode() + " is stopping due to internal reasons");
        synchronized (this) {
            desiredState = ReceiverState.STOPPED;
            state = ReceiverState.STOPPING;
        }
    }

    protected void innerTerminate() {
        Log.d(TAG, this.hashCode() + " is stopping due to internal reasons");
        synchronized (this) {
            desiredState = ReceiverState.TERMINATED;
            state = ReceiverState.TERMINATING;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Properties">
    @NonNull
    @CheckResult
    public ReceiverState getReceiverState() {
        return state;
    }
    //</editor-fold>

    protected void setUp() {
    }

    protected abstract void serveConnection() throws InterruptedException;

    //==============================================================================================

    //<editor-fold desc="Synchronization">
    public void awaitStop() throws InterruptedException {
        BTUtil.wait(this, new BTUtil.WaitPredicate() {
            @Override
            public boolean requiredConditionIsReached() {
                return state == ReceiverState.STOPPED;
            }
        });
    }

    public boolean awaitStop(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return BTUtil.wait(this, timeout, timeUnit, new BTUtil.WaitPredicate() {
            @Override
            public boolean requiredConditionIsReached() {
                return state == ReceiverState.STOPPED;
            }
        });
    }
    //</editor-fold>

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public interface Listener {
        void onStarted();

        void onStopped();

        void onTerminated();

        /**
         * @param t
         * @return Whether receiver should continue working.
         */
        boolean onException(Throwable t);

        void onCrash(Throwable t);
    }

    /**
     * @deprecated Should only be used as a temporal dummy during development.
     */
    @Deprecated
    public static class NullListener implements Listener {
        @Override
        public void onStarted() {

        }

        @Override
        public void onStopped() {

        }

        @Override
        public void onTerminated() {

        }

        @Override
        public boolean onException(Throwable t) {
            return true;
        }

        @Override
        public void onCrash(Throwable t) {

        }
    }
}
