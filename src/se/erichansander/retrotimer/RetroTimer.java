package se.erichansander.retrotimer;

//TODO: clean out all logging before publication
//TODO: add graphics: launcher icon, notification icons,
//		options menu icons?
//TODO: fix license notices in every file
//TODO: add help screen?
//TODO: add donate button?
//TODO: handle different screen orientations
//TODO: handle different screen sizes

//TODO: animate TimerAlert?
//TODO: handle setting alarm with the trackball

// Bugs and testing:
//TODO: check what happens if e.g. a alarm clock alarm triggers while the timer alerts
//TODO: check what happens if timer alerts while alarm clock triggers


import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * Main application class. Handles state shared between activities,
 * and holds shared constants etc.
 */
public class RetroTimer extends Application {
	
	private static final String DEBUG_TAG = "RetroTimer";

	/**
	 * When broadcasted, will cause the alarm to sound/vibrate and
	 * put up a notification, that when clicked dismisses the alarm
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
	 * When broadcasted, will silence alarm and remove any notifications
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


	/**
	 * Initializes the app state, and initializes the AlarmManager is 
	 * any alarms are pending.
	 * 
	 * Should be called at device boot and at application start (in 
	 * case the app has been killed).
	 */
	public static void initAlarm(Context context) {
        SharedPreferences prefs = 
        	PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(PREF_ALARM_SET, false)) {
        	if (getMillisLeftToAlarm(context) > 1000) {
        		setAlarmAt(context, prefs.getLong(PREF_ALARM_TIME, 0));
        	} else {
            	SharedPreferences.Editor ed = prefs.edit();
                ed.putLong(RetroTimer.PREF_ALARM_TIME, 0);
        		ed.putBoolean(RetroTimer.PREF_ALARM_SET, false);
        		ed.commit();
        	}        	
        }
	}
	
	/**
	 * Convenience method for setting an alarm in millisLeft millis.
	 */
    public static void setAlarmDelayed(Context context, long millisLeft) {
    	setAlarmAt(context, System.currentTimeMillis() + millisLeft);    	
    }
    
    /**
     * Sets an alarm at absolute time alarmTime (in millis from epoch)
     */
    public static void setAlarmAt(Context context, long alarmTime) {
    	Log.v(DEBUG_TAG, "RetroTimer alarm set for " +
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

//      TODO: show timer icon in status bar

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
//  		TODO: assert mAlarmTime > curr time?
    		return prefs.getLong(RetroTimer.PREF_ALARM_TIME, 0)
    				- System.currentTimeMillis();
    	} else {
    		return 0;
    	}
    }
}
