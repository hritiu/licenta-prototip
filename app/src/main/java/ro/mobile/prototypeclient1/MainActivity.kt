package ro.mobile.prototypeclient1

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import java.util.ArrayList

import com.google.gson.Gson
import java.io.*

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mContext: Context
    private lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    private lateinit var mAdapter: DetectedActivitiesAdapter
    private lateinit var walkingActivityLog: ActivityLog
    private lateinit var drivingActivityLog: ActivityLog

    lateinit var mainHandler: Handler

    private var isDriving = true
    private var isWalking = false

    private val updateTextTask = object : Runnable {
        override fun run() {
            val task =
                mActivityRecognitionClient.requestActivityUpdates(
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    getActivityDetectionPendingIntent()
                )

            requestActivityUpdates()
            updateDetectedActivitiesList()
            mainHandler.postDelayed(this, Constants.DETECTION_INTERVAL_IN_MILLISECONDS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.walkingActivityLog = ActivityLog(Constants.WALKING, HashMap<Int, String>())
        this.drivingActivityLog = ActivityLog(Constants.DRIVING, HashMap<Int, String>())

        mContext = this;
        val detectedActivitiesListView: ListView = findViewById(R.id.detected_activities_listview)

        val detectedActivities: ArrayList<DetectedActivity?>? = Utils.detectedActivitiesFromJson(
            this.getSharedPreferences("ro.mobile.prototypeclient1_preferences", 0).getString(
                Constants.KEY_DETECTED_ACTIVITIES,
                ""
            )!!
        )

        mAdapter = DetectedActivitiesAdapter(this, detectedActivities)
        detectedActivitiesListView.adapter = mAdapter
        mActivityRecognitionClient = ActivityRecognitionClient(this)

        mainHandler = Handler(Looper.getMainLooper())
        updateTextTask.run()
        requestActivityUpdates()
    }

    @Override
    override fun onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    fun getActivityDetectionPendingIntent(): PendingIntent {
        val intent = Intent(this, DetectedActivitiesIntentService::class.java)
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun requestActivityUpdates() {

        val requestTask = object : Runnable {
            override fun run() {
                val task = mActivityRecognitionClient.requestActivityUpdates(
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    getActivityDetectionPendingIntent()
                )
                mainHandler.postDelayed(this, Constants.DETECTION_INTERVAL_IN_MILLISECONDS)
            }
        }
    }

    fun getUpdatesRequestedState(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_ACTIVITY_UPDATES_REQUESTED, false)
    }

    fun removeActivityUpdatesButtonHandler(view: View?) {
        val task =
            mActivityRecognitionClient.removeActivityUpdates(
                getActivityDetectionPendingIntent()
            )
        task.addOnSuccessListener {
            Toast.makeText(
                mContext,
                getString(R.string.activity_updates_removed),
                Toast.LENGTH_SHORT
            )
                .show()
            // Reset the display.
            mAdapter.updateActivities(java.util.ArrayList())
        }
        task.addOnFailureListener {
            Log.w(Constants.TAG, "Failed to enable activity recognition.")
            Toast.makeText(
                mContext, getString(R.string.activity_updates_not_removed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateDetectedActivitiesList() {
        val detectedActivities: ArrayList<DetectedActivity?>? =
            Utils.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
            )

        if (detectedActivities?.get(0)!!.type == DetectedActivity.IN_VEHICLE) {
            isDriving = true;
            isWalking = false;

            //!!!!!!!!!!!!!!!!!!!!!!!!!
                //add accuracy and location to driving log
            val location = getCurrentLocation()
            this.drivingActivityLog.addActivity(detectedActivities[0]!!. gv, locationToString(location!!))
            //!!!!!!!!!!!!!!!!!!!!!!!!!

        } else if (detectedActivities[0]!!.type == DetectedActivity.ON_FOOT || detectedActivities[0]!!.type == DetectedActivity.WALKING) {

            //!!!!!!!!!!!!!!!!!!!!!!!!!
                //add accuracy and location to walking log
            val location = getCurrentLocation()
            this.walkingActivityLog.addActivity(detectedActivities[0]!!.confidence, locationToString(location!!))
            //!!!!!!!!!!!!!!!!!!!!!!!!!

            if (isDriving) {
                //determineLocation()
                getCurrentLocation()?.let { addLocationToFile(it) }
                Toast.makeText(this, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG).show()
                val vibrator = mContext?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(200)
                }
            }
            isDriving = false;
            isWalking = true;
        }

        mAdapter.updateActivities(detectedActivities)
    }

    @Override
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(Constants.KEY_DETECTED_ACTIVITIES)) {
            updateDetectedActivitiesList()
        }
    }

    //location methods
    //checks if the user grant the access to location
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    //requests the necessary permissions if the user didn't gave access
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            Constants.PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Granted. Start getting the location info
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
        }
    }

    //deletes everything from the locations file
    fun clearFile() {
        var filePath = mContext.filesDir.path.toString() + "Locations.json"
        var locationsFile: File = File(filePath)
        locationsFile.createNewFile()

        locationsFile.printWriter().print("")
    }

    //gets the current location
    private fun determineLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                var mLocationRequest = LocationRequest()
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                mLocationRequest.interval = 0
                mLocationRequest.fastestInterval = 0
                mLocationRequest.numUpdates = 1

                var mFusedLocationAClient = LocationServices.getFusedLocationProviderClient(this)
                mFusedLocationAClient!!.requestLocationUpdates(
                    mLocationRequest, mLocationCallback,
                    Looper.myLooper()
                )

                mFusedLocationAClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    var locationPoint = Location(location?.let { locationToString(it) })
                    locationPoint.latitude = location!!.latitude
                    locationPoint.longitude = location!!.longitude

                    addLocationToFile(locationPoint)
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    private fun getCurrentLocation(): Location? {
        var locationPoint: Location? = null

        if(checkPermissions()) {
            if(isLocationEnabled()) {
                val locationRequest = LocationRequest()
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                locationRequest.interval = 0
                locationRequest.fastestInterval = 0
                locationRequest.numUpdates = 1

                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient!!.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper())

                fusedLocationClient.lastLocation.addOnCompleteListener(this) {task ->
                    val location: Location? = task.result
                    locationPoint = Location(location?.let { locationToString(it) })
                    locationPoint!!.latitude = location!!.latitude
                    locationPoint!!.longitude = location.longitude
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }

        return locationPoint
    }

    //adds a given location to the json file that stores the locations
    private fun addLocationToFile(location: Location) {
        val locationsFile: File = File(mContext.filesDir.path, Constants.FILE_LOCATION)
        val gson = Gson()
        lateinit var area: Area

        if (locationsFile.exists()) {
            var jsonString = this.readJsonFromFile(locationsFile)
            area = gson.fromJson(jsonString, Area::class.java)
        } else {
            locationsFile.createNewFile()
            var areaStructure: HashMap<String, ArrayList<String>> = HashMap()
            area = Area(areaStructure)
        }

        //add location in the area
        this.addLocationToParkingAreas(location, area)

        //write the area into the file
        val jsonStringToWrite = gson.toJson(area)
        this.writeJsonToFile(jsonStringToWrite, locationsFile)
    }

    //computes the distance between 2 coordinates
    private fun determineDistance(locationA: Location, locationB: Location): Float {
        return locationA.distanceTo(locationB)
    }

    //adds a parking location to an area of parking locations
    private fun addLocationToParkingAreas(location: Location, parkingArea: Area) {
        var found = false
        for (coordinate in parkingArea.areas.keys) {
            if (determineDistance(stringToLocation(coordinate), location) <= 50) {
                parkingArea.areas.get(coordinate)?.add(locationToString(location))
                found = true
            }
        }

        if (!found) {
            parkingArea.areas.put(locationToString(location), ArrayList<String>())
        }
    }

    //writes the locations into a json file
    private fun writeJsonToFile(jsonString: String, file: File) {
        val output: Writer = BufferedWriter(FileWriter(file))
        output.write(jsonString)
        output.close()
    }

    //reads data from json file that stores the locations
    private fun readJsonFromFile(file: File): String? {
        val jsonString: String

        try {
            jsonString = file.readText()
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            Log.v("BUBA_JSON", "It seems that we have an exception")
            return null
        }

        return jsonString
    }

    //converts location to string
    private fun locationToString(location: Location): String {
        return location.latitude.toString() + "," + location.longitude.toString()
    }

    //converts string to location
    private fun stringToLocation(stringLocation: String): Location {
        var location: Location = Location(stringLocation)

        val strings = stringLocation.split(",").toTypedArray()
        location.latitude = strings[0].toDouble()
        location.longitude = strings[1].toDouble()

        return location
    }

    //methods for DRIVE and WALK buttons
    fun setDrivingActivity(view: View) {
        val detectedActivity: DetectedActivity = DetectedActivity(DetectedActivity.IN_VEHICLE, 99)
        updateDetectedActivitiesListButtons(detectedActivity)
    }

    fun setWalkingActivity(view: View) {
        val detectedActivity: DetectedActivity = DetectedActivity(DetectedActivity.WALKING, 99)
        updateDetectedActivitiesListButtons(detectedActivity)
    }

    private fun updateDetectedActivitiesListButtons(detectedActivity: DetectedActivity) {
        val detectedActivities: ArrayList<DetectedActivity?>? =
            Utils.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
            )

        detectedActivities?.clear()
        detectedActivities?.add(detectedActivity)

        if (detectedActivities?.get(0)!!.type == DetectedActivity.IN_VEHICLE) {
            isDriving = true;
            isWalking = false;
        } else if (detectedActivities[0]!!.type == DetectedActivity.ON_FOOT || detectedActivities[0]!!.type == DetectedActivity.WALKING) {
            if (isDriving == true) {
//                determineLocation()
                getCurrentLocation()?.let { addLocationToFile(it) }
                Toast.makeText(this, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG).show()
            }
            isDriving = false;
            isWalking = true;
        }

        mAdapter.updateActivities(detectedActivities)
    }

    private fun chooseLocation(activity: String, accuracy: Int, location: String) {

    }
}
