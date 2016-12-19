package com.example.weis.cv_grabber;

import android.hardware.Camera;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import java.util.ArrayList;
import java.util.List;

import static com.example.weis.cv_grabber.MainActivity.getCameraInstance;

/**
 * Created by weis on 19.12.16.
 */


public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        populate_resolution_list();
    }

    public void populate_resolution_list(){
        Camera mCamera;
        mCamera = getCameraInstance();
        // get available cam parameters
        final Camera.Parameters params = mCamera.getParameters();

        // Check what resolutions are supported by your camera
        final List<Camera.Size> sizes = params.getSupportedPictureSizes();
        // List dialog to select resolution
        List<String> itemslist = new ArrayList<String>();
        List<String> valueslist = new ArrayList<String>();

        int cnt = 0;
        for (android.hardware.Camera.Size size : sizes) {
            itemslist.add(size.width + "x" + size.height);
            valueslist.add("" + cnt);
            cnt += 1;
        }
        final CharSequence[] entries = itemslist.toArray(new CharSequence[itemslist.size()]);
        final CharSequence[] values = valueslist.toArray(new CharSequence[valueslist.size()]);

        final ListPreference lp = (ListPreference) findPreference("pref_resolutions");

        lp.setEntries(entries);
        lp.setDefaultValue("0");
        lp.setEntryValues(values);
        mCamera.release();
    }
}
