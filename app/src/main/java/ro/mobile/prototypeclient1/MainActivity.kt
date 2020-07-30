package ro.mobile.prototypeclient1

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import java.util.ArrayList

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mContext: Context
    private lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    private lateinit var mAdapter: DetectedActivitiesAdapter
    private lateinit var mainHandler: Handler

    private var isDriving = true
    private var isWalking = false
    private var activityHandler = ActivityHandler(isDriving, isWalking)
    private var fileHandler = FileHandler()
    private var locationHandler = LocationHandler()

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    @Override
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
       if (key.equals(Constants.KEY_DETECTED_ACTIVITIES)) {
            val updateDetectedActivitiesResponse = activityHandler.updateDetectedActivitiesList(
                PreferenceManager.getDefaultSharedPreferences(mContext), this@MainActivity
            )
            val detectedActivities =
                arrayOf(updateDetectedActivitiesResponse["detectedActivities"]) as ArrayList<DetectedActivity>
            mAdapter.updateActivities(detectedActivities)

            if (updateDetectedActivitiesResponse["callDetermineLocation"] == true) {
                determineLocation()
                Toast.makeText(this@MainActivity, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private val updateTextTask = object : Runnable {
        override fun run() {
            val task =
                mActivityRecognitionClient.requestActivityUpdates(
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    getActivityDetectionPendingIntent()
                )

            requestActivityUpdates()

            val updateDetectedActivitiesResponse = activityHandler.updateDetectedActivitiesList(
                PreferenceManager.getDefaultSharedPreferences(mContext), this@MainActivity
            )
            val detectedActivities = updateDetectedActivitiesResponse["detectedActivities"] as ArrayList<DetectedActivity>
            mAdapter.updateActivities(detectedActivities)

            if (updateDetectedActivitiesResponse["callDetermineLocation"] == true) {
                determineLocation()
                Toast.makeText(this@MainActivity, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG)
                    .show()
            }

            mainHandler.postDelayed(this, 3000)
        }
    }

    private fun getActivityDetectionPendingIntent(): PendingIntent {
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
                mainHandler.postDelayed(this, 3000)
            }
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
                    var locationPoint = Location(location?.let { Utils.locationToString(it) })
                    locationPoint.latitude = location!!.latitude
                    locationPoint.longitude = location!!.longitude

                    fileHandler.addLocationToFile(locationPoint, mContext)
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
                determineLocation()
                Toast.makeText(this, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG).show()
            }
            isDriving = false;
            isWalking = true;
        }

//        mAdapter.updateActivities(detectedActivities)
    }
}
