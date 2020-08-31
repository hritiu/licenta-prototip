package ro.mobile.prototypeclient1.domain

import android.content.Context
import android.location.Location
import ro.mobile.prototypeclient1.common.Utils

class ParkingDetection(private val context: Context) {

    private var fileHandler = FileHandler()
    private var activityLog: ActivityLog

    init {
        activityLog = fileHandler.getActivityLogFromFile(context)
    }

    fun detectParkingLocation(): Location? {
        var location: Location? = null
        val activityLog = fileHandler.getActivityLogFromFile(context)
        var resultPosition = -1

        var minimum = 101
        if (activityLog.pairs.size != 0) {
            for (position in activityLog.pairs.size - 1 downTo 0) {
                val pair = activityLog.pairs[position]
                if (pair.first.toInt() != -1) {
                    if (checkTripletsBefore(position) && pair.first.toInt() < 10) {
                        minimum = pair.first.toInt()
                        resultPosition = position
                        break
                    } else if (pair.first.toInt() <= minimum) {
                        minimum = pair.first.toInt()
                        resultPosition = position

                        if (position > 0 && activityLog.pairs[position - 1].first.toInt() != pair.first.toInt() && (minimum == 2 || minimum == 1)) {
                            break
                        }
                    }

                    if (minimum == 1) {
                        break
                    }
                }
            }

            if (resultPosition != -1) {
                location = Utils.stringToLocation(activityLog.pairs[resultPosition].second)
            }
        }

        return location
    }

    private fun checkTripletsBefore(position: Int): Boolean {
        if (position < 2) {
            return false
        } else if (activityLog.pairs[position - 1].first != activityLog.pairs[position].first ||
            activityLog.pairs[position - 2].first != activityLog.pairs[position].first) {
            return false
        }

        return true
    }
}