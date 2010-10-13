package se.erichansander.retrotimer;

import se.erichansander.retrotimer.TimerAlertView.TimerAlertListener;
import android.app.Activity;
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

/** Activity to show while alarm is playing. */
public class TimerAlert extends Activity implements TimerAlertListener {

	private static final String DEBUG_TAG = "TimerAlert";

	private TimerAlertView mTimer;

	// Received to handle ALARM_SILENCE_ACTION and ALARM_DISMISS_ACTION
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	finish();
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

        setContentView(R.layout.timer_alert);

    	mTimer = (TimerAlertView) findViewById(R.id.timer_alert_view);
    	mTimer.setTimerAlertListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RetroTimer.ALARM_DISMISS_ACTION);
        filter.addAction(RetroTimer.ALARM_SILENCE_ACTION);
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
//    			TODO: should this count as dismiss or silence?
    			dismissAlarm();
    			break;
    		}

    	default:
    		break;
    	}
    	return super.dispatchKeyEvent(event);
    }

    public void onAlertDismissed() {
    	Log.d(DEBUG_TAG, "onAlertDismissed()");
    	dismissAlarm();
    }
    
    private void dismissAlarm() {
        Intent intent = new Intent(RetroTimer.ALARM_DISMISS_ACTION);
        sendBroadcast(intent);

        finish();
    }
}
