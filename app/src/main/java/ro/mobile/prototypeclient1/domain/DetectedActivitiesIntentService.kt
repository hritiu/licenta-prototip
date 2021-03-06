package ro.mobile.prototypeclient1.domain

import android.app.IntentService
import android.content.Intent
import androidx.preference.PreferenceManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import ro.mobile.prototypeclient1.common.Constants
import ro.mobile.prototypeclient1.common.Utils
import java.util.*

class DetectedActivitiesIntentService : IntentService("DetectedActivitiesIS") {

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
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString(
                Constants.KEY_DETECTED_ACTIVITIES,
                Utils.detectedActivitiesToJson(detectedActivities)
            )
            .apply()
    }
}