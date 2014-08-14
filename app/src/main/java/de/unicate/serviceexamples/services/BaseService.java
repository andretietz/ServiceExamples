package de.unicate.serviceexamples.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import de.unicate.serviceexamples.callback.IService;
import de.unicate.serviceexamples.callback.IServiceCallback;


/**
 * This is the Service we start and bind to
 *
 * Created by andre.tietz on 13.08.2014.
 */
public abstract class BaseService extends Service {
    /**
     * This is the callback, with which we can send data to the activity
     */
    private IServiceCallback mCallback;

    /**
     * This is a Thread in which we do some working simulation in the Service
     */
    private Thread mUpdateThread;

    /**
     * The binder is of Type IService (defined in IService.aidl) and it
     * contains methods for the activity to call
     */
    private final IService.Stub mBinder = new IService.Stub() {
        /**
         * @return the Process ID
         * @throws RemoteException
         */
        @Override
        public int getPID() throws RemoteException {
            return android.os.Process.myPid();
        }

        /**
         * @return the Thread ID
         * @throws RemoteException
         */
        @Override
        public long getTID() throws RemoteException {
            return Thread.currentThread().getId();
        }

        /**
         * Registeres a callback from the activity. With this callback the
         * service sends data to the activity
         * @param callback Callback to register
         * @throws RemoteException
         */
        @Override
        public void registerCallback(IServiceCallback callback) throws RemoteException {
            // set the callback
            mCallback = callback;
            // start a thread that sends data through the callback, back to the activity
            mUpdateThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isRunning = true;
                    // run as long as "isRunning" is true
                    // it will be false, after someone called {@link Thread#interrupt()}
                    while(isRunning) {
                        try {
                            // if there's still a callback to call
                            if(null != mCallback) {
                                // send data (in this example, its the current time) to the activity
                                mCallback.receiveInformation(System.nanoTime());
                            }
                            // wait fot 10 seconds
                            Thread.sleep(10000);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            // if got interrupted, end up the thread loop
                            isRunning = false;
                            e.printStackTrace();
                        }
                    }
                }
            });
            // start the thread
            mUpdateThread.start();
        }
    };


    @Override
    public void onCreate() {
        Log.e(getTag(), "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.e(getTag(), "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(getTag(), "onStartCommand");
        if(startId == Service.START_NOT_STICKY) {
            Log.e(getTag(), "Start not sticky");
        } else if(startId == Service.START_STICKY) {
            Log.e(getTag(), "Start sticky");
        } else if(startId == Service.START_REDELIVER_INTENT) {
            Log.e(getTag(), "Start redeliver Content");
        }
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     *
     * @param intent thats coming from the startService Intent
     * @return The Binder we defined in this Service
     *
     * @see {@link IService.Stub}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Called when the activity unbinds
     * @param intent Intent which is supposed to be unbound
     * @return
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(getTag(), "unbinding");
        // delete the callback, because we cannot use it anymore
        mCallback = null;
        // stop the thread
        mUpdateThread.interrupt();
        // returning true means that onUnbind is called again, after a Service
        // rebinds
        return true;
    }

    protected abstract String getTag();

    /**
     * This is a pretty good solution for checking if a Service is running or not.
     * I found this on stackoverflow:
     * http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android
     *
     * I changed it so that it can be called as static
     * @param context Context which you need in this method
     * @param clazz Class of the Service shich you want to check if its running
     * @return <code>true</code> if the service is running, <code>false</code> if not
     */
    public static boolean isRunning(Context context, Class<?> clazz) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (clazz.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
