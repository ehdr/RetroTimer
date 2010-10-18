package se.erichansander.retrotimer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/** Custom view for displaying the timer and drawing the scale on it */
public class TimerView extends ImageView {

	private static final String DEBUG_TAG = "TimerView";

	public static final long TIMER_MAX_MINS = 59;

	private Paint mScalePaint;
	private Path mScalePath;

	// variables used for drawing the scale, with correct length
	// and position, etc
	private float mDensityScale;
	private float mLetterWidth;
	private float mPathLen;
	private int mLettersInScale;
	private float mScaleStartOffset;

	protected long mMillisLeft = 0;

	public TimerView (Context context, AttributeSet attrs) {
		super(context, attrs);

		mScalePaint = new Paint();
		mScalePaint.setColor(
				getResources().getColor(R.color.timer_scale));
		mScalePaint.setTypeface(Typeface.MONOSPACE);
		mScalePaint.setAntiAlias(true);
		// set the size in onSizeChanged, when we know how big the view is
	}
	
	/** Re-calculate all drawing related variables when view size changes */
	@Override
	public void  onSizeChanged  (int intw, int inth, int oldw, int oldh) {
		mDensityScale = getResources().getDisplayMetrics().density;
		Log.d(DEBUG_TAG, "mDensityScale=" + mDensityScale);

		// set the color and font size for the scale
		mScalePaint.setTextSize(32*mDensityScale);

		// measure the width of one letter (same for all since MONOSPACE)
		float widths[] = new float[1];
		mScalePaint.getTextWidths("...", 0, 1, widths);
		mLetterWidth = widths[0];
		Log.d(DEBUG_TAG, "mLetterWidth=" + mLetterWidth);

		// create a curved path for drawing the text scale on
		mScalePath = new Path();

		float h = (float) inth;
		float w = (float) intw;

		//TODO: check actual measured h and w on different devices,
		//and see if the correlate to mDensityScale. If so, make do
		//without the hScale and wScale.
		final float hScale = h/386f;
		final float wScale = w/296f;

		float middle = h/2-10f*hScale;
		float sidePadding = 20f*wScale;
		float ovalHeight = 90f*hScale/2;

		mScalePath.moveTo(sidePadding, middle);
		mScalePath.addArc(new RectF(sidePadding,
				middle-ovalHeight,
				w-sidePadding,
				middle+ovalHeight),
				150, -120);

		mPathLen = (new PathMeasure(mScalePath, false)).getLength();
		Log.d(DEBUG_TAG, 
				"view h=" + h + ", w=" + w + 
				", mPathLen=" + mPathLen);
		
		mLettersInScale = 
				(int) Math.round(Math.floor(mPathLen / mLetterWidth));
		if (mLettersInScale % 2 == 0) mLettersInScale -= 1;
		mScaleStartOffset =
			(mPathLen - mLetterWidth * mLettersInScale) / 2
			- mLetterWidth / 6;
		Log.d(DEBUG_TAG,
				"mLettersInScale=" + mLettersInScale +
				", mScaleStartOffset=" + mScaleStartOffset);
	}

	/** Sets the amount of millis left to zero, and redraws the timer */
    public void setMillisLeft(long millis) {
    	mMillisLeft = millis;

		invalidate(); // redraw the egg
    }

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int minute = Math.round((float) mMillisLeft / 60000f);

		canvas.drawTextOnPath(
				getScaleString(mLettersInScale, minute),
				mScalePath, mScaleStartOffset, 0, mScalePaint);
	}

	/** Returns a string of given length, centered on given minute
	 * 
	 * E.g. length = 5 and centerMinute 8 would give:
	 * ....1
	 *
	 * @precond length must be odd 
	 */
	private String getScaleString(int length, int centerMinute) {

		StringBuilder s = new StringBuilder();
		int bef = 0;
		int aft = 0;

		// Start with the center minute number
		if (centerMinute % 5 == 0) {
			s.append(centerMinute);
			if (centerMinute > 5) bef++;
		} else {
			s.append(".");
		}
		
//		Log.d(DEBUG_TAG, "bef=" + bef + ",aft=" + aft +
//				", s=" + s);

		/* work our way outward, in both the positive and negative
		 * directions
		 */
		for (int i = 1; i <= length / 2; i++) {
			int min;

			min = centerMinute - i;
			if (min < 0) {
				s.insert(0, " ");
			} else if (min % 5 == 0) {
				s.insert(0, min);
				if (min > 5) bef++;
			} else {
				s.insert(0, ".");
			}
			bef++;

			min = centerMinute + i;
			if (min > TIMER_MAX_MINS) {
				s.append(" ");				
			} else if (min % 5 == 0) {
				s.append(min);
				if (min > 5) aft++;
			} else {
				s.append(".");
			}
			aft++;
			
//			Log.d(DEBUG_TAG, "i=" + i +
//					",bef=" + bef + ",aft=" + aft +
//					", s=" + s);
		}
		
//		Log.d(DEBUG_TAG, "c=" + centerMinute + 
//				",bef=" + bef + ",aft=" + aft + 
//				",s.length()=" + s.length() + 
//				", s=" + s +
//				", s.substr=" + 
//				s.substring(bef - length/2, 
//						bef - length/2 + length));

		/* since the two-digit numbers (10, 15...) may have caused 
		 * the centerMinute not to be precisely in the middle, we
		 * need to center it, using substring.
		 */
		return s.substring(bef - length/2,
				bef - length/2 + length);
	}
}
