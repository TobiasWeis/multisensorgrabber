package com.example.weis.cv_grabber;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.R.attr.id;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    /* stupid android 6.0 permission management sucks */
    int MY_PERMISSION_REQUEST = 1;

    boolean recording = false;
    private Handler handler = new Handler();

    android.hardware.Camera.Size p_picture_size;
    Camera.Parameters parameters;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mCamera.takePicture(null, null, mPicture);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startMain();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startMain();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button settingsButton = (Button) findViewById(R.id.button_settings);
        settingsButton.setOnClickListener(
                new android.view.View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    }
                }
        );

        final Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setBackgroundColor(Color.GREEN);

        captureButton.setOnClickListener(
                new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!recording) {
                            // get an image from the camera
                            handler.postDelayed(runnable, 100);
                            recording = true;
                            captureButton.setBackgroundColor(Color.RED);
                        }else{
                            handler.removeCallbacks(runnable);
                            recording = false;
                            captureButton.setBackgroundColor(Color.GREEN);
                        }
                    }
                }
        );


        /*
         * FIXME: this is all asynchronous, so WHILE the user gets asked for permission,
         * the program continues to run and crashes.
         * Temporary "fix": install, run, let it crash. Go to Settings->Apps and assign the permission by hand
         */
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("main", "Requesting permissions");

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE }, MY_PERMISSION_REQUEST);
        }else {
            Log.d("main", "Already have permission, not asking");
            startMain();
        }
    }

    /* start camera preview and enable button */
    private void startMain(){
        // Create an instance of Camera
        mCamera = getCameraInstance();

        parameters = mCamera.getParameters();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String selected = prefs.getString("pref_resolutions", ""); // this gives the value
        Log.d("---", "Selected resolution index was: |" + selected + "|" );
        if (selected != "") {
            final List<android.hardware.Camera.Size> sizes = parameters.getSupportedPictureSizes();
            Integer width = sizes.get(Integer.parseInt(selected)).width;
            Integer height = sizes.get(Integer.parseInt(selected)).height;
            parameters.setPictureSize(width, height);
            mCamera.setParameters(parameters);
            Toast.makeText(this, "Set image resolution to " + width + "x" + height, Toast.LENGTH_LONG);
        }

        /*
        // get available cam parameters
        parameters = mCamera.getParameters();

        final List<android.hardware.Camera.Size> sizes = parameters.getSupportedPictureSizes();
        // List dialog to select resolution
        List<String> itemslist = new ArrayList<String>();
        for (android.hardware.Camera.Size size : sizes) {
            itemslist.add(size.width + "x" + size.height);
        }
        final CharSequence[] items = itemslist.toArray(new CharSequence[itemslist.size()]);
        //final CharSequence[] items = {"640x480", "720x360", "1920x1080"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Available resolutions");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
                Log.d("Res selection", "Value: " + items[item]);
                p_picture_size = sizes.get(item);
                parameters.setPictureSize(p_picture_size.width, p_picture_size.height);
                mCamera.setParameters(parameters);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        */

        // Create Preview view and set it as the content of our activity.
        // this HAS to be done in order to able to take pictures (WTF?!)
        mPreview = new CameraPreview(this, mCamera);
        RelativeLayout preview = (RelativeLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            /* restart the picture-taking from here,
             * b/c we have to wait for the callback to finish
             */
            handler.postDelayed(runnable, 1);
        }
    };


    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera mCamera;
    private CameraPreview mPreview;

    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        long timeStamp = System.currentTimeMillis();
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


}
