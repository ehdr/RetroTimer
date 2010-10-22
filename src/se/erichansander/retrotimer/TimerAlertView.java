/* 
 * Copyright (C) 2010  Eric Hansander
 *
 *  This file is part of RetroTimer.
 *
 *  RetroTimer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RetroTimer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RetroTimer.  If not, see <http://www.gnu.org/licenses/>.
 */

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
