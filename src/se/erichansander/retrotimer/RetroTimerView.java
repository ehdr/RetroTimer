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
	
	private static final String TAG = "RetroTimerView";
	
	private GestureDetector mGestures;
	
	private RetroTimerListener mListener;
	
	private final long mTimerMaxVal = (59*60 + 59)*1000;
	private final String mCompleteScale = 
		"0....5....10....15....20....25....30....35....40....45....50....55....";
	private Paint mScalePaint;
	
	private long mTimerVal = 0;
	private long mTimerTempVal = 0;

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
    
    public void setTimeLeft(long millis) {
    	mTimerVal = millis;
    }

	private void onTurn(float dx) {
		int h = this.getHeight();
		int w = this.getWidth();
		Log.d(TAG, "onTurn(dx=" + dx + ")");
		Log.d(TAG, "getHeight()=" + h + ", getWidth()=" + w);

		Log.d(TAG, "change in mins=" + ((float) dx / w) * 30*60);
		mTimerTempVal = mTimerVal + (long) (((float) dx / w) * 30*60*1000);
		
		if (mTimerTempVal < 0) {
			mTimerTempVal = 0;
		} else if (mTimerTempVal > mTimerMaxVal) {
			mTimerTempVal = mTimerMaxVal;
		}

		invalidate(); // redraw the egg

		mListener.onTimerTempValue(mTimerTempVal);
	}
	
	private void onSet() {
		setTimeLeft(mTimerTempVal);

		mListener.onTimerSetValue(mTimerVal);
		
		invalidate(); // redraw the egg
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

		canvas.drawTextOnPath(Long.toString(mTimerTempVal / (60*1000)),
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
