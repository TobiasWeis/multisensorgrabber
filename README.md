# Multi-Sensor Grabber

App that is able to grab videos (in form of image-frames) that are annotated with

* GPS
* Accelerometer
* Gyroscope
* Timestamp

to **create low-cost, mobile sequence capturing devices**.

![Multi-Sensor Grabber](app_screenshot.png "Multi-Sensor Grabber")

## Currently implemented
* Acquire rights (camera, gps, etc.)
* Fullscreen app with overlayed button start/stop capturing
* Getting and setting desired image resolution
* Selection of image resolution
* Selection of framerates (10,15,...)
* Frame includes: Timestamp, Image, GPS-data (lat,lon,accuracy,speed) 
* Save images to folder, metadata to xml-file

Current XML-Output:
```xml
<sequence 
    folder="/storage/0000-0000/Android/data/com.example.weis.cv_grabber/files/multisensorgrabber_1482583173808" 
    sensor="samsungSM-G903F" 
    ts="1482583173808" 
    whitebalance="auto"
>

    <Frame 
        uri="IMG_1482583174241.jpg" 
        lat="50.16629447649796" 
        lon="8.658641955498677" 
        acc="48.0" 
        img_w="960" 
        img_h="720" 
        speed="0.0" 
        ts_cam="1482583174241" 
        avelx="-0.012356800958514214" 
        avely="0.020737236365675926" 
        avelz="-0.014283332042396069"
    />

    <Frame 
        uri="IMG_1482583174530.jpg" 
        lat="50.16629447649796" 
        lon="8.658641955498677" 
        acc="48.0" 
        img_w="960" 
        img_h="720" 
        speed="0.0" 
        ts_cam="1482583174530" 
        avelx="0.0036221654154360294" 
        avely="-0.002698581200093031" 
        avelz="-0.02280544675886631"
    />

    ...
</sequence>
```

## TODO
* ~~Settings Activity?~~
* ~~choose framerate (and implement)~~
* ~~gps grabbing~~
* accel/gyro grabbing
* Saving: ~~xml~~/sqlite ?
