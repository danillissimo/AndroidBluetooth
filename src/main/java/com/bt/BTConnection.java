package com.bt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bt.debug.InputStreamHook;
import com.bt.debug.OutputStreamHook;
import com.bt.debug.StreamHookListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Such objects are usually not reused, so reusable behaviour is not implemented.
 * Instances of this class are not safe to use until fully initialized with init() method.
 *
 * Class is considered comprehensive, and any further extensions are case specific, thus not
 * directly related with BTConnection class.
 */
public final class BTConnection implements BTConnectionObserver, BTConnectionController {
    //<editor-fold desc="Static fields">
    public final static String ACTION_BT_CONNECTING = "com.bt.BTConnection.action.CONNECTING";
    private final static String TAG = "BTConnection";
    protected static final IntentFilter connectionActionFilter = getFilterInitializer();
    //</editor-fold>

    //<editor-fold desc="Public fields">
    @NonNull
    protected EventListener listener;
    //</editor-fold>

    //<editor-fold desc="Protected fields">
    @NonNull
    protected final Context context;
    protected BroadcastReceiver receiver;
    protected AsyncConnectionTask asyncConnectionTask;

    @NonNull
    protected final UUID uuid;
    protected BluetoothSocket socket;
    @NonNull
    protected final BluetoothDevice device;
    //protected String deviceMAC; //can be obtained from device
    protected OutputStream output;
    protected InputStream input;
    protected BTConnectionState state;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public BTConnection(
            @NonNull BluetoothDevice device,
            /*One device can own multiple uuids, we can't guess the one to be used*/
            @NonNull UUID uuid,
            @NonNull Context context,
            @NonNull EventListener listener) {
        this.device = device;
        this.uuid = uuid;
        this.context = context;
        this.listener = listener;

        if (BluetoothAdapter.getDefaultAdapter() != null) {
            setState(BTConnectionState.UNINITIALIZED);
        } else {
            Log.d(TAG, "Constructor couldn't find bluetooth");
            setState(BTConnectionState.BT_NOT_FOUND);
        }
    }

    /**
     * Same as {@link BTConnection#BTConnection(BluetoothDevice, Context, EventListener)}
     * with UUID defaulted to {@link BTUtil#DefaultBTSerialBoardUUID}
     */
    public BTConnection(
            @NonNull BluetoothDevice device,
            @NonNull Context context,
            @NonNull EventListener listener
    ) {
        this(device, BTUtil.DefaultBTSerialBoardUUID, context, listener);
    }

    @NonNull
    @CheckResult
    private static IntentFilter getFilterInitializer() {
        IntentFilter result = new IntentFilter();
        result.addAction(ACTION_BT_CONNECTING);
        result.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        result.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        result.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        return result;
    }
    //</editor-fold>

    //<editor-fold desc="Initializers">
    @Override
    @CheckResult
    public boolean init(boolean createSecureConnection,
                        @Nullable Handler eventListener,
                        @NonNull BTControlMode btControlMode) {

        if (getState() != BTConnectionState.UNINITIALIZED) {
            Log.d(TAG, this.hashCode() + "is already initialized");
            return true;
        }

        if (!enableBt(btControlMode)) {
            return false;
        }

        return doInit(createSecureConnection, eventListener);
    }

    // TODO: 13.06.2019 initAsync не помешал бы с указанием потока для выполнения

    /**
     * Performs the part of the construction, that requires bluetooth to be on.
     *
     * @param createSecureConnection Controls whether the connection to be established is protected.
     * @param eventListener          Handler identifying the thread to listen connection events. If null, the
     *                               main thread of the process will be used.
     * @return Whether succeeded or not.
     */
    @Override
    @CheckResult
    public boolean init(
            boolean createSecureConnection,
            @Nullable Handler eventListener) {

        if (getState() != BTConnectionState.UNINITIALIZED) {
            Log.d(TAG, this.hashCode() + "is already initialized");
            return true;
        }

        return doInit(createSecureConnection, eventListener);
    }

    protected boolean doInit(
            boolean createSecureConnection,
            @Nullable Handler eventListener) {
        try {
            if (createSecureConnection) {
                socket = device.createRfcommSocketToServiceRecord(uuid);
            } else {
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            }
        } catch (IOException e) {
            Log.d(TAG, this.hashCode() + " couldn't create a BluetoothSocket. Maybe bluetooth" +
                    " is off?");
            return false;
        }
        Log.d(TAG, this.hashCode() + " created a" + ((createSecureConnection) ? (" secure") : ("n insecure")) +
                " BluetoothSocket");

        receiver = new BTBroadcastEventReceiver();

        context.registerReceiver(receiver, connectionActionFilter, null, eventListener);
        Log.d(TAG, this.hashCode() + " registered BTConnectionBroadcastEventReceiver");

        setState(BTConnectionState.DISCONNECTED);

        return true;
    }

    @NonNull
    @CheckResult
    protected BTBroadcastEventReceiver getBroadcastReceiver() {
        return new BTBroadcastEventReceiver();
    }
    //</editor-fold>

    //<editor-fold desc="Connection">

    /**
     * Establishes a new, or tries to recover a lost connection. <b>It takes some time, so you'll
     * probably want to use {@link BTConnection#getAsyncConnectionTask()} instead.</b>
     * Requires {@link Manifest.permission#ACCESS_COARSE_LOCATION} and/or
     * {@link Manifest.permission#ACCESS_FINE_LOCATION}.
     * It is highly recommended that you check the returned status.
     *
     * @param btControlMode Controls behaviour when bluetooth is off.
     *                      {@link BTControlMode#NONE} interrupts and returns
     *                      {@link BTConnectionState#BT_DISABLED}.
     *                      {@link BTControlMode#BY_REQUEST} requests user to turn adapter on;
     *                      interrupts and returns {@link BTConnectionState#BT_DISABLED} if the user
     *                      refuses to.
     *                      {@link BTControlMode#AS_ADMIN} silently turns adapter on.
     *                      Requires {@link Manifest.permission#BLUETOOTH_ADMIN}.
     * @return {@link BTConnectionState}
     */
    @Override
    @NonNull
    @CheckResult
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public synchronized BTConnectionState connect(@NonNull BTControlMode btControlMode) {
        {
            Log.d(TAG, this.hashCode() + " started connecting to " + device.getName());
            /*
             * Do nothing if not initialized
             * Do nothing if already connected or connecting.
             * Also do nothing if disconnecting: still connected;
             * Can't proceed if state is BT_NOT_FOUND(no bt at all) or CLOSED.
             */

            BTConnectionState currentState = getState();
            if (currentState.ordinal() > BTConnectionState.DISCONNECTED.ordinal()) {
                Log.d(TAG, this.hashCode() + " can't connect right now, state is "
                        + currentState.name());
                return currentState;
            }
            if (currentState == BTConnectionState.UNINITIALIZED) {
                Log.d(TAG, this.hashCode() + " can't connect yet, state is "
                        + currentState.name());
                return currentState;
            }
            if (currentState == BTConnectionState.BT_NOT_FOUND || currentState == BTConnectionState.CLOSED) {
                Log.d(TAG, this.hashCode() + " can't connect at all, state is "
                        + currentState.name());
                return currentState;
            }
        }


        if (!enableBt(btControlMode)) {
            return setState(BTConnectionState.BT_DISABLED);
        }


        setState(BTConnectionState.CONNECTING);
        {
            Intent intent = new Intent(ACTION_BT_CONNECTING);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            /*Receiver already set in #init*/
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        try {
            socket.connect();
            input = socket.getInputStream();
            Log.v(TAG, this.hashCode() + " assigned " + input.hashCode() + " to its input");
            output = socket.getOutputStream();
            Log.v(TAG, this.hashCode() + " assigned " + output.hashCode() + " to its output");
        } catch (IOException e) {
            {
                String debugMessage = this.hashCode() + " couldn't connect to " + device.getName();
                Log.d(TAG, debugMessage);
                Log.v(TAG, debugMessage, e);
            }
            setState(BTConnectionState.CONNECTION_ERROR);
            listener.onConnectionFailure();
            return getState();
        }

        Log.d(TAG, this.hashCode() + " successfully connected to " + device.getName());
        return getState();
    }

    /**
     * Retrieves async connection task, associated with this bluetooth connection, that can be run
     * on any thread and provides callback and synchronization tools.
     *
     * @return {@link AsyncConnectionTask}
     */
    @Override
    public AsyncConnectionTask getAsyncConnectionTask() {
        if (asyncConnectionTask == null) {
            asyncConnectionTask = new AsyncConnectionTask();
        }
        return asyncConnectionTask;
    }

    /**
     * Closes the connection, ending its lifecycle.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        setState(BTConnectionState.CLOSED);
        socket.close();
        Log.d(TAG, this.hashCode() + " is now closed");
        // FIXME: 03.06.2019 Сказано, что это удалит ВСЕ фильтры, связанные с этим ресивером.
        //  Не ясно, подразумевается ли что это затронет все ресивиры, завязаные на те же
        //  фильтры, или нет?
        context.unregisterReceiver(receiver);
        receiver = null;
        Log.d(TAG, this.hashCode() + " unregistered its receiver");
    }
    //</editor-fold>

    //<editor-fold desc="Properties">

    @Nullable
    @CheckResult
    public EventListener getListener(){
        if (!(listener instanceof EmptyEventListener)){
            return listener;
        }else{
            return null;
        }
    }

    public void setListener(@Nullable EventListener listener){
        if (listener != null){
            this.listener = listener;
        }else{
            this.listener = new EmptyEventListener();
        }
    }

    /**
     * @return {@link BTConnectionState}
     * @see BTConnection#isValidForIO()
     */
    @Override
    @NonNull
    @CheckResult
    public BTConnectionState getState() {
        return state;
    }

    @Override
    @CheckResult
    public boolean isConnected() {
        return getState().isConnected();
    }

    /**
     * @return Returns true, if connection was established at least once, even if signal is lost at
     * the moment, and is not closed yet.
     */
    @Override
    @CheckResult
    public boolean isSetUp() {
        return input != null;
    }

    /**
     * Convenience function, can sometimes improve readability.
     */
    @Override
    @CheckResult
    public boolean isValidForIO() {
        return isConnected();
    }

    @Override
    @CheckResult
    public boolean isInitialized() {
        return getState() != BTConnectionState.UNINITIALIZED;
    }

    @Override
    @NonNull
    @CheckResult
    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    @NonNull
    @CheckResult
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return Null if {@link BTConnection#init(boolean, Handler, BTControlMode)} was not called yet.
     */
    @CheckResult
    public BluetoothSocket getSocket() {
        return socket;
    }

    @CheckResult
    public OutputStream getOutputStream() {
        return output;
    }

    @CheckResult
    public InputStream getInputStream() {
        return input;
    }
    //</editor-fold>

    //<editor-fold desc="Util">
    protected boolean enableBt(BTControlMode btControlMode) {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (!bt.isEnabled()) {
            switch (btControlMode) {
                case NONE: {
                    Log.d(TAG, this.hashCode() + " can't turn bt on in "
                            + BTControlMode.NONE.name() + " mode");
                    return false;
                }
                case BY_REQUEST: {
                    return BTUtil.requestUserToTurnBtOn(context);
                }
                case AS_ADMIN: {
                    boolean result = bt.enable();
                    Log.d(TAG, this.hashCode() + " attempted to turn bluetooth on as admin" +
                            " with result " + result);
                    return result;
                }
            }
        }
        return true;
    }

    protected BTConnectionState setState(BTConnectionState status) {
        state = status;
        return status;
    }

    @Override
    protected void finalize() throws Throwable {
        if (receiver != null) {
            close();
            Log.w(TAG, "Detected that an instance of " + TAG + " was not properly destroyed." +
                    " Always call 'disconnect()' for connections that are no longer needed to " +
                    "prevent security breaches");
        }
    }

    //</editor-fold>

    //<editor-fold desc="Synchronization">
    //I guess one is supposed to know if he's using sync features or not
    //And should be punished if he doesn't

    //<editor-fold desc="awaitStateChange"
//    @Override
//    public void awaitStateChange() throws InterruptedException {
//        //if (receiver instanceof SynchronizingBTBroadcastEventReceiver) {
//            ((SynchronizingBTBroadcastEventReceiver) receiver).awaitStateChange();
//        //}
//    }
//
//    @Override
//    public void awaitStateChange(
//            @IntRange(from = 1) long timeout,
//            @NonNull TimeUnit unit)
//            throws InterruptedException {
//        //if (receiver instanceof SynchronizingBTBroadcastEventReceiver) {
//            ((SynchronizingBTBroadcastEventReceiver) receiver).awaitStateChange(timeout, unit);
//        //}
//    }
//
//    @Override
//    public void awaitStateChange(
//            @IntRange(from = 1) long timeout)
//            throws InterruptedException {
//        //if (receiver instanceof SynchronizingBTBroadcastEventReceiver) {
//            ((SynchronizingBTBroadcastEventReceiver) receiver).awaitStateChange(timeout);
//        //}
//    }
//
//    //</editor-fold desc="awaitStateChange"
//    //<editor-fold desc="awaitState"
//
//    @Override
//    public void awaitState(@NonNull final BTConnectionState state) throws InterruptedException {
//        //if (receiver instanceof SynchronizingBTBroadcastEventReceiver) {
//            ((SynchronizingBTBroadcastEventReceiver) receiver).awaitState(state);
//        //}
//    }
//
//    @Override
//    public void awaitState(@NonNull final BTConnectionState state,
//                           @IntRange(from = 1) long timeout,
//                           @NonNull TimeUnit unit) throws InterruptedException {
//        //if (receiver instanceof SynchronizingBTBroadcastEventReceiver) {
//            ((SynchronizingBTBroadcastEventReceiver) receiver).awaitState(state, timeout, unit);
//        //}
//    }
//
//    @Override
//    public void awaitState(@NonNull final BTConnectionState state,
//                           @IntRange(from = 1) long timeout) throws InterruptedException {
//        //if (receiver instanceof SynchronizingBTBroadcastEventReceiver) {
//            ((SynchronizingBTBroadcastEventReceiver) receiver).awaitState(state, timeout);
//        //}
//    }

    //</editor-fold desc="awaitState"

    //</editor-fold desc="Synchronization">

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public enum BTConnectionState {
        BT_NOT_FOUND,
        /*
         * If allowBtControl == true, but this state is returned, then you probably lack
         * Manifest.permission#BLUETOOTH_ADMIN
         */
        BT_DISABLED,
        UNINITIALIZED,
        /*
         * Connection can only be created via a valid instance of BluetoothDevice, which is only
         * valid when provided with a valid MAC
         */
        //INVALID_MAC,
        /*Almost certainly indicates that bluetooth was off*/
        ERROR_CREATING_SOCKET,
        /**
         * See {@link BluetoothSocket#connect()} for possible errors
         */
        CONNECTION_ERROR,
        CLOSED,
        DISCONNECTED,
        DISCONNECTING,
        CONNECTING,
        CONNECTED;

        /**
         * @see BTConnection#isConnected()
         */
        @CheckResult
        public boolean isConnected() {
            return this == CONNECTED;
        }

        /**
         * Convenience function, can sometimes improve readability.
         *
         * @see BTConnection#isValidForIO()
         */
        @CheckResult
        public boolean isValidForIO() {
            return this.isConnected();
        }

        @CheckResult
        public boolean connectionSucceeded() {
            return this == CONNECTING || this == CONNECTED;
        }
    }

    public enum BTControlMode {
        NONE,
        BY_REQUEST,
        AS_ADMIN,
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public interface EventListener {
        void onConnecting();

        void onConnected();

        void onConnectionFailure();

        void onDisconnectRequested();

        void onDisconnected();
    }

    public static class EmptyEventListener implements EventListener{
        @Override
        public void onConnecting() {

        }

        @Override
        public void onConnected() {

        }

        @Override
        public void onConnectionFailure() {

        }

        @Override
        public void onDisconnectRequested() {

        }

        @Override
        public void onDisconnected() {

        }
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @Override
    public InputHook getInputHook() {
        return new InputHook();
    }

    @Override
    public OutputHook getOutputHook() {
        return new OutputHook();
    }

    public class InputHook {
        protected InputStream originalInput;

        public void inject(@NonNull StreamHookListener listener) {
            originalInput = BTConnection.this.input;
            BTConnection.this.input = new InputStreamHook(originalInput, listener);
        }

        public void eject() {
            BTConnection.this.input = originalInput;
        }
    }

    public class OutputHook {
        protected OutputStream originalOutput;

        public void inject(@NonNull StreamHookListener listener) {
            originalOutput = BTConnection.this.output;
            BTConnection.this.output = new OutputStreamHook(originalOutput, listener);
        }

        public void eject() {
            BTConnection.this.output = originalOutput;
        }
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Executed here and now by connecting thread once done (successfully or not)
     */
    public interface OnAsyncConnectedCallback {
        void onAsyncConnectionFinished(@NonNull BTConnectionState connectionState);
    }

    public class AsyncConnectionTask implements Runnable {
        @NonNull
        public BTControlMode btControlMode = BTControlMode.NONE;
        @Nullable
        public OnAsyncConnectedCallback callback = null;

        protected BTConnectionState state;
        protected final BTUtil.WaitPredicate predicate = new BTUtil.WaitPredicate() {
            @Override
            public boolean requiredConditionIsReached() {
                return state != null;
            }
        };

        protected AsyncConnectionTask() {
        }

        @Override
        @RequiresPermission(Manifest.permission.BLUETOOTH)
        public void run() {
            Log.d(TAG, this.hashCode() + " started asynchronous connection to " + device.getName());

            state = null;
            state = BTConnection.this.connect(btControlMode);
            synchronized (this) {
                this.notifyAll();
            }
            if (callback != null) {
                Log.d(TAG, BTConnection.this.hashCode() + " finished asynchronous BT connection" +
                        " and will now perform a callback");
                callback.onAsyncConnectionFinished(state);
            }
        }

        /**
         * @return Null if connection continues, otherwise a {@link BTConnectionState}.
         */
        @Nullable
        @CheckResult
        public BTConnectionState getResult() {
            return state;
        }

        @NonNull
        @CheckResult
        public BTConnectionState waitForResult() throws InterruptedException {
            BTUtil.wait(this, predicate);
            return state;
        }

        @Nullable
        @CheckResult
        public BTConnectionState waitForResult(
                @IntRange(from = 1) long timeout,
                @NonNull TimeUnit unit)
                throws InterruptedException {
            BTUtil.wait(this, timeout, unit, predicate);
            return state;
        }
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    protected class BTBroadcastEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (d.getAddress().equals(device.getAddress())) {
                /*if d!=null, then action!=null too*/
                String action = intent.getAction();
                Log.d(TAG, BTConnection.this.hashCode() + ": device '" + device.getName() +
                        "' state changed to " + action);
                switch (action) {
                    case ACTION_BT_CONNECTING: {
                        /*State already set by #connect*/
                        listener.onConnecting();
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_CONNECTED: {
                        setState(BTConnectionState.CONNECTED);
                        listener.onConnected();
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED: {
                        setState(BTConnectionState.DISCONNECTING);
                        listener.onDisconnectRequested();
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                        setState(BTConnectionState.DISCONNECTED);
                        listener.onDisconnected();
                        break;
                    }
                }
            }
        }
    }

    protected class SynchronizingBTBroadcastEventReceiver extends BTBroadcastEventReceiver {
        public final Object onConnecting = new Object();
        public final Object onConnected = new Object();
        public final Object onDisconnecting = new Object();
        public final Object onDisconnected = new Object();

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (d.getAddress().equals(device.getAddress())) {
                /*if d!=null, then action!=null too*/
                String action = intent.getAction();
                Log.d(TAG, BTConnection.this.hashCode() + ": device '" + device.getName() +
                        "' state changed to " + action);
                switch (action) {
                    case ACTION_BT_CONNECTING: {
                        /*State already set by #connect*/
                        doNotify(onConnecting);
                        listener.onConnecting();
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_CONNECTED: {
                        setState(BTConnectionState.CONNECTED);
                        doNotify(onConnected);
                        listener.onConnected();
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED: {
                        setState(BTConnectionState.DISCONNECTING);
                        doNotify(onDisconnecting);
                        listener.onDisconnectRequested();
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                        setState(BTConnectionState.DISCONNECTED);
                        doNotify(onDisconnected);
                        listener.onDisconnected();
                        break;
                    }
                }
            }
        }

        protected void doNotify(@NonNull final Object syncPoint) {
            synchronized (syncPoint) {
                syncPoint.notifyAll();
            }
            synchronized (this) {
                this.notifyAll();
            }
        }

        //<editor-fold desc="awaitStateChange"
        public void awaitStateChange() throws InterruptedException {
            final BTConnectionState oldState = getState();
            BTUtil.wait(this, new BTUtil.WaitPredicate() {
                @Override
                public boolean requiredConditionIsReached() {
                    return getState() != oldState;
                }
            });
        }

        public void awaitStateChange(
                @IntRange(from = 1) long timeout,
                @NonNull TimeUnit unit)
                throws InterruptedException {
            final BTConnectionState oldState = getState();
            BTUtil.wait(this, timeout, unit, new BTUtil.WaitPredicate() {
                @Override
                public boolean requiredConditionIsReached() {
                    return getState() != oldState;
                }
            });
        }

        public void awaitStateChange(
                @IntRange(from = 1) long timeout)
                throws InterruptedException {
            final BTConnectionState oldState = getState();
            BTUtil.wait(this, timeout, new BTUtil.WaitPredicate() {
                @Override
                public boolean requiredConditionIsReached() {
                    return getState() != oldState;
                }
            });
        }

        //</editor-fold desc="awaitStateChange"
        //<editor-fold desc="awaitState"

        public void awaitState(@NonNull final BTConnectionState state) throws InterruptedException {
            Object syncPoint;
            switch (state) {
                case CONNECTING:
                    syncPoint = onConnecting;
                    break;
                case CONNECTED:
                    syncPoint = onConnected;
                    break;
                case DISCONNECTING:
                    syncPoint = onDisconnecting;
                    break;
                case DISCONNECTED:
                    syncPoint = onDisconnected;
                    break;
                default:
                    return;
            }
            BTUtil.wait(syncPoint, new BTUtil.WaitPredicate() {
                @Override
                public boolean requiredConditionIsReached() {
                    return getState() == state;
                }
            });
        }

        public void awaitState(@NonNull final BTConnectionState state,
                               @IntRange(from = 1) long timeout,
                               @NonNull TimeUnit unit) throws InterruptedException {
            Object syncPoint;
            switch (state) {
                case CONNECTING:
                    syncPoint = onConnecting;
                    break;
                case CONNECTED:
                    syncPoint = onConnected;
                    break;
                case DISCONNECTING:
                    syncPoint = onDisconnecting;
                    break;
                case DISCONNECTED:
                    syncPoint = onDisconnected;
                    break;
                default:
                    return;
            }
            BTUtil.wait(syncPoint, timeout, unit, new BTUtil.WaitPredicate() {
                @Override
                public boolean requiredConditionIsReached() {
                    return getState() == state;
                }
            });
        }

        public void awaitState(@NonNull final BTConnectionState state,
                               @IntRange(from = 1) long timeout) throws InterruptedException {
            Object syncPoint;
            switch (state) {
                case CONNECTING:
                    syncPoint = onConnecting;
                    break;
                case CONNECTED:
                    syncPoint = onConnected;
                    break;
                case DISCONNECTING:
                    syncPoint = onDisconnecting;
                    break;
                case DISCONNECTED:
                    syncPoint = onDisconnected;
                    break;
                default:
                    return;
            }
            BTUtil.wait(syncPoint, timeout, new BTUtil.WaitPredicate() {
                @Override
                public boolean requiredConditionIsReached() {
                    return getState() == state;
                }
            });
        }

        //</editor-fold desc="awaitState"
    }
}