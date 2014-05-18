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
