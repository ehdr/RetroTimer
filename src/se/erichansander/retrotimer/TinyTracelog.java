/*
 * Copyright (C) 2010-2014  Eric Hansander
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Single string tracelog
 */
public class TinyTracelog {
    public static final String PREFID_TINYTRACELOG = "tinytracelog";

    private static SharedPreferences sPrefs = null;

    public static void init(Context c) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        }
    }

    public static synchronized void clear() {
        SharedPreferences.Editor ed = sPrefs.edit();
        ed.putString(PREFID_TINYTRACELOG, "");
        ed.commit();
    }

    public static synchronized void trace(String msg) {
        SharedPreferences.Editor ed = sPrefs.edit();
        ed.putString(PREFID_TINYTRACELOG, getTracelog() + msg + ";");
        ed.commit();
    }

    public static String getTracelog() {
        return sPrefs.getString(PREFID_TINYTRACELOG, "");
    }
}
