package se.erichansander.retrotimer;

import se.erichansander.retrotimer.RetroTimerView.RetroTimerListener;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

public class RetroTimer extends Activity implements RetroTimerListener {
	
	private static final String TAG = "RetroTimer";
	
	private RetroTimerView mTimer;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        FrameLayout frame = (FrameLayout) findViewById(R.id.timer_holder);
    	this.mTimer = new RetroTimerView(this);
    	this.mTimer.setRetroTimerListener(this);
    	frame.addView(this.mTimer);
    }
    
    public void onTimerTempValue(long millis) {
    	Log.d(TAG, "onTimerTempValue(millis=" + millis + ")");
    	if (millis <= 0) {
//    		TODO: vibrate
    	}
    }
    
    public void onTimerSetValue (long millis) {
    	Log.d(TAG, "onTimerSetValue(millis=" + millis + ")");
    	if (millis <= 0) {
//    		TODO: cancel timer
    	} else {
//    		TODO: set new timer
    	}
    }
}
