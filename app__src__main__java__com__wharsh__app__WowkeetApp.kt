package com.wharsh.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.google.firebase.database.FirebaseDatabase

class WowkeetApp : Application() {

    companion object {
        const val CHANNEL_ID = "wowkeet_private_chat_v2"
        const val CHANNEL_NAME = "Wowkeet Chat Alerts"
        var activeChatSellerId: String? = null
    }

    override fun onCreate() {
        super.onCreate()

        // Enable Firebase Offline Persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already enabled
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/raw/wowkeet_order_alert")

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for 1-to-1 chats and shop status"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}