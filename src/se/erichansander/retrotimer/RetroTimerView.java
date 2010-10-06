package se.erichansander.retrotimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

public class RetroTimerView extends ImageView {

	public static final long TIMER_MAX_MILLIS = (59*60 + 59)*1000;

	private static final String TAG = "RetroTimerView";
	
	private GestureDetector mGestures;
	private RetroTimerListener mListener;

	private final String mCompleteScale = 
		"0....5....10....15....20....25....30....35....40....45....50....55....";
	private Paint mScalePaint;

	private long mMillisLeft = 0;
	
	private boolean mBeingChanged = false;
	private long mMillisLeftBefore = 0;

	public interface RetroTimerListener {
		abstract void onTimerTempValue(long millis);
		abstract void onTimerSetValue(long millis);
	}

	public RetroTimerView (Context context) {
		super(context);
		setImageResource(R.drawable.timer);
		mGestures = new GestureDetector(context,
							new TimerGestureListener(this));
		
		// set the color and font size
		mScalePaint = new Paint();
		mScalePaint.setColor(Color.RED);
		mScalePaint.setTextSize(32);
		mScalePaint.setAntiAlias(true);
	}

    public void setRetroTimerListener(RetroTimerListener listener) {
        mListener = listener;
    }
    
    public void setMillisLeft(long millis) {
    	mMillisLeft = millis;

		invalidate(); // redraw the egg
    }

	private void onTurn(float dx) {
		int h = this.getHeight();
		int w = this.getWidth();
		
		Log.d(TAG, "onTurn(dx=" + dx + "), mBeingChanged=" + mBeingChanged);
		Log.d(TAG, "getHeight()=" + h + ", getWidth()=" + w);

		if (mBeingChanged == false) {
			mMillisLeftBefore = mMillisLeft;
			mBeingChanged = true;
		}

		mMillisLeft = mMillisLeftBefore + (long) (((float) dx / w) * 30*60*1000);

		if (mMillisLeft <= 0) {
    		mMillisLeft = 0;
    	} else if (mMillisLeft > TIMER_MAX_MILLIS) {
    		mMillisLeft = TIMER_MAX_MILLIS;
    	}
		
		Log.d(TAG, "change in mins=" + ((mMillisLeftBefore - mMillisLeft) / 1000));

		mListener.onTimerTempValue(mMillisLeft);
	}
	
	private void onSet() {
		mListener.onTimerSetValue(mMillisLeft);
		mBeingChanged = false;
		mMillisLeftBefore = 0;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		Path mScalePath = new Path();

		float middle = this.getHeight()/2f-10;
		float sidePadding = 20f;
		float ovalHeight = 90/2f;
		
		mScalePath.moveTo(sidePadding, middle);
		mScalePath.addArc(new RectF(sidePadding,
				middle-ovalHeight,
				this.getWidth()-sidePadding,
				middle+ovalHeight),
				150, -120);

		canvas.drawTextOnPath(Long.toString(mMillisLeft / (60*1000)),
				mScalePath, 0, 0, mScalePaint);
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	boolean retVal = mGestures.onTouchEvent(event);
    	
    	int action = event.getAction();
        if (action == MotionEvent.ACTION_UP ||
        		action == MotionEvent.ACTION_CANCEL) {
            // Helper method to detect when scrolling is finished
            onSet();
            retVal = true;
        }

        return retVal;
    }

	private class TimerGestureListener 
			extends GestureDetector.SimpleOnGestureListener {

		private RetroTimerView mView;
		
		public TimerGestureListener (RetroTimerView view) {
			this.mView = view;
		}
		
//		For some reason, this method must return true for onScroll to be called?!
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2,
				final float velocityX, final float velocityY) {
			Log.d(TAG, "onFling");
//			TODO: call callback for flinging
			return true;
		}

		@Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            Log.d(TAG, "onScroll");
			mView.onTurn(e2.getX() - e1.getX());
            return true;
		}
	}
}
