package ro.mobile.prototypeclient1.domain

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import android.view.View
import com.google.gson.Gson
import ro.mobile.prototypeclient1.common.Constants
import java.io.*
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

class FileHandler {

    private var locationHandler =
        LocationHandler()

    //adds a given location to the json file that stores the locations
    fun addLocationToFile(location: Location, mContext: Context) {
        val locationsFile: File = File(mContext.filesDir.path, Constants.FILE_LOCATION)
        val gson = Gson()
        lateinit var area: Area

        if (locationsFile.exists()) {
            var jsonString = this.readJsonFromFile(locationsFile)
            area = gson.fromJson(jsonString, Area::class.java)
        } else {
            locationsFile.createNewFile()
            var areaStructure: HashMap<String, ArrayList<String>> = HashMap()
            area = Area(areaStructure)
        }

        if(area.areas == null) {
            area.areas = HashMap<String, ArrayList<String>>()
        }

        //add location in the area
        locationHandler.addLocationToParkingAreas(location, area)

        //write the area into the file
        val jsonStringToWrite = gson.toJson(area)
        this.writeJsonToFile(jsonStringToWrite, locationsFile)
    }

    fun writeLogToFile(log: String, file: File) {
        var string = this.readLogFile(file)

        if(string != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val date = LocalDateTime.now()
                string += "$date\n"
            } else {
                string += "invalid sdk version\n"
            }
            string += "$log"

            val output: Writer = BufferedWriter(FileWriter(file))
//            output.write("")
            output.write(string)
            output.close()
        }
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
        //uncomment in order to erase the content from the file
//        output.write(jsonString)
        output.write("{}")
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
}