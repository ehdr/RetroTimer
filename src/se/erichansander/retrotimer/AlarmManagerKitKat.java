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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class AlarmManagerKitKat {
    public static void set(AlarmManager am, long alarmTime,
            PendingIntent sender) {
        am.setExact(AlarmManager.RTC_WAKEUP, alarmTime, sender);
    }
}
