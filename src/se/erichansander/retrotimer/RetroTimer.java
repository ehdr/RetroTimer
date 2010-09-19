package se.erichansander.retrotimer;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

public class RetroTimer extends Activity {
	
	private static final String TAG = "RetroTimer";
	
	private RetroTimerView mTimer;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        FrameLayout frame = (FrameLayout) findViewById(R.id.timer_holder);
    	this.mTimer = new RetroTimerView(this);
    	frame.addView(this.mTimer);
    }
}
