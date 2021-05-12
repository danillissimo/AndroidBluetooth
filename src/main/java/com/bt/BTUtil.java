package com.bt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class BTUtil {
    public final static UUID DefaultBTSerialBoardUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static String TAG = "BTUtil";

    //==============================================================================================

    /**
     * Подвязывает ресивер событий состояния адаптера, который самовыпиливается при получении
     * нужного события, предварительно отметившись в возвращенной структуре об успехе. Следует
     * вызывать перед командами, изменяющими состояние блюпупа. В общем должны использовааться
     * готовые обертки, а не эта функция напрямую.
     *
     * @param context
     * @param desiredState
     * @return
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    @CheckResult
    @NonNull
    public static BTAdapterStateOccurrence registerNextAdapterStateOccurrence(
            @NonNull Context context,
            final int desiredState) {
        final BTAdapterStateOccurrence result = new BTAdapterStateOccurrence();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (newState == desiredState) {
                    {
                        String debugMessage = this.hashCode() + "(bluetooth state listener) received "
                                + "desired state signal '" + desiredState + "'";
                        Log.d(TAG, debugMessage);
                        Log.v(TAG, debugMessage, new Throwable());
                    }
                    result.occurred = true;
                    synchronized (result) {
                        result.notifyAll();
                    }
                    context.unregisterReceiver(this);
                }
            }
        };

        context.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        Log.d(TAG, receiver.hashCode() + "(bluetooth state listener) is registered "
                + " and is waiting for '" + desiredState + "' event");
        Log.v(TAG, receiver.hashCode() + "(bluetooth state listener) is registered "
                + " and is waiting for '" + desiredState + "' event at", new Throwable());
        return result;
    }

    public static void bringAdapterIntoTurningOnState(@NonNull Context context)
            throws InterruptedException {
        BTAdapterStateOccurrence turningOnState =
                registerNextAdapterStateOccurrence(context, BluetoothAdapter.STATE_TURNING_ON);
        BluetoothAdapter.getDefaultAdapter().enable();
        turningOnState.await();
    }

    public static void bringAdapterIntoTurnedOnState(@NonNull Context context)
            throws InterruptedException {
        BTAdapterStateOccurrence turnedOnState =
                registerNextAdapterStateOccurrence(context, BluetoothAdapter.STATE_ON);
        BluetoothAdapter.getDefaultAdapter().enable();
        turnedOnState.await();
    }

    public static void bringAdapterIntoTurningOffState(@NonNull Context context)
            throws InterruptedException {
        BTAdapterStateOccurrence turningOffState =
                registerNextAdapterStateOccurrence(context, BluetoothAdapter.STATE_TURNING_OFF);
        BluetoothAdapter.getDefaultAdapter().enable();
        turningOffState.await();
    }

    public static void bringAdapterIntoTurnedOffState(@NonNull Context context)
            throws InterruptedException {
        BTAdapterStateOccurrence turnedOffState =
                registerNextAdapterStateOccurrence(context, BluetoothAdapter.STATE_OFF);
        BluetoothAdapter.getDefaultAdapter().enable();
        turnedOffState.await();
    }

    //==============================================================================================

    /**
     * @param context
     * @return True if succeeds.
     */
    public static boolean requestUserToTurnBtOn(Context context) {
        /*
         * https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#ACTION_REQUEST_ENABLE
         * It says: "This system activity will return once Bluetooth has completed
         * turning on, or the user has decided not to turn Bluetooth on."
         * So don't make any additional checks
         */
        {
            String debugMessage = "Requesting user to turn bluetooth on";
            Log.d(TAG, debugMessage);
            Log.v(TAG, debugMessage, new Throwable());
        }
        context.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        /*
         * Can't use Activity#onActivityResult(int, int, Intent) to retrieve state, but bt state
         * must be already updated after previous line of code.
         */
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    //==============================================================================================

    /*
     * Spurious wakeups are real
     * https://github.com/hazelcast/hazelcast/pull/3207/files
     */

    /*
     * Loop in a block, synchronized on target object
     * Thus, if predicate works on the same object, it'll ensure nothing happens after check
     *  and before wait
     * If it doesn't, that's no bad until predicate is very heavy to execute, what's very unlikely
     *  and would result in synchronized object being locked most time
     */

    public static boolean wait(
            @NonNull Object synchronizationObject,
            @IntRange(from = 1) long timeout,
            @NonNull WaitPredicate predicate
    ) throws InterruptedException {
        return wait(synchronizationObject, timeout, TimeUnit.MILLISECONDS, predicate);
    }

    /**
     * Spurious wake-up aware wait
     *
     * @param synchronizationObject
     * @param timeout
     * @param unit
     * @param predicate             Executed in a block, synchronized on {@code synchronizationObject}.
     * @return Whether succeeded or not.
     * @throws InterruptedException
     */
    public static boolean wait(
            @NonNull Object synchronizationObject,
            @IntRange(from = 1) long timeout,
            @NonNull TimeUnit unit,
            @NonNull WaitPredicate predicate)
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        long start = System.nanoTime();
        synchronized (synchronizationObject) {
            for (; ; ) {
                if (predicate.requiredConditionIsReached()) {
                    return true;
                } else {
                    //Используем таймер максимальной точности, за отсутствием high precision таймера
                    //будет использоваться обычный миллисекундный
                    //А спим миллисекундами, потому что ява все равно не дает указать больше одной
                    //миллисекунды наносекундами
                    synchronizationObject.wait(TimeUnit.NANOSECONDS.toMillis(nanos));
                    if (System.nanoTime() - start >= nanos) {
                        break;
                    }
                }
            }
            return predicate.requiredConditionIsReached();
        }
    }

    /**
     * Same as {@link #wait(Object, long, TimeUnit, WaitPredicate)}, but waits forever
     *
     * @param synchronizationObject
     * @param predicate             Executed in a block, synchronized on {@code synchronizationObject}.
     * @throws InterruptedException
     */
    public static void wait(
            @NonNull Object synchronizationObject,
            @NonNull WaitPredicate predicate)
            throws InterruptedException {
        synchronized (synchronizationObject) {
            for (; ; ) {
                if (predicate.requiredConditionIsReached()) {
                    return;
                } else {
                    synchronizationObject.wait();
                }
            }
        }
    }

    //==============================================================================================

    /**
     * Можно вынести в отдельный класс в принципе
     * <p>
     * Don't forget you may require some atomic operations here.
     */
    public interface WaitPredicate {
        boolean requiredConditionIsReached();
    }

    //==============================================================================================

    /**
     * В рамках предполагаемого применения структура достаточна и не нуждается в расширении, хотя
     * оное, безусловно, возможно (но не нужно)
     */
    public final static class BTAdapterStateOccurrence {
        private boolean occurred = false;
        private final WaitPredicate predicate = new WaitPredicate() {
            @Override
            public boolean requiredConditionIsReached() {
                return occurred;
            }
        };

        private BTAdapterStateOccurrence() {
        }

        public boolean waitMillis(@IntRange(from = 1) long timeout) throws InterruptedException {
            return this.wait(timeout, TimeUnit.MILLISECONDS);
        }

        public boolean wait(
                @IntRange(from = 1) long timeout,
                @NonNull TimeUnit unit)
                throws InterruptedException {
            return BTUtil.wait(this, timeout, unit, predicate);
        }

        public void await() throws InterruptedException {
            BTUtil.wait(this, predicate);
        }

        public boolean occurred() {
            return occurred;
        }
    }

    //==============================================================================================

    public static class TaskSequence {
        private static final String TAG = "TaskSequence";
        protected Handler handler;
        protected ArrayList<Task> tasks = new ArrayList<>();
        // TODO: 14.06.2019 Тут должен бы быть список, и, соответственно указатель на его член
        //  Но с этим вроде какие-то проблемы, а потому пока и так пойдет, с пивом конечно же
        protected int currentTask = 0;

        public TaskSequence(
                @NonNull Handler handler,
                @IntRange(from = 1) int numTasks
        ) {
            Log.v(TAG, "constructing a sequence with handler " + handler);
            this.handler = handler;
            tasks.ensureCapacity(numTasks);
        }

        public void start() {
            Task task = tasks.get(0);
            Log.v(TAG, "starting sequence " + this.hashCode() + " on " + handler.hashCode()
                    + " at " + task.hashCode());
            handler.post(tasks.get(0));
        }

        public abstract class Task implements Runnable {
            public Task() {
                tasks.add(this);
                Log.v(TAG, "added a new task " + this.hashCode() + " to sequence "
                        + TaskSequence.this.hashCode());
            }

            protected final void restartTask() {
                handler.post(this);
            }

            protected final void endTask() {
                currentTask++;
                if (currentTask != tasks.size()) {
                    handler.post(tasks.get(currentTask));
                }
            }

            protected final void interruptSequence() {
                /*Yep.Do nothing.*/
            }
        }
    }

    //==============================================================================================

    public abstract static class DelayedTask implements Runnable {
        private static final String TAG = "TaskSequence";
        private Handler handler;
        private long executionTime;
        private boolean posted = false;

        public DelayedTask(Handler handler) {
            this.handler = handler;
        }

        public Handler getHandler() {
            return handler;
        }

        public void setHandler(Handler handler) {
            this.handler = handler;
        }

        /**
         * Set exact execution time
         *
         * @param time
         */
        public void setExecutionTime(long time) {
            executionTime = time;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        /**
         * Set execution time to be delayed from now on given value
         *
         * @param delay
         */
        public void setDelay(long delay) {
            executionTime = SystemClock.uptimeMillis() + delay;
        }

        /**
         * Delays execution for given amount
         *
         * @param delay
         */
        public void delay(long delay) {
            executionTime += delay;
        }

        public void schedule() {
            if (!posted) {
                posted = true;
                handler.postAtTime(this, executionTime);
            }
        }

        public void scheduleAtTime(long time) {
            setExecutionTime(time);
            schedule();
        }

        public void scheduleWithDelay(long delay) {
            setDelay(delay);
            schedule();
        }

        public void cancel() {
            executionTime = Long.MIN_VALUE;
        }

        @Override
        public void run() {
            if (executionTime == Long.MIN_VALUE) return;
            if (System.currentTimeMillis() >= executionTime) {
                posted = false;
                delayedActions();
            } else {
                handler.postAtTime(this, executionTime);
            }
        }

        public abstract void delayedActions();
    }

    //==============================================================================================

    public static class SimpleEventBus {

        public interface SimpleEventListener {
            void onEvent(Object event);
        }

        //++++++++++++++++++++++++++++++++++++

        protected ArrayList<SimpleEventListener> listeners;

        public SimpleEventBus(@IntRange(from = 1) int expectedNumListeners) {
            listeners = new ArrayList<>(expectedNumListeners);
        }

        public void subscribe(@NonNull SimpleEventListener listener) {
            listeners.add(listener);
        }

        public void unsubscribe(@NonNull SimpleEventListener listener) {
            listeners.remove(listener);
        }

        public void broadcast(Object event) {
            Iterator<SimpleEventListener> i = listeners.iterator();
            while (i.hasNext()) {
                i.next().onEvent(event);
            }
        }
    }

    //==============================================================================================

    public static void runIfNotNull(@Nullable Runnable r) {
        if (r != null) {
            r.run();
        }
    }

    public static void showShortToast(
            @NonNull final Activity activity,
            @NonNull final String text) {
        showToast(activity,text,Toast.LENGTH_SHORT);
    }

    public static void showLongToast(final Activity activity, final String text){
        showToast(activity,text,Toast.LENGTH_LONG);
    }

    public static void showToast(
            @NonNull final Activity activity,
            @NonNull final String text,
            final int duration) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, duration).show();
            }
        });
    }

    //==============================================================================================

    //public int static byte[] CRC16() {}
}