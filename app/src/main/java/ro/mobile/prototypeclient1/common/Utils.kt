package ro.mobile.prototypeclient1.common

import android.content.Context
import android.content.res.Resources
import android.location.Location
import com.google.android.gms.location.DetectedActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ro.mobile.prototypeclient1.R

object Utils {

    fun getActivityString(context: Context, detectedActivityType: Int): String {
        var resources: Resources = context.resources

        when(detectedActivityType){
            DetectedActivity.IN_VEHICLE -> return "In a vehicle"
            DetectedActivity.ON_BICYCLE -> return "On a bicycle"
//            DetectedActivity.ON_FOOT -> return "On foot"
            DetectedActivity.RUNNING -> return "Running"
            DetectedActivity.STILL -> return "Still"
            DetectedActivity.TILTING -> return "Tilting"
            DetectedActivity.UNKNOWN -> return "Unknown activity"
//            DetectedActivity.WALKING -> return "Walking"
            DetectedActivity.WALKING -> return "W A L K I N G"
            else -> return resources.getString(R.string.unidentifiable_activity, detectedActivityType)
        }
    }

    fun activityTypeToString(activityType: Int): String {
        when(activityType) {
            DetectedActivity.IN_VEHICLE -> return "IN_VEHICLE"
            DetectedActivity.ON_BICYCLE -> return "ON_BICYCLE"
            DetectedActivity.RUNNING -> return "RUNNING"
            DetectedActivity.STILL -> return "STILL"
            DetectedActivity.TILTING -> return "TILTING"
            DetectedActivity.UNKNOWN -> return "UNKNOWN"
            DetectedActivity.WALKING -> return "WALKING"
            DetectedActivity.ON_FOOT -> return "ON_FOOT"
            else -> return "UNIDENTIFIABLE ACTIVITY"
        }
    }

    fun detectedActivitiesFromJson(jsonArray: String): ArrayList<DetectedActivity?>? {
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

    //computes the distance between 2 coordinates
    fun determineDistance(locationA: Location, locationB: Location): Float {
        return locationA.distanceTo(locationB)
    }

    //converts location to string
    fun locationToString(location: Location): String {
        return location.latitude.toString() + "," + location.longitude.toString()
    }

    //converts string to location
    fun stringToLocation(stringLocation: String): Location {
        var location: Location = Location(stringLocation)

        val strings = stringLocation.split(",").toTypedArray()
        location.latitude = strings[0].toDouble()
        location.longitude = strings[1].toDouble()

        return location
    }
}