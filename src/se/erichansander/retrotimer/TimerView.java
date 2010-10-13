package se.erichansander.retrotimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TimerView extends ImageView {

	private static final String DEBUG_TAG = "TimerView";

	public static final long TIMER_MAX_MINS = 59;

	private final String mCompleteScale = 
		"0....5....10....15....20....25....30....35....40....45....50....55....";
	private Paint mScalePaint;

	protected long mMillisLeft = 0;

	public TimerView (Context context, AttributeSet attrs) {
		super(context, attrs);
		setImageResource(R.drawable.timer);
		
		// set the color and font size
		mScalePaint = new Paint();
//		TODO: fixed width font
		mScalePaint.setColor(Color.rgb(230, 26, 96));
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

		final float scale = 
			getContext().getResources().getDisplayMetrics().density;
		
		float h = this.getHeight() * scale;
		float w = this.getWidth() * scale;

		Path mScalePath = new Path();

		float middle = h/2f-10;
		float sidePadding = 20f;
		float ovalHeight = 90/2f;
		
		mScalePath.moveTo(sidePadding, middle);
		mScalePath.addArc(new RectF(sidePadding,
				middle-ovalHeight,
				w-sidePadding,
				middle+ovalHeight),
				150, -120);

		canvas.drawTextOnPath(
				Long.toString(Math.round((float) mMillisLeft / 60000f)),
				mScalePath, 0, 0, mScalePaint);
	}
}
