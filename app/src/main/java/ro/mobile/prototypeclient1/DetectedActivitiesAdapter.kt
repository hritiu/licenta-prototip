package ro.mobile.prototypeclient1

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.android.gms.location.DetectedActivity

internal class DetectedActivitiesAdapter(
    context: Context?,
    detectedActivities: ArrayList<DetectedActivity?>?
) : ArrayAdapter<DetectedActivity?>(context!!, 0, detectedActivities!!) {

    lateinit var viewVariable: View //might not work

    @Override
    @NonNull
    override fun getView(position: Int, @Nullable view: View?, parent: ViewGroup): View {
//        viewVariable = view!!
        var detectedActivity: DetectedActivity = getItem(position)!!
        if (view == null) {
            viewVariable = LayoutInflater.from(context).inflate(
                R.layout.detected_activity, parent, false
            )
        } else {
            viewVariable = view
        }

        var activityName: TextView = viewVariable.findViewById(R.id.detected_activity_name)
        var activityConfidenceLevel: TextView =
            viewVariable.findViewById(R.id.detected_activity_confidence_level)

        if (detectedActivity != null) {
            activityName.text = Utils.getActivityString(context, detectedActivity.type)
            activityConfidenceLevel.text =
                context.getString(R.string.percent, detectedActivity.confidence)
        }

        return viewVariable
    }

    fun updateActivities(detectedActivities: java.util.ArrayList<DetectedActivity>) {
        var detectedActivitiesMap: HashMap<Int, Int> = HashMap()
        for (activity: DetectedActivity? in detectedActivities) {
            detectedActivitiesMap[activity!!.type] =
                activity.confidence //might not work and should be changed to put
        }

        var tempList: ArrayList<DetectedActivity> = ArrayList()
        for (i in 0..Constants.MONITORED_ACTIVITIES.size - 1) {
            var confidence: Int?
            if (detectedActivitiesMap.containsKey(Constants.MONITORED_ACTIVITIES[i])) {
                confidence = detectedActivitiesMap[Constants.MONITORED_ACTIVITIES[i]]
            } else {
                confidence = 0
            }

            val detectedActivity: DetectedActivity =
                DetectedActivity(Constants.MONITORED_ACTIVITIES[i], confidence!!)
            tempList.add(detectedActivity)
        }

        var sortedList = tempList.sortedWith(compareByDescending { it.confidence })

        this.clear()

        for (detectedActivity: DetectedActivity in sortedList) {
            if (detectedActivity.confidence > 20) {
                this.add(detectedActivity)
            }
        }
    }
}