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
