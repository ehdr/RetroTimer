/*
 * Copyright (C) 2010-2012  Eric Hansander
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

import se.erichansander.retrotimer.TimerAlertView.TimerAlertListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

/**
 * Activity to show while alarm is playing
 * 
 * Touching it will dismiss the alarm.
 */
public class TimerAlert extends Activity implements TimerAlertListener {

    private TimerAlertView mTimer;

    /*
     * Receiver to handle ALARM_SILENCE_ACTION and ALARM_DISMISS_ACTION intents,
     * by closing this activity
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setToShowOverLockScreen(getWindow());

        setContentView(R.layout.timer_alert);

        mTimer = (TimerAlertView) findViewById(R.id.timer_alert_view);
        mTimer.setTimerAlertListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RetroTimer.ALARM_DISMISS_ACTION);
        filter.addAction(RetroTimer.ALARM_SILENCE_ACTION);
        registerReceiver(mReceiver, filter);
    }

    /*
     * Show the activity over the lock screen when the alarm triggers while the
     * phone is locked.
     * 
     * Some of these parameters where introduced with API level 5 and 8, but
     * cause no harm on earlier versions.
     */
    @TargetApi(8)
    private void setToShowOverLockScreen(Window win) {
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
         * When this activity is shown, we know for sure that the alarm has
         * triggered, i.e. there is zero time left to alarm
         */
        mTimer.setMillisLeft(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        switch (event.getKeyCode()) {
        // Volume keys and camera keys dismiss the alarm
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_CAMERA:
        case KeyEvent.KEYCODE_FOCUS:
            if (event.getAction() == KeyEvent.ACTION_UP) {
                // TODO: should this count as dismiss or silence?
                dismissAlarm();
                break;
            }

        default:
            break;
        }
        return super.dispatchKeyEvent(event);
    }

    public void onAlertDismissed() {
        dismissAlarm();
    }

    private void dismissAlarm() {
        // Broadcast ALARM_DISMISS_ACTION to kill the TimerKlaxon
        Intent intent = new Intent(RetroTimer.ALARM_DISMISS_ACTION);
        sendBroadcast(intent);

        finish();
    }
}
