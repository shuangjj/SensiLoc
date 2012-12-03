Introduction
---------------------
This is the final project for CIS8590. It's an Android application to evaluate the accuracy and energy efficiency with different kinds of location sensors and some auxiliary sensors.

In this project, we propose a method to coordinate the usage of GPS and WiFi based on the current moving status of the device. In order to get the moving status of the device, we use accelerometer and magnetic sensors to detect the orientation changes of the device. Current supported moving status includes:<br />
 * Turning
 * Walk straight

Following is the information about the development environments:
 * CentOS 6.5
 * Eclipse  + Android ADT
 * Android SDK
 

Program Structure
--------------------

Acknowlegement
--------------------

Known Problems
------------
1. Setup linux to detect your hardware device
If you're developing on linux OS (Ubuntu, CentOS), you need to add a udev rules file that contains a USB configuration for the device you want to use for development.
 * Search vender ID of your device <br />
   Plug your device and look up the vendor ID using command ``lsusb``, you'll get information about your device like this: <br />
   ```
   Bus 001 Device 007: ID 22b8:428c Motorola PCS
   ``` 
   <br />
   _22b8_ is the vendor ID of my device.
 * Create USB udev file for your device <br />
   The file you need to create is: `/etc/udev/rules.d/51-android.rules`. <br />
   Use this format to add each vendor to this file: <br />
   ```
   SUBSYSTEM=="usb", ATTR{idVendor}=="0bb4", MODE="0666", GROUP="plugdev" 
   ```


