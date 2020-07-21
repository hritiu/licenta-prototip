package ro.mobile.prototypeclient1

//{"WALKING": {10: "lat,lon", 25: "lat,lon", 51: "lat,lon"}}
data class ActivityLog(var activityType: String, var accuracyLog: HashMap<Int, String>) {

    fun addActivity(accuracy: Int, coordinate: String) {
        accuracyLog.put(accuracy, coordinate)
    }

    fun isAccuracyAscending(): Boolean {
        var ascendingPairs = 0
        var descendingPairs = 0
        var accuracies = accuracyLog.values

        for(position in 1..accuracies.size) {
            if(accuracies.toTypedArray()[position] >= accuracies.toTypedArray()[position + 1]) {
                ascendingPairs += 1
            } else {
                descendingPairs += 1
            }
        }

        return ascendingPairs >= descendingPairs
    }
}