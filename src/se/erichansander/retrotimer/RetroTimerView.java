package se.erichansander.retrotimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class RetroTimerView extends ImageView {
	
	private static final String TAG = "RetroTimerView";
	
	private GestureDetector mGestures;
	
	private final String mCompleteScale = 
		"0....5....10....15....20....25....30....35....40....45....50....55....";
	private Paint mScalePaint;

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

	private void onTurn(float dx) {
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		Path mScalePath = new Path();

		float middle = this.getHeight()/2f;
		float sidePadding = 5f;
		float ovalHeight = 80/2f;
		
		mScalePath.moveTo(sidePadding, middle);
		mScalePath.addArc(new RectF(sidePadding,
				middle-ovalHeight,
				this.getWidth()-sidePadding,
				middle+ovalHeight),
				90, -90);

		canvas.drawTextOnPath(mCompleteScale, mScalePath, 0, 0, mScalePaint);
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestures.onTouchEvent(event);
    }

	private class TimerGestureListener 
			extends GestureDetector.SimpleOnGestureListener {

		private View mView;
		
		public TimerGestureListener (View view) {
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
//			Log.v(TAG, "onFling");
			return true;
		}

		@Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
//            Log.v(TAG, "onScroll");
            return true;
		}
	}
}
