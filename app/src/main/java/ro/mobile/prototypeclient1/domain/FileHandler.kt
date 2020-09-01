package ro.mobile.prototypeclient1.domain

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import ro.mobile.prototypeclient1.common.Constants
import ro.mobile.prototypeclient1.common.Utils
import java.io.*
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

class FileHandler() {

    private var locationHandler = LocationHandler()
    private val gson = Gson()

    //adds a given location to the json file that stores the locations
    fun addLocationToFile(location: Location?, mContext: Context) {
        val locationsFile = File(mContext.filesDir.path, Constants.FILE_LOCATION)

        val area = getAreasFromFile(mContext)
        //add location in the area
        locationHandler.addLocationToParkingAreas(location, area)

        //write the area into the file
        val jsonStringToWrite = gson.toJson(area)
        this.writeJsonToFile(jsonStringToWrite, locationsFile)
    }

    fun writeLogToFile(confidence: Int, location: Location?, mContext: Context) {
        val logFile = File(mContext.filesDir.path, Constants.LOG_FILE_LOCATION)
        val activityLog = getActivityLogFromFile(mContext)
        activityLog.pairs.add(Pair(confidence.toString(), Utils.locationToString(location!!)))

        //write the area into the file
        val jsonStringToWrite = gson.toJson(activityLog)
        this.writeJsonToFile(jsonStringToWrite, logFile)
        Log.v("BUBA", "Location written to log file confidence = $confidence  location = ${Utils.locationToString(location)}")
    }

    fun writeExtraLog(log: String, location: Location?, mContext: Context) {
        val logFile = File(mContext.filesDir.path, Constants.EXTRA_LOG_FILE_LOCATION)

        var string = this.readLogFile(logFile)

        if (string != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val date = LocalDateTime.now()
                string += "$date\n"
            } else {
                string += "invalid sdk version\n"
            }
            string += "$log "

            if(location != null) {
                string += " ${Utils.locationToString(location)}"
            } else {
                string += " Could not determine location"
            }
            string += "\n"

            val output: Writer = BufferedWriter(FileWriter(logFile))
            output.write(string)
            output.close()
        }
    }

    fun getActivityLogFromFile(mContext: Context): ActivityLog {
        lateinit var activityLog: ActivityLog
        val logFile = File(mContext.filesDir.path, Constants.LOG_FILE_LOCATION)

        if (logFile.exists()) {
            var jsonString = this.readJsonFromFile(logFile)
            if(jsonString == "") {
                jsonString = "{}"
            }
            activityLog = gson.fromJson(jsonString, ActivityLog::class.java)
        } else {
            logFile.createNewFile()
            val logStructure: ArrayList<Pair<String, String>> = ArrayList()
            activityLog = ActivityLog(logStructure)
        }

        if (activityLog.pairs == null) {
            activityLog.pairs = ArrayList<Pair<String, String>>()
        }

        return activityLog
    }

    fun getAreasFromFile(mContext: Context): Area {
        lateinit var area: Area
        val locationsFile = File(mContext.filesDir.path, Constants.FILE_LOCATION)

        if (locationsFile.exists()) {
            val jsonString = this.readJsonFromFile(locationsFile)
            area = gson.fromJson(jsonString, Area::class.java)
        } else {
            locationsFile.createNewFile()
            val areaStructure: HashMap<String, ArrayList<Pair<String, Int>>> = HashMap()
            area = Area(areaStructure)
        }

        if (area.areas == null) {
            area.areas = HashMap<String, ArrayList<Pair<String, Int>>>()
        }

        return area
    }

    private fun readLogFile(file: File): String? {
        val log: String

        try{
            log = file.readText()
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            Log.v("BUBA_JSON", "It seems that we have an exception")
            return null
        }

        return log
    }

    private fun writeJsonToFile(jsonString: String, file: File) {
        val output: Writer = BufferedWriter(FileWriter(file))
        output.write(jsonString)
        output.close()
    }

    private fun readJsonFromFile(file: File): String? {
        val jsonString: String

        try {
            jsonString = file.readText()
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            Log.v("BUBA_JSON", "It seems that we have an exception")
            return null
        }

        return jsonString
    }

    fun clearFiles(mContext: Context) {
        val locationsFile = File(mContext.filesDir.path, Constants.FILE_LOCATION)
        var output: Writer = BufferedWriter(FileWriter(locationsFile))
        output.write("{}")
        output.close()

        val logFile = File(mContext.filesDir.path, Constants.LOG_FILE_LOCATION)
        output = BufferedWriter(FileWriter(logFile))
        output.write("{}")
        output.close()

        val logExtraFile = File(mContext.filesDir.path, Constants.EXTRA_LOG_FILE_LOCATION)
        output = BufferedWriter(FileWriter(logExtraFile))
        output.write("")
        output.close()
    }

    fun clearLogFiles(mContext: Context) {
        val logFile = File(mContext.filesDir.path, Constants.LOG_FILE_LOCATION)
        var output = BufferedWriter(FileWriter(logFile))
        output.write("{}")
        output.close()
    }

    fun checkIfLocationIsNewAreaPoint(location: Location, context: Context): Boolean {
        val area = getAreasFromFile(context)

        if(area.areas.containsKey(Utils.locationToString(location))) {
            return false
        } else {
            for(areaPoint in area.areas.keys) {
                if(Utils.determineDistance(Utils.stringToLocation(areaPoint), location) <= 50) {
                    return false
                }
            }
        }

        return true
    }

    fun getMaxDistanceBetweenKeyAndLocation(mContext: Context, key: String): Float {
        lateinit var area: Area
        val locationsFile = File(mContext.filesDir.path, Constants.FILE_LOCATION)
        var maxDistance: Float = (-1).toFloat()

        if (locationsFile.exists()) {
            val jsonString = this.readJsonFromFile(locationsFile)
            area = gson.fromJson(jsonString, Area::class.java)
            if(area.areas != null && area.areas.containsKey(key)) {
                val values = area.areas.get(key)

                for(coordinate in values!!) {
                    val distance = Utils.determineDistance(Utils.stringToLocation(coordinate.first), Utils.stringToLocation(key))
                    if(distance > maxDistance) {
                        maxDistance = distance
                    }
                }
            }
        }

        return maxDistance
    }
}