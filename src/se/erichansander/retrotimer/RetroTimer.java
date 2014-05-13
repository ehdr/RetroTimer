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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;

/**
 * Main application class. Handles state shared between activities, and holds
 * shared constants etc.
 */
public class RetroTimer extends Application {
    /**
     * When broadcasted, will cause the alarm to sound/vibrate and issue a
     * notification, that will dismiss the alarm when clicked.
     */
    public static final String ALARM_TRIGGER_ACTION = "se.erichansander.retrotimer.ALARM_TRIGGER";
    /**
     * When sent to the TimerKlaxon service, will sound alarm and/or vibrate
     * device
     */
    public static final String ALARM_PLAY_ACTION = "se.erichansander.retrotimer.ALARM_PLAY";
    /**
     * When broadcasted, will silence alarm and put up a notification stating
     * that the timer has been silenced.
     */
    public static final String ALARM_SILENCE_ACTION = "se.erichansander.retrotimer.ALARM_SILENCE";
    /**
     * When broadcasted, will silence alarm and remove any notifications.
     */
    public static final String ALARM_DISMISS_ACTION = "se.erichansander.retrotimer.ALARM_DISMISS";

    /** For passing the alarm time through an intent */
    public static final String ALARM_TIME_EXTRA = "intent.extra.alarmtime";

    /** Is true when an alarm is set */
    public static final String PREF_ALARM_SET = "prefs.alarm_set";
    /** Max num of millis to play alarm before silencing it automatically */
    public static final String PREF_ALARM_TIMEOUT_MILLIS = "prefs.alarm_timeout_millis";
    /** Absolute time when alarm should go off, in millis since epoch */
    public static final String PREF_ALARM_TIME = "prefs.alarm_time";
    /** Is true if alert should play audio */
    public static final String PREF_RING_ON_ALARM = "prefs.ring_on_alarm";
    /** Is true if alert should vibrate device */
    public static final String PREF_VIBRATE_ON_ALARM = "prefs.vibrate_on_alarm";
    /**
     * Is true if the licensing dialog has been shown (when the app was started
     * for the first time).
     */
    public static final String PREF_HAVE_SHOWN_LICENSE = "prefs.have_shown_license";

    /** Notification ID for messages about pending alarms */
    public static final int NOTIF_SET_ID = 1;
    /**
     * Notification ID for messages about triggering or triggered alarms
     */
    public static final int NOTIF_TRIGGERED_ID = 2;

    /**
     * Initializes the app state, and initializes the AlarmManager if any alarms
     * are pending from before the device was rebooted.
     * 
     * Should be called at device boot and at application start (in case the app
     * has been killed).
     */
    public static void initAlarm(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        if (prefs.getBoolean(PREF_ALARM_SET, false)
                || prefs.getLong(PREF_ALARM_TIME, 0) > 0) {
            if (getMillisLeftToAlarm(context) > 1000) {
                /*
                 * If there is time left until the alarm should trigger, we
                 * register it again with the AlarmManager
                 */
                setAlarmAt(context, prefs.getLong(PREF_ALARM_TIME, 0));
            } else {
                /* Otherwise, we do some clean-up */
                clearAlarm(context);
            }
        }
    }

    /**
     * Convenience method for setting an alarm to trigger in millisLeft millis.
     */
    public static void setAlarmDelayed(Context context, long millisLeft) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = prefs.edit();
        /*
         * Set the alarm timeout to 5 + (mins to alarm)/2 seconds, i.e. timeout
         * will be in the range 5..34.5 seconds if the time max countdown is 59
         * minutes
         */
        ed.putLong(RetroTimer.PREF_ALARM_TIMEOUT_MILLIS,
                15000 + millisLeft / 60);
        ed.commit();

        setAlarmAt(context, System.currentTimeMillis() + millisLeft);
    }

    /**
     * Sets an alarm at absolute time alarmTime (in millis from epoch)
     */
    public static void setAlarmAt(Context context, long alarmTime) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putLong(RetroTimer.PREF_ALARM_TIME, alarmTime);
        ed.putBoolean(RetroTimer.PREF_ALARM_SET, true);
        ed.commit();

        AlarmManager am = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(RetroTimer.ALARM_TRIGGER_ACTION);
        intent.putExtra(RetroTimer.ALARM_TIME_EXTRA, alarmTime);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        setAlarmManagerAlarm(am, alarmTime, sender);

        // Trigger a notification that, when clicked, will open TimerSet
        Intent viewAlarm = new Intent(context, TimerSet.class);
        viewAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingNotify = PendingIntent.getActivity(context, 0,
                viewAlarm, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context)
                .setContentIntent(pendingNotify)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_alarm_set)
                .setContentTitle(context.getString(R.string.notify_set_label))
                .setContentText(
                        context.getString(R.string.notify_set_text, DateFormat
                                .getTimeFormat(context).format(alarmTime)));

        /*
         * Send the notification using the alarm id to easily identify the
         * correct notification.
         */
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(RetroTimer.NOTIF_SET_ID, mBuilder.build());
    }

    /**
     * Activate alarm in AlarmManager, taking Android version into account
     * 
     * Since the API for activating the alarm changed in Kitkat, we need to
     * treat the two versions differently.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void setAlarmManagerAlarm(AlarmManager am, long alarmTime,
            PendingIntent sender) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            am.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, alarmTime, sender);
        }
    }

    /** Cancels the alarm in the AlarmManager and updates app state */
    public static void cancelAlarm(Context context) {
        // Cancel the alarm in AlarmManager
        AlarmManager am = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0,
                new Intent(RetroTimer.ALARM_TRIGGER_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);

        // Cancel the notification
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(RetroTimer.NOTIF_SET_ID);

        clearAlarm(context);
    }

    /**
     * Clears the shared information about the alarm.
     * 
     * Can be called either after the alarm has been cancelled, or after it has
     * triggered.
     */
    public static void clearAlarm(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = prefs.edit();

        // Update the shared state
        ed.putLong(RetroTimer.PREF_ALARM_TIME, 0);
        ed.putBoolean(RetroTimer.PREF_ALARM_SET, false);
        ed.commit();
    }

    /** Returns millis left to alarm, or zero if no alarm is set */
    public static long getMillisLeftToAlarm(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        if (prefs.getBoolean(RetroTimer.PREF_ALARM_SET, false)) {
            return prefs.getLong(RetroTimer.PREF_ALARM_TIME, 0)
                    - System.currentTimeMillis();
        } else {
            return 0;
        }
    }

    /**
     * Returns the absolute time when alarm will trigger, in millis since epoch
     */
    public static long getAlarmTime(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        if (prefs.getBoolean(RetroTimer.PREF_ALARM_SET, false)) {
            return prefs.getLong(RetroTimer.PREF_ALARM_TIME, 0);
        } else {
            return 0;
        }
    }
}
