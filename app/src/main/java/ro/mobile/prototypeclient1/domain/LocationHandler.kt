package ro.mobile.prototypeclient1.domain

import android.location.Location
import ro.mobile.prototypeclient1.common.Utils
import java.util.ArrayList

class LocationHandler {

    //adds a parking location to an area of parking locations
    fun addLocationToParkingAreas(location: Location?, parkingArea: Area) {
        var found = false
        for (coordinate in parkingArea.areas.keys) {
            if (Utils.determineDistance(
                    Utils.stringToLocation(coordinate), location!!
                ) <= 50
            ) {
                parkingArea.areas.get(coordinate)?.add(Utils.locationToString(location))
                found = true
            }
        }

        if (!found) {
            parkingArea.areas.put(Utils.locationToString(location!!), ArrayList<String>())
        }
    }
}