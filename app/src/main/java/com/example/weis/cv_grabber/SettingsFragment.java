package com.example.weis.cv_grabber;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

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

        final boolean m_newFolderEnabled = false;

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final EditTextPreference folderPicker = (EditTextPreference) findPreference("folderPicker");
        folderPicker.setSummary( prefs.getString("folderPicker", "N/A"));

        populate_resolution_list();

        /* Took this out, setting a specific directory is next to impossible with shitty new
        * android rights management
        * /
        /*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final EditTextPreference folderPicker = (EditTextPreference) findPreference("folderPicker");
        folderPicker.setSummary(prefs.getString("folderPicker", "N/A"));

        final String m_chosenDir = prefs.getString("folderPicker", "N/A");
        Log.d("Prefs", "m_chosenDir is: " + m_chosenDir);
        folderPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DirectoryChooserDialog directoryChooserDialog = new DirectoryChooserDialog(getActivity(), new DirectoryChooserDialog.ChosenDirectoryListener() {
                    @Override
                    public void onChosenDir(String chosenDir) {
                        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                        prefs.putString("folderPicker", chosenDir);
                        prefs.commit();


                        SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        folderPicker.setSummary(prefs2.getString("folderPicker", "N/A"));

                        Log.d("FolderPicker", "Chosen folder: " + chosenDir);
                    }
                });
                // Toggle new folder button enabling
                directoryChooserDialog.setNewFolderEnabled(m_newFolderEnabled);
                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.
                directoryChooserDialog.chooseDirectory(m_chosenDir);

                return true;

            }
        });
        */
    }

    public void populate_resolution_list() {
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

