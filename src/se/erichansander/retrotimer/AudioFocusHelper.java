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

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;

/**
 * Helper class for handling AudioFocus
 * 
 * Needed to maintain backwards compatibility with pre-API level 8 Android.
 */
@TargetApi(8)
public class AudioFocusHelper {
    Context mContext;
    AudioManager mAudioManager;

    public AudioFocusHelper(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean requestFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager
                .requestAudioFocus(null, AudioManager.STREAM_ALARM,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    public boolean abandonFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager
                .abandonAudioFocus(null);
    }
}
