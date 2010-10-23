/* 
 * Copyright (C) 2010  Eric Hansander
 *
 *  This file is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This file is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this file.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.erichansander.retrotimer;

import android.util.Log;

public class Elog {
	public final static String DEBUG_TAG = RetroTimer.DEBUG_TAG;

	static void e(String subtag, String msg) {
		Log.e(DEBUG_TAG, subtag + ": " + msg);
	}

	static void w(String subtag, String msg) {
		Log.w(DEBUG_TAG, subtag + ": " + msg);
	}

	static void i(String subtag, String msg) {
		Log.i(DEBUG_TAG, subtag + ": " + msg);
	}

	static void d(String subtag, String msg) {
		if (!RetroTimer.DEBUG) return;
		Log.d(DEBUG_TAG, subtag + ": " + msg);
	}

	static void v(String subtag, String msg) {
		if (!RetroTimer.DEBUG) return;
		Log.v(DEBUG_TAG, subtag + ": " + msg);
	}
}
