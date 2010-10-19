package se.erichansander.retrotimer;

import se.erichansander.retrotimer.TimerSetView.TimerSetListener;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/** Main activity for setting the timer */
public class TimerSet extends Activity implements TimerSetListener {

	private static final String DEBUG_TAG = "TimerSet";

    // How long to vibrate when timer dial goes to zero
    private final long mZeroVibrateDurationMillis = 200;

    // Handles to stuff we need to interact with
    private SharedPreferences mPrefs;
    private Vibrator mVibrator;
	private TimerSetView mTimer;

	// True when timer dial has is at zero
	private boolean mTempAtZero = false;

	// Handler for updating the TimerView with time remaining
	private final Handler mHandler = new Handler();
    private final BroadcastReceiver mTickReceiver = new BroadcastReceiver() {    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		/* This runs on the minute, but we delay the actual update
    		 * to the same offset from the whole minute as the alarm time
    		 */
    		long minOffset = 0;

        	if (mPrefs.getBoolean(RetroTimer.PREF_ALARM_SET, false)) {
        		minOffset = 
        				mPrefs.getLong(RetroTimer.PREF_ALARM_TIME, 0) % 60000
        				- 1000;
        		if (minOffset < 0) {
        			minOffset += 60000;
        			
        			// Update right away as well, to get the start value right
                	mHandler.post(new Runnable() {
                		public void run() {
                			updateTimeLeft();
                		}
                	});
        		}
        	}
        	
    		// Post a runnable to avoid blocking the broadcast
        	mHandler.postDelayed(new Runnable() {
    			public void run() {
    		    	updateTimeLeft();
    			}
        	}, minOffset);
    	}
    };

	private void updateTimeLeft() {
		long millisLeft = RetroTimer.getMillisLeftToAlarm(this);
		updateTimeLeft(millisLeft);
	}

	private void updateTimeLeft(long millisLeft) {
		mTimer.setMillisLeft(millisLeft);
	}

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        RetroTimer.initAlarm(this);

        setContentView(R.layout.timer_set);

    	mTimer = (TimerSetView) findViewById(R.id.timer_set_view);
    	mTimer.setTimerSetListener(this);
    }

	@Override
    protected void onStart() {
        super.onStart();

        /* install intent receiver for the events we need to update 
         * timer view */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK); // the passage of time
//      TODO: need special handling of _TIME_CHANGED and 
//		  _TIMEZONE_CHANGED eventually
        filter.addAction(Intent.ACTION_TIME_CHANGED); // new system time set
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED); // system timezone changed
        registerReceiver(mTickReceiver, filter);

        if (RetroTimer.getMillisLeftToAlarm(this) <= 0) {
    		mTempAtZero = true;
    	}
        updateTimeLeft();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		/* Make sure to dismiss the alarm, if one should happen to be
		 * playing.
		 * 
		 * The user could have escaped from the TimerAlert activity
		 * e.g. by pressing Home, which is fine, but chaos will ensue
		 * if they are allowed to enter TimerSet with an alarm running!
		 */
        Intent intent = new Intent(RetroTimer.ALARM_DISMISS_ACTION);
        sendBroadcast(intent);
	}

	@Override
	protected void onStop() {
		super.onStop();

		/* stop updating clock when we are no longer running */
		unregisterReceiver(mTickReceiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    return true;
	}

	/** Sets the correct titles in the options menu, depending on the
	 * current values of PREF_RING_ON_ALARM and PREF_VIBRATE_ON_ALARM.
	 */
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		MenuItem item;
		
		item = menu.findItem(R.id.ring_on_alarm);
		if (mPrefs.getBoolean(RetroTimer.PREF_RING_ON_ALARM, true)) {
			item.setTitle(R.string.ring_on_alarm_turn_off);
		} else {
			item.setTitle(R.string.ring_on_alarm_turn_on);			
		}

		item = menu.findItem(R.id.vibrate_on_alarm);
		if (mPrefs.getBoolean(RetroTimer.PREF_VIBRATE_ON_ALARM, true)) {
			item.setTitle(R.string.vibrate_on_alarm_turn_off);
		} else {
			item.setTitle(R.string.vibrate_on_alarm_turn_on);			
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean tmp;
		SharedPreferences.Editor ed = mPrefs.edit();
		switch (item.getItemId()) {
		  case R.id.ring_on_alarm:
			  tmp =
				  mPrefs.getBoolean(RetroTimer.PREF_RING_ON_ALARM, true);
			  ed.putBoolean(RetroTimer.PREF_RING_ON_ALARM, !tmp);
			  ed.commit();
	
			  if (mPrefs.getBoolean(RetroTimer.PREF_RING_ON_ALARM, true)) {
				  Toast.makeText(this, 
						  getResources().getString(
								  R.string.ring_on_alarm_turned_on), 
						  Toast.LENGTH_SHORT).show();
			  } else {
				  Toast.makeText(this, 
						  getResources().getString(
								  R.string.ring_on_alarm_turned_off), 
						  Toast.LENGTH_SHORT).show();
			  }
			  return true;

		  case R.id.vibrate_on_alarm:
			  tmp =
				  mPrefs.getBoolean(RetroTimer.PREF_VIBRATE_ON_ALARM, true);
			  ed.putBoolean(RetroTimer.PREF_VIBRATE_ON_ALARM, !tmp);
			  ed.commit();

			  if (mPrefs.getBoolean(RetroTimer.PREF_VIBRATE_ON_ALARM, true)) {
				  Toast.makeText(this, 
						  getResources().getString(
								  R.string.vibrate_on_alarm_turned_on), 
						  Toast.LENGTH_SHORT).show();
			  } else {
				  Toast.makeText(this, 
						  getResources().getString(
								  R.string.vibrate_on_alarm_turned_off), 
						  Toast.LENGTH_SHORT).show();
			  }
			  return true;
			  
		  default:
			  Log.w(DEBUG_TAG, "Unknown selection from options menu!");
			  return super.onOptionsItemSelected(item);
		}
	}
	

	// Callback functions for the TimerSetView class
	
	/** Displays the new (temporary) time left and vibrates if the dial is 
	 * turned all the way down to zero.
	 * 
	 * This method is called when the user is in the process of turning
	 * the dial, before releasing it (which sets it).
	 */
	public void onTimerTempValue(long millisLeft) {
//    	Log.d(DEBUG_TAG, "onTimerTempValue(millisLeft=" + millisLeft + ")");

    	if (mPrefs.getBoolean(RetroTimer.PREF_ALARM_SET, true)) {
    		RetroTimer.cancelAlarm(this);
    	}

    	if (millisLeft <= 0) {
    		millisLeft = 0;
    		
    		// Only vibrate if vibration is turned on, and 
    		// we are not already at zero
    		if (mPrefs.getBoolean(RetroTimer.PREF_VIBRATE_ON_ALARM, true) &&
    				!mTempAtZero) {
    			mVibrator.vibrate(mZeroVibrateDurationMillis);
    		}
    		mTempAtZero = true;
		} else {
			mTempAtZero = false;
		}

    	updateTimeLeft(millisLeft);
    }

	/** Actually sets the new timer value
	 * 
	 * If at zero, cancels the alarm instead. 
	 */
    public void onTimerSetValue (long millisLeft) {
    	Log.d(DEBUG_TAG, "onTimerSetValue(millisLeft=" + millisLeft + ")");

    	if (millisLeft > 0) {
    		RetroTimer.setAlarmDelayed(this, millisLeft);
    	} else {
    		RetroTimer.cancelAlarm(this);
    	}

    	updateTimeLeft();
    }
}
