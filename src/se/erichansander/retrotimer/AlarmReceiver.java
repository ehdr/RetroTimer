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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (C) 2007 The Android Open Source Project
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package se.erichansander.retrotimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives intents from AlarmManager and triggers necessary actions
 * 
 * Receives intents: ALARM_TRIGGER_ACTION, ALARM_SILENCE_ACTION,
 * ALARM_DISMISS_ACTION
 * 
 * and distributes actions to the other parts of the app (by starting services
 * and activities, and triggering notifications).
 */
public class AlarmReceiver extends BroadcastReceiver {

    /**
     * If the alarm is older than STALE_WINDOW seconds, ignore. It is probably
     * the result of a time or timezone change
     */
    private final static int STALE_WINDOW = 60 * 30;

    @Override
    public void onReceive(Context context, Intent intent) {
        long alarmTime = intent.getLongExtra(RetroTimer.ALARM_TIME_EXTRA, 0);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            RetroTimer.initAlarm(context);
        } else if (RetroTimer.ALARM_TRIGGER_ACTION.equals(intent.getAction())) {
            handleAlarmTrigger(context, alarmTime);
        } else if (RetroTimer.ALARM_SILENCE_ACTION.equals(intent.getAction())) {
            // No action needed, since TimerKlaxon already stopped everything
        } else if (RetroTimer.ALARM_DISMISS_ACTION.equals(intent.getAction())) {
            handleAlarmDismiss(context);
        } else {
            // Unknown intent!
        }
    }

    /**
     * Trigger the alarm, which means make some preparations and start the
     * TimerKlaxon service.
     * 
     * This is triggered by the AlarmManager.
     */
    private void handleAlarmTrigger(Context context, long alarmTime) {
        long now = System.currentTimeMillis();
        if (now > alarmTime + STALE_WINDOW * 1000) {
            // Stale alarm. Just ignore it.
            return;
        }

        /*
         * Maintain a CPU wake lock until the TimerKlaxon has started.
         */
        WakeLockHolder.acquireScreenCpuWakeLock(context);

        Intent playAlarm = new Intent(context, TimerKlaxon.class);
        playAlarm.putExtra(RetroTimer.ALARM_TIME_EXTRA, alarmTime);
        context.startService(playAlarm);
    }

    /**
     * Stops the TimerKlaxon.
     * 
     * This is normally triggered by a user action to dismiss the alarm.
     */
    private void handleAlarmDismiss(Context context) {
        // kill the Klaxon
        context.stopService(new Intent(context, TimerKlaxon.class));
    }
}
