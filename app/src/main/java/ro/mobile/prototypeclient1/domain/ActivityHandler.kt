package ro.mobile.prototypeclient1.domain

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.location.DetectedActivity
import ro.mobile.prototypeclient1.common.Constants
import ro.mobile.prototypeclient1.common.Utils
import java.util.ArrayList

class ActivityHandler(var isDriving: Boolean, var isWalking: Boolean) {

    private lateinit var mAdapter: DetectedActivitiesAdapter

    fun updateDetectedActivitiesList(
        defaultSharedPreferences: SharedPreferences,
        context: Context
    ): HashMap<String, Any> {
        var callDetermineLocation = false
        val detectedActivities: ArrayList<DetectedActivity?>? =
            Utils.detectedActivitiesFromJson(
                defaultSharedPreferences.getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
            )

        if (detectedActivities?.get(0)!!.type == DetectedActivity.IN_VEHICLE) {
            isDriving = true;
            isWalking = false;
        } else if (detectedActivities[0]!!.type == DetectedActivity.ON_FOOT || detectedActivities[0]!!.type == DetectedActivity.WALKING) {
            if (isDriving == true) {
                callDetermineLocation = true
            }
            isDriving = false;
            isWalking = true;
        }

        var response: HashMap<String, Any> = HashMap()
        response.put("detectedActivities", detectedActivities)
        response.put("callDetermineLocation", callDetermineLocation)

        return response
    }
}