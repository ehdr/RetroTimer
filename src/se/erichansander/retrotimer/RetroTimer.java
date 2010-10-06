package se.erichansander.retrotimer;

import se.erichansander.retrotimer.RetroTimerView.RetroTimerListener;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.FrameLayout;

public class RetroTimer extends Activity implements RetroTimerListener {
	
	private static final String TAG = "RetroTimer";
	
	private RetroTimerView mTimer;
	
	private boolean mAlarmSet = false;
	private long mAlarmTime = 0;

	private final Handler mHandler = new Handler();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		// Post a runnable to avoid blocking the broadcast
    		mHandler.post(new Runnable() {
    			public void run() {
//  				TODO: need special handling of _TIME_CHANGED and _TIMEZONE_CHANGED eventually
    		    	mTimer.setMillisLeft(calcTimeLeft());
    			}
    		});
    	}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        FrameLayout frame = (FrameLayout) findViewById(R.id.timer_holder);
    	this.mTimer = new RetroTimerView(this);
    	this.mTimer.setRetroTimerListener(this);
    	frame.addView(this.mTimer);
    }
    
	@Override
    protected void onResume() {
        super.onResume();

        /* install intent receiver for the events we need: */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK); // the passage of time
//      TODO: are these two needed even when not running?
        filter.addAction(Intent.ACTION_TIME_CHANGED); // new system time set
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED); // system timezone changed
        registerReceiver(mIntentReceiver, filter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		/* stop updating clock when we are no longer running */
		unregisterReceiver(mIntentReceiver);
	}

	public void onTimerTempValue(long millisLeft) {
    	Log.d(TAG, "onTimerTempValue(millisLeft=" + millisLeft + ")");

    	if (mAlarmSet) cancelAlarm();

    	if (millisLeft <= 0) {
    		millisLeft = 0;
//    		TODO: vibrate
		}

    	mTimer.setMillisLeft(millisLeft);
    }


    public void onTimerSetValue (long millisLeft) {
    	Log.d(TAG, "onTimerSetValue(millisLeft=" + millisLeft + ")");

//    	TODO: require millisLeft > someSmallThreshold?
    	if (millisLeft > 0) {
//    		TODO: set new alarm
    		mAlarmTime = System.currentTimeMillis() + millisLeft;
        	mAlarmSet = true;
    	} else {
    		cancelAlarm();
    	}

    	mTimer.setMillisLeft(calcTimeLeft());
    }
    
    private long calcTimeLeft() {
    	if (mAlarmSet) {
//  		TODO: assert mAlarmTime > curr time?
    		return mAlarmTime - System.currentTimeMillis();
    	} else {
    		return 0;
    	}
    }

    private void soundAlarm() {
//    	TODO: sound alarm
//    	TODO: clean up needed?

    	mAlarmSet = false;
    	mAlarmTime = 0;
    }

    private void cancelAlarm() {
		mAlarmSet = false;
//		TODO: cancel alarm
		mAlarmTime = 0;
	}
}
