package ro.mobile.prototypeclient1.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.widget.Toast
import ro.mobile.prototypeclient1.common.Constants

class ParkingDetectionNotification: Application() {

    @Override
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val parkingDetectionChannel = NotificationChannel(
                Constants.CHANNEL_1,
                "ParkingDetectionChannel",
                NotificationManager.IMPORTANCE_HIGH
            )
            parkingDetectionChannel.description = "This is a channel for parking detection notifications"

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(parkingDetectionChannel)
        }
    }
}