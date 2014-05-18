package se.erichansander.retrotimer;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TimerSettings extends PreferenceActivity {
    // We need addPreferencesFromResource() for backwards compatibility
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
