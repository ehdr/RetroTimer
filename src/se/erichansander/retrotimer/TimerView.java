package se.erichansander.retrotimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.widget.ImageView;

public class TimerView extends ImageView {

	private static final String DEBUG_TAG = "TimerView";

	public static final long TIMER_MAX_MINS = 59;

	private final String mCompleteScale = 
		"0....5....10....15....20....25....30....35....40....45....50....55....";
	private Paint mScalePaint;

	protected long mMillisLeft = 0;

	public TimerView (Context context) {
		super(context);
		setImageResource(R.drawable.timer);
		
		// set the color and font size
		mScalePaint = new Paint();
//		TODO: fixed width font
		mScalePaint.setColor(Color.RED);
		mScalePaint.setTypeface(Typeface.MONOSPACE);
		mScalePaint.setTextSize(32);
		mScalePaint.setAntiAlias(true);
	}
    
    public void setMillisLeft(long millis) {
    	mMillisLeft = millis;

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

		canvas.drawTextOnPath(Long.toString(Math.round((double) mMillisLeft / 60000d)),
				mScalePath, 0, 0, mScalePaint);
	}
}
