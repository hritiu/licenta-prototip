package ro.mobile.prototypeclient1

import android.app.IntentService
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import java.util.*

class DetectedActivitiesIntentService: IntentService("DetectedActivitiesIS") {

    val TAG: String = "DetectedActivitiesIS"

    @Override
    override fun onCreate() {
        super.onCreate()
    }

    @Override
    override fun onHandleIntent(intent: Intent?) {
        val result: ActivityRecognitionResult = ActivityRecognitionResult.extractResult(intent)

        val detectedActivities: ArrayList<DetectedActivity> =
            result.probableActivities as ArrayList<DetectedActivity>
        Log.v("BUBA", "onHandleIntent probableActivities.size = ${result.probableActivities.size}")
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString(Constants.KEY_DETECTED_ACTIVITIES,
                Utils.detectedActivitiesToJson(detectedActivities))
            .apply()

        Log.i(TAG, "activities detected")
        for (da in detectedActivities) {
            Log.i(
                TAG, Utils.getActivityString(
                    applicationContext,
                    da.type
                ) + " " + da.confidence + "%"
            )
        }
    }
}