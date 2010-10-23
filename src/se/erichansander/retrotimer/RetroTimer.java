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

//TODO: show timer icon in status bar when alarm is active?
//TODO: add support for flinging the timer dial
//TODO: handle _TIME_CHANGED and _TIMEZONE_CHANGED(?)
//TODO: handle different screen orientations
//TODO: handle different screen sizes

//TODO: animate TimerAlert?
//TODO: handle setting alarm with the trackball


import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

/**
 * Main application class. Handles state shared between activities,
 * and holds shared constants etc.
 */
public class RetroTimer extends Application {
	
	public static final String DEBUG_TAG = "RetroTimer";
	public static final boolean DEBUG = false;

	/**
	 * When broadcasted, will cause the alarm to sound/vibrate and
	 * issue a notification, that will dismiss the alarm when clicked.
	 */
	public static final String ALARM_TRIGGER_ACTION =
			"se.erichansander.retrotimer.ALARM_TRIGGER";
	/**
	 * When sent to the TimerKlaxon service, will sound alarm and/or
	 * vibrate device
	 */
	public static final String ALARM_PLAY_ACTION =
			"se.erichansander.retrotimer.ALARM_PLAY";
	/**
	 * When broadcasted, will silence alarm and put up a notification 
	 * stating that the timer has been silenced.
	 */
	public static final String ALARM_SILENCE_ACTION =
			"se.erichansander.retrotimer.ALARM_SILENCE";
	/** 
	 * When broadcasted, will silence alarm and remove any notifications.
	 */
	public static final String ALARM_DISMISS_ACTION =
			"se.erichansander.retrotimer.ALARM_DISMISS";


	/** For passing the alarm time through an intent */
	public static final String ALARM_TIME_EXTRA = "intent.extra.alarmtime";
	

	/** Is true when an alarm is set */
	public static final String PREF_ALARM_SET = 
			"prefs.alarm_set";
	/** Absolute time when alarm should go off, in millis since epoch */ 
	public static final String PREF_ALARM_TIME = 
			"prefs.alarm_time";
	/** Is true if alert should play audio */
	public static final String PREF_RING_ON_ALARM =
			"prefs.ring_on_alarm";
	/** Is true if alert should vibrate device */
	public static final String PREF_VIBRATE_ON_ALARM =
			"prefs.vibrate_on_alarm";
	/** Is true if the licensing dialog has been shown (when the app
	 * was started for the first time). */
	public static final String PREF_HAVE_SHOWN_LICENSE =
			"prefs.have_shown_license";

	
	/**
	 * Initializes the app state, and initializes the AlarmManager if 
	 * any alarms are pending from before the device was rebooted.
	 * 
	 * Should be called at device boot and at application start (in 
	 * case the app has been killed).
	 */
	public static void initAlarm(Context context) {
        SharedPreferences prefs = 
        	PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(PREF_ALARM_SET, false) ||
        		prefs.getLong(PREF_ALARM_TIME, 0) > 0) {
        	if (getMillisLeftToAlarm(context) > 1000) {
        		/* If there is time left until the alarm should trigger,
            	 * we register it again with the AlarmManager */
        		setAlarmAt(context, prefs.getLong(PREF_ALARM_TIME, 0));
        	} else {
        		/* Otherwise, we do some clean-up */
            	SharedPreferences.Editor ed = prefs.edit();
                ed.putLong(RetroTimer.PREF_ALARM_TIME, 0);
        		ed.putBoolean(RetroTimer.PREF_ALARM_SET, false);
        		ed.commit();
        	}        	
        }
	}
	
	/**
	 * Convenience method for setting an alarm to trigger in
	 * millisLeft millis.
	 */
    public static void setAlarmDelayed(Context context, long millisLeft) {
    	setAlarmAt(context, System.currentTimeMillis() + millisLeft);    	
    }
    
    /**
     * Sets an alarm at absolute time alarmTime (in millis from epoch)
     */
    public static void setAlarmAt(Context context, long alarmTime) {
    	Elog.d(DEBUG_TAG, "RetroTimer alarm set for " +
    			DateFormat.format("hh:mm:ss", alarmTime) +
    			" (now is " +
    			DateFormat.format("hh:mm:ss", System.currentTimeMillis()) +
    			")");

        SharedPreferences prefs = 
        	PreferenceManager.getDefaultSharedPreferences(context);
    	SharedPreferences.Editor ed = prefs.edit();
    	
    	AlarmManager am = (AlarmManager)
        	context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(RetroTimer.ALARM_TRIGGER_ACTION);
        intent.putExtra(RetroTimer.ALARM_TIME_EXTRA, alarmTime);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        ed.putLong(RetroTimer.PREF_ALARM_TIME, alarmTime);
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);

    	ed.putBoolean(RetroTimer.PREF_ALARM_SET, true);
    	ed.commit();
    }

    /** Cancels the alarm in the AlarmManager and updates app state */
    public static void cancelAlarm(Context context) {
        SharedPreferences prefs = 
        	PreferenceManager.getDefaultSharedPreferences(context);
    	SharedPreferences.Editor ed = prefs.edit();

		AlarmManager am = (AlarmManager)
				context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(
				context, 0, new Intent(RetroTimer.ALARM_TRIGGER_ACTION),
				PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(sender);

        ed.putLong(RetroTimer.PREF_ALARM_TIME, 0);
		ed.putBoolean(RetroTimer.PREF_ALARM_SET, false);
    	ed.commit();
    }

    /** Returns millis left to alarm, or zero if no alarm is set */
    public static long getMillisLeftToAlarm(Context context) {
        SharedPreferences prefs = 
        	PreferenceManager.getDefaultSharedPreferences(context);
    	if (prefs.getBoolean(RetroTimer.PREF_ALARM_SET, false)) {
    		return prefs.getLong(RetroTimer.PREF_ALARM_TIME, 0)
    				- System.currentTimeMillis();
    	} else {
    		return 0;
    	}
    }
}
