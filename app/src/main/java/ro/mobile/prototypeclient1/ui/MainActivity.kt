package ro.mobile.prototypeclient1.ui

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
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.android.awaitFrame
import ro.mobile.prototypeclient1.R
import ro.mobile.prototypeclient1.common.Constants
import ro.mobile.prototypeclient1.common.Utils
import ro.mobile.prototypeclient1.domain.*
import ro.mobile.prototypeclient1.domain.DetectedActivitiesAdapter
import java.io.File
import java.lang.Runnable
import java.util.ArrayList
import kotlin.Result as Result

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mContext: Context
    private lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    private lateinit var mAdapter: DetectedActivitiesAdapter
    private lateinit var mainHandler: Handler

    private var isDriving = true
    private var isWalking = false
    private var writeLog = true
    private var activityHandler = ActivityHandler()
    private var fileHandler = FileHandler()
    private var locationHandler = LocationHandler()
    private var activityLog = ActivityLog(ArrayList<Pair<String, String>>())

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

        mAdapter = DetectedActivitiesAdapter(
            this,
            detectedActivities
        )
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
                PreferenceManager.getDefaultSharedPreferences(mContext),
                this@MainActivity,
                isDriving,
                isWalking
            )
        }
//            isDriving = updateDetectedActivitiesResponse["isDriving"] as Boolean
//            isWalking = updateDetectedActivitiesResponse["isWalking"] as Boolean
//
//            val detectedActivities =
//                arrayOf(updateDetectedActivitiesResponse["detectedActivities"]) as ArrayList<DetectedActivity>
//            mAdapter.updateActivities(detectedActivities)
//
//            if (updateDetectedActivitiesResponse["callDetermineLocation"] == true) {
//                val parkingDetection = ParkingDetection(mContext)
//                val determinedLocation = parkingDetection.detectParkingLocation()
//
//                var writeLocationToFile = false
//                if (determinedLocation != null) {
//                    fileHandler.addLocationToFile(determinedLocation, mContext)
//                } else {
//                    writeLocationToFile = true
//                }
//
//                determineLocation(detectedActivities, writeLocationToFile)
//
//                Toast.makeText(this@MainActivity, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG)
//                    .show()
//                Log.v("ACTIVITY_LOG", "L O C A T I O N    S A V E D")
//            }
//            if (isWalking) {
//                for (detectedActivity in detectedActivities) {
//                    fileHandler.writeExtraLog(
//                        "detectedActivity = ${Utils.activityTypeToString(detectedActivity.type)}",
//                        location,
//                        mContext
//                    )
//                    if (Utils.activityTypeToString(detectedActivity.type) == "WALKING" || Utils.activityTypeToString(
//                            detectedActivity.type
//                        ) == "ON_FOOT"
//                    ) {
//                        if (Utils.activityTypeToString(detectedActivities[0].type) != "UNKNOWN") {
//                            fileHandler.writeLogToFile(
//                                detectedActivity.confidence,
//                                location,
//                                mContext
//                            )
//                        } else {
//                            fileHandler.writeLogToFile(-1, location, mContext)
//                        }
//                        break
//                    }
//                }
//            }
//
//            if (updateDetectedActivitiesResponse["callDetermineLocation"] == true) {
//                var parkingDetection = ParkingDetection(mContext)
//                val determinedLocation = parkingDetection.detectParkingLocation()
//
//                if (determinedLocation != null) {
//                    fileHandler.addLocationToFile(determinedLocation, mContext)
//                } else {
//                    fileHandler.addLocationToFile(location, mContext)
//                }
////                fileHandler.addLocationToFile(location, mContext)
//                Toast.makeText(this@MainActivity, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG)
//                    .show()
//                Log.v("ACTIVITY_LOG", "L O C A T I O N    S A V E D")
//            }
        //}
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
                PreferenceManager.getDefaultSharedPreferences(mContext),
                this@MainActivity,
                isDriving,
                isWalking
            )

            isDriving = updateDetectedActivitiesResponse["isDriving"] as Boolean
            isWalking = updateDetectedActivitiesResponse["isWalking"] as Boolean

            val detectedActivities =
                updateDetectedActivitiesResponse["detectedActivities"] as ArrayList<DetectedActivity>
            mAdapter.updateActivities(detectedActivities)

            var activityConfidence = -2
            for(activity in detectedActivities) {
                if(Utils.activityTypeToString(activity.type) == "WALKING") {
                    if(Utils.activityTypeToString(detectedActivities[0].type) == "UNKNOWN") {
                        Log.v("BUBA", "U P D A T E main activity = UNKNOWN    activity = WALKING   confidence = ${activity.confidence}")
                        activityConfidence = -1
                    } else {
                        Log.v("BUBA", "U P D A T E activity = WALKING   confidence = ${activity.confidence}")
                        activityConfidence = activity.confidence
                    }
                }
            }

            if (updateDetectedActivitiesResponse["callDetermineLocation"] == true) {
                val parkingDetection = ParkingDetection(mContext)
                val determinedLocation = parkingDetection.detectParkingLocation()

                var writeLocationToFile = false
                if (determinedLocation != null) {
                    fileHandler.addLocationToFile(determinedLocation, mContext)
                    clearLogFiles()
                    writeLog = false
                    Toast.makeText(this@MainActivity, "D E T E R M I N E D", Toast.LENGTH_LONG).show()

                    Log.v("BUBA", "")
                    Log.v("BUBA", "Start Activity log print")
                    for(pair in activityLog.pairs) {
                        Log.v("BUBA", "confidence = ${pair.first}   location = ${pair.second}")
                    }
                    Log.v("BUBA", "")
                    Log.v("BUBA", "End Activity log print")
                } else {
                    writeLocationToFile = true
                    Toast.makeText(this@MainActivity, "L A S T", Toast.LENGTH_LONG).show()
                }

                determineLocation(activityConfidence, writeLocationToFile)
            } else {
                determineLocation(activityConfidence, false)
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
    private fun determineLocation(activitiConfidence: Int, writeLocationToFile: Boolean) {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if(isDriving) {
                    this.writeLog = true
                }

                val mLocationRequest = LocationRequest()
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                mLocationRequest.interval = 0
                mLocationRequest.fastestInterval = 0
                mLocationRequest.numUpdates = 1

                val mFusedLocationAClient = LocationServices.getFusedLocationProviderClient(this)
                mFusedLocationAClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())

                Log.v("BUBA", "Log in D E T E R M I N E")

                mFusedLocationAClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    val locationPoint = Location(location?.let { Utils.locationToString(it) })
                    locationPoint.latitude = location!!.latitude
                    locationPoint.longitude = location.longitude

                    if(activitiConfidence != -2 && writeLog) {
                        fileHandler.writeLogToFile(activitiConfidence, locationPoint, mContext)
                        Log.v("BUBA", "D E T E R M I N E   T A S K confidence = $activitiConfidence")
                        Log.v("BUBA", "")
                        activityLog.pairs.add(Pair(activitiConfidence.toString(), Utils.locationToString(locationPoint)))
                    } else {
                        Log.v("BUBA", "D E T E R M I N E   T A S K   N O    W A L K I N G")
                        if(!writeLog) {
                            Log.v("BUBA", "D E T E R M I N E   T A S K   N O    write log permission")
                        }
                        Log.v("BUBA", "")
                    }

                    if(writeLocationToFile) {
                        fileHandler.addLocationToFile(locationPoint, mContext)
                        this.writeLog = false
                        clearLogFiles()
                    }
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

    fun clearFiles(view: View) {
        val fileHandler = FileHandler()
        fileHandler.clearFiles(mContext)
    }

    fun clearFilesNoView() {
        val fileHandler = FileHandler()
        fileHandler.clearFiles(mContext)
    }

    private fun clearLogFiles() {
        val fileHandler = FileHandler()
        fileHandler.clearLogFiles(mContext)
    }

    //methods for DRIVE and WALK buttons
//    fun setDrivingActivity(view: View) {
//        val detectedActivity: DetectedActivity = DetectedActivity(DetectedActivity.IN_VEHICLE, 99)
//        updateDetectedActivitiesListButtons(detectedActivity)
//    }
//
//    fun setWalkingActivity(view: View) {
//        val detectedActivity: DetectedActivity = DetectedActivity(DetectedActivity.WALKING, 99)
//        updateDetectedActivitiesListButtons(detectedActivity)
//    }

//    private fun updateDetectedActivitiesListButtons(detectedActivity: DetectedActivity) {
//        val detectedActivities: ArrayList<DetectedActivity?>? =
//            Utils.detectedActivitiesFromJson(
//                PreferenceManager.getDefaultSharedPreferences(mContext)
//                    .getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
//            )
//
//        detectedActivities?.clear()
//        detectedActivities?.add(detectedActivity)
//
//        if (detectedActivities?.get(0)!!.type == DetectedActivity.IN_VEHICLE) {
//            isDriving = true;
//            isWalking = false;
//        } else if (detectedActivities[0]!!.type == DetectedActivity.ON_FOOT || detectedActivities[0]!!.type == DetectedActivity.WALKING) {
//            if (isDriving == true) {
//                determineLocation(detectedActivities, false)
//                Toast.makeText(this, "L O C A T I O N    S A V E D", Toast.LENGTH_LONG).show()
//            }
//            isDriving = false;
//            isWalking = true;
//        }
//
////        mAdapter.updateActivities(detectedActivities)
//    }
//
//    fun startWalking(view: View) {
//        val logFile: File = File(mContext.filesDir.path, Constants.LOG_FILE_LOCATION)
////        fileHandler.writeLogToFile("S T A R T     W A L K I N G\n\n\n", null, mContext)
//        fileHandler.writeExtraLog(
//            "S T A R T     W A L K I N G\n\n\n",
//            determineLocation(),
//            mContext
//        )
//    }
//
//    fun endWalking(view: View) {
//        val logFile: File = File(mContext.filesDir.path, Constants.LOG_FILE_LOCATION)
////        fileHandler.writeLogToFile("E N D    W A L K I N G\n\n\n", mContext)
//        fileHandler.writeExtraLog("E N D    W A L K I N G\\n\\n\\n", determineLocation(), mContext)
//    }
}
