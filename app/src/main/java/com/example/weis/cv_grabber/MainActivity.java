package com.example.weis.cv_grabber;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    /* stupid android 6.0 permission management sucks */
    int MY_PERMISSION_REQUEST = 1;

    int _framerate = 10;
    static String _folder = "";
    long _ts_lastframe = 0;
    static String _last_fname = "";
    static long _ts_lastpic = 0;
    static long _seq_timestamp = 0;

    double _gyro_head = 0.;
    double _gyro_pitch = 0.;
    double _gyro_roll = 0.;

    double _accel_x = 0.;
    double _accel_y = 0.;
    double _accel_z = 0.;

    boolean recording = false;
    private Handler handler = new Handler();
    private Handler sys_handler = new Handler();
    FileOutputStream fileos;
    XmlSerializer serializer = Xml.newSerializer();
    static File _mediaStorageDir; // sequence directory

    android.hardware.Camera.Size p_picture_size;
    Camera.Parameters parameters;

    TextView textview_coords;
    TextView textview_fps;
    TextView textview_battery;
    TextView textview_sensors;

    LocationManager mLocationManager;
    Criteria criteria = new Criteria();
    String bestProvider;
    android.location.Location _loc;
    boolean _pic_returned = true;

    boolean _main_runs = false;

    /*
    this runnable is meant to be called only every other second,
    to fetch data about the system (e.g. battery state)
     */
    private Runnable grab_system_data = new Runnable() {
        @Override
        public void run() {
            textview_battery.setText("BAT: "+getBatteryLevel()+"%");

            textview_sensors.setText("head: " + String.format("%.01f", _gyro_head) +
                    " pitch: " + String.format("%.01f", _gyro_pitch) +
                    " roll: " + String.format("%.01f", _gyro_roll) +
                    " ax " + String.format("%.01f", _accel_x) +
                    " ay " + String.format("%.01f", _accel_y) +
                    " az " + String.format("%.01f", _accel_z));

            sys_handler.postDelayed(grab_system_data, 500);
        }
    };

    private Runnable grab_frame = new Runnable() {
        @Override
        public void run() {
            if (_pic_returned == true) { // check if picture-take-callback has returned already
                double diff = System.currentTimeMillis() - _ts_lastframe;
                if (diff >= (1. / _framerate) * 1000.) {

                    _pic_returned = false;
                    try {
                        mCamera.takePicture(null, null, mPicture);
                    }catch(Exception e){
                        //Log.e("TakePicture", "Exception: ", e);
                        Toast.makeText(MainActivity.this, "TakePicture: " + e, Toast.LENGTH_LONG);
                    }
                    bestProvider = mLocationManager.getBestProvider(criteria, false);
                    _loc = mLocationManager.getLastKnownLocation(bestProvider);

                    // unsmoothed
                    double fps = 1000. / diff;

                    textview_coords.setText("Coordinates: " + _loc.getLatitude() + ", " + _loc.getLongitude() + ", Acc:" + _loc.getAccuracy());
                    textview_fps.setText(String.format( "FPS: %.1f", fps ));
                    try {
                        serializer.startTag(null, "Frame");
                        serializer.attribute(null, "uri", _last_fname);
                        serializer.attribute(null, "lat", ""+_loc.getLatitude());
                        serializer.attribute(null, "lon", ""+_loc.getLongitude());
                        serializer.attribute(null, "acc", ""+ _loc.getAccuracy());
                        serializer.attribute(null, "img_w", ""+parameters.getPictureSize().width);
                        serializer.attribute(null, "img_h", ""+parameters.getPictureSize().height);
                        serializer.attribute(null, "speed", ""+_loc.getSpeed());
                        serializer.attribute(null, "ts_cam", ""+_ts_lastpic);
                        serializer.attribute(null, "avelx", ""+_gyro_roll);
                        serializer.attribute(null, "avely", ""+_gyro_head);
                        serializer.attribute(null, "avelz", ""+_gyro_pitch);
                        serializer.attribute(null, "accx", ""+_accel_x);
                        serializer.attribute(null, "accy", ""+_accel_y);
                        serializer.attribute(null, "accz", ""+_accel_z);

                        serializer.endTag(null, "Frame");
                        serializer.flush();
                    }catch(IOException e){
                        Toast.makeText(MainActivity.this, "Serializer IOExcept: " + e, Toast.LENGTH_LONG);
                        //Log.e("serializer", "IOException: " + e);
                    }
                    _ts_lastframe = System.currentTimeMillis();
                }
            }
            handler.postDelayed(grab_frame, 1);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE }, MY_PERMISSION_REQUEST);
        }else {
            Log.d("OnResume", "--------------------------------- startMain");
            startMain();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        _main_runs = false;
    }

    @Override
    protected void onPause() {
        Log.d("OnPause", "--------------------------------- onPause has been called!");
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        _main_runs = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d("OnPermResult", "--------------------------------- startMain");
            startMain();
        }
    };

    final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {}
        public void onProviderDisabled(String provider){}
        public void onProviderEnabled(String provider){ }
        public void onStatusChanged(String provider, int status, Bundle extras){ }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // FIXME: there is only really one directory on SD we can possibly write to,
        // so took this choice from the user...
        // FIXME: what if there is no SD-card?
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            prefs.edit().putString("folderPicker", getExternalFilesDirs(null)[1].toString()).commit();
        }catch(Exception e){
            Toast.makeText(this, "Unable to get external storage dir, defaulting to internal", Toast.LENGTH_LONG);
            prefs.edit().putString("folderPicker", getExternalFilesDir(null).toString()).commit();
        }

        final ImageButton settingsButton = (ImageButton) findViewById(R.id.button_settings);
        settingsButton.setOnClickListener(
                new android.view.View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        /*if (mCamera != null) {
                            mCamera.stopPreview();
                            mCamera.release();
                            mCamera = null;
                        }*/
                        startActivity(intent);
                    }
                }
        );

        final ImageButton captureButton = (ImageButton) findViewById(R.id.button_capture);
        //captureButton.setBackgroundColor(Color.GREEN);

        textview_coords = (TextView) findViewById(R.id.textview_coords);
        textview_fps = (TextView) findViewById(R.id.textview_fps);
        textview_battery = (TextView) findViewById(R.id.textview_battery);
        textview_sensors = (TextView) findViewById(R.id.textview_sensors);

        captureButton.setOnClickListener(
                new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!recording) {
                            // get an image from the camera
                            try {
                                _seq_timestamp = System.currentTimeMillis();
                                _mediaStorageDir = new File(_folder , "multisensorgrabber_" + _seq_timestamp);
                                if (! _mediaStorageDir.exists()){
                                    if (! _mediaStorageDir.mkdirs()){
                                        Toast.makeText(MainActivity.this, "Could not cretae dir: " + _mediaStorageDir, Toast.LENGTH_LONG);
                                        return;
                                    }
                                }

                                fileos = new FileOutputStream(getOutputMediaFile("xml"));
                                serializer.setOutput(fileos, "UTF-8");
                                serializer.startDocument(null, Boolean.valueOf(true));
                                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                                serializer.startTag(null, "sequence");
                                serializer.attribute(null, "folder", _mediaStorageDir.getAbsolutePath());
                                String manufacturer = Build.MANUFACTURER;
                                String model = Build.MODEL;
                                serializer.attribute(null, "sensor", manufacturer+model);
                                serializer.attribute(null, "ts", ""+_seq_timestamp);
                                serializer.attribute(null, "whitebalance", mCamera.getParameters().get("whitebalance").toString());

                            }catch(FileNotFoundException e){
                                Toast.makeText(MainActivity.this, "File not found: " + fileos.toString(), Toast.LENGTH_LONG);
                            }catch(IOException e){
                                Toast.makeText(MainActivity.this, "Serializer IOExcept: " + e, Toast.LENGTH_LONG);
                            }

                            handler.postDelayed(grab_frame, 100);
                            recording = true;
                            //captureButton.setBackgroundColor(Color.RED);
                            captureButton.setImageResource(R.mipmap.button_icon_rec_on);
                        }else{
                            handler.removeCallbacks(grab_frame);
                            recording = false;
                            try {
                                serializer.endTag(null, "sequence");
                                serializer.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                fileos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            captureButton.setImageResource(R.mipmap.button_icon_rec);
                        }
                    }
                }
        );

    }

    private final SensorEventListener sel = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                updateOrientation(event.values[0], event.values[1], event.values[2]);
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                updateAccels(event.values[0], event.values[1], event.values[2]);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    // FIXME: are semaphores needed here?
    private void updateOrientation(float heading, float pitch, float roll) {
        _gyro_head = heading;
        _gyro_pitch = pitch;
        _gyro_roll = roll;
    }

    private void updateAccels(float x, float y, float z){
        _accel_x = x;
        _accel_y = y;
        _accel_z = z;
    }

    /* start camera preview and enable button */
    private void startMain(){
        if(!_main_runs) {
            _main_runs = true;

            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            bestProvider = mLocationManager.getBestProvider(criteria, false);
            mLocationManager.requestLocationUpdates(bestProvider, 10,10, locationListener);

            SensorManager sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            sm.registerListener(sel,
                    sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    SensorManager.SENSOR_DELAY_UI);
            sm.registerListener(sel,
                    sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI);

            handler.postDelayed(grab_system_data, 1);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            String selected_fr = prefs.getString("pref_framerates", "");
            if (selected_fr != "") {
                _framerate = Integer.parseInt(selected_fr);
            }

            // this is the main folder.
            // sequence folders (_mediaStorageDir) will be sub-directories of this one
            _folder = prefs.getString("folderPicker", "/");

            mCamera = getCameraInstance();
            parameters = mCamera.getParameters();

            // FIXME: implement this in a way to enable user to save to file and send to me!
            Log.d("Paremters", mCamera.getParameters().flatten());

            String selected_res = prefs.getString("pref_resolutions", ""); // this gives the value
            if (selected_res != "") {
                final List<android.hardware.Camera.Size> sizes = parameters.getSupportedPictureSizes();
                Integer width = sizes.get(Integer.parseInt(selected_res)).width;
                Integer height = sizes.get(Integer.parseInt(selected_res)).height;
                parameters.setPictureSize(width, height);
            }

            String focusmode = prefs.getString("pref_focusmode", "infinity");
            Log.d("focusmode:", focusmode);
            parameters.setFocusMode(focusmode);

            mCamera.setParameters(parameters);

            // Create Preview view and set it as the content of our activity.
            // this HAS to be done in order to able to take pictures (WTF?!)
            if(mPreview == null){
                mPreview = new CameraPreview(this, mCamera);
                RelativeLayout preview = (RelativeLayout) findViewById(R.id.camera_preview);
                preview.addView(mPreview);

            }else{
                RelativeLayout preview = (RelativeLayout) findViewById(R.id.camera_preview);

                mPreview = new CameraPreview(this, mCamera);
                preview.removeAllViews();
                preview.addView(mPreview);
            }

            /*********************************** GPS ************************/
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile("jpg");
            if (pictureFile == null){
                Toast.makeText(MainActivity.this, "Could not create PictureFile", Toast.LENGTH_LONG);
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                _pic_returned = true;
            } catch (FileNotFoundException e) {
                Toast.makeText(MainActivity.this, "FileNotFoundExc: "+e.getMessage(), Toast.LENGTH_LONG);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "IOexcept: "+ e.getMessage(), Toast.LENGTH_LONG);
            }
        }
    };

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

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(String ending){
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        long timeStamp = System.currentTimeMillis();
        File mediaFile;
        if (ending == "jpg"){
            mediaFile = new File(_mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
            _last_fname ="IMG_"+ timeStamp + ".jpg"; // this goes only to img_uri in xml
            _ts_lastpic = timeStamp;
        } else if(ending == "xml") {
            mediaFile = new File(_mediaStorageDir.getPath() + File.separator +
                    timeStamp + ".xml");
        } else {
            return null;
        }

        return mediaFile;
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }
}
