package com.hw20

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(API_KEY)

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = "Notification channel"
        val descriptionText = "Simple description"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val API_KEY = "7bf47b1d-2db3-4cf4-81ea-f88687636eea"
        const val NOTIFICATION_CHANNEL_ID = "CHANNEL_ID"
    }
}


