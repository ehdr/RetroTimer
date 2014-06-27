/*
 * Copyright (C) 2010-2014  Eric Hansander
 *
 *  This file is part of Retro Timer.
 *
 *  Retro Timer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Retro Timer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Retro Timer.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.erichansander.retrotimer;

import se.erichansander.retrotimer.TimerSetView.TimerSetListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/** Main activity for setting the timer */
public class TimerSet extends Activity implements TimerSetListener {

    // Interval between redraws of the timer to update time left
    private static final int UPDATE_INTERVAL_MILLIS = 60 * 1000;

    // How long to vibrate when timer dial goes to zero
    private final long mSetAlarmDelayMillis = 750;

    // How long to vibrate when timer dial goes to zero
    private final long mZeroVibrateDurationMillis = 200;

    // Handles to stuff we need to interact with
    private SharedPreferences mPrefs;
    private Vibrator mVibrator;
    private TimerSetView mTimer;
    private TextView mVolumeZeroWarning;

    // True when the user has interacted with the app since the activity was
    // resumed
    private boolean mUserInteracted = false;
    // True when timer dial is at zero
    private boolean mTempAtZero = false;
    /*
     * Time left (in millis) that the alarm will be set for, when the
     * delaySetAlarm is triggered
     */
    private long mTimeLeftToBeSet = 0;

    // Handler for updating the TimerView with time remaining
    private final Handler mHandler = new Handler();
    private final Runnable runTimeUpdate = new Runnable() {
        public void run() {
            mHandler.postDelayed(this, UPDATE_INTERVAL_MILLIS);
            updateTimeLeft();
        }
    };
    private final Runnable delaySetAlarm = new Runnable() {
        public void run() {
            if (mTimeLeftToBeSet > 0) {
                RetroTimer.setAlarmDelayed(TimerSet.this, mTimeLeftToBeSet);
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

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        RetroTimer.initAlarm(this);

        setContentView(R.layout.timer_set);

        mTimer = (TimerSetView) findViewById(R.id.timer_set_view);
        mTimer.setTimerSetListener(this);

        mVolumeZeroWarning = (TextView) findViewById(R.id.volume_zero_warning);
        mVolumeZeroWarning.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(TimerSet.this, TimerSettings.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        crashIfMissedAlarm(this);

        if (RetroTimer.getMillisLeftToAlarm(this) > 0) {
            startUpdatingTimeLeft();
        } else {
            mTempAtZero = true;
            updateTimeLeft();
        }
    }

    @Override
    public void onUserInteraction() {
        if (!mUserInteracted) {
            /*
             * Make sure to dismiss the alarm, if one should happen to be
             * playing. Only on the first user interaction after each resume
             * though, that is enough.
             * 
             * The user could have escaped from the TimerAlert activity e.g. by
             * pressing Home, which is fine, but chaos will ensue if they are
             * allowed to set a new countdown with an alarm running!
             */
            Intent intent = new Intent(RetroTimer.ALARM_DISMISS_ACTION);
            sendBroadcast(intent);
            mUserInteracted = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUserInteracted = false;

        /* Only show the license notice if we haven't already */
        if (!mPrefs.getBoolean(RetroTimer.PREF_HAVE_SHOWN_LICENSE, false)) {
            showLicenseDialog();

            // remember that we have showed the license
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putBoolean(RetroTimer.PREF_HAVE_SHOWN_LICENSE, true);
            ed.commit();
        }

        // Display warning of ringing is on but alarm volume is zero
        boolean ring = mPrefs.getBoolean(RetroTimer.PREF_RING_ON_ALARM, true);
        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (ring && am.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
            mVolumeZeroWarning.setVisibility(TextView.VISIBLE);
        } else {
            mVolumeZeroWarning.setVisibility(TextView.INVISIBLE);
        }

        // Update the time remaining to alarm
        mTimer.invalidate();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // stop updating the display
        mHandler.removeCallbacks(runTimeUpdate);
    }

    private void showLicenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.license_notice_title)
                .setView(View.inflate(this, R.layout.license_notice, null))
                .setCancelable(false)
                .setPositiveButton(R.string.license_notice_button, null);
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.show_settings:
            startActivity(new Intent(this, TimerSettings.class));
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void startUpdatingTimeLeft() {
        long alarmTime = RetroTimer.getAlarmTime(this);
        long delay;

        /*
         * calculate how long after a minute tick the alarm will go off
         */
        delay = alarmTime % 60000;
        /*
         * subtract a small amount of time, so the view is updated slightly
         * before the alarm triggers
         */
        delay -= 500;
        /*  */
        delay -= System.currentTimeMillis() % 60000;
        /* handle the corner case */
        if (delay < 0) {
            /*
             * if now is within the first 500 millis of a minute, delay will be
             * negative, so we wait a minute before updating
             */
            delay += 60000;
        }

        mHandler.postDelayed(runTimeUpdate, delay);
        updateTimeLeft();
    }

    /**
     * Will throw a RuntimeException if time left to alarm is too negative
     */
    private void crashIfMissedAlarm(Context c) {
        /*
         * Must leave some wiggle room for the alarm to be delayed for various
         * reasons, and since the basic resolution of this timer is one minute,
         * lets crash if the alarm is more than 30 seconds late.
         */
        long millisLeft = RetroTimer.getMillisLeftToAlarm(c);
        if (millisLeft < -(30 * 1000)) {
            /*
             * Should never happen - something has gone very wrong (some process
             * killed by task killer? some unknown race condition?)
             */
            RetroTimer.handleFatalError(c);

            TinyTracelog.trace("e1 " + System.currentTimeMillis() + ","
                    + millisLeft);
            throw new RuntimeException(
                    "alarm never triggered (getMillisLeftToAlarm is negative)\n"
                            + TinyTracelog.getTracelog());
        }
    }

    private void updateTimeLeft() {
        long millisLeft = RetroTimer.getMillisLeftToAlarm(this);

        crashIfMissedAlarm(this);

        updateTimeLeft(millisLeft);
    }

    private void updateTimeLeft(long millisLeft) {
        mTimer.setMillisLeft(millisLeft);
    }

    // Callback functions for the TimerSetView class

    /**
     * Displays the new (temporary) time left and vibrates if the dial is turned
     * all the way down to zero.
     * 
     * This method is called when the user is in the process of turning the
     * dial, before releasing it (which sets a new alarm).
     */
    public void onTimerTempValue(long millisLeft) {
        if (mPrefs.getBoolean(RetroTimer.PREF_ALARM_SET, true)) {
            RetroTimer.cancelAlarm(this);
        }

        mHandler.removeCallbacks(runTimeUpdate);
        mHandler.removeCallbacks(delaySetAlarm);

        if (millisLeft <= 0) {
            millisLeft = 0;

            /*
             * Only vibrate if vibration is turned on, and we are not already at
             * zero
             */
            if (mPrefs.getBoolean(RetroTimer.PREF_VIBRATE_ON_ALARM, true)
                    && !mTempAtZero) {
                mVibrator.vibrate(mZeroVibrateDurationMillis);
            }
            mTempAtZero = true;
        } else {
            mTempAtZero = false;
        }

        updateTimeLeft(millisLeft);
    }

    /**
     * Actually sets the new timer value
     * 
     * If at zero, cancels the alarm instead.
     */
    public void onTimerSetValue(long millisLeft) {
        if (millisLeft > 0) {
            mTimeLeftToBeSet = millisLeft;
            mHandler.removeCallbacks(delaySetAlarm);
            mHandler.postDelayed(delaySetAlarm, mSetAlarmDelayMillis);
        }
    }
}
