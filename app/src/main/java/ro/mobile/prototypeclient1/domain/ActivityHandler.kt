package ro.mobile.prototypeclient1.domain

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.location.DetectedActivity
import ro.mobile.prototypeclient1.common.Constants
import ro.mobile.prototypeclient1.common.Utils
import java.util.ArrayList

class ActivityHandler() {

    private lateinit var mAdapter: DetectedActivitiesAdapter

    fun updateDetectedActivitiesList(
        defaultSharedPreferences: SharedPreferences,
        context: Context,
        isDriving: Boolean,
        isWalking: Boolean
    ): HashMap<String, Any> {

        var callDetermineLocation = false
        val detectedActivities: ArrayList<DetectedActivity?>? = Utils.detectedActivitiesFromJson(
            defaultSharedPreferences.getString(
                Constants.KEY_DETECTED_ACTIVITIES,
                ""
            )!!
        )

        var driving: Boolean? = null
        var walking: Boolean? = null
        if (detectedActivities?.get(0)!!.type == DetectedActivity.IN_VEHICLE) {
            driving = true
            walking = false
        } else if (detectedActivities[0]!!.type == DetectedActivity.ON_FOOT || detectedActivities[0]!!.type == DetectedActivity.WALKING) {
            if (isDriving) {
                callDetermineLocation = true
            }
            driving = false
            walking = true
        }

        val response: HashMap<String, Any> = HashMap()
        response["detectedActivities"] = detectedActivities
        response.put("callDetermineLocation", callDetermineLocation)
        if (driving == null) {
            response["isDriving"] = isDriving
        } else {
            response["isDriving"] = driving
        }
        if (walking == null) {
            response["isWalking"] = isWalking
        } else {
            response["isWalking"] = walking
        }

        return response
    }
}