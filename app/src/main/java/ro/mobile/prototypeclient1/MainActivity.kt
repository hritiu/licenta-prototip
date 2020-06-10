package ro.mobile.prototypeclient1

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import androidx.preference.Preference
import com.google.android.gms.location.*
import org.w3c.dom.Text
import java.util.ArrayList
import java.util.jar.Manifest

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "MainActivity"
    private lateinit var mContext: Context
    private lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    private lateinit var mAdapter: DetectedActivitiesAdapter
    lateinit var mainHandler: Handler

    private val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient

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
            mainHandler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //getLastLocation()

        mContext = this;
        val detectedActivitiesListView: ListView = findViewById(R.id.detected_activities_listview)


        val detectedActivities: ArrayList<DetectedActivity?>? = Utils.detectedActivitiesFromJson(
            this.getSharedPreferences("ro.mobile.prototypeclient1_preferences", 0).getString(
                Constants.KEY_DETECTED_ACTIVITIES,
                ""
            )!!
        )

//        Utils.detectedActivitiesFromJson(
//            this.getSharedPreferences("ro.mobile.prototypeclient1_preferences", 0).getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
//        )


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
                mainHandler.postDelayed(this, 10000)
            }
        }

//        val task = mActivityRecognitionClient.requestActivityUpdates(
//            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
//            getActivityDetectionPendingIntent()
//        )

//        task.addOnSuccessListener {
//            Log.v("BUBA", "success")
//            Toast.makeText(
//                mContext,
//                getString(R.string.activity_updates_enabled),
//                Toast.LENGTH_SHORT
//            )
//                .show()
//
//            updateDetectedActivitiesList()
//        }
//
//        task.addOnFailureListener {
//            Log.v("BUBA", "fail")
//            Toast.makeText(
//                mContext,
//                getString(R.string.activity_updates_not_enabled),
//                Toast.LENGTH_SHORT
//            )
//                .show()
//        }
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
            Log.w(TAG, "Failed to enable activity recognition.")
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
        Toast.makeText(this, "A C T I V I T Y   C H A N G E D", Toast.LENGTH_LONG).show()

        if (detectedActivities?.get(0)!!.type == DetectedActivity.IN_VEHICLE) {
            isDriving = true;
            isWalking = false;
        } else if (detectedActivities[0]!!.type == DetectedActivity.ON_FOOT || detectedActivities[0]!!.type == DetectedActivity.WALKING) {
            if(isDriving == true) {
                determineLocation()
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
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID) {
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

    fun clearFile(view: View) {
        var filePath = mContext.filesDir.path.toString() + "locations.txt"
        var locationsFile: File = File(filePath)
        locationsFile.createNewFile()

        locationsFile.printWriter().print("")
    }

    fun determineLocation() {
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
                    var locationPoint = Location("Location Point")
                    locationPoint.latitude = location!!.latitude
                    locationPoint.longitude = location.longitude

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

    private fun addLocationToFile(location: Location) {
        var filePath = mContext.filesDir.path.toString() + "locations.txt"
        var locationsFile: File = File(filePath)
        locationsFile.createNewFile()

        var locations: ArrayList<String> =
            locationsFile.bufferedReader().readLines() as ArrayList<String>

        locations.add("${location.latitude},${location.longitude}")

        locationsFile.printWriter()
            .use { out -> locations.forEach { out.println(it); } }

        logCoordinatesAndDistance(locations)
    }

    private fun determineDistance(locationA: Location, locationB: Location): Float {
        return locationA.distanceTo(locationB)
    }

    private fun logCoordinatesAndDistance(locations: ArrayList<String>) {
        var coordinateLocations: ArrayList<Location> = ArrayList()

        locations.forEach {
            var coordinates = it.split(",")
            var latitude = coordinates[0].toDouble()
            var longitude = coordinates[1].toDouble()

            val coordinateLocation = Location("")
            coordinateLocation.latitude = latitude
            coordinateLocation.longitude = longitude

            coordinateLocations.add(coordinateLocation)
        }

        for (pos in 1..coordinateLocations.size - 1) {
            Log.v(
                "GPS_Location",
                "[pos - 1]: lat = ${coordinateLocations[pos - 1].latitude} lon = ${coordinateLocations[pos - 1].longitude}"
            )
            Log.v(
                "GPS_Location",
                "[pos]: lat = ${coordinateLocations[pos].latitude} lon = ${coordinateLocations[pos].longitude}"
            )
            Log.v(
                "GPS_Location",
                "distance = ${determineDistance(
                    coordinateLocations[pos - 1],
                    coordinateLocations[pos]
                )}"
            )
        }
    }
}
