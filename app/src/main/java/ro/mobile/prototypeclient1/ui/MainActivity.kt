package ro.mobile.prototypeclient1.ui

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import ro.mobile.prototypeclient1.R
import ro.mobile.prototypeclient1.common.Constants
import ro.mobile.prototypeclient1.common.Utils
import ro.mobile.prototypeclient1.domain.*
import ro.mobile.prototypeclient1.domain.DetectedActivitiesAdapter
import java.lang.Runnable
import java.util.ArrayList

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.*
import com.google.gson.JsonSyntaxException

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener,
    OnMapReadyCallback {

    private lateinit var mContext: Context
    private lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    private lateinit var mAdapter: DetectedActivitiesAdapter
    private lateinit var mainHandler: Handler
    private lateinit var map: GoogleMap

    private var isDriving = true
    private var isWalking = false
    private var writeLog = false
    private var activityHandler = ActivityHandler()
    private var fileHandler = FileHandler()
    private var activityLog = ActivityLog(ArrayList<Pair<String, String>>())
    private var mapCircles = ArrayList<Circle>()

    private lateinit var notificationManagerCompat: NotificationManagerCompat


    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this
        preventAddingInvalidData()
        if (!checkPermissions()) {
            requestPermissions()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        notificationManagerCompat = NotificationManagerCompat.from(this)

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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        refreshMap()

        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (isDriving) {
                    this.writeLog = true
                }

                val mLocationRequest = LocationRequest()
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                mLocationRequest.interval = 0
                mLocationRequest.fastestInterval = 0
                mLocationRequest.numUpdates = 1

                val mFusedLocationAClient = LocationServices.getFusedLocationProviderClient(this)
                mFusedLocationAClient!!.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.myLooper()
                )

                mFusedLocationAClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    val locationPoint = Location(location?.let { Utils.locationToString(it) })
                    locationPoint.latitude = location!!.latitude
                    locationPoint.longitude = location.longitude

                    // Add a marker in Sydney and move the camera
                    val currrentLocation = LatLng(locationPoint.latitude, locationPoint.longitude)
                    map.addMarker(MarkerOptions().position(currrentLocation).title("You are here"))
                    val zoomLevel = 16.0f
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currrentLocation, zoomLevel))
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
            for (activity in detectedActivities) {
                if (Utils.activityTypeToString(activity.type) == "WALKING") {
                    if (Utils.activityTypeToString(detectedActivities[0].type) == "UNKNOWN") {
                        activityConfidence = -1
                    } else {
                        activityConfidence = activity.confidence
                    }
                }
            }

            if (updateDetectedActivitiesResponse["callDetermineLocation"] == true) {
                val parkingDetection = ParkingDetection(mContext)
                val determinedLocation = parkingDetection.detectParkingLocation()

                sendParkingDetectedNotification()

                var writeLocationToFile = false
                var parkingLocation = ""
                if (determinedLocation != null) {
                    parkingLocation = Utils.locationToString(determinedLocation)
                    fileHandler.writeExtraLog(
                        "\n\n Location was determined by the algorithm \n\n",
                        determinedLocation,
                        mContext
                    )
                    clearLogFiles()
                    writeLog = false
                } else {
                    writeLocationToFile = true
                }

                requestParkingConfirmation(activityConfidence, writeLocationToFile, parkingLocation)
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

    // // //location methods
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
        val locationManager: LocationManager =
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
    private fun determineLocation(activityConfidence: Int, writeLocationToFile: Boolean) {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (isDriving) {
                    this.writeLog = true
                }

                val mLocationRequest = LocationRequest()
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                mLocationRequest.interval = 0
                mLocationRequest.fastestInterval = 0
                mLocationRequest.numUpdates = 1

                val mFusedLocationAClient = LocationServices.getFusedLocationProviderClient(this)
                mFusedLocationAClient!!.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.myLooper()
                )

                mFusedLocationAClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    val locationPoint = Location(location?.let { Utils.locationToString(it) })
                    locationPoint.latitude = location!!.latitude
                    locationPoint.longitude = location.longitude

                    if (activityConfidence != -2 && writeLog) {
                        fileHandler.writeLogToFile(activityConfidence, locationPoint, mContext)
                        activityLog.pairs.add(
                            Pair(
                                activityConfidence.toString(),
                                Utils.locationToString(locationPoint)
                            )
                        )
                    }

                    if (writeLocationToFile) {
                        fileHandler.addLocationToFile(locationPoint, mContext)
                        if(fileHandler.checkIfLocationIsNewAreaPoint(locationPoint, mContext)) {
                            map.addCircle(CircleOptions()
                                .center(LatLng(locationPoint.latitude, locationPoint.longitude))
                                .radius(50.0)
                                .strokeColor(Color.GREEN)
                                .fillColor(Color.BLUE)
                            )
                        }
                        this.writeLog = false
                        clearLogFiles()
                        fileHandler.writeExtraLog(
                            "\n\n Location is the last known one \n\n",
                            locationPoint,
                            mContext
                        )
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

    private fun requestParkingConfirmation(
        activityConfidence: Int,
        writeLocationToFile: Boolean,
        location: String
    ) {
        lateinit var dialog: AlertDialog
        val builder = AlertDialog.Builder(this)

        builder.setMessage("Did you park on a legal parking spot?")
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    if (location != "") {
                        fileHandler.addLocationToFile(Utils.stringToLocation(location), mContext)
                    } else {
                        determineLocation(activityConfidence, writeLocationToFile)
                    }
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    determineLocation(activityConfidence, false)
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    determineLocation(activityConfidence, false)
                }
            }
        }

        builder.setPositiveButton("Yes", dialogClickListener)
        builder.setNegativeButton("No", dialogClickListener)

        dialog = builder.create()
        dialog.show()
    }

    fun sendParkingDetectedNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val contentView = RemoteViews(packageName, R.layout.activity_main)

        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_1)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("Parking detected")
            .setContentText("Did you park on a legal parking spot?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManagerCompat.notify(1, notification)
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

    fun setCurrentLocationOnMap(view: View) {
        if (isLocationEnabled()) {
            if (isDriving) {
                this.writeLog = true
            }

            val mLocationRequest = LocationRequest()
            mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequest.interval = 0
            mLocationRequest.fastestInterval = 0
            mLocationRequest.numUpdates = 1

            val mFusedLocationAClient = LocationServices.getFusedLocationProviderClient(this)
            mFusedLocationAClient!!.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.myLooper()
            )

            mFusedLocationAClient.lastLocation.addOnCompleteListener(this) { task ->
                val location: Location? = task.result
                val locationPoint = Location(location?.let { Utils.locationToString(it) })
                locationPoint.latitude = location!!.latitude
                locationPoint.longitude = location.longitude

                // Add a marker in Sydney and move the camera
                val currrentLocation = LatLng(locationPoint.latitude, locationPoint.longitude)
                map.addMarker(MarkerOptions().position(currrentLocation).title("You are here"))
                val zoomLevel = 16.0f
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currrentLocation, zoomLevel))
            }


        } else {
            Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    private fun refreshMap() {
        for(circle in mapCircles) {
            circle.remove()
        }

        val area = fileHandler.getAreasFromFile(mContext)
        for (areaPoint in area.areas.keys) {
            val location = Utils.stringToLocation(areaPoint)
            var radius = fileHandler.getMaxDistanceBetweenKeyAndLocation(mContext, areaPoint)
            if (radius < 20) {
                radius = (20).toFloat()
            }
            val circle = map.addCircle(CircleOptions()
                .center(LatLng(location.latitude, location.longitude))
                .radius(radius.toDouble())
                .strokeColor(Color.parseColor("#2271cce7"))
                .fillColor(0x79a402fc)
            )
            mapCircles.add(circle)
        }
    }

    private fun preventAddingInvalidData() {
        try{
            fileHandler.getAreasFromFile(mContext)
        } catch (error: JsonSyntaxException) {
            clearFilesNoView()
        }
    }
}
