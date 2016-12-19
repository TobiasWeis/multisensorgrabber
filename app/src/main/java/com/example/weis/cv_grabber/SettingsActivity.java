package com.example.weis.cv_grabber;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

/**
 * Created by weis on 19.12.16.
 */

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
