package se.erichansander.retrotimer;

import se.erichansander.retrotimer.TimerSetView.TimerSetListener;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.widget.FrameLayout;

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
    		// Post a runnable to avoid blocking the broadcast
    		mHandler.post(new Runnable() {
    			public void run() {
//  				TODO: need special handling of _TIME_CHANGED and 
//    					  _TIMEZONE_CHANGED eventually
    		    	mTimer.setMillisLeft(calcTimeLeft());
    			}
    		});
    	}
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        FrameLayout frame = (FrameLayout) findViewById(R.id.timer_holder);
    	mTimer = new TimerSetView(this);
    	mTimer.setTimerSetListener(this);
    	frame.addView(this.mTimer);
    }

	@Override
    protected void onStart() {
        super.onStart();

        /* install intent receiver for the events we need to update timer view */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK); // the passage of time
        filter.addAction(Intent.ACTION_TIME_CHANGED); // new system time set
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED); // system timezone changed
        registerReceiver(mTickReceiver, filter);

    	mTimer.setMillisLeft(calcTimeLeft());
    	if (calcTimeLeft() == 0) {
    		mTempAtZero = true;
    	}
	}

	@Override
	protected void onStop() {
		super.onStop();

		/* stop updating clock when we are no longer running */
		unregisterReceiver(mTickReceiver);
	}

	public void onTimerTempValue(long millisLeft) {
//    	Log.d(DEBUG_TAG, "onTimerTempValue(millisLeft=" + millisLeft + ")");

    	if (mPrefs.getBoolean(RetroTimer.PREF_ALARM_SET, true)) {
    		cancelAlarm();
    	}

    	if (millisLeft <= 0) {
    		millisLeft = 0;
    		
    		// Only vibrate if we are not already at zero
    		if (!mTempAtZero) {
    			mVibrator.vibrate(mZeroVibrateDurationMillis);
    		}
    		mTempAtZero = true;
		} else {
			mTempAtZero = false;
		}

    	mTimer.setMillisLeft(millisLeft);
    }


    public void onTimerSetValue (long millisLeft) {
    	Log.d(DEBUG_TAG, "onTimerSetValue(millisLeft=" + millisLeft + ")");

    	if (millisLeft > 0) {
    		setAlarm(millisLeft);
    	} else {
    		cancelAlarm();
    	}

    	mTimer.setMillisLeft(calcTimeLeft());
    }
    
    private long calcTimeLeft() {
    	if (mPrefs.getBoolean(RetroTimer.PREF_ALARM_SET, false)) {
//  		TODO: assert mAlarmTime > curr time?
    		return mPrefs.getLong(RetroTimer.PREF_ALARM_TIME, 0)
    				- System.currentTimeMillis();
    	} else {
    		return 0;
    	}
    }
    
    private void setAlarm(long millisLeft) {
    	SharedPreferences.Editor editor = mPrefs.edit();
    	AlarmManager am = (AlarmManager)
        	getSystemService(Context.ALARM_SERVICE);
        
    	long alarmTime = System.currentTimeMillis() + millisLeft;

        Intent intent = new Intent(RetroTimer.ALARM_TRIGGER_ACTION);
        intent.putExtra(RetroTimer.ALARM_TIME_EXTRA, alarmTime);
        PendingIntent sender = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        editor.putLong(RetroTimer.PREF_ALARM_TIME, alarmTime);
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);

//      TODO: show timer status in status bar

    	editor.putBoolean(RetroTimer.PREF_ALARM_SET, true);
    	editor.commit();
    }

    private void cancelAlarm() {
    	SharedPreferences.Editor editor = mPrefs.edit();

		AlarmManager am =
				(AlarmManager) getSystemService(Context.ALARM_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(
				this, 0, new Intent(RetroTimer.ALARM_TRIGGER_ACTION),
				PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(sender);

        editor.putLong(RetroTimer.PREF_ALARM_TIME, 0);
		editor.putBoolean(RetroTimer.PREF_ALARM_SET, false);
    	editor.commit();
    }
}
