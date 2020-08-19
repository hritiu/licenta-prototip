package ro.mobile.prototypeclient1.domain

import com.google.android.gms.location.DetectedActivity

data class ActivityLog(var pairs: ArrayList<Pair<String, String>>) {
//    private var activities: HashMap<String, ArrayList<String>> = HashMap()
//
//    init {
//        activities["STILL"] = ArrayList<String>()
//        activities["WALKING"] = ArrayList<String>()
//        activities["RUNNING"] = ArrayList<String>()
//        activities["ON_BICYCLE"] = ArrayList<String>()
//        activities["IN_VEHICLE"] = ArrayList<String>()
//        activities["TILTING"] = ArrayList<String>()
//        activities["UNKNOWN"] = ArrayList<String>()
//    }
//
//    private fun detectParking() {
//
//    }
//
//    private fun mapDetectedActivities(detectedActivities: ArrayList<DetectedActivity>) {
//        for(detectedActivity in detectedActivities) {
//            when(detectedActivity.type) {
//                DetectedActivity.STILL -> activities["STILL"]?.add(detectedActivity.confidence.toString())
//                DetectedActivity.WALKING -> activities["WALKING"]?.add(detectedActivity.confidence.toString())
//                DetectedActivity.RUNNING -> activities["RUNNING"]?.add(detectedActivity.confidence.toString())
//                DetectedActivity.ON_BICYCLE -> activities["ON_BICYCLE"]?.add(detectedActivity.confidence.toString())
//                DetectedActivity.IN_VEHICLE -> activities["IN_VEHICLE"]?.add(detectedActivity.confidence.toString())
//                DetectedActivity.TILTING -> activities["TILTING"]?.add(detectedActivity.confidence.toString())
//                DetectedActivity.UNKNOWN -> activities["UNKNOWN"]?.add(detectedActivity.confidence.toString())
//            }
//        }
//    }
//
//    private fun addActivity(activityName: String, activityConfidence: String) {
//        if(activities.containsKey(activityName)) {
//            activities[activityName]?.add(activityConfidence)
//        } else {
//            val arrayList: ArrayList<String> = ArrayList()
//            arrayList.add(activityConfidence)
//            activities.put(activityName, arrayList)
//        }
//    }
//
//    private fun clearActivitiesMap() {
//        for(activity in activities) {
//            activity.value.clear()
//        }
//    }
}