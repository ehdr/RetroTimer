/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.erichansander.retrotimer;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Glue class: Receives intents:
 * ALARM_TRIGGER_ACTION
 * ALARM_KILLED_ACTION
 */
public class AlarmReceiver extends BroadcastReceiver {

	private static final String DEBUG_TAG = "AlarmReceiver";

    /** If the alarm is older than STALE_WINDOW seconds, ignore.  It
        is probably the result of a time or timezone change */
    private final static int STALE_WINDOW = 60 * 30;

    private SharedPreferences mPrefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    	long alarmTime =
    		intent.getLongExtra(RetroTimer.ALARM_TIME_EXTRA, 0);

    	if (RetroTimer.ALARM_KILLED_ACTION.equals(intent.getAction())) {
            // The alarm has been killed, update the notification
            updateNotification(context, alarmTime);
            return;
        }
    	
    	// Not ALARM_KILLED_ACTION, so from now on it's ALARM_TRIGGER_ACTION

        // Intentionally verbose: always log the alarm time to provide useful
        // information in bug reports.
        long now = System.currentTimeMillis();
        SimpleDateFormat format =
                new SimpleDateFormat("HH:mm:ss.SSS aaa");
        Log.v(DEBUG_TAG, "AlarmReceiver.onReceive() id setFor "
                + format.format(new Date(alarmTime)));

        if (now > alarmTime + STALE_WINDOW * 1000) {
            Log.v(DEBUG_TAG, "AlarmReceiver ignoring stale alarm");
            return;
        }

        // Maintain a cpu wake lock until the AlarmAlert and AlarmKlaxon can
        // pick it up.
        AlarmAlertWakeLock.acquireCpuWakeLock(context);

        /* Close dialogs and window shade */
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        /* launch UI, explicitly stating that this is not due to user action
         * so that the current app's notification management is not disturbed */
        Intent timerAlert = new Intent(context, TimerAlert.class);
        timerAlert.putExtra(RetroTimer.ALARM_TIME_EXTRA, alarmTime);
        timerAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        context.startActivity(timerAlert);

        // Play the alarm alert and vibrate the device.
        Intent playAlarm = new Intent(context, TimerKlaxon.class);
        playAlarm.putExtra(RetroTimer.ALARM_TIME_EXTRA, alarmTime);
        context.startService(playAlarm);
        
        // Update the shared state
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(RetroTimer.PREF_ALARM_SET, false);
        editor.putLong(RetroTimer.PREF_ALARM_TIME, 0);
        editor.commit();

        // Trigger a notification that, when clicked, will show the TimerAlert
        // activity. No need to check for fullscreen since this will always be
        // launched from a user action.
        Intent notify = new Intent(context, TimerAlert.class);
        PendingIntent pendingNotify =
        		PendingIntent.getActivity(context, 0, notify, 0);

        // Use the alarm's label or the default label as the ticker text and
        // main text of the notification.
        String label = context.getString(R.string.app_name);
        Notification n = new Notification(R.drawable.stat_notify_alarm,
                label, alarmTime);
        n.setLatestEventInfo(context, label,
        		context.getString(R.string.alarm_notify_text),
                pendingNotify);
        n.flags |= Notification.FLAG_SHOW_LIGHTS
                | Notification.FLAG_ONGOING_EVENT;
        n.defaults |= Notification.DEFAULT_LIGHTS;

        // Send the notification using the alarm id to easily identify the
        // correct notification.
        NotificationManager nm = (NotificationManager)
        		context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, n);
    }

    private void updateNotification(Context context, long alarmTime) {
        NotificationManager nm = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Launch the TimerSet activity when clicked
        Intent viewAlarm = new Intent(context, TimerSet.class);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, viewAlarm, 0);

        // Update the notification to indicate that the alert has been
        // silenced.
        String label = context.getString(R.string.app_name);
        Notification n = new Notification(R.drawable.stat_notify_alarm,
                label, alarmTime);
        n.setLatestEventInfo(context, label,
                context.getString(R.string.alarm_alert_alert_silenced),
                intent);
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        // We have to cancel the original notification since it is in the
        // ongoing section and we want the "killed" notification to be a plain
        // notification.
        nm.cancel(0);
        nm.notify(0, n);
    }
}
