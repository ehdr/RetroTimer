/*
 * Copyright (C) 2010-2012  Eric Hansander
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *     Copyright (C) 2008 The Android Open Source Project
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package se.erichansander.retrotimer;

import java.lang.ref.WeakReference;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;

/**
 * Plays alarm and vibrates. Runs as a service so that it can continue to play
 * if another activity overrides the TimerAlert view.
 * 
 * It will play alarm and start vibrating, show a notification that allows
 * dismissing the alarm, and start the TimerAlert activity, that also allows
 * dismissing.
 * 
 * Receives the ALARM_PLAY_ACTION intent when started.
 */
public class TimerKlaxon extends Service {

    /*
     * Comment from the DeskClock app:
     * 
     * Volume suggested by media team for in-call alarms.
     */
    private static final float IN_CALL_VOLUME = 0.125f;

    private static final long[] sVibratePattern = new long[] { 500, 500 };

    // Handles to stuff we need to interact with
    private SharedPreferences mPrefs;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private TelephonyManager mTelephonyManager;
    private AudioFocusHelper mAudioFocusHelper;

    private boolean mPlaying = false;
    private long mAlarmTime = 0;
    private int mInitialCallState;

    // Internal messages. Handles alarm timeouts.
    private static final int TIMEOUT_ID = 1000;

    private static class TimeoutHandler extends Handler {
        private final WeakReference<TimerKlaxon> mKlaxonRef;

        public TimeoutHandler(TimerKlaxon klaxon) {
            mKlaxonRef = new WeakReference<TimerKlaxon>(klaxon);
        }

        @Override
        public void handleMessage(Message msg) {
            TimerKlaxon k = mKlaxonRef.get();

            switch (msg.what) {
            case TIMEOUT_ID:
                if (k != null) {
                    k.handleAlarmSilence(k.mAlarmTime);
                    k.stopSelf();
                }
                break;
            }
        }
    };

    private Handler mHandler = new TimeoutHandler(this);

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            /*
             * The user might already be in a call when the alarm fires. When we
             * register onCallStateChanged, we get the initial in-call state
             * which kills the alarm. Check against the initial call state so we
             * don't kill the alarm during a call.
             */
            if (state != TelephonyManager.CALL_STATE_IDLE
                    && state != mInitialCallState) {
                handleAlarmSilence(mAlarmTime);
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Listen for incoming calls to kill the alarm.
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

        if (android.os.Build.VERSION.SDK_INT >= 8) {
            mAudioFocusHelper = new AudioFocusHelper(this);
        } else {
            mAudioFocusHelper = null;
        }

        WakeLockHolder.acquireScreenCpuWakeLock(this);
    }

    @Override
    public void onDestroy() {
        stop();

        // Cancel the notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(RetroTimer.NOTIF_TRIGGERED_ID);

        mTelephonyManager.listen(mPhoneStateListener, 0);
        WakeLockHolder.releaseCpuLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // For backwards compatibility with pre-API 8 systems
        onStartCommand(intent, 0, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No intent, tell the system not to restart us.
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        boolean ring = mPrefs.getBoolean(RetroTimer.PREF_RING_ON_ALARM, true);
        boolean vibrate = mPrefs.getBoolean(RetroTimer.PREF_VIBRATE_ON_ALARM,
                true);
        long timeoutMillis = mPrefs.getLong(
                RetroTimer.PREF_ALARM_TIMEOUT_MILLIS, 10 * 1000);
        mAlarmTime = intent.getLongExtra(RetroTimer.ALARM_TIME_EXTRA, 0);

        // Close dialogs and window shade
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        // Trigger a notification that, when clicked, will dismiss the alarm.
        Intent notify = new Intent(RetroTimer.ALARM_DISMISS_ACTION);
        PendingIntent pendingNotify = PendingIntent.getBroadcast(this, 0,
                notify, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setContentIntent(pendingNotify)
                .setDefaults(Notification.DEFAULT_LIGHTS).setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_alarm_triggered)
                .setContentTitle(getString(R.string.notify_triggered_label))
                .setContentText(getString(R.string.notify_triggered_text));

        /*
         * Send the notification using the alarm id to easily identify the
         * correct notification.
         */
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(RetroTimer.NOTIF_SET_ID);
        nm.notify(RetroTimer.NOTIF_TRIGGERED_ID, mBuilder.build());

        /*
         * launch UI, explicitly stating that this is not due to user action so
         * that the current app's notification management is not disturbed
         */
        Intent timerAlert = new Intent(this, TimerAlert.class);
        timerAlert.putExtra(RetroTimer.ALARM_TIME_EXTRA, mAlarmTime);
        timerAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        startActivity(timerAlert);

        play(ring, vibrate);
        startTimeoutCountdown(timeoutMillis);

        // Record the initial call state here so that the new alarm has the
        // newest state.
        mInitialCallState = mTelephonyManager.getCallState();

        // Update the shared state
        RetroTimer.clearAlarm(this);

        return START_STICKY;
    }

    /**
     * Wrap up after the alarm sound has timed out, with no user dismissal
     * 
     * Will stop playing alarm, stop vibrating and display a notification saying
     * when the alarm triggered.
     */
    private void handleAlarmSilence(long alarmTime) {
        // Display notification
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Launch the TimerSet activity when clicked
        Intent viewAlarm = new Intent(this, TimerSet.class);
        viewAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pintent = PendingIntent
                .getActivity(this, 0, viewAlarm, 0);

        // Update the notification to indicate that the alert has been
        // silenced.
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this)
                .setContentIntent(pintent)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_stat_alarm_triggered)
                .setContentTitle(getString(R.string.notify_silenced_label))
                .setContentText(
                        getString(R.string.notify_silenced_text, DateFormat
                                .getTimeFormat(this).format(mAlarmTime)));
        // We have to cancel the original notification since it is in the
        // ongoing section and we want the "killed" notification to be a plain
        // notification.
        nm.cancel(RetroTimer.NOTIF_TRIGGERED_ID);
        nm.notify(RetroTimer.NOTIF_SILENCED_ID, mBuilder.build());

        Intent intent = new Intent(RetroTimer.ALARM_SILENCE_ACTION);
        intent.putExtra(RetroTimer.ALARM_TIME_EXTRA, alarmTime);
        sendBroadcast(intent);

        stopSelf();
    }

    private void play(boolean ring, boolean vibrate) {
        // stop() checks to see if we are already playing.
        stop();

        if (ring) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.stop();
                    mp.release();
                    mMediaPlayer = null;
                    return true;
                }
            });

            try {
                /*
                 * Check if we are in a call. If we are, use the in-call alarm
                 * resource at a low volume to not disrupt the call.
                 */
                if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                    mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                    setDataSourceFromResource(getResources(), mMediaPlayer,
                            R.raw.in_call_alarm);
                } else {
                    setDataSourceFromResource(getResources(), mMediaPlayer,
                            R.raw.classic_alarm);
                }
                startAlarm(mMediaPlayer);
            } catch (Exception ex) {
                // Failed to play ring tone. Not much we can do to save
                // the situation though...
            }
        }

        /* Start the vibrator after everything is ok with the media player */
        if (vibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        } else {
            mVibrator.cancel();
        }

        mPlaying = true;
    }

    // Do the common stuff when starting the alarm.
    private void startAlarm(MediaPlayer player) throws java.io.IOException,
            IllegalArgumentException, IllegalStateException {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /*
         * do not play alarms if stream volume is 0 (typically because ringer
         * mode is silent).
         */
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            if (mAudioFocusHelper != null) {
                mAudioFocusHelper.requestFocus();
            }
            player.start();
        }
    }

    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    /**
     * Stops alarm audio and disables alarm
     */
    public void stop() {
        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                if (mAudioFocusHelper != null) {
                    mAudioFocusHelper.abandonFocus();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // Stop vibrator
            mVibrator.cancel();
        }
        cancelTimeoutCountdown();
    }

    /**
     * Kills alarm audio after timeoutMillis millis, so the alarm won't run all
     * day.
     */
    private void startTimeoutCountdown(long timeoutMillis) {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(TIMEOUT_ID),
                timeoutMillis);
    }

    /* Cancels the timeout countdown */
    private void cancelTimeoutCountdown() {
        mHandler.removeMessages(TIMEOUT_ID);
    }
}
