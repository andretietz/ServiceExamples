package de.unicate.serviceexamples;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import de.unicate.serviceexamples.callback.IService;
import de.unicate.serviceexamples.callback.IServiceCallback;
import de.unicate.serviceexamples.databinding.ActivityServiceBinding;
import de.unicate.serviceexamples.services.BaseService;
import de.unicate.serviceexamples.services.LocalService;
import de.unicate.serviceexamples.services.RemoteService;

/**
 * This is a simple activity that provides some ui elements
 * for the user to start and stop services
 */
public class ServiceActivity extends Activity {

	/**
	 * Debug Tag
	 **/
	private static final String TAG = ServiceActivity.class.getSimpleName();

	// store an intent for a local service
	private Intent mLocalServiceIntent;
	// stores an intent for a remote service
	private Intent mRemoteServiceIntent;
	// a callback from the service, defined in aidl
	private IService mService;


	private ActivityServiceBinding binding;
	/**
	 * this Handler receives Messages, that are sent from the callback of the service
	 */
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// do smth with the data received from the service
			Toast.makeText(ServiceActivity.this, "Time received: " + msg.getData().getLong("time"), Toast.LENGTH_SHORT).show();
		}
	};
	/**
	 * This is the {@link android.content.ServiceConnection} with which
	 * we bound our activity to the service
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		// this bundle is used for synchronizing the asynchron
		// callback from the activity with the main thread of the activity
		// to be able to make ui updates
		private Bundle bundle = new Bundle();

		/**
		 * gets called when the service is bound to the activity
		 */
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.e(TAG, "Service connected");
			// get the methods of the service (defined in IService.aidl)
			mService = IService.Stub.asInterface(service);
			try {
				// register a callback in the service
				// so that the service can send callbacks to the activity
				mService.registerCallback(new IServiceCallback.Stub() {
					/**
					 * If there is a call coming from the service
					 *
					 * @param time
					 * @throws RemoteException
					 */
					@Override
					public void receiveInformation(long time) throws RemoteException {
						// put the data from the service into a bundle
						bundle.putLong("time", time);
						// put the bundle into a message
						Message msg = Message.obtain();
						msg.setData(bundle);
						// and send the message to a handler
						mHandler.sendMessage(msg);
					}
				});
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			updateUI();
		}

		/**
		 * Gets called when the service disconnects unexcpected.
		 * THIS METHOD DOES NOT GET CALLED WHEN THE SERVICE UNBINDS!
		 */
		public void onServiceDisconnected(ComponentName className) {
			Log.e(TAG, "Service disconnected");
			mService = null;
		}
	};

	/**
	 * If the Activity stops
	 */
	@Override
	protected void onStop() {
		super.onStop();
		// check if the service is bound
		// this is the case, when the callback is not null
		if (null != mService) {
			// and unbind the service
			unbindService(mConnection);
		}
	}

	/**
	 * If the configuration changes, we store if the user was bound
	 * to a service or not.
	 *
	 * @param outState Bundle where we save the state
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("isBound", null != mService);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = DataBindingUtil.setContentView(this, R.layout.activity_service);
		// create the service intents
		mLocalServiceIntent = new Intent(this, LocalService.class);
		mRemoteServiceIntent = new Intent(this, RemoteService.class);
		// show the pid and tid of the activity
		binding.textViewPID.setText(String.format("%d", android.os.Process.myPid()));
		binding.textViewTID.setText(String.format("%d", Thread.currentThread().getId()));

		// create the content of the Spinner
		binding.spinnerChooser.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"Local Service", "Remote Service"}));
		// if the Spinner changes, update the ui
		binding.spinnerChooser.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				updateUI();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				updateUI();
			}
		});
		// if start button clicked
		binding.buttonStart.setOnClickListener(v -> {
			// get if the user choose local or remote service
			boolean isRemote = binding.spinnerChooser.getSelectedItemId() != 0;
			// choose the correct intent to start
			Intent intent = !isRemote ? mLocalServiceIntent : mRemoteServiceIntent;
			// if the service is running already
			if (BaseService.isRunning(ServiceActivity.this, isRemote ? RemoteService.class : LocalService.class)) {
				// stop the service
				stopService(intent);
			} else {
				// if not, start the service
				startService(intent);
			}
			// and update the ui
			updateUI();
		});
		// if the user clicked the bind button
		binding.buttonBind.setOnClickListener(v -> {
			// get if the user choose local or remote service
			boolean isRemote = binding.spinnerChooser.getSelectedItemId() != 0;
			// choose the correct intent to start
			Intent intent = !isRemote ? mLocalServiceIntent : mRemoteServiceIntent;
			// if there is a callback, the service is bound already
			if (null != mService) {
				// so unbind the service
				unbindService(mConnection);
				// and reset the callback, which cannot be used anymore
				mService = null;
			} else {
				// if not, bind the service
				bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
			}
			// and update the ui
			updateUI();
		});

		/**
		 * if the screen was rotated, check if the service was bound
		 * if so, then bind it again
		 */
		if (null != savedInstanceState && savedInstanceState.containsKey("isBound") && savedInstanceState.getBoolean("isBound")) {
			// choose the correct intent to bind to
			boolean isRemote = binding.spinnerChooser.getSelectedItemId() != 0;
			Intent intent = !isRemote ? mLocalServiceIntent : mRemoteServiceIntent;
			// bind to the service
			bindService(intent, mConnection, BIND_AUTO_CREATE);
		}
		updateUI();
	}

	private void updateUI() {
		// checks if the selected service is remote or local
		boolean isRemote = binding.spinnerChooser.getSelectedItemId() != 0;
		// if the service is running
		if (BaseService.isRunning(this, isRemote ? RemoteService.class : LocalService.class)) {
			// the buttons shows the option to stop the service
			binding.buttonStart.setText("Stop Service");
		} else {
			// the button shows the option to start the service
			binding.buttonStart.setText("Start Service");
		}
		// if the service is not bound
		if (null == mService) {
			// show the option to bind the service
			binding.buttonBind.setText("Bind to the Service");
			// no pid or tid of the service can be shown
			binding.textViewServicePID.setText("-");
			binding.textViewServiceTID.setText("-");
		} else {
			// show the option to unbind the service
			binding.buttonBind.setText("Unbind from the Service");
			try {
				// update pid and tid of the service
				binding.textViewServicePID.setText(String.format("%d", mService.getPID()));
				binding.textViewServiceTID.setText(String.format("%d", mService.getTID()));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}


}
