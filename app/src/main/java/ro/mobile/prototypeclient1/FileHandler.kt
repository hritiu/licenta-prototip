package ro.mobile.prototypeclient1

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import java.io.*
import java.util.ArrayList

class FileHandler {

    private var locationHandler = LocationHandler()

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
//        this.addLocationToParkingAreas(location, area)
        locationHandler.addLocationToParkingAreas(location, area)

        //write the area into the file
        val jsonStringToWrite = gson.toJson(area)
        this.writeJsonToFile(jsonStringToWrite, locationsFile)
    }

    private fun writeJsonToFile(jsonString: String, file: File) {
        val output: Writer = BufferedWriter(FileWriter(file))
//        output.write(jsonString)
        //uncomment in order to erase the content from the file
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