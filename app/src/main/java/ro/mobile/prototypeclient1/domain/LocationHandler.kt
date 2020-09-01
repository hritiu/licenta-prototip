package ro.mobile.prototypeclient1.domain

import android.location.Location
import ro.mobile.prototypeclient1.common.Utils
import java.util.ArrayList

class LocationHandler {

    //adds a parking location to an area of parking locations
    fun addLocationToParkingAreas(location: Location?, parkingArea: Area) {
        var found = false
        for (key in parkingArea.areas.keys) {
            val distance = Utils.determineDistance(Utils.stringToLocation(key), location!!)
            if (distance <= 50) { //if distance is < 50, the coordinates are associated with the key

                //checks if there are 2 parking spots that overlap. If yes, the counter for the
                //existing one is incremented
                val incremented = incrementExistingLocationCount(key, location, parkingArea)
                if(!incremented) { //if no incrementation is performed, the location is added
                    parkingArea.areas.get(key)?.add(Pair(Utils.locationToString(location), 1))
                }

                found = true
                break
            }
        }

        if (!found) {
            parkingArea.areas.put(Utils.locationToString(location!!), ArrayList<Pair<String, Int>>())
        }
    }

    private fun incrementExistingLocationCount(key: String, location: Location?, parkingArea: Area): Boolean {
        var found = false
        lateinit var newCoordinateValue: Pair<String, Int>
        var valuePosition = -1
        val valuesList = parkingArea.areas.get(key)
            for (position in 0 until valuesList!!.size) {
                val coordinate = valuesList[position]
                if (Utils.determineDistance(Utils.stringToLocation(coordinate.first), location!!) <= 1.5) {
                    newCoordinateValue = Pair(coordinate.first, coordinate.second + 1)
                    valuePosition = position
                    found = true
                    break
                }
            }

            if(found) {
                valuesList.removeAt(valuePosition)
                valuesList.add(newCoordinateValue)
            }
        return found
    }
}