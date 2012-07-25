/*
 * Copyright (C) 2010-2011  Eric Hansander
 *
 *  This file is part of Retro Timer.
 *
 *  Retro Timer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Retro Timer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Retro Timer.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.erichansander.retrotimer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;

/** Custom view for displaying the timer and drawing the scale on it.
 *
 *  For interacting with the timer, see the special classes
 *  TimerSetView and TimerAlertView. */
public class TimerView extends ImageView {

	/** The max number of minutes the countdown can be set to */
	public static final long TIMER_MAX_MINS = 89;

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

        /*
         * The drawTextOnPath() operation (used below) is not supported with HW
         * acceleration, so we need to disable it with setLayerType(). However,
         * that method only exists in API rev 11 and up, so we must be careful.
         */
        try {
            Method method = this.getClass().getMethod("setLayerType",
                    new Class[] { int.class, Paint.class });
            method.invoke(this, new Object[] { LAYER_TYPE_SOFTWARE, null });
        } catch (NoSuchMethodException e) {
             // setLayerType() does not exist, which should mean that API rev is
             // less than 11 and we won't have problems with HW acceleration
             // anyway
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

	/** Re-calculate all drawing related variables when view size changes */
	@Override
	public void  onSizeChanged  (int intw, int inth, int oldw, int oldh) {
		mDensityScale = getResources().getDisplayMetrics().density;

		// set the color and font size for the scale
		mScalePaint.setTextSize(32*mDensityScale);

		// measure the width of one letter (same for all since MONOSPACE)
		float widths[] = new float[1];
		mScalePaint.getTextWidths("...", 0, 1, widths);
		mLetterWidth = widths[0];

		// create a curved path for drawing the text scale on
		mScalePath = new Path();

		float h = inth;
		float w = intw;

		float middle = h*0.47f;
		float sidePadding = w*0.02f;
		float ovalHeight = h*0.14f;

		mScalePath.moveTo(sidePadding, middle);
		mScalePath.addArc(new RectF(sidePadding,
				middle-ovalHeight,
				w-sidePadding,
				middle+ovalHeight),
				150, -120);

		mPathLen = (new PathMeasure(mScalePath, false)).getLength();

		mLettersInScale =
				Math.round(mPathLen / mLetterWidth);
		if (mLettersInScale % 2 == 0) mLettersInScale -= 1;
		mScaleStartOffset =
			(mPathLen - mLetterWidth * mLettersInScale) / 2
			- mLetterWidth / (12*mDensityScale);
	}

	/** Sets the amount of millis left to zero, and redraws the timer */
    public void setMillisLeft(long millis) {
    	mMillisLeft = millis;

		invalidate(); // redraw the egg
    }

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int minute = Math.round(mMillisLeft / 60000f);

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
		/* The string might become slightly longer then length
		 * while we are building it, so add some spare. It will
		 * be trimmed before we return it. */
		StringBuilder s = new StringBuilder(length + 3);
		int bef = 0;
		int aft = 0;

		// Start with the center minute number
		if (centerMinute % 5 == 0) {
			s.append(centerMinute);
			if (centerMinute > 5) bef++;
		} else {
			s.append(".");
		}

		/* work our way outward, in both the positive and negative
		 * directions from centerMinute */
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
		}

		/* since the two-digit numbers (10, 15...) may have caused
		 * the centerMinute not to be precisely in the middle, we
		 * need to center it, using substring.
		 */
		return s.substring(bef - length/2,
				bef - length/2 + length);
	}
}
