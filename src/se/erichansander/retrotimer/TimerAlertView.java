package se.erichansander.retrotimer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

public class TimerAlertView extends TimerView implements OnClickListener {

	private static final String DEBUG_TAG = "TimerAlertView";

	private TimerAlertListener mListener = null;

	public interface TimerAlertListener {
		abstract void onAlertDismissed();
	}

	public TimerAlertView (Context context, AttributeSet attrs) {
		super(context, attrs);

		this.setOnClickListener(this);
	}

    public void setTimerAlertListener(TimerAlertListener listener) {
        mListener = listener;
    }
    
    @Override
    public void onClick(View v) {
//    	Log.d(DEBUG_TAG, "onClick");
    	
    	mListener.onAlertDismissed();
    }
}
