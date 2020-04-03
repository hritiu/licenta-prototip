package ro.mobile.prototypeclient1

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.preference.Preference
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.DetectedActivity
import java.util.ArrayList


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "MainActivity"
    private lateinit var mContext: Context
    private lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    private lateinit var mAdapter: DetectedActivitiesAdapter
    lateinit var mainHandler: Handler

    private val updateTextTask = object : Runnable {
        override fun run() {
            val task =
                mActivityRecognitionClient.requestActivityUpdates(
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    getActivityDetectionPendingIntent()
                )

            requestActivityUpdates()
            updateDetectedActivitiesList()
            mainHandler.postDelayed(this, 30000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this;
        val detectedActivitiesListView: ListView = findViewById(R.id.detected_activities_listview)

        val detectedActivities: ArrayList<DetectedActivity?>? = Utils.detectedActivitiesFromJson(
           this.getSharedPreferences("ro.mobile.prototypeclient1_preferences", 0).getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
        )

//        Utils.detectedActivitiesFromJson(
//            this.getSharedPreferences("ro.mobile.prototypeclient1_preferences", 0).getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
//        )


        mAdapter = DetectedActivitiesAdapter(this, detectedActivities)
        detectedActivitiesListView.adapter = mAdapter
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

    fun getActivityDetectionPendingIntent(): PendingIntent {
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
                mainHandler.postDelayed(this, 30000)
            }
        }

//        val task = mActivityRecognitionClient.requestActivityUpdates(
//            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
//            getActivityDetectionPendingIntent()
//        )

//        task.addOnSuccessListener {
//            Log.v("BUBA", "success")
//            Toast.makeText(
//                mContext,
//                getString(R.string.activity_updates_enabled),
//                Toast.LENGTH_SHORT
//            )
//                .show()
//
//            updateDetectedActivitiesList()
//        }
//
//        task.addOnFailureListener {
//            Log.v("BUBA", "fail")
//            Toast.makeText(
//                mContext,
//                getString(R.string.activity_updates_not_enabled),
//                Toast.LENGTH_SHORT
//            )
//                .show()
//        }
    }

    fun getUpdatesRequestedState(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_ACTIVITY_UPDATES_REQUESTED, false)
    }

    fun removeActivityUpdatesButtonHandler(view: View?) {
        val task =
            mActivityRecognitionClient.removeActivityUpdates(
                getActivityDetectionPendingIntent()
            )
        task.addOnSuccessListener {
            Toast.makeText(
                mContext,
                getString(R.string.activity_updates_removed),
                Toast.LENGTH_SHORT
            )
                .show()
            // Reset the display.
            mAdapter.updateActivities(java.util.ArrayList())
        }
        task.addOnFailureListener {
            Log.w(TAG, "Failed to enable activity recognition.")
            Toast.makeText(
                mContext, getString(R.string.activity_updates_not_removed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateDetectedActivitiesList() {
        val detectedActivities: ArrayList<DetectedActivity?>? =
            Utils.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString(Constants.KEY_DETECTED_ACTIVITIES, "")!!
            )
        Log.v(
            "BUBA",
            "MainActivity.updateDetectedActivitiesList(): The length of the detected activities is ${detectedActivities?.size}"
        )
        mAdapter.updateActivities(detectedActivities)
    }

    @Override
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.v("BUBA", "Hopa ca intra si aicia")
        if (key.equals(Constants.KEY_DETECTED_ACTIVITIES)) {
            updateDetectedActivitiesList()
        }
    }
}
