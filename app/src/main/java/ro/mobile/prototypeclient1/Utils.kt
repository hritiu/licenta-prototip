package ro.mobile.prototypeclient1

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Utils {

    fun getActivityString(context: Context, detectedActivityType: Int): String {
        var resources: Resources = context.resources

        when(detectedActivityType){
            DetectedActivity.IN_VEHICLE -> return "In a vehicle"
            DetectedActivity.ON_BICYCLE -> return "On a bicycle"
            DetectedActivity.ON_FOOT -> return "On foot"
            DetectedActivity.RUNNING -> return "Running"
            DetectedActivity.STILL -> return "Still"
            DetectedActivity.TILTING -> return "Tilting"
            DetectedActivity.UNKNOWN -> return "Unknown activity"
            DetectedActivity.WALKING -> return "Walking"
            else -> return resources.getString(R.string.unidentifiable_activity, detectedActivityType)
        }
    }

    fun detectedActivitiesFromJson(jsonArray: String): ArrayList<DetectedActivity?>? {
//        Log.v("BUBA", "Utils.detectedActivitiesFromJson: The number of the detected activities is = ${jsonArray.length} ")
//        Log.v("BUBA", "jsonArray = ${jsonArray}")
        val listType = object : TypeToken<ArrayList<DetectedActivity?>?>(){}.type
        var detectedActivities = Gson().fromJson<java.util.ArrayList<DetectedActivity?>>(jsonArray, listType)
        if(detectedActivities == null) {
            detectedActivities = ArrayList()
        }

        return detectedActivities
    }

    fun detectedActivitiesToJson(detectedActivitiesList: ArrayList<DetectedActivity>): String {
        val type = object : TypeToken<java.util.ArrayList<DetectedActivity?>?>() {}.type

        return Gson().toJson(detectedActivitiesList, type)
    }
}