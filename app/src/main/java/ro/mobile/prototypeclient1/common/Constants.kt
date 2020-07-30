package ro.mobile.prototypeclient1.common

import com.google.android.gms.location.DetectedActivity

object Constants {
    val MONITORED_ACTIVITIES = intArrayOf(
        DetectedActivity.STILL,
        DetectedActivity.WALKING,
        DetectedActivity.RUNNING,
        DetectedActivity.ON_BICYCLE,
        DetectedActivity.IN_VEHICLE,
        DetectedActivity.TILTING,
        DetectedActivity.UNKNOWN
    )

    private const val PACKAGE_NAME : String = "com.google.android.gms.location.activityrecognition"

    const val KEY_DETECTED_ACTIVITIES : String = "$PACKAGE_NAME.DETECTED_ACTIVITIES"

    const val DETECTION_INTERVAL_IN_MILLISECONDS : Long = 10 //10 * 1000 // 10 seconds

    const val FILE_LOCATION ="Locations.json"

    const val TAG = "MainActivity"

    const val PERMISSION_ID = 42
}