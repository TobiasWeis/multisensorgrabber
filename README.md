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

## TODO
* ~~Settings Activity?~~
* ~~choose framerate (and implement)~~
* ~~gps grabbing~~
* accel/gyro grabbing
* Saving: ~~xml~~/sqlite ?
