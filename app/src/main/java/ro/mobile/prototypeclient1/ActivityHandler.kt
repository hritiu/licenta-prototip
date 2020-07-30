package ro.mobile.prototypeclient1

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.location.DetectedActivity
import java.util.ArrayList

class ActivityHandler(var isDriving: Boolean, var isWalking: Boolean) {

    private lateinit var mAdapter: DetectedActivitiesAdapter

    fun updateDetectedActivitiesList(
        defaultSharedPreferences: SharedPreferences,
        context: Context
//    ): ArrayList<DetectedActivity?>? {
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

//        if(callDetermineLocation) {
//            return detectedActivities
//        } else {
//            return null
//        }

        var response: HashMap<String, Any> = HashMap()
        response.put("detectedActivities", detectedActivities)
        response.put("callDetermineLocation", callDetermineLocation)

        return response

//        return detectedActivities
    }
}