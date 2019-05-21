# geoCommunicator

Android app that communicates GPS and sensor information to a database. 

------------
Note
------------
* The code is written in Kotlin. It is recent language and has some key advantages over Java for making Android apps. I would recommend familiarizing with the language: https://kotlinlang.org/

* The app compiles with libraries from AndroidX. It needs to be set in the project. Read more about it here: https://developer.android.com/jetpack/androidx



-------------
MainActivity
-------------

Receives sensor and GPS information from all the background services, posts them to the database, and updates the UI.
 
----------------
Background services
----------------

* ActivityUpdateService: Gives updates about user activity (e.g. walking, running, on bike, etc) from the goole Activity API: https://developer.android.com/reference/android/app/Activity

* BatteryMonitoringService: Provides information about the status of the battery; the battery level, whether it's charging or not, battery status, etc: https://developer.android.com/training/monitoring-device-state/battery-monitoring

* LocationUpdateService: Provides GPS information; latitude, longitude, horizontal accuracy, altitude, etc. More information about these parameters and functions can be found here: 

	1. https://developer.android.com/reference/android/location/Location
	2. https://developer.android.com/training/location/receive-location-updates

Something for future work: Geocoding: https://developer.android.com/reference/android/location/Geocoder

* SensorService: Gives information about different sensors, e.g. accelerometer, light, temperature, etc. https://developer.android.com/guide/topics/sensors/sensors_overview


--------
Other files
--------
* sendDeviceDetails: Updates the database with JSON objects consisting of the GPS and sensor information.

* Functions: Includes a few functions, e.g. to create JSON object for the database.

* User: The data structure for the 'User' object, which defines all the values of interest. Includes all the sensor and GPS values. 

* FirebaseConstructor: Only interesting if experimenting with Firebase.

* Constants: File including permission codes and authentication information used in the project.
