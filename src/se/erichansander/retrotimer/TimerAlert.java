package se.erichansander.retrotimer;

import se.erichansander.retrotimer.TimerAlertView.TimerAlertListener;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class TimerAlert extends Activity implements TimerAlertListener {

	private static final String DEBUG_TAG = "TimerAlert";

	private TimerAlertView mTimer;

	// Received to handle ALARM_KILLED_ACTION
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	dismissAlarm(true);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        // Turn on the screen
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        		| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        		| WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        setContentView(R.layout.main);

        FrameLayout frame = (FrameLayout) findViewById(R.id.timer_holder);
    	mTimer = new TimerAlertView(this);
    	mTimer.setTimerAlertListener(this);
    	frame.addView(this.mTimer);

        IntentFilter filter = 
        		new IntentFilter(RetroTimer.ALARM_KILLED_ACTION);
        registerReceiver(mReceiver, filter);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	mTimer.setMillisLeft(0);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // No longer care about the alarm being killed.
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
//    			TODO: should the arg be true or false?
    			dismissAlarm(false);
    			break;
    		}

    	default:
    		break;
    	}
    	return super.dispatchKeyEvent(event);
    }

    public void onAlertDismissed() {
    	Log.d(DEBUG_TAG, "onAlertDismissed()");
    	dismissAlarm(false);
    }
    
    private void dismissAlarm(boolean killed) {
        Log.i(DEBUG_TAG, 
        		killed ? "Alarm killed" : "Alarm dismissed by user");

        // If the klaxon told us that the alarm has been killed, do not modify
        // the notification or stop the service.

        if (!killed) {
            // Cancel the notification
            NotificationManager nm = 
            	(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(0);

            // kill the Klaxon
            stopService(new Intent(this, TimerKlaxon.class));
        }
        finish();
    }
}
