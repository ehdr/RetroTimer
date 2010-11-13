/* 
 * Copyright (C) 2010  Eric Hansander
 *
 *  This file is part of RetroTimer.
 *
 *  RetroTimer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RetroTimer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RetroTimer.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.erichansander.retrotimer;

import se.erichansander.retrotimer.TimerSetView.TimerSetListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/** Main activity for setting the timer */
public class TimerSet extends Activity implements TimerSetListener {

	private static final String DEBUG_TAG = "TimerSet";

	// Identifier for the dialog displaying the license notice
	private static final int DIALOG_LICENSE_ID = 1;

	// Interval between redraws of the timer to update time left
	private static final int UPDATE_INTERVAL_MILLIS = 60*1000;

    // How long to vibrate when timer dial goes to zero
	private final long mSetAlarmDelayMillis = 750;

	// How long to vibrate when timer dial goes to zero
    private final long mZeroVibrateDurationMillis = 200;

    // Handles to stuff we need to interact with
    private SharedPreferences mPrefs;
    private Vibrator mVibrator;
	private TimerSetView mTimer;

	// True when timer dial is at zero
	private boolean mTempAtZero = false;
	/* Time left (in millis) that the alarm will be set for, when
	 * the delaySetAlarm is triggered */
	private long mTimeLeftToBeSet = 0;

	// Handler for updating the TimerView with time remaining
	private final Handler mHandler = new Handler();
	private final Runnable runTimeUpdate = new Runnable() {
		public void run() {
			mHandler.postDelayed(this, UPDATE_INTERVAL_MILLIS);
//	    	if (RetroTimer.DEBUG) {
//	    		Elog.d(DEBUG_TAG, "runTimeUpdate at " + 
//	    				DateFormat.format("hh:mm:ss", 
//	    						System.currentTimeMillis()));
//	    	}
			updateTimeLeft();
		}
	};
	private final Runnable delaySetAlarm = new Runnable() {
		public void run() {
//	    	if (RetroTimer.DEBUG) {
//	    		Elog.d(DEBUG_TAG, "delaySetAlarm at " + 
//	    				DateFormat.format("hh:mm:ss", 
//	    						System.currentTimeMillis()));
//	    	}

	    	if (mTimeLeftToBeSet > 0) {
	    		RetroTimer.setAlarmDelayed(
	    				TimerSet.this, 
	    				mTimeLeftToBeSet);
	    		startUpdatingTimeLeft();
	    	} else {
	    		RetroTimer.cancelAlarm(TimerSet.this);
	    		updateTimeLeft();
	    	}
		}
	};

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

        if (RetroTimer.getMillisLeftToAlarm(this) > 0) {
        	startUpdatingTimeLeft();
        } else {
    		mTempAtZero = true;
    		updateTimeLeft();
    	}
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

        /* Only show the license notice if we haven't already */
        if (!mPrefs.getBoolean(RetroTimer.PREF_HAVE_SHOWN_LICENSE,
        		false)) {
        	showDialog(DIALOG_LICENSE_ID);

        	// remember that we have showed the license
			SharedPreferences.Editor ed = mPrefs.edit();
			ed.putBoolean(RetroTimer.PREF_HAVE_SHOWN_LICENSE, true);
			ed.commit();
        }
	}

	@Override
	protected void onStop() {
		super.onStop();

		// stop updating the display
		mHandler.removeCallbacks(runTimeUpdate);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
		/* Display a dialog showing the license notice */
		case DIALOG_LICENSE_ID:
			// use a custom View to get clickable links in the dialog
			View view = View.inflate(this, R.layout.license_notice, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.license_notice_title)
					.setView(view)
					.setCancelable(false)
					.setPositiveButton(
							R.string.license_notice_button,
							null);
			dialog = builder.create();
			break;

		default:
			Elog.w(DEBUG_TAG, "Unknown dialog ID in onCreateDialog()");
			return null;
		}
		
		return dialog;
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
		switch (item.getItemId()) {
		  case R.id.ring_on_alarm:
			  toggleRingOnAlarm();
			  return true;

		  case R.id.vibrate_on_alarm:
			  toggleVibrateOnAlarm();
			  return true;
			  
		  case R.id.donate:
			  Intent viewIntent = 
				  new Intent(Intent.ACTION_VIEW, 
						  Uri.parse(getString(R.string.donate_url)));
			  startActivity(viewIntent);  
			  return true;

		  default:
			  Elog.w(DEBUG_TAG, "Unknown selection from options menu!");
			  return super.onOptionsItemSelected(item);
		}
	}

	private void toggleRingOnAlarm() {
		SharedPreferences.Editor ed = mPrefs.edit();
		boolean tmp =
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
	}

	private void toggleVibrateOnAlarm() {
		SharedPreferences.Editor ed = mPrefs.edit();
		boolean tmp =
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
	}
	
	private void startUpdatingTimeLeft() {
		long alarmTime = RetroTimer.getAlarmTime(this);
		long delay;

//    	if (RetroTimer.DEBUG) {
//    		Elog.v(DEBUG_TAG, "startUpdatingTimeLeft() at " + 
//    				DateFormat.format("hh:mm:ss", System.currentTimeMillis()));
//    	}

		/* calculate how long after a minute tick the alarm will 
		 * go off */
		delay = alarmTime % 60000;
		/* subtract a small amount of time, so the view is updated
		 * slightly before the alarm triggers */
		delay -= 500;
		/*  */
		delay -= System.currentTimeMillis() % 60000;
		/* handle the corner case */
		if (delay < 0) {
			/* if now is within the first 500 millis of a minute,
			 * delay will be negative, so we wait a minute before 
			 * updating */ 
			delay += 60000;
		}
		
//    	if (RetroTimer.DEBUG) {
//    		Elog.v(DEBUG_TAG,
//    				" posting first runTimeUpdate with delay " + delay);
//    	}
		mHandler.postDelayed(runTimeUpdate, delay);
		updateTimeLeft();
	}

	private void updateTimeLeft() {
		long millisLeft = RetroTimer.getMillisLeftToAlarm(this);
		updateTimeLeft(millisLeft);
	}

	private void updateTimeLeft(long millisLeft) {
		mTimer.setMillisLeft(millisLeft);
	}


	// Callback functions for the TimerSetView class
	
	/** Displays the new (temporary) time left and vibrates if the dial is 
	 * turned all the way down to zero.
	 * 
	 * This method is called when the user is in the process of turning
	 * the dial, before releasing it (which sets a new alarm).
	 */
	public void onTimerTempValue(long millisLeft) {
//    	Elog.v(DEBUG_TAG, "onTimerTempValue(millisLeft=" + millisLeft + ")");

    	if (mPrefs.getBoolean(RetroTimer.PREF_ALARM_SET, true)) {
    		RetroTimer.cancelAlarm(this);
    	}
    	
    	mHandler.removeCallbacks(runTimeUpdate);
    	mHandler.removeCallbacks(delaySetAlarm);

    	if (millisLeft <= 0) {
    		millisLeft = 0;
    		
    		/* Only vibrate if vibration is turned on, and we are not 
    		 * already at zero */
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
//    	if (RetroTimer.DEBUG) {
//    		Elog.d(DEBUG_TAG, "onTimerSetValue(millisLeft=" + millisLeft +
//    				") at " + 
//    				DateFormat.format("hh:mm:ss", 
//    						System.currentTimeMillis()));
//    	}
    	
    	if (millisLeft > 0) {
    		mTimeLeftToBeSet = millisLeft;
    		mHandler.removeCallbacks(delaySetAlarm);
    		mHandler.postDelayed(
    				delaySetAlarm, 
    				mSetAlarmDelayMillis);
    	}
    }
}
