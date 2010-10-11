package se.erichansander.retrotimer;

//TODO: handle escaping from the TimerAlert activity (by home button, back button, etc)
//TODO: check what happens if e.g. a alarm clock alarm triggers while the timer alerts
//TODO: check what happens if timer alerts while alarm clock triggers

public class RetroTimer {
	
	public static final String ALARM_TRIGGER_ACTION =
		"se.erichansander.retrotimer.ALARM_TRIGGER";
	public static final String ALARM_PLAY_ACTION =
		"se.erichansander.retrotimer.ALARM_PLAY";
	public static final String ALARM_KILLED_ACTION =
		"se.erichansander.retrotimer.ALARM_KILLED";


	// This string is used when passing the alarm time through an intent
	public static final String ALARM_TIME_EXTRA = "intent.extra.alarmtime";
	

	// true when an alarm is set
	public static final String PREF_ALARM_SET = "prefs.alarm_set";
	// Absolute time when alarm should go off, in millis since epoch 
	public static final String PREF_ALARM_TIME = "prefs.alarm_time";
	// true if alert should not play audio
	public static final String PREF_SILENT = "prefs.silent";
	// true if alert should vibrate device
	public static final String PREF_VIBRATE = "prefs.vibrate";

}
